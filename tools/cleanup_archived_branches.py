#!/usr/bin/env python3
"""One-time, SHA-pinned repository cleanup. Never touches app data or kept branches."""
import argparse
import json
import os
import subprocess
import urllib.error
import urllib.parse
import urllib.request

REPO = 'drkarimelshenawy/todo-shift-android'
BASELINE = '2bc80943c90215cecea0db9c037f59ee9915f3cb'
STABLE = 'premium/stable-candidate'
KEPT = {
    'main': '2e4c3e4919182c633b52644bd39a888097a14b98',
    'Luxury-v4': 'e7ca3ba8d9b72f1d2c570d24a334117e5543107a',
    'fix/neo-luxury-4.1-data-integrity': 'e4533af1c4dc2b80cd7bc61bce8c2bbcf83b94a4',
}
ARCHIVE = {
    'work/todo-shift-premium-rescue': ('ef4281b867472d753bf2163d82062c4562cd08e8', 'premium-rescue-archived'),
    'feat/neo-luxury-4.1-ultimate': ('e178b134658afad04d645b044f24d236594cbe33', 'neo-luxury-4.1-archive'),
    'Premium-V-byWork': ('7ad1f0658df968b213179a129e7facec05f11961', 'archive/Premium-V-byWork-2026-11-10'),
    'rc9-luxury-upgrade': ('520534c0e341ba24b425200c2edbeb55f879c007', 'archive/rc9-luxury-upgrade-2026-11-10'),
    'v0.9-development': ('e500c383dd3ca25fc7e5fa3dacfc92de105638ff', 'archive/v0.9-development-2026-11-10'),
    'v0.5-development': ('2ff68c2b4e4aec0c195e4c1864cc9db06b7187ec', 'archive/v0.5-development-2026-11-10'),
}


def api(method, path, payload=None):
    request = urllib.request.Request(
        f'https://api.github.com/repos/{REPO}/{path}'.rstrip('/'),
        data=None if payload is None else json.dumps(payload).encode(),
        method=method,
        headers={
            'Authorization': 'Bearer ' + os.environ['GH_TOKEN'],
            'Accept': 'application/vnd.github+json',
            'Content-Type': 'application/json',
            'X-GitHub-Api-Version': '2022-11-28',
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as failure:
        if failure.code == 404 and method == 'GET':
            return None
        raise RuntimeError(f'{method} {path}: HTTP {failure.code}') from None


def require(condition, reason):
    if not condition:
        raise RuntimeError('STOP: ' + reason)


def ref(kind, name):
    result = api('GET', f'git/ref/{kind}/{name}')
    return result['object']['sha'] if result else None


def all_pages(path, key=None):
    page = 1
    while True:
        data = api('GET', f'{path}{"&" if "?" in path else "?"}per_page=100&page={page}')
        require(data is not None, 'cannot inspect ' + path)
        rows = data[key] if key else data
        yield from rows
        if len(rows) < 100:
            break
        page += 1


def check_kept(expected_sha):
    require(os.environ.get('GITHUB_REPOSITORY') == REPO, 'repository mismatch')
    require(os.environ.get('GITHUB_REF') == 'refs/heads/' + STABLE, 'wrong execution branch')
    require(ref('heads', STABLE) == expected_sha, 'stable branch moved during maintenance')
    for branch, sha in KEPT.items():
        require(ref('heads', branch) == sha, 'kept branch changed: ' + branch)
    repository = api('GET', '')
    require(repository and repository['default_branch'] == 'main', 'unexpected default branch')


def check_dependencies():
    for pr in all_pages('pulls?state=open'):
        require(pr['head']['ref'] not in ARCHIVE and pr['base']['ref'] not in ARCHIVE,
                'open PR depends on a deletion candidate: #' + str(pr['number']))
    active = {'queued', 'in_progress', 'waiting', 'requested', 'pending'}
    # Each branch is checked independently; completed historical runs do not block cleanup.
    for branch in ARCHIVE:
        encoded = urllib.parse.quote(branch, safe='')
        for run in all_pages('actions/runs?branch=' + encoded, 'workflow_runs'):
            require(run['status'] not in active, 'active workflow on ' + branch)


def run(mode, expected_sha):
    check_kept(expected_sha)
    check_dependencies()
    tags = {'premium-stable-candidate': BASELINE}
    tags.update({tag: sha for sha, tag in ARCHIVE.values()})
    # Validate every candidate and every existing tag before the first write.
    for branch, (sha, tag) in ARCHIVE.items():
        current = ref('heads', branch)
        require(current in (None, sha), 'candidate moved: ' + branch)
        if current is None:
            require(ref('tags', tag) == sha, 'missing branch has no verified archive: ' + branch)
    for tag, sha in tags.items():
        require(ref('tags', tag) in (None, sha), 'tag already points elsewhere: ' + tag)
    if mode == 'archive':
        for tag, sha in tags.items():
            if ref('tags', tag) is None:
                api('POST', 'git/refs', {'ref': 'refs/tags/' + tag, 'sha': sha})
    for tag, sha in tags.items():
        require(ref('tags', tag) == sha, 'archive not verified: ' + tag)
    print('All seven archive tags verified; original Premium source preserved at ' + BASELINE)
    if mode == 'delete':
        for branch, (sha, tag) in ARCHIVE.items():
            check_kept(expected_sha)
            if ref('heads', branch) is None:
                print('Already absent: ' + branch)
                continue
            require(ref('tags', tag) == sha, 'archive mismatch before deletion: ' + branch)
            # A lease makes deletion conditional on the exact reviewed remote SHA.
            # This does not rewrite any branch history and never addresses a kept branch.
            subprocess.run([
                'git', 'push', '--force-with-lease=refs/heads/' + branch + ':' + sha,
                'origin', ':refs/heads/' + branch,
            ], check=True)
            require(ref('heads', branch) is None, 'deletion not confirmed: ' + branch)
            print('Archived then deleted: ' + branch)
        branches = {b['name'] for b in all_pages('branches')}
        require(branches == set(KEPT) | {STABLE}, 'unexpected final branch inventory')
        print('Final inventory: ' + ', '.join(sorted(branches)))
    check_kept(expected_sha)
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as output:
            output.write('## ' + mode + '\n\n')
            output.write('Verified tags:\n\n| Tag | SHA |\n|---|---|\n')
            for tag, sha in tags.items():
                output.write(f'| `{tag}` | `{sha}` |\n')
            output.write('\nKept branches verified; app source unchanged from 2bc8094.\n')


if __name__ == '__main__':
    import urllib.parse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('archive', 'delete'))
    parser.add_argument('--expected-sha', required=True)
    args = parser.parse_args()
    subprocess.run(['git', 'diff', '--exit-code', BASELINE, 'HEAD', '--', 'app'], check=True)
    run(args.mode, args.expected_sha)

#!/usr/bin/env python3
"""Verify a candidate's actual APK identity/signature; never asserts installed compatibility."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def run(*args):
    return subprocess.check_output([str(x) for x in args], text=True, stderr=subprocess.STDOUT)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--build-tools', type=Path, required=True)
    parser.add_argument('--expected-certificate', required=True, help='Public SHA-256 fingerprint')
    parser.add_argument('--expected-sha', required=True)
    parser.add_argument('--expected-branch', help='Explicit reviewed build branch; defaults to candidate target branch')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    expected = json.loads((ROOT / 'tools/PREMIUM_CANDIDATE.json').read_text())
    sha = run('git', '-C', ROOT, 'rev-parse', 'HEAD').strip()
    branch = run('git', '-C', ROOT, 'branch', '--show-current').strip()
    if run('git', '-C', ROOT, 'status', '--porcelain').strip():
        raise SystemExit('STOP: dirty source tree cannot identify an exact committed candidate')
    if sha != args.expected_sha or branch != (args.expected_branch or expected['branch']):
        raise SystemExit('STOP: candidate source commit or branch mismatch')
    if not re.fullmatch('[a-fA-F0-9]{64}', args.expected_certificate):
        raise SystemExit('STOP: invalid expected certificate fingerprint')
    badging = run(args.build_tools / 'aapt', 'dump', 'badging', args.apk)
    match = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
    actual = dict(zip(('applicationId', 'versionCode', 'versionName'), match.groups())) if match else {}
    if actual != {k: str(expected[k]) for k in actual} or len(actual) != 3:
        raise SystemExit('STOP: APK package or version does not match candidate definition')
    signature = run(args.build_tools / 'apksigner', 'verify', '--verbose', '--print-certs', args.apk)
    certs = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})', signature)
    if len(certs) != 1 or certs[0].lower() != args.expected_certificate.lower():
        raise SystemExit('STOP: APK signing certificate mismatch')
    alignment = run(args.build_tools / 'zipalign', '-c', '-v', '4', args.apk)
    args.output.mkdir(parents=True, exist_ok=True)
    result = dict(actual, sourceCommit=sha, branch=branch,
                  certificateSHA256=certs[0].lower(), apkSHA256=hashlib.sha256(args.apk.read_bytes()).hexdigest(),
                  apkBytes=args.apk.stat().st_size, signatureVerification='PASS', zipalign='PASS',
                  installedCertificate='NOT VERIFIED', releaseGate='HOLD',
                  classification=expected['classification'])
    for name, content in [('APK-BADGING.txt', badging), ('APK-SIGNATURE.txt', signature), ('ZIPALIGN.txt', alignment)]:
        (args.output / name).write_text(content)
    (args.output / 'BUILD-IDENTITY.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()

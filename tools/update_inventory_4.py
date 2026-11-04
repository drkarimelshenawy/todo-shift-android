#!/usr/bin/env python3
"""Update source-change inventory while retaining the immutable attached 3.0 baseline hashes."""
from pathlib import Path
import hashlib
import json
ROOT = Path(__file__).resolve().parents[1]
path = ROOT / 'tools/BASELINE_INTEGRITY_4.0.json'
manifest = json.loads(path.read_text())
base = manifest['original_files']
manifest['unchanged_files'] = {p: h for p, h in base.items() if (ROOT / p).is_file() and hashlib.sha256((ROOT / p).read_bytes()).hexdigest() == h}
manifest['modified_original_files'] = [p for p in base if p not in manifest['unchanged_files'] and (ROOT / p).is_file()]
path.write_text(json.dumps(manifest, indent=2) + '\n')
skip = {'build', '.gradle', '.git', '__pycache__', '.idea', '.kotlin', 'node_modules'}
files = [p.relative_to(ROOT).as_posix() for p in ROOT.rglob('*') if p.is_file() and not any(s in skip for s in p.relative_to(ROOT).parts)]
result = {'baseline_files': len(base), 'retained_baseline_files': sum((ROOT / p).is_file() for p in base),
          'unchanged_count': len(manifest['unchanged_files']), 'modified_count': len(manifest['modified_original_files']),
          'new_count': len([p for p in files if p not in base]), 'modified': manifest['modified_original_files'],
          'new': sorted(p for p in files if p not in base), 'critical_unchanged': manifest['critical_unchanged_files']}
(ROOT / 'FILES_CHANGED_4.0.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps({k: v for k, v in result.items() if k.endswith('count') or k.endswith('files')}, indent=2))

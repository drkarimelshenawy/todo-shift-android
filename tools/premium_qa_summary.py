#!/usr/bin/env python3
"""Summarize retained JUnit/lint evidence without replacing the underlying reports."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path('delivery/qa')
suites = [ET.parse(p).getroot() for p in (root / 'junit').glob('TEST-*.xml')]
result = {key: sum(int(s.get(key, '0')) for s in suites)
          for key in ('tests', 'failures', 'errors', 'skipped')}
result['suites'] = len(suites)
result['failedCases'] = [s.get('name', '') + '.' + t.get('name', '')
                         for s in suites for t in s.findall('testcase')
                         if t.find('failure') is not None or t.find('error') is not None]
lint = root / 'lint-results-debug.xml'
result['lint'] = {}
if lint.exists():
    for issue in ET.parse(lint).getroot().findall('issue'):
        severity = issue.get('severity', 'Unknown')
        result['lint'][severity] = result['lint'].get(severity, 0) + 1
root.mkdir(parents=True, exist_ok=True)
(root / 'SUMMARY.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))

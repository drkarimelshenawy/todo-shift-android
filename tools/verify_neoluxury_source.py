#!/usr/bin/env python3
"""Offline static guards only: NOT Kotlin compilation or Android runtime testing."""
from pathlib import Path
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
from kotlin_lexical_guard import scan

ROOT = Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT / 'tools/BASELINE_INTEGRITY.json').read_text())
failures = []
def require(condition, message):
    if not condition:
        failures.append(message)

for relative, digest in manifest['unchanged_files'].items():
    path = ROOT / relative
    require(path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest() == digest, 'Baseline changed: ' + relative)
print(f"CHECK: {len(manifest['unchanged_files'])} unchanged baseline files")

kt_files = list(ROOT.rglob('*.kt'))
for path in kt_files:
    text = path.read_text()
    for position, message in scan(text)[1]:
        failures.append(f'{path.relative_to(ROOT)}:{text.count(chr(10), 0, position) + 1}: {message}')
print(f'CHECK: strings, comments, templates and delimiters in {len(kt_files)} Kotlin files (lexical only)')

xml_files = list(ROOT.rglob('*.xml'))
for path in xml_files:
    try:
        tree = ET.parse(path)
        if tree.getroot().tag == 'resources':
            names = [(el.tag, el.attrib.get('name')) for el in tree.getroot() if 'name' in el.attrib]
            require(len(names) == len(set(names)), f'Duplicate resource definition: {path}')
    except ET.ParseError as error:
        failures.append(f'XML error {path}: {error}')
print(f'CHECK: {len(xml_files)} XML files and duplicate definitions')

resources = {kind: set() for kind in ['string', 'drawable', 'layout', 'xml', 'color', 'mipmap']}
for path in (ROOT / 'app/src/main/res').rglob('*'):
    if not path.is_file():
        continue
    kind = path.parent.name.split('-')[0]
    if kind in resources:
        resources[kind].add(path.stem)
    if kind == 'values' and path.suffix == '.xml':
        for element in ET.parse(path).getroot():
            if element.tag in resources and 'name' in element.attrib:
                resources[element.tag].add(element.attrib['name'])
for path in kt_files:
    for kind, name in re.findall(r'(?<!android\.)\bR\.(string|drawable|layout|xml|color|mipmap)\.(\w+)', path.read_text()):
        require(name in resources[kind], f'Missing R.{kind}.{name} in {path.name}')
print('CHECK: local string/drawable/layout/xml/color/mipmap references')

v11 = ROOT / 'app/src/main/java/com/todoshift/app/v11'
model = (v11 / 'NeoAppearance.kt').read_text().split('data class NeoAppearance(', 1)[1].split(') {', 1)[0]
fields = re.findall(r'val (\w+):', model)
store = (v11 / 'NeoAppearanceStore.kt').read_text()
load = store.split('fun load()', 1)[1].split('fun save(', 1)[0]
save = store.split('fun save(', 1)[1]
for field in fields:
    require(f'"{field}"' in load and f'"{field}"' in save, 'Missing appearance persistence key: ' + field)
print(f'CHECK: read/write paths for {len(fields)} appearance settings (not a process-restart test)')
require('applicationId = "com.todoshift.app"' in (ROOT / 'app/build.gradle.kts').read_text(), 'Application ID changed')
require('if (experience == V11Experience.CLASSIC)' in (v11 / 'ui/V11Theme.kt').read_text(), 'Classic theme branch missing')
require('if (!showDayMoney) "••••••"' in (v11 / 'ui/V11DayDetailsUi.kt').read_text(), 'Day-detail amount guard missing')
require('p.showFinanceChart && visible' in (v11 / 'ui/NeoSections.kt').read_text(), 'Hidden finance chart guard missing')
for path in ROOT.rglob('*'):
    if path.is_file():
        require(path.suffix.lower() not in {'.jks', '.keystore', '.p12', '.pfx', '.apk', '.aab', '.class', '.jar'}, 'Excluded artifact: ' + str(path))
print('CHECK: identity, Classic branch, privacy guards and excluded artifacts')
if failures:
    print('\n'.join('FAIL: ' + failure for failure in failures))
    sys.exit(1)
print('PASS: static source guards. Kotlin compilation, rendering, persistence and runtime behavior remain unverified.')

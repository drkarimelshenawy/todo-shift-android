#!/usr/bin/env python3
"""Offline static checks. This script does NOT compile Kotlin or run Android UI/restore tests."""
from pathlib import Path
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
from html.parser import HTMLParser
from kotlin_lexical_guard import scan

ROOT = Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT / 'tools/BASELINE_INTEGRITY_4.0.json').read_text())
failures = []
checks = []
def require(condition, message):
    if not condition:
        failures.append(message)
def checked(name, detail):
    checks.append({'check': name, 'result': 'PASS' if not failures else 'SEE_FAILURES', 'detail': detail})

for name in manifest['original_files']:
    require((ROOT / name).is_file(), 'Missing baseline file: ' + name)
for name in manifest['critical_unchanged_files']:
    require(hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == manifest['original_files'][name], 'Core changed: ' + name)
for name, sha in manifest['unchanged_files'].items():
    require(hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == sha, 'Declared unchanged file changed: ' + name)
checked('baseline', f"All {len(manifest['original_files'])} baseline files retained; {len(manifest['critical_unchanged_files'])} core files unchanged by SHA-256")

kt_files = list(ROOT.rglob('*.kt'))
for path in kt_files:
    text = path.read_text()
    for position, message in scan(text)[1]:
        failures.append(f'{path.relative_to(ROOT)}:{text.count(chr(10), 0, position) + 1}: {message}')
checked('kotlin_lexical', f'{len(kt_files)} files: strings/comments/templates/delimiters only; NOT Kotlin parsing or type checking')

resources = {kind: set() for kind in ['string', 'drawable', 'layout', 'xml', 'color', 'mipmap', 'id']}
xml_files = list((ROOT / 'app/src/main').rglob('*.xml'))
for path in xml_files:
    try:
        tree = ET.parse(path)
        if tree.getroot().tag == 'resources':
            names = [(el.tag, el.attrib.get('name')) for el in tree.getroot() if 'name' in el.attrib]
            require(len(names) == len(set(names)), f'Duplicate resource in {path}')
        for element in tree.iter():
            for value in element.attrib.values():
                if value.startswith('@+id/'):
                    resources['id'].add(value[5:])
    except ET.ParseError as error:
        failures.append(f'{path}: {error}')
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
    for kind, name in re.findall(r'(?<!android\.)\bR\.(string|drawable|layout|xml|color|mipmap|id)\.(\w+)', path.read_text()):
        require(name in resources[kind], f'Missing R.{kind}.{name} in {path.name}')
checked('resources', f'{len(xml_files)} XML files parsed and local Kotlin resource references checked; AAPT NOT RUN')

v11 = ROOT / 'app/src/main/java/com/todoshift/app/v11'
model = (v11 / 'NeoAppearance.kt').read_text().split('data class NeoAppearance(', 1)[1].split(') {', 1)[0]
fields = re.findall(r'val (\w+):', model)
codec = (v11 / 'Neo4AppearanceCodec.kt').read_text()
encode = codec.split('fun encode(', 1)[1].split('fun decode(', 1)[0]
decode = codec.split('fun decode(', 1)[1]
for field in fields:
    require(f'"{field}"' in encode and f'"{field}"' in decode, 'Missing appearance codec field: ' + field)
require('todo_shift_neo3_appearance' in (v11 / 'NeoAppearanceStore.kt').read_text(), 'Old appearance store identity changed')
checked('appearance_persistence_source', f'{len(fields)} fields have encode/decode paths; process-restart test NOT RUN')

geometry = re.sub(r'\s+', '', (v11 / 'Neo4ShiftPaint.kt').read_text())
for snippet in ['FULL_DAY->Neo4Coverage(0f,1f)', 'LONG->Neo4Coverage(0f,0.5f)', 'NIGHT->Neo4Coverage(0.5f,1f)', 'MORNING->Neo4Coverage(0f,1f/3f)', 'EVENING->Neo4Coverage(1f/3f,2f/3f)']:
    require(snippet in geometry, 'Mandatory geometry definition changed: ' + snippet)
calendar = (v11 / 'ui/NeoCalendar.kt').read_text()
home = (v11 / 'ui/V11Home.kt').read_text()
require('NeoDashboard(' not in home, 'Fixed dashboard still mounted on calendar')
require('ModalBottomSheet' in calendar and 'Card(' not in calendar, 'Permanent calendar detail card or missing sheet')
require(calendar.index('NeoCalendarView.entries.forEach') < calendar.index('AnimatedContent('), 'View controls outside toolbar sequence')
require('Modifier.weight(1f)' in calendar and 'maxOf(5,' in calendar, 'Calendar no longer owns remaining height or 5/6 rows')
require('Neo4ShiftFill' in calendar, 'Calendar does not use full-cell paint')
checked('calendar_source', 'Mandatory coverage constants, full-cell renderer, grid weight, single toolbar sequence and sheet presence checked; rendered geometry NOT TESTED')

build = ROOT / 'app/build.gradle.kts'
require(hashlib.sha256(build.read_bytes()).hexdigest() == manifest['original_files']['app/build.gradle.kts'], 'Build dependencies/signing/version changed')
require('applicationId = "com.todoshift.app"' in build.read_text(), 'Application ID changed')
manifest_tree = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
permissions = {el.get('{http://schemas.android.com/apk/res/android}name') for el in manifest_tree.getroot().findall('uses-permission')}
require(not permissions.intersection({'android.permission.READ_EXTERNAL_STORAGE', 'android.permission.MANAGE_EXTERNAL_STORAGE', 'android.permission.INTERNET'}), 'Unexpected broad storage/network permission')
require('PickVisualMedia' in (v11 / 'ui/Neo4StudioUi.kt').read_text(), 'Photo Picker path missing')
cloud = (ROOT / 'app/src/main/java/com/todoshift/app/cloud/CloudContracts.kt').read_text()
require('override val configured = false' in cloud, 'Cloud readiness is unexpectedly enabled')
backup = (ROOT / 'app/src/main/java/com/todoshift/app/backup/Neo4LocalBackup.kt').read_text()
whitelist = backup.split('private val uiPreferences =', 1)[1].split('\n', 1)[0]
require('security' not in whitelist and 'token' not in whitelist, 'Security data in backup allow-list')
require('confirmedToken == prepared.token' in backup and 'validate(prepared)' in backup, 'Restore confirmation/revalidation missing')
require('writeAtomic(journal' in backup and 'editor.commit()' in backup and 'recoverIfNeeded' in backup, 'Durable rollback path missing')
widget = (ROOT / 'app/src/main/java/com/todoshift/app/widget/Neo4SummaryWidgets.kt').read_text()
require('security.protectAccounts || security.hideAmounts' in widget and 'Neo4WidgetPrivacy.amountsHidden' in widget, 'Finance widget privacy guard missing')
require('Neo4WidgetPainter.render' in (ROOT / 'app/src/main/java/com/todoshift/app/widget/CalendarWidgetFactory.kt').read_text(), 'Calendar widget geometry renderer missing')
checked('identity_and_safety_source', 'Build/signing preserved, no added permissions/dependencies, disabled cloud, explicit restore/rollback and widget privacy guards found; runtime NOT TESTED')

class Links(HTMLParser):
    def __init__(self):
        super().__init__(); self.links = []
    def handle_starttag(self, tag, attrs):
        if tag == 'a':
            self.links.extend(value for key, value in attrs if key == 'href')
html_files = list((ROOT / 'legal').glob('*.html'))
require(len(html_files) == 8, 'Expected eight bilingual legal HTML files')
for path in html_files:
    parser = Links(); parser.feed(path.read_text())
    for link in parser.links:
        if '://' not in link:
            require((path.parent / link).is_file(), 'Broken legal link: ' + link)
require(len(list((ROOT / 'app/src/main/assets/legal').glob('*.txt'))) == 8, 'Missing in-app legal text')
require(len(list((ROOT / 'app/src/main/res/drawable').glob('neo4_*.xml'))) == 6, 'Missing illustration theme')
checked('legal_and_assets', 'Eight in-app texts, eight HTML pages and relative links, six original vector themes checked; legal approval NOT CLAIMED')

print(json.dumps({'status': 'PASS' if not failures else 'FAIL', 'checks': checks, 'failures': failures,
                  'android_build': 'BLOCKED: Gradle/SDK unavailable', 'device_tests': 'NOT TESTED', 'benchmarks': 'NOT TESTED'}, ensure_ascii=False, indent=2))
sys.exit(bool(failures))

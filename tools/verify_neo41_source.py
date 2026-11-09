#!/usr/bin/env python3
"""4.1 static safety checks. Compilation/UI/unit checks are separate Gradle tasks."""
from pathlib import Path
import hashlib, json, re, subprocess, sys, xml.etree.ElementTree as ET
from kotlin_lexical_guard import scan
ROOT = Path(__file__).resolve().parents[1]
BASE = 'f71bf40a87aa5df7cc10da7085b3e44ac196dad0'
REFERENCE = json.loads((ROOT/'tools/NEO41_BASELINE_SHA256.json').read_text())
def unchanged(path): return hashlib.sha256((ROOT/path).read_bytes()).hexdigest() == REFERENCE['files'][path]
failures = []
def require(value, message):
    if not value: failures.append(message)
def text(path): return (ROOT / path).read_text()
critical = ['app/src/main/java/com/todoshift/app/v11/' + name for name in [
    'V11Models.kt', 'V11Storage.kt', 'V11MigrationEngine.kt', 'V11FinanceEngine.kt', 'V11RateEngine.kt',
    'V11SettlementEngine.kt', 'V11ObligationEngine.kt', 'V11ShiftConflictEngine.kt', 'V11WorkplaceSync.kt']]
critical += ['app/src/main/java/com/todoshift/app/data/AppStorage.kt', 'app/src/main/java/com/todoshift/app/storage/StorageRead.kt', 'app/src/main/java/com/todoshift/app/storage/StorageValidation.kt']
for p in critical: require(unchanged(p), 'Protected baseline changed: ' + p)
build = text('app/build.gradle.kts')
require('applicationId = "com.todoshift.app"' in build, 'Application ID changed')
require(int(re.search(r'versionCode = (\d+)', build)[1]) > REFERENCE['versionCode'], 'Version code did not increase')
require('versionName = "4.1.0"' in build, 'Wrong version name')
def signing(s): return s[s.index('    signingConfigs {'):s.index('    compileOptions {')]
require(hashlib.sha256(signing(build).encode()).hexdigest() == REFERENCE['signing_sha256'], 'Signing configuration changed')
for p in (ROOT/'app/src').rglob('*.kt'):
    for pos, message in scan(p.read_text())[1]: failures.append(f'{p.relative_to(ROOT)}:{message}')
for p in (ROOT/'app/src/main').rglob('*.xml'):
    try: ET.parse(p)
    except ET.ParseError as e: failures.append(str(e))
manifest = ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
require(unchanged('app/src/main/AndroidManifest.xml'), 'Manifest identity or permissions changed')
for p in (ROOT/'brand-references').glob('*.zip'):
    require(unchanged(str(p.relative_to(ROOT))), 'Brand archive modified: ' + p.name)
print(json.dumps({'status':'FAIL' if failures else 'PASS', 'protected_files':len(critical), 'checks':['P0 parser and business-rule byte identity', 'application/signing/launcher identity', 'version increment', 'Kotlin lexical scan', 'XML parse', 'brand archives unchanged'], 'failures':failures, 'limitations':'Static checks do not replace Gradle, UI, device or durability testing.'},indent=2))
sys.exit(bool(failures))

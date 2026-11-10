#!/usr/bin/env python3
"""Additive software coverage inventory. Never promotes original or physical acceptance gates."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
# Named regression subsets, not a claim that every clause of each Q case executed.
QA_TESTS = {
    'Q01': 'MoneyInputTest FinancialRequestIdentityTest ClassicReceiptSafetyTest PremiumP01UiTest ModernUiIntegrationTest',
    'Q02': 'PremiumActivitySaveTest NeoTaskCommitTest NewReferenceSafetyTest WorkplaceSaveAcceptanceTest CommittedEffectsTest PremiumP01UiTest',
    'Q03': 'NoteTransactionTest PremiumPaintingActivityTest',
    'Q04': 'BackupRecoveryTest AttachmentBackupTest PremiumLargeBackupTest ClassicBackupAuthorizationTest',
    'Q05': 'FinancialExportSafetyTest ModernSafetyTest PremiumWorkflowUiTest ClassicBackupAuthorizationTest',
    'Q06': 'NeoNavigationTest PremiumNavigationActivityTest NeoRescueUiTest PremiumSettingsUiTest',
    'Q07': 'PremiumNavigationActivityTest NeoRescueIntegrityTest',
    'Q08': 'PremiumNavigationActivityTest NeoNavigationTest NeoWidgetIntegrationTest',
    'Q09': 'PremiumPaintingActivityTest WidgetPaintParityTest',
    'Q10': 'PremiumPaintingActivityTest',
    'Q11': 'CalendarUiRegressionTest NeoCalendarPresentationTest NeoPaintChangesTest WidgetPaintParityTest',
    'Q12': 'PremiumPaintingActivityTest ModernOperationsTest NeoPaintChangesTest',
    'Q13': 'BackupRecoveryTest CalendarUiRegressionTest NewReferenceSafetyTest',
    'Q14': 'NeoTaskCommitTest PremiumP01UiTest PremiumWorkflowUiTest',
    'Q15': 'PremiumP01UiTest NeoWidgetIntegrationTest CommittedEffectsTest',
    'Q16': 'NeoSchedulePresentationTest PremiumWorkflowUiTest ModernOperationsTest',
    'Q17': 'ModernOperationsTest NeoRatePolicyTest FinancialExportSafetyTest ClassicReceiptSafetyTest',
    'Q18': 'AttachmentBackupTest PhotoImportTest NeoRescueIntegrityTest',
    'Q19': 'PremiumSettingsUiTest NeoRescueIntegrityTest NeoRescueUiTest',
    'Q20': 'PremiumSettingsUiTest NeoRescueIntegrityTest CalendarUiRegressionTest',
    'Q21': 'PremiumSettingsUiTest NeoRescueUiTest NeoRescueIntegrityTest',
    'Q22': 'CommittedEffectsTest ReminderPublicVersionTest ModernSafetyTest',
    'Q23': 'WidgetPresentationSafetyTest WidgetPaintParityTest NeoWidgetIntegrationTest',
    'Q24': 'NeoRescueIntegrityTest ModernSafetyTest',
    'Q25': '', 'Q26': '', 'Q27': 'PremiumLargeBackupTest',
    'Q28': 'CalendarUiRegressionTest PremiumSettingsUiTest NeoRescueUiTest',
}
FIXES = {
    'D01':'Single exact MoneyInput parse across inherited and Neo forms; no historical recalculation.',
    'D02':'Saved NeoNavigator owns header/body/tab; guarded atomic route transitions.',
    'D03':'Premium floating + enters painting directly; header brush removed per Blueprint correction.',
    'D04':'Back consumes review/type/selection/paint layers; Done clears bulk mode.',
    'D05':'Compact workplace strip and temporary types; fixed accessible eraser/Done.',
    'D06':'Preview shares production cell/header/navigation/task renderers and atmosphere; synthetic read-only fixture.',
    'D07':'Operation-linked checked commit completion; pending/error keeps editor and draft.',
    'D08':'Original bundled icon, explicit hide respected; physical incident not reproduced.',
    'D09':'Measured date/caption bounds; independent task/event/shift counts; workplace fill retained.',
    'D10':'Named whole-segment review and explicit note deletion confirmation; unrelated records retained.',
    'D11':'Single language/direction page, settings-only search, six groups, layout preset reachable.',
    'D12':'Shared widget projections/painter; inactive cloud honest status with stored preferences retained.',
    'D13':'Raw HEX and route leave guards; apply/discard wait for checked persistence.',
    'D14':'Compact month/filter controls, natural empty/search states, saved filters and explicit unknown clocks.',
    'D15':'No installed certificate or devices; upgrade remains BLOCKED, isolated test only.',
    'D16':'QuickTask editor remains visible on failure; stable-ID checked retry retained.',
    'D17':'Recoverable joint note-text/decoration transaction with boundary fault tests.',
    'D18':'Durable replay of confirmed data; separate widget-only replay; acknowledge after RemoteViews request.',
    'D19':'Private notification visibility and neutral public version; physical delivery unverified.',
    'D20':'Cold/warm explicit entry, consume once, saved consumed flag and editor leave guard; ordinary launcher resumes.',
    'D21':'Shared widget segment index avoids per-cell regrouping; physical comparative performance still unmeasured.',
    'D22':'Saveable task/schedule filters and navigation state; safe deleted-result handling.',
    'D23':'Blank differs from invalid; same validated cents result enables/saves financial forms.',
    'D24':'Restore journal/asset staging checks and recovery at preference boundaries; no real data used.',
    'D25':'Isolated manual QA workflow and candidate-based metadata verifier prepared; CI not executed/no push.',
    'D26':'Cached widget adapters recheck privacy before returning content; actual Launcher timing remains blocked.',
    'D27':'Current lint counts and IDs compared with P02/P05 XML; no disabled checks or baseline suppression.',
    'D28':'24 MiB attachment/large database export-preview-restore regression measured on JVM only; physical peak memory pending.',
    'D29':'20-snapshot session-only undo policy preserved and labelled; physical memory/process acceptance pending.',
}
PERSISTENCE = {
    '01':'V11Storage.days + NeoAppearanceStore', '02':'NeoAppearanceStore; immutable bundled brand assets',
    '03':'NeoAppearanceStore + saved route/date state', '04':'NeoAppearanceStore',
    '05':'V11Storage.calendars + V11ActiveCalendarStore (independent)',
    '06':'V11Storage.days; checked commit/effects journal; saved painting selection',
    '07':'NeoAppearanceStore / Neo4Profiles / V11UiSettingsStore',
    '08':'V11Storage.tasks + NeoSmartStore attachments/templates',
    '09':'V11Storage.days/calendars; saved filters (computed summaries)',
    '10':'V11Storage rates/payments/obligations/allocations; V11SecurityStore',
    '11':'See STORE_OWNERS.md (cross-cutting; no new business schema)',
    '12':'V11Storage + NeoSmartStore; undo session memory; reminder/widget preferences',
    '13':'NeoSmartStore task templates; Neo4Profiles/NeoAppearanceStore; local runtime focus',
    '14':'V11Storage.dayItems + NeoSmartStore note styles/attachments',
    '15':'NeoAppearanceStore / NeoModernPreferences',
    '16':'V11Storage + NeoSmartStore; private attachment files; transient focus/drafts',
    'UX':'Existing owners in STORE_OWNERS.md; routing saved state; shared appearance settings',
    'PM':'Bundled assets / saved routing / existing appearance stores; Git and candidate metadata',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence', type=Path, required=True)
    parser.add_argument('--source-sha', required=True)
    args = parser.parse_args()
    out = ROOT / 'docs/premium/recovery'
    out.mkdir(exist_ok=True)
    suites = {}
    for file in (args.evidence/'P06/junit').glob('TEST-*.xml'):
        root = ET.parse(file).getroot()
        suites[root.get('name').split('.')[-1]] = {
            'suite': root.get('name'), 'evidence': str(file),
            'tests': int(root.get('tests')), 'failures': int(root.get('failures')),
            'errors': int(root.get('errors')), 'skipped': int(root.get('skipped')),
        }
    assert suites and all(x['failures']==x['errors']==x['skipped']==0 for x in suites.values())
    cases = []
    for q, names in QA_TESTS.items():
        refs = [suites[name] for name in names.split()]  # Missing evidence must fail, never silently pass.
        cases.append(dict(id=q, software_evidence=refs,
            software_result='PASS — associated regression subset only' if refs else 'See local build/identity reports' if q=='Q25' else 'NOT RUN — physical upgrade',
            acceptance='PENDING', device_result='BLOCKED — ENVIRONMENT',
            outstanding='Execute full original Q criterion on authorized Android/Honor with matching signing, real providers/IME/Launcher/TalkBack and comparative performance. Named tests do not prove every editor/interleaving or visual combination.'))
    by_q = {q['id']: q for q in cases}
    old = json.loads((ROOT/'docs/premium/ACCEPTANCE.json').read_text())
    requirements = []
    for row in old['requirements']:
        qa_ids = re.findall(r'Q\d+', row['qa'])
        defects = re.findall(r'D\d+', row['defects'])
        requirements.append(dict(id=row['id'], original_intent=row['intent'],
            user_facing_location=row['baseline_location'], acceptance_criteria=row['acceptance'],
            current_implementation='Retained approved functionality; corrections below. Full device acceptance is not asserted.',
            corrected_implementation=[FIXES[d] for d in defects],
            phases=re.findall(r'P0\d', row['phase'])[::2],
            persistence=PERSISTENCE[row['id'].split('.')[0]],
            relevant_tests=sorted({x['suite'] for q in qa_ids for x in by_q[q]['software_evidence']}),
            actual_result='Associated automated regression subsets PASS; manual/full criterion acceptance PENDING. Read QA_DEVELOPMENT.json for limits.',
            device_manual='BLOCKED — ENVIRONMENT for physical cases; unexecuted combinations remain NOT RUN, not PASS.',
            status='acceptance pending', accepted=False))
    assert len(requirements)==147 and len({r['id'] for r in requirements})==147
    (out/'TRACEABILITY.json').write_text(json.dumps(dict(source_sha=args.source_sha, original_acceptance_unchanged=True, release_gate='HOLD', requirements=requirements),indent=2,ensure_ascii=False)+'\n')
    (out/'QA_DEVELOPMENT.json').write_text(json.dumps(dict(source_sha=args.source_sha,cases=cases),indent=2,ensure_ascii=False)+'\n')
    lines=['# Protected requirements — development evidence', '', '147 original IDs retained. This additive matrix does not replace or promote original acceptance. Associated suites cover subsets; no row is physically accepted. Full details/criteria/persistence/evidence: TRACEABILITY.json and QA_DEVELOPMENT.json.', '', '| ID | Intent | Location | Tests / QA | Status |', '|---|---|---|---|---|']
    for r,row in zip(requirements,old['requirements']):
        lines.append(f"| {r['id']} | {r['original_intent']} | {r['user_facing_location']} | {row['qa']} | Acceptance pending; automated subsets in JSON |")
    (out/'TRACEABILITY.md').write_text('\n'.join(lines)+'\n')
    # Static call-site inventory is reproducible, and explicitly not dynamic acceptance.
    pattern = re.compile(r'AlertDialog\(|Dialog\(|onSave\s*=|onDelete\s*=|rememberNeoEditorExit|neoRegisterLeaveGuard|saveIfCurrent\(|commitDatabase\(|saveDatabase\(|\.commit\(')
    inventory=[]
    for file in sorted((ROOT/'app/src/main').rglob('*.kt')):
        matches=[dict(line=i, code=line.strip()) for i,line in enumerate(file.read_text().splitlines(),1) if pattern.search(line)]
        if matches:
            inventory.append(dict(file=str(file.relative_to(ROOT)),sha256=hashlib.sha256(file.read_bytes()).hexdigest(),calls=matches))
    (out/'EDITOR_INVENTORY.json').write_text(json.dumps(dict(method='Static source search, includes Premium/Classic; not exhaustive dynamic/editor acceptance',files=inventory),indent=2,ensure_ascii=False)+'\n')
    defects=[dict(id=d,software_work=work,status='BLOCKED — ENVIRONMENT' if d=='D15' else 'PARTIAL — physical performance pending' if d in ['D21','D28','D29'] else 'IMPLEMENTED / automated subsets tested; acceptance pending',original_device_incident_reproduced=False) for d,work in FIXES.items()]
    (out/'DEFECTS.json').write_text(json.dumps(dict(source_sha=args.source_sha,defects=defects),indent=2,ensure_ascii=False)+'\n')
    print('147 requirements,28 QA cases,29 defects and static editor inventory recorded; acceptance remains pending')


if __name__ == '__main__':
    main()

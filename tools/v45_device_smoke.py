#!/usr/bin/env python3
"""Isolated emulator acceptance of the actual signed release APK and a v44 update."""
import json, pathlib, subprocess, sys, time, re, xml.etree.ElementTree as ET
APP='com.todoshift.app'
OUT=pathlib.Path('device-smoke');OUT.mkdir(exist_ok=True)
PREF='/data/user/0/'+APP+'/shared_prefs/todo_shift_v11.xml'
def adb(*args,binary=False):
    return subprocess.check_output(['adb',*args],text=not binary,timeout=60)
def shell(*args):return adb('shell',*args)
def wait(fn,seconds=70):
    deadline=time.monotonic()+seconds;last=None
    while time.monotonic()<deadline:
        try:
            result=fn()
            if result:return result
        except Exception as exc:last=exc
        time.sleep(.6)
    raise AssertionError('Acceptance timed out: '+str(last))
def database():
    root=ET.fromstring(adb('exec-out','cat',PREF))
    return json.loads(next(n.text for n in root if n.get('name')=='database'))
def dump():
    def snapshot():
        shell('rm','-f','/sdcard/v45-smoke.xml')
        shell('uiautomator','dump','/sdcard/v45-smoke.xml')
        return ET.fromstring(adb('exec-out','cat','/sdcard/v45-smoke.xml'))
    # Android accessibility can briefly return a null root during launch/rotation.
    root=wait(snapshot,seconds=35)
    # Only recover the external launcher ANR observed in the captured system dialog.
    if any(n.get('text')=="Pixel Launcher isn't responding" for n in root.iter('node')):
        close=next((n for n in root.iter('node') if n.get('resource-id')=='android:id/aerr_close'),None)
        if close is not None:
            tap(close)
            (OUT/'external-launcher-recovery.txt').write_text('Closed the observed Pixel Launcher ANR dialog; application failures are not suppressed.\n')
            time.sleep(.5)
            shell('uiautomator','dump','/sdcard/v45-smoke.xml')
            root=ET.fromstring(adb('exec-out','cat','/sdcard/v45-smoke.xml'))
    return root
def capture(name):
    root=dump();(OUT/(name+'.xml')).write_text(ET.tostring(root,encoding='unicode'))
    (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p',binary=True))
def launch(**extras):
    cmd=['am','start','-W','-n',APP+'/.MainActivity']
    for key,value in extras.items():cmd+=['--es',key,value]
    shell(*cmd)
def tap(node):
    bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))));assert len(bounds)==4
    shell('input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
def install(path):assert 'Success' in adb('install','-r',str(path))
def run():
    baseline,candidate=map(pathlib.Path,sys.argv[1:3])
    adb('root');adb('wait-for-device');adb('logcat','-c')
    shell('wm','size','720x1280');shell('wm','density','240')
    install(baseline);launch();wait(lambda:database().get('calendars'))
    shell('am','force-stop',APP)
    raw=adb('exec-out','cat',PREF);root=ET.fromstring(raw)
    node=next(n for n in root if n.get('name')=='database');before=json.loads(node.text)
    date='2026-11-14';calendar=before['calendars'][0]['id']
    before['tasks'].append(dict(id='v45-update-task',title='UpgradeFixture',details='',date=date,calendarId=calendar,status='TODO',priority='NORMAL',repeat='NONE',createdAt=1,updatedAt=1,completionHistory=[]))
    for key,title,body,locked in [('v45-note','AutosaveFixture','Before',False),('v45-private','SecretFixture','SECRET_MUST_BE_ENCRYPTED',True)]:
        before['dayItems'].append(dict(id=key,date=date,calendarId=calendar,type='NOTE',title=title,details=body,completed=False,createdAt=1,updatedAt=1,noteLocked=locked))
    node.text=json.dumps(before,ensure_ascii=False)
    subprocess.run(['adb','shell','sh','-c',"'cat > "+PREF+"'"],input=ET.tostring(root,encoding='unicode'),text=True,check=True,timeout=30)
    install(candidate);launch();wait(lambda:'sealedContentV45' in adb('exec-out','cat',PREF))
    after=database()
    for key in ['calendars','days','tasks','payments','settlements']:
        assert before[key]==after[key], 'Update changed existing '+key
    assert {n['id'] for n in before['dayItems']}=={n['id'] for n in after['dayItems']}
    assert 'SECRET_MUST_BE_ENCRYPTED' not in adb('exec-out','cat',PREF)
    assert 'SecretFixture' not in adb('exec-out','cat',PREF)
    capture('v45-update-preserved')
    launch(todo_shift_task_action='open',todo_shift_task_open_id='v45-note')
    def body_field():
        fields=[n for n in dump().iter('node') if n.get('class')=='android.widget.EditText']
        return next((n for n in fields if 'Before' in n.get('text','')),None)
    tap(wait(body_field));shell('input','text','_Autosaved')
    time.sleep(.65)
    shell('input','text','_Continued')
    wait(lambda:any(n['id']=='v45-note' and '_Autosaved' in n.get('details','') and '_Continued' in n.get('details','') for n in database()['dayItems']))
    capture('v45-note-autosaved-without-save-button')
    shell('am','force-stop',APP);launch();wait(lambda:any(n['id']=='v45-note' and '_Autosaved' in n.get('details','') for n in database()['dayItems']))
    # Arabic layout on a small screen; capture actual native rendering.
    shell('am','force-stop',APP)
    appearance_path='/data/user/0/'+APP+'/shared_prefs/todo_shift_neo3_appearance.xml'
    appearance_root=ET.fromstring(adb('exec-out','cat',appearance_path))
    appearance_node=next((n for n in appearance_root if n.get('name')=='neo4Appearance'),None)
    if appearance_node is None:appearance_node=ET.SubElement(appearance_root,'string',{'name':'neo4Appearance'})
    appearance=json.loads(appearance_node.text or '{}');appearance.update(language='ARABIC',uiDirection='RTL')
    appearance_node.text=json.dumps(appearance)
    subprocess.run(['adb','shell','sh','-c',"'cat > "+appearance_path+"'"],input=ET.tostring(appearance_root,encoding='unicode'),text=True,check=True,timeout=30)
    launch()
    wait(lambda:any('التقويم' in n.get('text','') for n in dump().iter('node')))
    shell('wm' ,'size','360x800');shell('wm','density','160');shell('settings','put','system','font_scale','1.3')
    capture('v45-small-screen')
    log=adb('logcat','-d','-v','threadtime');(OUT/'logcat.txt').write_text(log)
    assert not re.search(r'(ANR in com\.todoshift\.app|Process: com\.todoshift\.app|am_anr.*com\.todoshift\.app)',log), 'App crash or ANR observed'
    (OUT/'RESULT.txt').write_text('PASS: signed v44 to v45 update; fixture IDs and financial/task records retained; protected note content encrypted; note autosaves without Save and survives process restart; no observed crash/ANR.\n')
    print((OUT/'RESULT.txt').read_text())
try:run()
except Exception:
    try:capture('failure')
    except Exception:pass
    try:(OUT/'logcat.txt').write_text(adb('logcat','-d','-v','threadtime'))
    except Exception:pass
    raise

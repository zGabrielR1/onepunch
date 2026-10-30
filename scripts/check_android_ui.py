"""Smoke-test only the disposable onepunch-qa Android emulator, never a phone."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/qa-android'
ADB = str(Path(os.environ['ANDROID_HOME']) / 'platform-tools/adb')
SERIAL = 'emulator-5580'
APK = ROOT / 'android/app/build/outputs/apk/debug/app-debug.apk'
CATALOG_SIZE = len(json.loads((ROOT / 'shared/catalog.json').read_text(encoding='utf-8')))
checks = []


def adb(*args, binary=False):
    result = subprocess.run([ADB, '-s', SERIAL, *args], check=True, capture_output=True, timeout=40)
    return result.stdout if binary else result.stdout.decode('utf-8', errors='replace').strip()


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/onepunch-qa.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/onepunch-qa.xml'))


def find(value, *, attribute='text', scroll=False):
    for attempt in range(8 if scroll else 4):
        nodes = tree()
        for node in nodes.iter('node'):
            if node.get(attribute) == value:
                bounds = [int(v) for v in re.findall(r'\d+', node.get('bounds', ''))]
                if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
                    return bounds
        if scroll:
            adb('shell', 'input', 'swipe', '360', '1040', '360', '380', '350')
        else:
            time.sleep(1)
    raise AssertionError(f'Control not visible: {attribute}={value}')


def tap(value, **kwargs):
    left, top, right, bottom = find(value, **kwargs)
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(.6)


def tap_found(bounds):
    # Tap the node a previous dump returned; volatile labels (a running rest
    # timer prefixes the resume card) change before a second exact lookup.
    left, top, right, bottom = bounds
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(.6)


def screenshot(name):
    OUT.joinpath(f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    ET.ElementTree(tree()).write(OUT / f'{name}.xml', encoding='utf-8')


def launch():
    adb('shell', 'am', 'start', '-W', '-n', 'app.onepunch/.MainActivity')
    time.sleep(1)


def workspace():
    xml = ET.fromstring(adb('exec-out', 'run-as', 'app.onepunch', 'cat', 'shared_prefs/onepunch.xml'))
    return json.loads(next(node.text for node in xml if node.get('name') == 'workspace'))


def resume_active_if_needed():
    nodes = tree()
    for node in nodes.iter('node'):
        if 'Retomar treino' in node.get('text', ''):
            bounds = [int(v) for v in re.findall(r'\d+', node.get('bounds', ''))]
            if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
                tap_found(bounds)
                return


def check(condition, description):
    assert condition, description
    checks.append(description)
    print(f'PASS: {description}', flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    assert adb('shell', 'getprop', 'ro.boot.qemu.avd_name') == 'onepunch-qa', 'Refusing to control any other device'
    adb('shell', 'wm', 'size', '720x1280')
    adb('shell', 'wm', 'density', '320')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    launch()
    find('Vamos fazer acontecer.')
    screenshot('home-360')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.3')
    time.sleep(2)
    screenshot('home-large-font')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    time.sleep(2)
    tap('Exercícios')
    edit = next(node for node in tree().iter('node') if node.get('class') == 'android.widget.EditText')
    bounds = [int(v) for v in re.findall(r'\d+', edit.get('bounds'))]
    adb('shell', 'input', 'tap', str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
    adb('shell', 'input', 'text', 'desenvolvimento%smaquina')
    adb('shell', 'input', 'keyevent', '4')
    find(f'1 de {CATALOG_SIZE} exercícios')
    checks.append('Accent-insensitive multiword search finds the new machine variant')
    tap('Equipamento: Todos os equipamentos', scroll=True)
    tap('Máquina')
    find(f'1 de {CATALOG_SIZE} exercícios')
    screenshot('library-filtered')
    tap('Desenvolvimento na máquina', scroll=True)
    tap('Treinar este exercício', scroll=True)
    tap('Carga, série 1, Desenvolvimento na máquina', attribute='content-desc', scroll=True)
    adb('shell', 'input', 'text', '12.5')
    screenshot('session-keyboard')
    adb('shell', 'input', 'keyevent', '4')
    tap('Concluir série 1 de Desenvolvimento na máquina', attribute='content-desc', scroll=True)
    state = workspace()
    check(not state['workouts'], 'No history is created before finishing')
    check(state['active']['sets'][0]['done'], 'Completed set persists locally')
    check(float(state['active']['sets'][0]['weight']) == 12.5, 'Entered load persists exactly')
    screenshot('session-completed')
    adb('shell', 'am', 'force-stop', 'app.onepunch')
    launch()
    resume_active_if_needed()
    find('Desmarcar série 1 de Desenvolvimento na máquina', attribute='content-desc', scroll=True)
    checks.append('Active workout and completed set survive process restart')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.3')
    time.sleep(2)
    resume_active_if_needed()
    check(workspace()['active']['sets'][0]['done'], 'Completed set survives a font-scale configuration change')
    screenshot('session-large-font')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    time.sleep(2)
    resume_active_if_needed()
    tap('Finalizar treino', scroll=True)
    find('Revisar treino')
    screenshot('finish-review')
    tap('Salvar treino')
    state = workspace()
    check(state['active'] is None, 'Finishing clears the active session')
    check(len(state['workouts']) == 1 and len(state['workouts'][0]['sets']) == 1, 'Only the one explicitly completed set enters history')
    check(state['workouts'][0]['sets'][0]['exerciseId'] == 'machine-shoulder-press', 'New catalog ID is retained in native history')
    screenshot('history')
    OUT.joinpath('report.json').write_text(json.dumps({
        'serial': SERIAL, 'avd': 'onepunch-qa', 'android': adb('shell', 'getprop', 'ro.build.version.release'),
        'apk_sha256': hashlib.sha256(APK.read_bytes()).hexdigest(), 'checks': checks,
    }, indent=2, ensure_ascii=False), encoding='utf-8')
    print(f'{len(checks)} Android emulator checks passed.', flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception:
        if OUT.exists():
            try:
                screenshot('failure')
            except Exception:
                pass
        raise

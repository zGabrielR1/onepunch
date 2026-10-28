"""Hosted CI: bind actual automated results and signed assets to their source."""
import hashlib
import json
import os
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from release_config import read_release_config


def sha256(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def verify_apk_metadata(signature, badging, expected_fingerprint, config):
    normalize = lambda value: re.sub(r'[:\s]', '', value).lower()
    expected = normalize(expected_fingerprint)
    if not re.fullmatch(r'[a-f0-9]{64}', expected):
        raise ValueError('Expected signing certificate SHA-256 is required')
    fingerprints = re.findall(r'^Signer #\d+ certificate SHA-256 digest: (.+)$', signature, re.M)
    if len(fingerprints) != 1 or normalize(fingerprints[0]) != expected:
        raise ValueError('APK signing identity does not match the owner certificate')
    if 'androiddebugkey' in signature.lower() or 'cn=android debug' in signature.lower():
        raise ValueError('Debug certificate cannot sign a release candidate')
    package = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging, re.M)
    if not package or package.groups() != ('app.onepunch', config['androidVersionCode'], config['version']):
        raise ValueError('APK identity/version disagrees with release configuration')
    if re.search(r'^application-debuggable', badging, re.M):
        raise ValueError('Release APK must not be debuggable')
    return expected


def collect_results(variant):
    if variant not in ('debug', 'release'):
        raise ValueError('Unknown Android verification variant')
    task = 'test' + variant.title() + 'UnitTest'
    reports = list(Path('android/app/build/test-results', task).glob('TEST-*.xml'))
    totals = {key: 0 for key in ('tests', 'failures', 'errors', 'skipped')}
    for report in reports:
        suite = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(suite.attrib.get(key, 0))
    if not totals['tests'] or any(totals[key] for key in ('failures', 'errors', 'skipped')):
        raise ValueError('Android JVM verification missing, failed or skipped')
    match = re.search(r'(\d+) browser checks passed', Path('build-browser-checks.txt').read_text())
    if not match or not int(match[1]):
        raise ValueError('Browser workflow result is missing')
    events = [json.loads(line) for line in Path('build-go-tests.json').read_text().splitlines() if line.strip()]
    count = sum(e.get('Action') == 'pass' and 'Test' in e for e in events)
    if not count or any(e.get('Action') in ('fail', 'skip') for e in events):
        raise ValueError('Go race verification missing, failed or skipped')
    return totals, count, int(match[1])


def main():
    config = read_release_config()
    variant = os.environ.get('ANDROID_VARIANT', 'debug')
    totals, go_tests, browser_checks = collect_results(variant)
    output = Path('build/release')
    signing = {'kind': 'ephemeral debug certificate; not a production signing identity'}
    if variant == 'release':
        signing = {
            'kind': 'owner release certificate',
            'certificate_sha256': verify_apk_metadata(
                (output / 'apk-signature.txt').read_text(),
                (output / 'apk-badging.txt').read_text(),
                os.environ.get('ONEPUNCH_CERT_SHA256', ''), config),
        }
    elif config['prerelease'] != 'true':
        raise ValueError('Stable artifacts require release signing')
    assets = sorted([*output.glob('*.apk'), *output.glob('*.tar.gz')])
    if len(list(output.glob('*.apk'))) != 1 or len(list(output.glob('*.tar.gz'))) != 1:
        raise ValueError('Expected exactly one APK and one Linux bundle')
    manifest = {
        'version': config['version'], 'prerelease': config['prerelease'] == 'true',
        'android_version_code': int(config['androidVersionCode']),
        'commit': os.environ['GITHUB_SHA'],
        'workflow': f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
        'android_variant': variant, 'android_jvm': totals, 'go_race_tests': go_tests,
        'browser_workflow_checks': browser_checks,
        'web_domain_tests': 'passed before production build',
        'android_device_ui': 'pending; exact-artifact device QA required before stable promotion',
        'apk_signing': signing,
        'assets': {asset.name: sha256(asset) for asset in assets},
        'build_location': 'GitHub-hosted Ubuntu runner; no local native compilation',
    }
    (output / 'verification.json').write_text(json.dumps(manifest, indent=2) + '\n')


if __name__ == '__main__':
    main()

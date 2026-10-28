"""Validate an existing candidate and device evidence; never rebuild release files."""
import argparse
import json
import os
from pathlib import Path
import re
from release_manifest import sha256, verify_apk_metadata

REQUIRED_CHECKS = {
    'clean_install', 'debug_backup_transition', 'signed_update', 'offline_workout',
    'interruption_recovery', 'backup_restore', 'sync_companion', 'storage_failures',
    'accessibility', 'multi_day_sessions', 'larger_history',
}


def validate_candidate(directory, qa, run, repository, expected_fingerprint):
    directory = Path(directory)
    manifest = json.loads((directory / 'verification.json').read_text())
    version = manifest['version']
    if not re.fullmatch(r'\d+\.\d+\.\d+', version) or manifest.get('prerelease') is not False:
        raise ValueError('Stable promotion requires a final version, not an RC')
    if not re.fullmatch(r'[0-9a-f]{40}', manifest['commit']):
        raise ValueError('Candidate source SHA is invalid')
    if (run.get('conclusion') != 'success' or run.get('event') != 'workflow_dispatch'
            or run.get('path') != '.github/workflows/candidate.yml'
            or run.get('head_sha') != manifest['commit']):
        raise ValueError('Candidate must come from a successful signed-candidate run')
    expected_workflow = f"https://github.com/{repository}/actions/runs/{run['id']}"
    if manifest['workflow'] != expected_workflow:
        raise ValueError('Candidate run provenance differs from its manifest')
    totals = manifest['android_jvm']
    if (manifest['android_variant'] != 'release' or totals['tests'] < 1
            or any(totals[key] for key in ('failures', 'errors', 'skipped'))
            or manifest['go_race_tests'] < 1 or manifest['browser_workflow_checks'] < 1
            or manifest['web_domain_tests'] != 'passed before production build'):
        raise ValueError('Candidate automated verification is incomplete')
    config = {'version': version, 'androidVersionCode': str(manifest['android_version_code'])}
    certificate = verify_apk_metadata((directory / 'apk-signature.txt').read_text(),
                                      (directory / 'apk-badging.txt').read_text(),
                                      expected_fingerprint, config)
    if (manifest['apk_signing'].get('kind') != 'owner release certificate'
            or manifest['apk_signing'].get('certificate_sha256') != certificate):
        raise ValueError('Candidate signing manifest is inconsistent')
    expected_assets = {f'onepunch-v{version}.apk', f'onepunch-v{version}-linux-amd64.tar.gz'}
    if set(manifest['assets']) != expected_assets:
        raise ValueError('Unexpected candidate asset names')
    for name, digest in manifest['assets'].items():
        if sha256(directory / name) != digest:
            raise ValueError('Candidate asset hash mismatch: ' + name)
    # Check all companion metadata, too; no paths or duplicate checksum entries.
    sums = {}
    for line in (directory / 'SHA256SUMS.txt').read_text().splitlines():
        digest, name = line.split(maxsplit=1)
        name = name.removeprefix('*').removeprefix('./')
        if (Path(name).name != name or name in sums
                or not re.fullmatch(r'[a-f0-9]{64}', digest)):
            raise ValueError('Invalid checksum entry')
        sums[name] = digest
    if set(sums) != expected_assets | {'verification.json', 'apk-signature.txt', 'apk-badging.txt'}:
        raise ValueError('Candidate checksums are incomplete')
    if any(sha256(directory / name) != digest for name, digest in sums.items()):
        raise ValueError('Candidate metadata checksum mismatch')
    apk = f'onepunch-v{version}.apk'
    if (qa.get('status') != 'passed' or qa.get('commit') != manifest['commit']
            or qa.get('candidate_run_id') != run['id']
            or qa.get('apk_sha256') != manifest['assets'][apk]
            or qa.get('certificate_sha256') != certificate):
        raise ValueError('Device QA does not approve this exact candidate')
    for field in ('tester', 'tested_at', 'evidence'):
        if not isinstance(qa.get(field), str) or not qa[field].strip():
            raise ValueError('Missing device QA field: ' + field)
    checks = qa.get('checks', {})
    if not REQUIRED_CHECKS <= set(checks) or any(checks[key] != 'passed' for key in REQUIRED_CHECKS):
        raise ValueError('Required device QA scenarios have not all passed')
    devices = qa.get('devices', [])
    if not devices or any(not device.get('model') or not device.get('android') for device in devices):
        raise ValueError('Device/OS evidence is missing')
    coverage = qa.get('coverage', {})
    if any(coverage.get(key) is not True for key in ('android_8', 'current_android', 'narrow_phone', 'large_fonts')):
        raise ValueError('Required device coverage is incomplete')
    if qa.get('open_blockers') != []:
        raise ValueError('Device QA has unresolved blockers')
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', required=True)
    parser.add_argument('--qa', required=True)
    parser.add_argument('--run', required=True)
    args = parser.parse_args()
    qa = json.loads(Path(args.qa).read_text())
    run = json.loads(Path(args.run).read_text())
    manifest = validate_candidate(args.directory, qa, run,
                                  os.environ['GITHUB_REPOSITORY'], os.environ['ONEPUNCH_CERT_SHA256'])
    print('RELEASE_TAG=v' + manifest['version'])
    print('RELEASE_SHA=' + manifest['commit'])


if __name__ == '__main__':
    main()

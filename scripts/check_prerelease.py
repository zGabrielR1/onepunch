"""Validate signed, automated-only prerelease assets; stable promotion is separate."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from release_config import read_release_config
from release_manifest import sha256, verify_apk_metadata


def require_unused_tag(repo, tag):
    result = subprocess.run(['gh', 'api', f'repos/{repo}/git/ref/tags/{tag}'],
                            capture_output=True, text=True)
    if result.returncode == 0:
        raise ValueError('Tag already exists; refusing to publish over older source')
    if '(HTTP 404)' not in result.stderr:
        raise ValueError('Could not establish that the release tag is unused')


def validate_prerelease(directory, config, source, fingerprint):
    directory = Path(directory)
    if config['prerelease'] != 'true' or '-' not in config['version']:
        raise ValueError('This publication path requires a prerelease version')
    manifest = json.loads((directory / 'verification.json').read_text())
    if (manifest['commit'] != source or manifest['version'] != config['version']
            or manifest['android_version_code'] != int(config['androidVersionCode'])
            or manifest['prerelease'] is not True or manifest['android_variant'] != 'release'):
        raise ValueError('Candidate source, version or release variant mismatch')
    certificate = verify_apk_metadata(
        (directory / 'apk-signature.txt').read_text(),
        (directory / 'apk-badging.txt').read_text(), fingerprint, config)
    if manifest['apk_signing'] != {'kind': 'owner release certificate', 'certificate_sha256': certificate}:
        raise ValueError('Candidate signing manifest mismatch')
    assets = {f'onepunch-v{config["version"]}.apk', f'onepunch-v{config["version"]}-linux-amd64.tar.gz'}
    if set(manifest['assets']) != assets:
        raise ValueError('Unexpected candidate assets')
    for name, digest in manifest['assets'].items():
        if sha256(directory / name) != digest:
            raise ValueError('Candidate asset hash mismatch')
    required = assets | {'verification.json', 'apk-signature.txt', 'apk-badging.txt'}
    checked = set()
    for line in (directory / 'SHA256SUMS.txt').read_text().splitlines():
        match = re.fullmatch(r'([a-f0-9]{64})  (?:\./)?([^/\\]+)', line)
        if not match or match[2] not in required or match[2] in checked:
            raise ValueError('Invalid candidate checksum entry')
        if sha256(directory / match[2]) != match[1]:
            raise ValueError('Candidate checksum mismatch')
        checked.add(match[2])
    if checked != required:
        raise ValueError('Incomplete candidate checksums')
    return manifest


if __name__ == '__main__':
    config = read_release_config()
    validate_prerelease(sys.argv[1], config, os.environ['GITHUB_SHA'], os.environ['ONEPUNCH_CERT_SHA256'])
    require_unused_tag(os.environ['GITHUB_REPOSITORY'], 'v' + config['version'])
    print('Exact signed prerelease files verified; Android device UI QA remains pending.')

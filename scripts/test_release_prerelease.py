"""Publication boundary regressions; no compilation, credentials or network."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import subprocess

from check_prerelease import require_unused_tag, validate_prerelease
from release_manifest import sha256


class PrereleaseTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.config = {'version': '1.0.0-rc.2', 'androidVersionCode': '5', 'prerelease': 'true'}
        self.fingerprint = 'ab' * 32
        self.source = 'c' * 40
        self.apk = 'onepunch-v1.0.0-rc.2.apk'
        self.bundle = 'onepunch-v1.0.0-rc.2-linux-amd64.tar.gz'
        (self.root / self.apk).write_bytes(b'test APK')
        (self.root / self.bundle).write_bytes(b'test archive')
        (self.root / 'apk-signature.txt').write_text(
            'Signer #1 certificate DN: CN=Onepunch Release\n'
            'Signer #1 certificate SHA-256 digest: ' + self.fingerprint + '\n')
        (self.root / 'apk-badging.txt').write_text(
            "package: name='app.onepunch' versionCode='5' versionName='1.0.0-rc.2'\n")
        self.manifest = {
            'commit': self.source, 'version': self.config['version'], 'android_version_code': 5,
            'prerelease': True, 'android_variant': 'release',
            'apk_signing': {'kind': 'owner release certificate', 'certificate_sha256': self.fingerprint},
            'assets': {name: sha256(self.root / name) for name in (self.apk, self.bundle)},
        }
        self.write_manifest(self.manifest)

    def write_manifest(self, value):
        (self.root / 'verification.json').write_text(json.dumps(value))
        files = [self.apk, self.bundle, 'verification.json', 'apk-signature.txt', 'apk-badging.txt']
        (self.root / 'SHA256SUMS.txt').write_text(''.join(
            f'{sha256(self.root / name)}  ./{name}\n' for name in files))

    def validate(self, config=None, fingerprint=None):
        return validate_prerelease(self.root, config or self.config, self.source, fingerprint or self.fingerprint)

    def test_exact_signed_candidate_is_accepted(self):
        self.assertEqual(self.validate(), self.manifest)

    def test_stable_settings_are_rejected(self):
        for change in ({'prerelease': 'false'}, {'version': '1.0.0'}):
            with self.assertRaises(ValueError):
                self.validate({**self.config, **change})

    def test_changed_source_version_variant_or_identity_is_rejected(self):
        for change in ({'commit': 'd' * 40}, {'version': '0.3.0'}, {'android_version_code': 4},
                       {'prerelease': False}, {'android_variant': 'debug'}):
            self.write_manifest({**self.manifest, **change})
            with self.assertRaises(ValueError):
                self.validate()

    def test_artifact_tampering_and_unexpected_assets_are_rejected(self):
        (self.root / self.apk).write_bytes(b'tampered')
        with self.assertRaises(ValueError):
            self.validate()
        changed = copy.deepcopy(self.manifest)
        changed['assets']['../outside.apk'] = '0' * 64
        self.write_manifest(changed)
        with self.assertRaises(ValueError):
            self.validate()

    def test_missing_duplicate_and_traversing_checksum_entries_are_rejected(self):
        sums = self.root / 'SHA256SUMS.txt'
        original = sums.read_text()
        for content in ('', original + original, original.replace('./verification.json', '../verification.json')):
            sums.write_text(content)
            with self.assertRaises(ValueError):
                self.validate()

    def test_unexpected_signing_certificate_is_rejected(self):
        with self.assertRaises(ValueError):
            self.validate(fingerprint='cd' * 32)

    def test_only_missing_tag_can_proceed(self):
        with patch('check_prerelease.subprocess.run', return_value=subprocess.CompletedProcess([], 1, '', 'gh: Not Found (HTTP 404)')):
            require_unused_tag('owner/repo', 'v1.0.0-rc.2')

    def test_existing_tag_and_api_errors_cannot_publish(self):
        for result in (subprocess.CompletedProcess([], 0, '{}', ''),
                       subprocess.CompletedProcess([], 1, '', 'HTTP 403'),
                       subprocess.CompletedProcess([], 1, '', 'Could not resolve host')):
            with patch('check_prerelease.subprocess.run', return_value=result), self.assertRaises(ValueError):
                require_unused_tag('owner/repo', 'v1.0.0-rc.2')

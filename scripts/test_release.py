"""Dependency-free regression tests for release trust boundaries; no compilers."""
import copy
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from release_manifest import collect_results, sha256, verify_apk_metadata
from promote_release import REQUIRED_CHECKS, validate_candidate
from release_config import read_release_config

FINGERPRINT = 'ab' * 32
CONFIG = {'version': '1.0.0', 'androidVersionCode': '5'}
SIGNATURE = 'Signer #1 certificate DN: CN=Onepunch Release\nSigner #1 certificate SHA-256 digest: ' + FINGERPRINT + '\n'
BADGING = "package: name='app.onepunch' versionCode='5' versionName='1.0.0' platformBuildVersionName='16'\n"


class SigningTest(unittest.TestCase):
    def test_owner_fingerprint_accepts_colons_and_case(self):
        self.assertEqual(verify_apk_metadata(SIGNATURE, BADGING, ':'.join(['AB'] * 32), CONFIG), FINGERPRINT)

    def test_wrong_missing_or_debug_identity_rejected(self):
        for signature, fingerprint in [(SIGNATURE, ''), (SIGNATURE, 'cd' * 32),
                                       (SIGNATURE.replace('Onepunch Release', 'Android Debug'), FINGERPRINT),
                                       (SIGNATURE + SIGNATURE, FINGERPRINT)]:
            with self.subTest(signature=signature, fingerprint=fingerprint):
                with self.assertRaises(ValueError):
                    verify_apk_metadata(signature, BADGING, fingerprint, CONFIG)

    def test_debuggable_and_wrong_package_or_version_rejected(self):
        for badging in [BADGING + 'application-debuggable\n', BADGING.replace('app.onepunch', 'app.other'),
                        BADGING.replace("versionCode='5'", "versionCode='4'"),
                        BADGING.replace("versionName='1.0.0'", "versionName='0.3.0'")]:
            with self.subTest(badging=badging), self.assertRaises(ValueError):
                verify_apk_metadata(SIGNATURE, badging, FINGERPRINT, CONFIG)


class ResultsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        previous = Path.cwd()
        os.chdir(self.temporary.name)
        self.addCleanup(os.chdir, previous)
        self.reports = Path('android/app/build/test-results/testReleaseUnitTest')
        self.reports.mkdir(parents=True)
        self.xml = self.reports / 'TEST-example.xml'
        self.xml.write_text('<testsuite tests="2" failures="0" errors="0" skipped="0"/>')
        Path('build-browser-checks.txt').write_text('116 browser checks passed\n')
        Path('build-go-tests.json').write_text('{"Action":"pass","Test":"TestStore"}\n')

    def test_requires_matching_variant_reports(self):
        self.assertEqual(collect_results('release'), ({'tests': 2, 'failures': 0, 'errors': 0, 'skipped': 0}, 1, 116))
        with self.assertRaises(ValueError):
            collect_results('debug')

    def test_failed_skipped_empty_jvm_reports_rejected(self):
        for xml in ['<testsuite tests="0"/>', '<testsuite tests="2" skipped="1"/>',
                    '<testsuite tests="2" errors="1"/>', '<testsuite tests="2" failures="1"/>']:
            self.xml.write_text(xml)
            with self.subTest(xml=xml), self.assertRaises(ValueError):
                collect_results('release')

    def test_missing_browser_and_failed_go_rejected(self):
        Path('build-browser-checks.txt').write_text('0 browser checks passed')
        with self.assertRaises(ValueError):
            collect_results('release')
        Path('build-browser-checks.txt').write_text('116 browser checks passed')
        Path('build-go-tests.json').write_text('{"Action":"fail"}\n')
        with self.assertRaises(ValueError):
            collect_results('release')


class PromotionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.run = {'id': 123, 'head_sha': 'a' * 40, 'conclusion': 'success',
                    'event': 'workflow_dispatch', 'path': '.github/workflows/candidate.yml'}
        self.manifest = {'version': '1.0.0', 'prerelease': False, 'android_version_code': 5,
                         'commit': 'a' * 40, 'workflow': 'https://github.com/owner/repo/actions/runs/123',
                         'android_variant': 'release', 'android_jvm': {'tests': 2, 'failures': 0, 'errors': 0, 'skipped': 0},
                         'go_race_tests': 1, 'browser_workflow_checks': 116,
                         'web_domain_tests': 'passed before production build',
                         'apk_signing': {'kind': 'owner release certificate', 'certificate_sha256': FINGERPRINT},
                         'assets': {}}
        for name in ['onepunch-v1.0.0.apk', 'onepunch-v1.0.0-linux-amd64.tar.gz']:
            (self.root / name).write_bytes(b'test artifact')
            self.manifest['assets'][name] = sha256(self.root / name)
        (self.root / 'apk-signature.txt').write_text(SIGNATURE)
        (self.root / 'apk-badging.txt').write_text(BADGING)
        self.qa = {'status': 'passed', 'candidate_run_id': 123, 'commit': 'a' * 40,
                   'apk_sha256': self.manifest['assets']['onepunch-v1.0.0.apk'],
                   'certificate_sha256': FINGERPRINT, 'tester': 'Tester', 'tested_at': '2026-10-27',
                   'evidence': 'docs/qa/results.md', 'devices': [{'model': 'Test device', 'android': '8'}],
                   'coverage': dict.fromkeys(['android_8', 'current_android', 'narrow_phone', 'large_fonts'], True),
                   'checks': dict.fromkeys(REQUIRED_CHECKS, 'passed'), 'open_blockers': []}
        self.write_manifest()

    def write_manifest(self):
        (self.root / 'verification.json').write_text(json.dumps(self.manifest))
        files = sorted(path for path in self.root.iterdir() if path.name != 'SHA256SUMS.txt')
        (self.root / 'SHA256SUMS.txt').write_text(''.join(f'{sha256(path)}  {path.name}\n' for path in files))

    def validate(self):
        return validate_candidate(self.root, self.qa, self.run, 'owner/repo', FINGERPRINT)

    def test_exact_checked_candidate_accepted(self):
        self.assertEqual(self.validate()['commit'], 'a' * 40)

    def test_rc_or_prerelease_flag_cannot_be_promoted(self):
        for version, prerelease in [('1.0.0-rc.1', True), ('1.0.0', True)]:
            self.manifest.update(version=version, prerelease=prerelease)
            self.write_manifest()
            with self.subTest(version=version), self.assertRaises(ValueError):
                self.validate()

    def test_failed_or_different_source_workflow_rejected(self):
        original = copy.deepcopy(self.run)
        for field, value in [('conclusion', 'failure'), ('head_sha', 'b' * 40),
                             ('path', '.github/workflows/release.yml'), ('event', 'pull_request')]:
            self.run = dict(original, **{field: value})
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.validate()

    def test_tampered_apk_or_metadata_rejected(self):
        for name in ['onepunch-v1.0.0.apk', 'apk-badging.txt']:
            path = self.root / name
            before = path.read_bytes()
            path.write_bytes(before + b'changed')
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.validate()
            path.write_bytes(before)

    def test_wrong_qa_hash_incomplete_coverage_and_blockers_rejected(self):
        original = copy.deepcopy(self.qa)
        for field, value in [('apk_sha256', '0' * 64), ('candidate_run_id', 124),
                             ('checks', {}), ('coverage', {}), ('open_blockers', ['R2']),
                             ('tester', ''), ('devices', [])]:
            self.qa = dict(original, **{field: value})
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.validate()

    def test_checksum_path_and_omission_rejected(self):
        sums = self.root / 'SHA256SUMS.txt'
        sums.write_text('0' * 64 + '  ../outside\n')
        with self.assertRaises(ValueError):
            self.validate()
        sums.write_text('')
        with self.assertRaises(ValueError):
            self.validate()


class ConfigTest(unittest.TestCase):
    def test_candidate_cannot_declare_stable(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / 'shared').mkdir()
            (root / 'shared/release.properties').write_text('version=1.0.0-rc.1\nandroidVersionCode=4\nprerelease=false\n')
            with patch('release_config.ROOT', root), self.assertRaisesRegex(ValueError, 'cannot be marked stable'):
                read_release_config()


if __name__ == '__main__':
    unittest.main()

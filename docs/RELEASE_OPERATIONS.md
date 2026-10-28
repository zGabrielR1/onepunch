# Building and promoting 1.0

The signed candidate and stable promotion workflows implement the packaging gates
in [the release plan](RELEASE_1_0_PLAN.md). Compilation runs on GitHub, never on
the development PC. The existing Hosted prerelease workflow remains available
for debug prereleases and refuses stable versions.

## Signing setup

Use one owner-controlled signing identity for all signed candidates and updates.
The owner must retain a secure keystore recovery copy and its passwords; GitHub
secrets cannot be downloaded as a backup. Losing the signing identity prevents
normal updates to installed release APKs.

Configure these repository Actions secrets, without placing their values in
source, shell history, release artifacts or chat:

| Secret | Value |
| --- | --- |
| `ONEPUNCH_KEYSTORE_BASE64` | Base64-encoded release keystore |
| `ONEPUNCH_KEYSTORE_PASSWORD` | Keystore password |
| `ONEPUNCH_KEY_ALIAS` | Release private-key alias |
| `ONEPUNCH_KEY_PASSWORD` | Key password |
| `ONEPUNCH_CERT_SHA256` | Release certificate SHA-256 fingerprint, hex with optional colons |

Creating a long-lived key requires an explicit owner choice; do not reuse a
GitHub runner's debug certificate. The candidate workflow requires all five
secrets, verifies the APK signature and expected fingerprint, rejects debug
certificates and debuggable APKs, and checks application ID/version/code.
Keystore material is restored only under RUNNER_TEMP and removed at job end.

## Build candidate

1. Review and commit intended source changes. Set `shared/release.properties`,
   matching web package/lock versions, and `docs/releases/v<VERSION>.md`.
   RCs use `prerelease=true`; the eventual final `1.0.0` uses `false` and an
   Android versionCode higher than every distributed candidate.
2. Run **Signed release candidate** (`candidate.yml`) on that source ref.
   It never publishes. It runs release-variant JVM tests, web domain/build,
   Go race checks and real browser workflows, then packages a release APK and
   Linux API/web archive. The archive is extracted into a clean temporary
   directory and smoke-tested using disposable API storage.
3. Download `onepunch-signed-candidate-<RUN_ID>` and preserve
   `onepunch-signed-verification-<RUN_ID>`. Hosted artifacts retain for 90 days
   subject to repository limits; retain reviewed evidence before expiry.
4. Check `SHA256SUMS.txt`. The verification manifest binds artifact hashes,
   signing fingerprint, source SHA and workflow. Automated checks do not replace
   device QA. Perform the detailed [native checks](NATIVE_QA.md) on a separate
   testing machine/device using these exact artifacts.

## Upgrade from a debug APK

The existing debug certificate may differ from the release certificate, so Android
may reject in-place updates. Keep the original installation and personal data
until an exported JSON backup is verified. On a disposable profile/device, test
that backup's restore into the signed candidate first. Sync alone omits active
workouts and routine drafts. Preserve the exported file before any uninstall;
never clear the only copy of personal records to bypass a signature error.

Subsequent release-signed updates keep `app.onepunch`, the same signing key,
existing storage keys and a strictly increasing versionCode. Rehearse an RC-to-final
update and verify history, active strings/deadline and drafts after updating.
A display-name change uses shared branding and does not change storage identity.

## Stable handoff, without rebuilding

1. Build the **final version** with publication disabled by using the candidate
   workflow. RC-version artifacts cannot be promoted as a final version.
2. Copy `docs/qa/device-report.template.json` to a report such as
   `docs/qa/v1_0_0.json`. Enter the actual candidate run ID, source SHA, APK hash,
   signing fingerprint, tester/date and evidence path or URL. List devices with
   `model` and `android` fields. Complete the required coverage and checks only
   after testing; leave `pending` for anything not demonstrated. Record defect
   IDs in `open_blockers`. The template intentionally fails the promotion gate.
3. Commit the completed report and its evidence. Run **Promote checked stable
   release** with the candidate run ID, report path and `publish=false` to validate
   the complete handoff. It downloads the existing candidate, checks the successful
   run's source and workflow, checks every delivered hash and verifies exact-artifact
   QA. It reads release notes from the candidate commit.
4. After reviewing the concrete handoff and receiving publication authorization,
   run promotion with `publish=true`. It uploads those existing checked files,
   device QA report and notes; no build tools run. Existing tags are never reused.
5. Read back the release and verify downloaded hashes, tag/source, stable flag
   and certificate. Keep the manifest's original automated-only device-QA status;
   the attached `device-qa.json` supplies the later manual evidence for its APK hash.

Do not publish while required QA is pending. A signing key, successful hosted
checks and actual device evidence remain external prerequisites; adding workflow
code does not establish them.

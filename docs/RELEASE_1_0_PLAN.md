# Onepunch 1.0 release plan

Planning baseline: 2026-10-27. Status: planned; implementation and release gates
below are not yet complete. This document does not authorize publication,
installation, repository visibility changes or local native compilation.

## Release promise and scope

1.0 is a dependable personal workout logger for Android, in Português (Brasil)
and kg, with a web companion and an optional personal Go sync service. A user can
prepare a routine, complete a workout offline, recover after interruption, review
real history and move their data safely to a new installation.

Keep the existing feature set as the scope baseline: 27 exercises, nine editable
templates, routine editing/lifecycle/drafts, free and routine workouts, completed
set logging, rest deadlines, exercise replacement/order, finish review, history,
replay, native backup/restore and optional routine/finished-history sync. Existing
native features are the primary product; complete web feature parity is not a
1.0 requirement. Document the companion's actual supported operations.

Defer accounts, public cloud hosting, social features, subscriptions, adaptive
training advice, wearables, new platforms, automatic background sync and promised
background timer alerts. App-store distribution is a separate scope decision.
The default delivery target is the existing GitHub APK and Linux API/web bundle
channel; stable release status does not require making the repository public.

## Baseline and gaps

Repository records report v0.3.0 from `738a12b` passed hosted compilation, 54
Android JVM tests, four Go race tests, 116 browser checks, web checks/build and
APK signature verification. These are historical results, not verification of
the current working tree. The later hosted build-only request remains recorded
as pending; reconcile its actual result before choosing a candidate commit.

The current Android package is debug-signed using a temporary certificate.
Device UI, document-picker and interruption QA remain pending. The workflow
assembles debug APKs and its verification manifest explicitly records untested
device UI and ephemeral signing. Release packaging therefore needs changes
before it can substantiate a stable release.

There are existing uncommitted edits in `App.kt`, `WorkoutEditing.kt` and the
`android/gradlew` executable mode. Review their ownership and intended behavior
before inclusion; never discard or silently treat them as tested 0.3.0 source.

## Milestones and acceptance gates

Execute in dependency order. Engineering can implement fixes and record evidence;
the owner decides product identity and controls the long-lived signing key. Device
QA requires a separate test machine/device. Dates follow those dependencies rather
than a fixed launch deadline.

| Milestone | Required work | Exit evidence |
| --- | --- | --- |
| M0 — scope and candidate baseline | Review working changes and latest hosted run; inventory client capabilities and known defects; settle display name and distribution channel | Agreed scope, tracked defects and an identified source commit; no ambiguous build provenance |
| M1 — persistence and recovery | Audit save/error paths; exercise legacy data, backup/restore, sync conflicts and interruption; fix data-loss/incorrect-history defects | Focused regression cases plus successful recovery scenarios; original unreadable data remains available |
| M2 — release packaging | Add persistent release signing and hosted release builds; update manifest generation and candidate promotion | Non-debuggable signed candidate, certificate fingerprint, source SHA, checksums and truthful verification metadata |
| M3 — candidate QA | Run all hosted checks against candidate source; complete device and cross-client matrix below; resolve blockers | Exact candidate artifact passes automated and manual gates; defect/evidence ledger complete |
| M4 — 1.0 delivery | Finalize docs/versioning, verify upgrade and clean install, obtain final publication authorization | Published checked artifacts from approved source and a verified download/install handoff |

M1 and preparation for M2 can proceed after M0; M3 requires both. Any candidate
code or packaging change invalidates affected evidence and requires a fresh
candidate. Do not reuse an occupied tag or attach new assets to older source.

### M0: product identity and compatibility

- Decide whether to retain Onepunch for 1.0 or rename through
  `shared/brand.properties`. Keep `app.onepunch`, storage keys and existing IDs
  stable so a display-name change does not become a data migration.
- Record Android 8+/SDK26 as the existing minimum and Linux amd64 as the bundled
  API target. List unsupported hosting/platform scenarios in release notes.
- Inventory Android/web differences in a short support table in README. Verify
  each claimed operation on its actual client; do not imply native backup or
  recoverable routine drafts exist on web without implementation and evidence.
- Create a defect ledger with ID, severity, reproduction, affected client/source,
  fix commit and verification result. All milestone findings go into that ledger.

### M1: protect workout data

- Review write-before-UI-success behavior for logging, finishing, discarding,
  drafts, archive/restore, sync and backup. Failed writes retain recoverable state
  and give an actionable error; unreadable records are never silently replaced.
- Preserve the version-1 contract unless a demonstrated defect requires a schema
  change. If changed, add explicit migration fixtures for existing records,
  unknown-version rejection and failure recovery before accepting the change.
- Verify zero kg and decimal commas, missing/invalid inputs, unchecked prefills,
  completed-set edits, exercise replacement and finish preview/save agreement.
  Only valid explicitly completed sets contribute to history and volume.
- Test backup round trips with routine drafts, partial inputs and rest deadlines;
  rejected imports, repeated merges and conflicting immutable history must leave
  existing data intact. Confirm the pre-restore safety copy is exportable.
- Verify two-client sync with newer/older/equal-date routines, archives, repeated
  sync, incompatible history and a disconnected API. Active workouts/drafts stay
  local. Keep the service personal, single-process and loopback by default.

Add tests for discovered risks and fixes, rather than expanding coverage counts
as a release target. Use disposable fixtures; never personal training records.

### M2: stable signing and hosted artifacts

- Establish an owner-controlled release keystore, secure recovery copy and hosted
  secrets. Do not commit private keys/passwords. Record the certificate fingerprint
  and key-recovery responsibility outside the secret itself.
- Configure a non-debuggable release build and explicit release signing. Hosted
  CI must fail on missing signing inputs, a debug certificate or an unexpected
  certificate fingerprint. Verify the final packaged APK, not just an intermediate.
- Adapt `.github/workflows/release.yml` and `scripts/release_manifest.py` to build
  and describe release APKs. Retain JVM, Go race, web and browser verification;
  identify their tested build variants and smoke-test the actual release artifact.
- Replace hard-coded debug-signing/device-QA manifest text with accurate evidence
  references. Record source SHA, workflow, artifact hashes, version/code,
  certificate and manual QA report for the exact candidate.
- Validate the Linux archive from a clean extracted directory: documented startup,
  bundled paths/catalog/web, loopback bind and data-file location. Verify offline
  web reload and that personal API responses stay outside service-worker caches.
- Define the debug-to-release transition: different signing identities may require
  a separate installation or uninstall/reinstall. Rehearse export, verified backup
  and restore on disposable data first; never instruct users to uninstall their
  only copy of personal records. State which supported upgrade paths preserve data.

### M3: exact-candidate verification

Use [NATIVE_QA.md](NATIVE_QA.md) as the detailed script and append dated results
with device/model, OS, candidate SHA/hash, tester, pass/fail and linked defects.
Its older pending-test prose is historical; reconcile it with hosted evidence
rather than presenting all written tests as still unexecuted.

| Area | Minimum acceptance scenario |
| --- | --- |
| Install/update | Clean install; prior debug backup-to-release transition; release-signed RC to final update with the same key, increasing versionCode and intact data |
| Android versions/layout | Oldest supported Android version and a current Android version; narrow phone, large fonts, keyboard, Back, scrolling and TalkBack labels/focus for core logging and recovery |
| Offline workout | Entire routine/free-workout flow without API; valid zero/decimal loads; timer, review, finish, history and replay agree |
| Interruptions | Activity recreation, background/foreground, force-stop/reopen during active workout, draft and picker flows; values/deadlines recover and no duplicate finish occurs |
| Backup/files | Picker cancel/retry, malformed/oversized files, conflict rejection, repeated restore, safety-copy export and fresh-install recovery |
| Sync/companion | Two clients and disposable API; active state retained; routine/archive propagation, immutable-history conflicts and connection errors; browser reload/offline behavior |
| Failure feedback | Supported storage-failure injection shows no false success or partial local replacement; unreadable-state recovery preserves raw data |

Run at least three complete disposable workout sessions across multiple days to
exercise history date grouping and normal use between restarts. Record elapsed
time/session size for a realistic larger-history fixture and investigate visible
typing, scrolling or save stalls; do not claim performance from empty-state QA.

All automated suites must pass against the candidate; no unexplained missing or
skipped results. A check count alone is not evidence of device behavior. Block
release for data loss/corruption, incorrect history/volume, unrecoverable logging,
broken install/update/signing, or failure of a promised core offline operation.
Minor cosmetic defects may be deferred with an explicit impact and follow-up.

### M4: candidate promotion and release

1. Prepare an RC version and unused Android versionCode through
   `shared/release.properties`, matching web package/lock versions and release
   notes. Build and retain it on GitHub; run device QA on those exact files.
2. Fix blockers and repeat affected gates on a new candidate. Preserve successful
   reports beyond the workflow's current seven-day artifact retention, linked to
   their artifact hashes/source rather than a mutable filename.
3. Prepare `1.0.0`, an Android code higher than all distributed RCs, matching web
   versions and `prerelease=false`; write `docs/releases/v1.0.0.md`. Update README,
   architecture and QA status to distinguish verified behavior and known limits.
4. Build final assets on GitHub with publication disabled. Run final-artifact
   installation/update and core recovery smoke tests. Prefer promotion of these
   exact tested files; if publication rebuilds them, verify and smoke-test the
   resulting artifacts before making them available as stable.
5. Review the concrete release draft, hashes, signing/upgrade instructions,
   backup guidance and all gate evidence with the owner. Obtain authorization
   for 1.0 publication; earlier prerelease authorization is not a new stable-release
   publication request.
6. Publish only checked assets from the approved source. Verify tag/source,
   stable flag, download hashes, certificate and extracted-bundle startup.

Required delivery: release-signed APK, Linux API/web bundle, `SHA256SUMS.txt`,
certificate details, verification manifest, release notes, manual QA evidence and
known limitations. Do not include private keys or personal data in artifacts.

## Stop, recovery and follow-up

Do not promote a candidate with an unresolved blocker. If a published release has
a data-integrity defect, promptly mark it unsuitable in release notes, preserve
provenance and prepare a new patch version. Do not silently replace assets under
the same tag. Prefer a forward fix: Android downgrades and data-format rollback
are not assumed safe. Recovery guidance starts with preserving/exporting data.

After launch, triage installation, recovery and logging defects first. Carry
deferred cosmetic issues into a 1.0.x backlog; reconsider new features only after
the stable logging/recovery flow has real usage evidence.

## Working rules and immediate next action

This is a planning change only. No versions are bumped and no workflow is run.
Continue to use `node --max-old-space-size=128 scripts/check-light.mjs` for
lightweight implementation checks. Compilation, APK generation, Go tests/builds
and browser suites belong on GitHub-hosted infrastructure; device QA belongs on
the separate testing machine. No local native compilation or emulator runs.

Start with M0: reconcile current source changes/latest hosted build, record the
capability/defect inventory, and resolve display-name/signing ownership before
implementing release packaging. No further feature expansion is needed to start.

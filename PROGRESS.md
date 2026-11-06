# Progress

## 2026-11-05 — 100 exercises, 17 templates, 1.0.0-rc.2 local validation

- Expanded shared/catalog.json 60 -> 100 with 40 stable IDs (smith-bench-press, smith-incline-bench, dips, incline-cable-fly, dumbbell-pullover, close-grip-bench, t-bar-row, assisted-pull-up, assisted-chin-up, one-arm-pulldown, cable-pullover, hyperextension, landmine-row, standing-calf-raise, sumo-deadlift, good-morning, smith-squat, hack-squat, walking-lunge, glute-bridge, calf-press, machine-hip-thrust, front-raise, arnold-press, upright-row, shrug, machine-lateral-raise, landmine-press, incline-curl, concentration-curl, spider-curl, cable-hammer-curl, dumbbell-skullcrusher, cable-kickback, diamond-push-up, dead-bug, russian-twist, pallof-press, hanging-leg-raise, machine-crunch). All rep-based; no timed holds forced into kg x reps.
- Added shared/routines.json ppl-push, ppl-pull, ppl-legs, personal-legs, personal-back-biceps, personal-chest-triceps, personal-back-biceps-b, personal-chest-triceps-extra (each 7 distinct exercises, 3x10 @90s). Fresh workspaces seed 17; existing workspaces never reseeded.
- Android readability bump: 10sp -> 11sp (Eyebrow, nav labels, week-day labels, history dates), Metric label 11->12sp. Updated README/ARCHITECTURE counts, catalog.test.mjs (Máquina×Supino now 3 hits) and check_catalog_ui.mjs (1->3).
- Bumped shared/release.properties to 1.0.0-rc.2/code5, web versions, added docs/releases/v1.0.0-rc.2.md. release_config.py + 13 release tests pass.
- Local evidence: check-light 100/17 + 9/9 tests; npm test 9/9; vite build ok; go test ./... ok (race deferred to hosted due to missing gcc); browser 120 + 52 = 172 pass vs disposable Go API; native assembleDebug + 71/71 JVM tests pass, APK sha256 156bf35d... 
- VM attempt: SDK 36 + emulator 37.2.13 installed under /var/tmp/onepunch-android-sdk, JDK 17 Temurin, AVD onepunch-qa created. Emulator reaches gfxstream/Vulkan init but exits before adb boot_completed on this host; no UI harness run. Hosted signed candidate remains the gate for release-variant + race coverage.
- Pushed via SSH: origin git@github.com:zGabrielR1/onepunch.git; release/1.0.0-rc.2 at a1ddef4. Publication still blocked pending signed-candidate run + exact-artifact device QA.

## 2026-10-30 — pending verification finished locally

- Closed the two items left open on 2026-10-29: the browser rerun after the final
  CSS refinements, and native build/emulator evidence. Source unchanged except a
  test-harness fix in `scripts/check_android_ui.py` (described below).
- Web build and six domain tests passed; `go test -race ./...` passed. Rebuilt Go
  API served the rebuilt web client on 127.0.0.1:8098.
- 168 browser checks passed: 116 workflow checks at 1440/1024/768/390/320 and 52
  catalog/layout checks at 1440/768/390/320. The two suites need separate fresh
  API state files; a shared file carries synced workouts into the next suite and
  fails its single-workout history assertion.
- Capped local native build passed (3 GiB process-tree cap, no swap, 200% CPU,
  one worker, 30-minute limit): `assembleDebug` plus 71 JVM tests with zero
  failures/errors/skips. Debug APK 18,430,663 bytes, SHA-256
  e7c52e739e85a14b42c6980a74886df3ad52848f6702f9e5ad3c1b5cc85c05c3.
- Disposable emulator AVD `onepunch-qa` (Android 15, 720x1280 @320dpi) passed nine
  checks: accent-insensitive multiword search found the new machine press,
  equipment filter reported 1 of 60, entered 12.5 kg persisted exactly, the set and
  active workout survived a process restart and a 1.3 font-scale change, no history
  existed before finishing, exactly one completed set entered history and kept the
  new catalog ID. Report and screenshots in `build/qa-android/`; the large-font and
  filtered-library screenshots were reviewed and show no clipping.
- Harness fixes needed to reach those checks: tap the resume card using the bounds
  from the dump that found it (a running rest timer prefixes its label), and scroll
  the restored set row into view. Product assertions were not weakened.
- Emulator stopped and no onepunch systemd units remain active. No publication and
  no physical-device installation. Real-device QA of the signed RC, independent
  signing-key backup and stable promotion are still pending.

## 2026-10-29 — product quality priorities

- Recorded owner feedback and source-grounded improvement backlog in
  docs/PRODUCT_QUALITY_BACKLOG.md: exercise coverage, active-set readability,
  picker filtering/empty feedback and consistent visual hierarchy.
- Distinguished confirmed source concerns from unverified device layout bugs.
  Recorded exact-candidate experimentation loop and acceptance evidence.
- Documentation-only pass; no runtime code, catalog or personal data changed.
  No compilation, browser suite, installation or publication performed.

## 2026-10-27 — signed 1.0 RC verified

- Owner requested continuing the 1.0 plan and explicitly chose creation of a new
  release signing key. Created PKCS12 recovery files with 0700 directory/0600 file
  permissions under ignored build/signing-recovery; configured all five Actions
  secrets without exposing their values. Independent owner backup remains needed.
- Fast-forwarded main to reviewed RC source 3a7c705 so new workflows register.
- Signed run 40607494288 passed: release compilation, 70 JVM tests with no failures,
  errors or skips; 13 release-tooling tests; four Go race tests; 116 browser checks;
  web checks/build; APK release identity/certificate and extracted Linux smoke test.
- Downloaded onepunch-signed-candidate-40607494288 to build/signed-40607494288;
  every checksum passed. Debug RC run 40607216559 also succeeded.
- Prepared exact-artifact device QA report with real run/source/hash/fingerprint,
  leaving all manual results pending. No installation or publication performed.

## 2026-10-27 — implementing the 1.0 candidate

- Prepared 1.0.0-rc.1 / Android code 4 with matching web versions and RC notes.
  Reviewed existing manual rest, draft replacement, exercise comparison and strict
  local-envelope changes for inclusion; preserved gradlew's unrelated mode change.
- Added owner-release signing config, a hosted signed-candidate workflow,
  certificate/application/version/non-debuggable checks, exact asset hashes and
  extracted Linux archive smoke test. Debug workflow now rejects stable settings.
- Added stable promotion of existing files without rebuilding, requiring successful
  candidate provenance, matching checksums, expected certificate and exact-artifact
  device evidence. Promotion defaults to validation only; no publication performed.
- Added original-record export from unreadable native workspace/draft screens:
  raw values are preserved without parsing, with explicit recovery-file wording.
  Added two RecoveryTest JVM cases for hosted execution.
- Added distribution/upgrade instructions, client capability inventory, blocker
  ledger, signing operations and pending device QA template. Signing-key choice
  requested; repository had no signing secrets. Device tests remain pending.
- 13 dependency-free Python regression tests passed; lightweight shared/web checks,
  version consistency, actionlint workflow validation and whitespace checks passed.
  No local native compilation, dependency installation, emulator or browser suite.
- Reconciled earlier build-only run 40606230105 from d80e67e: successful; publication
  skipped. That source predates the new candidate and its results are historical.

## 2026-10-27 — 1.0 release plan

- Added docs/RELEASE_1_0_PLAN.md and linked it from README. Defined a native-first
  personal logger scope with web/API companion, milestones M0–M4, measurable
  recovery/device/cross-client gates, stable signing and upgrade requirements,
  exact-artifact release evidence, publication handoff and patch response.
- Distinguished recorded 0.3.0 hosted results from the current working tree and
  pending device QA. Preserved existing App.kt/WorkoutEditing.kt edits and the
  gradlew mode difference. No feature code, versions or workflows changed.
- Documentation reviewed against architecture, QA handoff and release scripts;
  no compilation, tests, device installation or publication needed for this plan.

## 2026-10-27 — requested hosted rebuild

- Added a `publish` workflow input; false runs all existing build/verification
  steps and retains checked APK/API/web files as the `onepunch-build` artifact
  without creating or replacing a release. Default release behavior is preserved.
- Lightweight shared-data and web domain checks passed locally. Hosted run
  40606230105 succeeded for d80e67e: 54 Android JVM tests, four Go race tests,
  116 browser checks, web domain/build and APK signature verification passed.
- Downloaded APK/API/web bundle to build/hosted-40606230105 and verified all
  SHA256SUMS entries. No release published; device UI testing remains pending.

## 2026-10-27 — 0.3.0 published and verified

- GitHub run 40558518832 passed and published v0.3.0 from commit
  738a12b287b4abf952bf29249a6f721d133265c7. Read back the published verification
  manifest: 54 Android JVM tests, zero failures/errors/skips; four Go race tests;
  116 browser workflow checks; web domain/build and APK signature checks passed.
- Attached a fresh 18,413,315-byte debug APK, Linux API/web bundle, certificate
  details, SHA256SUMS.txt and exact-source verification.json. APK SHA-256:
  fab3d7cda5acd19fadc02e8189bf061b8fda906fe19083273bda94e5143335b8.
- Confirmed the release is published (not draft) and marked prerelease. Repository
  remains private. Builds/checks/packaging/publication ran on GitHub; no local
  native compilation, dependency installation or emulator/device execution.
- New document-picker UI and interruption behavior still require device QA.
  Debug certificate changes may prevent in-place updates from previous APKs.
- Release: https://github.com/zGabrielR1/onepunch/releases/tag/v0.3.0
  Run: https://github.com/zGabrielR1/onepunch/actions/runs/40558518832

## 2026-10-27 — 0.3.0 feature and hosted-release preparation

- Added native accent-insensitive history search by workout/exercise/equipment/
  note and inclusive 30/90-day filters; totals and recorded loads follow results.
- Added native JSON backup export/import using Android document contracts. Export
  includes active inputs/timer and draft; import validates format/references/IDs,
  numeric fields, history validity and an 8 MiB limit before offering merge.
- Merge preview preserves finished-history immutability, local active/draft state,
  newer routine edits and equal-date conflicts as copies. Repeated merges avoid
  duplicate history/conflict copies. Restore atomically retains an exportable
  pre-restore safety snapshot; unreadable snapshots are never silently overwritten.
- Wrote 12 BackupTest cases and three HistorySearchTest cases for hosted execution.
  Added manual picker/interruption/device QA. No local compilation or JVM run.
- Added shared release identity and dynamic workflow/manifest/CLI version handling
  for 0.3.0 (Android code 3), with consistency checks before hosted publication.
  GitHub build/tests/release are pending at this preparation commit.

## 2026-10-27 — RC1 published after hosted verification

- Published private-repository prerelease v0.2.0-rc.1 from source commit
  a5f2e87f3306f53ed480f14784f69f0a8072725d. Hosted workflow 40547042171 passed.
- Evidence read back from the published verification.json: 39 Android JVM tests
  (zero failures/errors/skips), four Go race tests, 116 browser workflow checks;
  web domain tests/build and APK compilation/signature verification also passed.
- Attached fresh 18,364,000-byte debug APK, Linux API/web/catalog bundle,
  verification.json, certificate details and SHA256SUMS.txt. APK SHA-256:
  c23a053c9e5c30a1fadc3e7ffe31a2490f3c2f31f42c2ab233406e07e31bc81e.
- Verified the release is published (not draft), marked prerelease, with assets
  and metadata bound to the checked source. Repository visibility stays private.
- No native compilation, dependency install or emulator ran on this PC. Existing
  local artifacts were not replaced. Android device UI remains untested; release
  notes explain temporary debug signing and potential update incompatibility.
- Release: https://github.com/zGabrielR1/onepunch/releases/tag/v0.2.0-rc.1
  Run: https://github.com/zGabrielR1/onepunch/actions/runs/40547042171

## 2026-10-27 — hosted prerelease preparation

- Owner requested a release. Added a manual GitHub-hosted Ubuntu pipeline for
  Android compilation/JVM tests, web/Go/browser checks, signed-APK verification,
  Linux companion packaging and checksums/verification metadata. No local builds.
- Versioned native/web source as 0.2.0-rc.1 (Android code 2). Publication is gated
  on all checks and must use the checked workflow commit, never the older APK.
- Added Portuguese release notes, exact-source verification manifest and signing
  limitations for ephemeral debug certificates. Native device UI QA stays separate.
- Corrected the remaining browser sync routine-count expectation for nine seeds.
- GitHub repository is private and Actions is enabled; no visibility change.
  Hosted checks and publication were pending when this source commit was prepared.

## 2026-10-27 — expanded routine and exercise library

- Expanded three four-exercise seed routines to nine seven-exercise examples:
  Superior A/B, Inferior A/B, Empurrar, Puxar, Corpo inteiro A/B and Com halteres.
  Defaults remain editable and examples explicitly invite removing/adjusting
  exercises; they are not personalized plans or a requirement to perform all sets.
- Expanded the shared catalog from 18 to 27 stable exercise IDs. Added extensora,
  afundo para trás, elevação pélvica, abdominal reverso, tríceps acima da cabeça,
  face pull, rosca inversa, goblet squat and dumbbell Romanian deadlift.
- Added native searchable Modelos prontos and web template previews/copy controls.
  New workspaces get expanded seeds; existing saved routines/history are untouched.
  Selecting a template creates a fresh editable copy, using native draft recovery.
- Added a native template-copy regression case (not run). Updated browser-suite
  count assertions to derive from shared data, including the additional rosca.
- Lightweight JSON-only validation passed: 27 unique catalog IDs, nine unique
  routine IDs, seven unique referenced exercises each, field lengths and numeric
  bounds. No dependency installation, compilation, bundling, native/JVM tests,
  browser/device suites or APK generation. Updated remote QA and API restart note.

## 2026-10-27 — session progress, naming and finish review

- Added editable native active-workout names with local recovery, nonblank finish
  validation and trimmed history titles. Routine names and past history are untouched.
- Added progress based on actual set rows: valid done sets count, unchecked and
  invalid marked-done sets do not. Empty sessions show no fabricated completion.
- Replaced the finish totals dialog with a scrollable exercise-by-exercise review
  of included sets (original row numbers), excluded counts, volume, time and note.
  Preview and finish share one selection helper; final-save errors remain inline
  with the review/active session retained. Back returns to training without saving.
- Failed discard and exercise-removal saves now retain their confirmations.
- Added seven WorkoutReviewTest regression cases and remote QA instructions.
  Reviewed source and whitespace only; no compilation, JVM/device testing,
  dependency installation or APK/artifact generation on the development PC.

## 2026-10-27 — active exercise order and replacement

- Added up/down exercise movement to the native training menus, preserving set
  identities, input strings, completion flags and current rest deadline.
- Added confirmed replacement of pending sets with another catalog exercise.
  Done sets remain under their original exercise; new unchecked rows use fresh
  IDs and only the target exercise's recorded history for prefills. Pending count
  (or planned count when no rows remain), target reps and rest provide defaults.
- Duplicate targets and fully completed source exercises are blocked. Selection,
  choosing another target and cancellation do not mutate the workout. Successful
  persistence precedes dismissal; failures retain the confirmation with an error.
- Finished history now orders valid completed sets by the visible exercise order,
  including later-added rows. Routine templates, old history and sync schema are
  unchanged; modified active sessions remain local and recover through persistence.
- Added eleven WorkoutEditingTest regression cases and remote QA steps. Reviewed
  source/call sites and whitespace only; no JVM tests, Gradle, compiler, dependency
  installation, emulator/device use or regenerated APK/artifacts on this PC.

## 2026-10-27 — durable native routine drafts

- Added local draft recovery for new, edited, archived and duplicated routines.
  Accepted changes persist separately from saved routines; Home/Treinos offer
  resume, and Continuar depois keeps the draft through app restarts. A pending
  draft is resumed instead of being overwritten by another edit request.
- Routine save and draft removal share one preferences commit. Confirmed discard
  removes only the draft. Read/write errors remain visible; unreadable/unknown
  drafts are preserved and block editor replacement without blocking workouts.
- Added conflict protection against changes to the original routine during sync
  or archiving: direct save is blocked; save-as-copy retains both versions.
  Drafts never enter Workspace/sync, and invalid draft counts cannot be committed.
- Added eight RoutineDraftTest cases for the other machine; not compiled or run.
  Reviewed persistence, editor callbacks, identity/conflict handling and whitespace
  only. No compiler, dependency installation, Gradle, device/emulator run or APK.
- Expanded docs/NATIVE_QA.md with interruption, recovery, conflict and fault cases.

## 2026-10-27 — exercise history and workout replay

- Replaced the native exercise info dialog with scrollable details: description,
  session count, highest recorded load, highest per-session volume, and dated
  sets from actual history. Empty history has no invented metrics.
- History is reachable from the library, active exercise menus and exercise names
  in workout detail. Session links open the full original workout. Detail dates
  include years; returning to an active exercise never adds a duplicate.
- Added repeat-workout action: new local workout/set IDs, recorded kg/reps as
  unchecked inputs, fresh time, no copied note/deadline, and explicit 90-second
  default rest because history does not record rest intervals. Existing active
  workouts are resumed rather than replaced. Saving precedes navigation.
- Shared source-only history helpers filter valid completed sets and leave
  original finished records unchanged. Latest-set prefills now use that filter.
- Wrote five JVM regression cases covering history filtering, zero-load replay,
  fresh IDs, unchanged history, active-session protection and incomplete-prefill
  rejection. These cases have NOT been compiled or run; added remote QA steps.
- Reviewed source and whitespace diff only. No Gradle, compilers, dependency
  installation, emulator/device runs or new build artifacts on this PC.

## 2026-10-27 — routine lifecycle and active workout corrections

- Native routine cards now offer duplication and confirmed archiving. Copies
  open as unsaved creation drafts with distinct IDs; active/archived filters,
  counts, empty states and restore controls complete the routine lifecycle.
- Archive/restore preserves finished history and active sessions, persists before
  updating UI, and advances updatedAt for the existing sync merge contract.
- Active workout exercise menus now offer rest interval selection for future
  completed sets and confirmed removal of individual sets, including completed
  sets. The current timer deadline and original routine are preserved.
- Routine save timestamps advance beyond their original value; copied routines
  become visible in the active filter after saving, including copies of archives.
- Corrected history detail so a workout note appears once after all exercises.
- Reviewed source paths for dismissal, saved-state transitions, history isolation,
  IDs and timestamps. No compilation, JVM tests, device run, dependency install
  or artifact generation in this milestone. Build/device verification is pending
  on the other machine. Added docs/NATIVE_QA.md with the manual QA handoff.

## 2026-10-27 — native routine editing

- Owner clarified: continue native implementation, skip compilation and build
  verification. Development is authorized even without executable checks.
- Native routine editor: confirmation before dismissing changed drafts, move-down
  control alongside move-up, and inline set/repetition range messages.
- Reviewed source changes and the sheet's dismissal callback. No Kotlin/Gradle
  compilation, APK generation, device tests or build-pass checks performed.
- An unrelated web dependency installation was stopped after clarification;
  no web bundle or native artifact was generated during this continuation.

## 2026-10-27 — resource-conscious continuation

- Recorded the owner's constraint against native/APK compilation during routine
  development on this PC in AGENTS.md, CURRENTTASK and README.md.
- Added dependency-free `scripts/check-light.mjs` and `make check-light`: validate
  shared catalog/template references and bounds, then run web domain tests
  serially with a 128 MiB old-generation heap per Node process. This is not a
  system-wide memory cap and does not establish safe APK build limits.
- Verification: lightweight command passed for 18 exercises, three templates
  and all four domain tests. No Gradle, Go compilation, package installation,
  browser automation or artifact regeneration was performed.
- Existing Android wrapper mode difference was left untouched. Prior native
  and browser results below describe the previous build, not a new verification.

## 2026-10-26 — first working foundation

- Empty project initialized with Kotlin/Jetpack Compose Android, React/JavaScript
  browser UI and standard-library Go API. Portuguese (Brazil), kg.
- Shared exercise catalog (18), editable routine templates (3), and one branding
  file for the intended rename. Original cream/charcoal/lime visual system.
- Functional logging loop in both clients: routines, free sessions, exercise/set
  editing, completed-set validation, previous performance, rest timer, local
  session recovery, finish review and history. Real recorded data drives metrics.
- Optional sync exchanges routines/finished workouts; active sessions remain local.
  Go store validates records, preserves history, serializes concurrent merges and
  persists atomic snapshots. Web service worker supports offline shell reload.
- Initial verification: native APK built, domain/serialization tests passed; Go
  race tests passed; React build and browser workflow checks passed. Final results
  are recorded below once the final pass completes. Android device UI unverified.

## Final verification

- Android assembleDebug + 7 Kotlin/JVM tests: pass; debug APK signature verified,
  min SDK26, target SDK36. Catalog/templates/brand are present in the APK.
- Go: 4 test cases pass with the race detector; native Linux executable built.
- Web: 4 domain cases and production build pass. 116 real-browser checks pass at
  five widths, including routine creation, set logging, rest timer, offline reload
  recovery, finish/history, library filtering, discard, cross-client sync through
  the real Go API, API-cache exclusion and preservation of unreadable local data.
- Browser screenshots reviewed. Android device UI testing remains unperformed.
- Ready outputs: build/onepunch-debug.apk, build/onepunch-api, web/dist.
- Owner requested source push to GitHub and removal of generated dependencies /
  Android build caches. No source ZIP or Drive upload performed by the agent.

## 2026-10-27 — manual native rest controls (unreleased)

- Active exercise menus can start/restart the configured rest without completing
  a set; zero-second intervals disable the action. No set/history changes.
- Added −30s beside +30s, stacked to keep timer controls compact. Shortening to
  zero clears the persisted deadline. Extension uses actual click time if the
  previous deadline expired, fixing a stale-tick race that could hide the timer.
- Added three Kotlin test cases for deadline changes, expired/absent timers and
  preservation of the session. These tests remain unrun under the native-build
  restriction. Device UI/restart QA remains pending.
- Passed `node --max-old-space-size=128 scripts/check-light.mjs` (27 exercises,
  nine templates, web domain test file) and `git diff --check`. No compilation,
  APK generation, installation or publication performed.

## 2026-10-27 — three-agent native continuation (unreleased)

- Owner explicitly requested three additional agents. Separate agents implemented
  routine-draft replacement, exercise-session comparisons and persistence safety;
  the main agent integrated both features into the native UI and reviewed diffs.
- Routine editor replacement keeps exercise position, targets (including unfinished
  counts), rest interval and metadata. Duplicate/unknown movements are rejected.
  Changes use the existing persisted draft callback before closing the picker.
- Exercise details compare the two latest valid saved sessions: sets, repetitions,
  maximum recorded load and volume with raw differences. No comparison appears
  until two sessions exist; no outcome/strength claims or predicted records.
- Local workspace and backup readers now reject unsupported/missing workspace
  versions, malformed/missing active fields and invalid sync metadata instead of
  silently losing data. Versionless sync snapshots remain compatible.
- Added 11 Kotlin tests (five routine replacement, three comparisons, three
  serialization regressions); they remain uncompiled/unrun. Source review and
  diff whitespace check passed. The lightweight check passed: 27 exercises,
  nine templates and web domain test file. No native build, APK, device install,
  Go compilation, browser suite, source push or publication performed.
# 2026-10-30 — web immutable-history sync continuation

- Found web sync reused the routine timestamp merge for finished workouts,
  allowing a conflicting remote record to replace local history. Added a
  separate immutable-history merge comparing explicit wire fields, normalizing
  numeric inputs and retaining local objects for identical records. Conflicts
  reject the entire merge, including routine updates and sync metadata.
- Moved merge validation outside the deferred React updater so failures reach
  the sync feedback handler. The response merges against current rendered state
  to preserve workout changes made while the request was in flight.
- Added regression cases for older/equal/newer conflicts, set values/order and
  metadata, numeric equivalents, repeated sync and unchanged local state.
  Browser regression covers conflict feedback without a page error, byte-for-byte
  unchanged local storage, then a successful real-API retry.
- Verified: shared catalog/templates plus nine domain tests; production web
  build (467 MB peak); 120 browser workflow checks across five widths (881.1 MB
  peak). Build/browser jobs used the enforced 3 GiB process-tree cap, zero swap
  and disposable API state. No native changes, publication or physical install.

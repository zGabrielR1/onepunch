# Progress

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

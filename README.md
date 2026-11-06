# Onepunch

A working first version of a personal workout planner and logger, inspired by
the training flows of apps such as Hevy. The name is provisional.

**Native Android:** Kotlin + Jetpack Compose. **Web:** React + JavaScript/Vite.
**API:** Go, compiled to a native executable. Portuguese (Brazil), kilograms.
No React Native or WebView is used in the Android app.

The [1.0 release plan](docs/RELEASE_1_0_PLAN.md) defines the stable-release scope,
signing/upgrade work, device QA gates and delivery checklist.
See [release operations](docs/RELEASE_OPERATIONS.md) for signed hosted candidates
and exact-artifact promotion, and [readiness status](docs/RELEASE_STATUS.md) for
the client capability inventory and open release blockers. The source is now a
1.0.0-rc.1 candidate; 0.3.0 below remains the last published version.

## What works

- Android history search by workout/exercise/equipment/note, with accent-insensitive
  matching and 30/90-day filters. Counts, volume and recorded loads follow filters.
- Android Backup e dados exports/imports JSON through the system file picker,
  including active inputs and routine drafts. Imports show a merge preview,
  preserve immutable history and keep an exportable pre-restore safety copy.

- Shared library of 100 exercises and 17 editable routine examples, each with
  seven exercises: Superior A/B, Inferior A/B, Empurrar, Puxar, Corpo inteiro A/B,
  Com halteres, PPL Empurrar/Puxar/Pernas, and the five-day personalized split
  (Pernas, Costas e bíceps, Peito e tríceps, Costas e bíceps B, Peito/tríceps e
  extras). Exercise choices and initial set/rep/rest values are editable.
- Modelos prontos in Treinos opens the full template library, including on an
  existing installation. Usar modelo opens a new editable copy before saving;
  existing routines, archived items and workout history are never replaced.
- Create/edit routines, set targets and choose rest intervals.
- Native routine duplication, archive/restore, and confirmation before discarding
  routine edits. Archived routines retain their history and remain syncable.
- Native routine drafts save locally while editing, can be resumed after restart,
  and offer saving a copy when the original routine changed during sync.
- Start a routine or a free workout; add exercises/sets and log kg/reps.
- Mark sets complete, see session volume and compare previous performance.
- Timestamp-based rest timer, skip/+30 seconds, and local session recovery.
- Native exercise menu: change rest for upcoming sets or remove an individual
  set with confirmation, without modifying the original routine.
- Reorder exercises during training or replace pending sets with another exercise;
  completed sets remain under their original exercise and the timer stays intact.
- Rename the active workout, track completed/remaining sets, and review exactly
  which sets and notes will be saved before finishing.
- Finish/review a workout, history, weekly activity and best recorded loads.
- Native exercise details show recorded sessions, highest load and highest
  session volume; repeat a finished workout as a new session with unchecked sets.
- Local-first persistence in both clients; the production web shell also caches
  for offline reload after its first successful online load.
- Optional personal-server sync of routines and **finished** workouts. An active
  workout stays on its originating device and is preserved during sync.

The initial state has no fabricated workouts, streaks or records. Templates are
examples that the user can change; this version does not generate adaptive plans.

## Try the already built versions

[Download OnePunch 0.3.0](https://github.com/zGabrielR1/onepunch/releases/tag/v0.3.0)
for the current debug APK and Linux API/web bundle, with checksums and exact-source
verification metadata. GitHub-hosted checks passed: 54 Android JVM tests, four Go
race tests and 116 browser workflow checks, plus web tests/build and APK signature
verification. Android device UI QA remains pending. Debug signing may prevent an
in-place update from an older APK; see the release notes before installation.

Current prerelease pipeline: update `shared/release.properties`, matching web
package versions and release notes, then run **Hosted prerelease** from GitHub Actions. Android
compilation, JVM/Go/web/browser checks and packaging run on GitHub-hosted Ubuntu,
not the development PC. Publication is blocked when a check fails. The release
contains a debug-signed test APK, Linux API/web bundle, checksums and verification
metadata tied to the exact commit. See [0.3.0 notes](docs/releases/v0.3.0.md)
for signing/update limitations. Device UI QA remains separate. The workflow checks
version consistency and refuses to reuse a published version or existing tag.

The older local build artifacts on this checkout predate the GitHub releases:

- `build/onepunch-debug.apk` — installable Android debug build, Android 8+.
- `build/onepunch-api` — native Linux API executable.
- `web/dist/` — compiled React web client, served by the Go executable.

To run the built web app from the project directory:

```sh
cd server
../build/onepunch-api
```

Open **http://127.0.0.1:8097**. API and web share the same origin. It writes sync
records to `server/data/state.json`; this runtime directory is ignored by Git.

To install the APK yourself with a connected Android device:

```sh
adb install -r build/onepunch-debug.apk
```

No device installation or public deployment is performed automatically.

## Build from source

### Lightweight development on this PC

Owner update (2026-10-29): local compilation and testing are authorized with a
maximum RAM limit. In local WSL/Linux, with JDK 17 and Android SDK 36 installed:

```sh
sudo bash scripts/local-limited.sh bash scripts/check-android-local.sh
```

This verifies and enforces a **3 GiB total process-tree memory cap**, disables
swap for the job, limits CPU use to two cores' equivalent and stops the job after
30 minutes. Gradle runs one worker, with a 1280 MiB heap and in-process Kotlin
compilation. The SDK defaults to `build/local-tools/sdk`; set `ANDROID_HOME` and
`JAVA_HOME` in the invoked script when using another installation. These scripts
need systemd/cgroup v2; they stop rather than silently run without the cap.
Run heavy checks serially. Existing hosted release workflows remain available.
Compiler snapshots and caches use disk-backed Linux `/var/tmp`, avoiding both
slow Windows-mounted cache I/O and this machine's RAM-backed `/tmp`.
No physical-device installation or publication is performed by these checks.

The owner uses this PC while development runs. Start with the dependency-free
check before running the capped compilation:

```sh
node --max-old-space-size=128 scripts/check-light.mjs
```

It validates shared catalog/routine data and runs web domain tests one at a time.
It does not install packages, bundle assets, invoke Gradle/Go or open browsers.
The 128 MiB setting limits each Node process's old-generation JavaScript heap;
it is **not** a total RAM limit. `make check-light` runs the same checks.

The commands below are manual build options. `make test` compiles Go tests;
`make run` builds both web and API. Run compilation inside the capped wrapper on
this PC. Existing artifacts remain available, but source-only checks do not
refresh or verify them.

Requirements: Node 22+ and npm, Go (tested with 1.26), JDK 17, Android SDK platform
36/build-tools 36.0.0. The Android Gradle wrapper is included; Android Studio can
open the `android/` directory. Keep SDK paths in ANDROID_HOME or local.properties.

```sh
npm ci --prefix web
make web api
make test
make android
```

`make android` builds the APK, runs Kotlin/JVM tests and copies the artifact into
`build/`. Override the compiler if needed: `make api GO=/path/to/go`.

For web development, start the Go API from `server/` in one terminal and run
`npm run dev --prefix web` in another. Vite serves **http://127.0.0.1:8788** and
proxies `/api` to the Go server on 8097. Offline service-worker caching is enabled
only in the production build, not the Vite development server.

## Connect the native app to the web workspace

The Android app is fully usable without a server. For optional synchronization:

1. Run the Go API on your computer.
2. In the app, tap the cloud icon and enter your personal API address.
3. The Android emulator default is `http://10.0.2.2:8097`.
4. For a USB-connected phone, `adb reverse tcp:8097 tcp:8097` lets the app use
   `http://127.0.0.1:8097` without exposing the API to the network.
5. Tap **Sincronizar** in each client when you want to exchange records.

The service binds loopback by default and has one personal workspace. It has no
public account/authentication system and should stay local. Completed workouts
are merged by stable ID and cannot be overwritten with different content.
Routine edits merge by `updatedAt`; clients should have correctly set clocks.
There are no cloud accounts or background sync promises in this first version.

## Rename it

Edit **`shared/brand.properties`**, then rebuild both clients. This changes the
web wordmark/title/footer and the Android application label/header. Keep storage
keys and `applicationId` stable if you want existing installations to retain their
workout data. The source folder/build project can keep its current internal name.

## Verification

0.3.0 native source was compiled and JVM-tested remotely by GitHub Actions; it was
not compiled or device-tested on this PC. See [native QA handoff](docs/NATIVE_QA.md)
for the remaining device checks. The older local APK predates these changes.

- `npm test --prefix web`: domain checks for logging, invalid inputs, volume,
  previous-set prefills, sync merging and date grouping.
- `cd server && go test -race ./...`: persistence, immutable history, malformed
  requests, concurrent merges, and static/API routing.
- Android `testDebugUnitTest`: domain rules plus local JSON and sync-payload
  interoperability checks. These are JVM tests, not device UI tests.
- `scripts/check_web.mjs`: real browser workflows at 1440/1024/768/390/320,
  including offline reload, routine editing, history and cross-client API sync.

To run browser checks, build the web and API, start a server with a **new empty
state path** on port 8098, and provide Playwright (or PLAYWRIGHT_MODULE) plus an
optional CHROME_PATH. The script creates test workout records; do not point it
at your personal data file. ONEPUNCH_QA_URL changes the test origin. Screenshots
go to `/tmp/onepunch-qa`.

Android compilation/JVM tests are verified here; no emulator or physical-device
UI run has been performed in this environment. Source and build outputs remain
in this project; nothing is deployed or archived.

## Implementation notes

See [ARCHITECTURE.md](ARCHITECTURE.md) for formats and state boundaries. Standard
setup references: [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler),
[Android Gradle compatibility](https://developer.android.com/build/releases/about-agp),
[Go downloads](https://go.dev/dl/).

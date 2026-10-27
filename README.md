# Onepunch

A working first version of a personal workout planner and logger, inspired by
the training flows of apps such as Hevy. The name is provisional.

**Native Android:** Kotlin + Jetpack Compose. **Web:** React + JavaScript/Vite.
**API:** Go, compiled to a native executable. Portuguese (Brazil), kilograms.
No React Native or WebView is used in the Android app.

## What works

- Shared library of 18 exercises and three editable routine templates.
- Create/edit routines, set targets and choose rest intervals.
- Start a routine or a free workout; add exercises/sets and log kg/reps.
- Mark sets complete, see session volume and compare previous performance.
- Timestamp-based rest timer, skip/+30 seconds, and local session recovery.
- Finish/review a workout, history, weekly activity and best recorded loads.
- Local-first persistence in both clients; the production web shell also caches
  for offline reload after its first successful online load.
- Optional personal-server sync of routines and **finished** workouts. An active
  workout stays on its originating device and is preserved during sync.

The initial state has no fabricated workouts, streaks or records. Templates are
examples that the user can change; this version does not generate adaptive plans.

## Try the already built versions

The build artifacts on this checkout are:

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

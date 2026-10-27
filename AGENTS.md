# Onepunch — provisional name

A gym/workout planner and logger. Native Kotlin + Jetpack Compose Android app,
Go API and React/JavaScript web client. Portuguese (Brazil), kg. Work stays in this
project. The owner intends to rename it; shared/brand.properties owns branding.

Read CURRENTTASK, PROGRESS.md and ARCHITECTURE.md before continuing.

## Product invariants

- Both clients must work locally without an API. Persist incomplete workout
  inputs, completed sets and rest-timer deadlines across restarts/reloads.
- Only valid, explicitly completed sets enter workout history and volume.
  Weight zero is valid; missing weight is not. Never seed fake workout history.
- Routine templates are editable examples, not personalized training advice.
- Keep shared/catalog.json, shared/routines.json and the documented sync contract
  compatible with both clients. Never sync the in-progress local workout.
- Finished history is immutable on the API. Routine sync uses updatedAt; this is
  a personal single-user/single-process service, not public multi-tenant hosting.
- Never silently replace unreadable local data. Keep original data available.
- Service worker caches only the app shell, never personal API responses.
- No publishing, device installation or code archives unless requested.

## Structure and checks

- web/src/lib/model.mjs: web domain and persistence helpers; React UI in App.jsx.
- android/app/src/main/java/app/onepunch/: Kotlin domain, JSON store and Compose UI.
- server/: standard-library Go service with atomic JSON persistence and race tests.
- scripts/check_web.mjs: browser checks; use an empty disposable API data file.
- README.md: setup and rename instructions. ARCHITECTURE.md: contracts and limits.

After relevant changes: npm test --prefix web; npm run build --prefix web;
(cd server && go test -race ./...); gofmt for Go files. Native changes require
./gradlew :app:assembleDebug :app:testDebugUnitTest from android/ with JDK17/SDK36.
Report JVM/build verification separately from real Android device UI testing.
Update CURRENTTASK and PROGRESS.md with evidence. Preserve existing user data.

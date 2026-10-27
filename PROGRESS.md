# Progress

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

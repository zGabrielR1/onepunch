# RC.4 physical-device QA

Use the signed RC.4 APK and verification.json from the same successful candidate run. Record source, run ID, APK SHA-256 and owner certificate in v1_0_0_rc_4.json before testing. Record the phone model, Android version, tester, time and evidence paths. Keep every unexecuted scenario pending.

## Setup and evidence

Use the owner-authorized connected test phone, selected by explicit ADB serial. Never use the emulator-only check_android_ui.py on a physical phone: it assumes debug run-as access and changes emulator display settings. Check installed version first. Do not uninstall, clear data or reset the phone. If the installed signing certificate prevents an update, preserve it and report the blocker rather than remove the app. Use app JSON exports to inspect non-debuggable release data.

Save screenshots, UI hierarchy and observed results under ignored build/qa-device-rc4/. Only test evidence belongs there; never commit personal backup data. File-picker interactions, TalkBack and multi-day scenarios require actual observations; leave them pending when unavailable.

## First connected-phone pass

1. Record device/model/OS, screen size and font scale; inspect installed Onepunch version. Install with replacement only after checking the exact signed APK identity. Record install result. For the signed-update check, capture/export RC.3 state before installing RC.4 and compare routines, history, active inputs, completed sets, timer deadline and routine draft afterwards. An installation without RC.3 pre-state cannot establish this check.
2. Open home, routines, exercise library and history. Expect 100 catalog exercises and 17 template examples on a fresh workspace, no fabricated workout history. Existing workspaces must retain their routines without reseeding.
3. Start a disposable free workout. Search without accents and filter by equipment. Log zero kg and decimal-comma kg, leave another input incomplete, complete a set and start rest. Capture keyboard-open and completed/pending rows. Rotate, background and force-stop/relaunch; verify the same inputs, completion and absolute timer deadline. Do not change global phone settings without preserving and restoring original values.
4. Use manual rest start/restart and −30/+30 seconds; observe expiry. Reorder exercises and replace pending sets while preserving completed rows. Rename the workout, inspect finish review and save exactly once. Verify only explicitly completed valid sets enter history and volume.
5. Log assisted pull-up/chin-up: kg de assistência labels, zero lifted volume, no highest-load records, persistence and accurate finish review. Test a mixed ordinary/assisted session.
6. Edit a routine draft, restart and resume it, replace an exercise and confirm targets/order survive. Archive/restore and duplicate without changing the original history.
7. Export a JSON backup with the system picker, cancel once and retry. Import preview/cancel must not mutate data; confirmed restore preserves immutable history and local active/draft priority. Repeated import must not duplicate history. Export the pre-restore safety copy. Keep malformed-file and storage-failure scenarios pending unless actually exercised safely.
8. Test narrow layout, enlarged fonts, keyboard reachability, TalkBack labels/focus, history search/date bounds and exact saved values. Capture screenshots at each reported issue. Restore any changed device setting afterwards.
9. Test optional local API sync with disposable data; active workouts and drafts stay local. Verify conflict feedback and no partial merge using the controlled scenarios in ../NATIVE_QA.md.

## Completion boundary

Link each result to screenshots/observations and the exact APK hash. Record partial results without marking the overall report passed. One phone does not prove Android 8/current-Android coverage; one session does not prove multi-day recovery. Required checks remain defined by device-report.template.json and promote_release.py. Independent signing-key backup confirmation remains separate.

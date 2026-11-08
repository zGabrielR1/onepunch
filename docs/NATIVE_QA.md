# Native QA handoff

## Local development QA policy — 2026-10-29

The owner now authorizes local builds and UI tests with a maximum RAM limit,
superseding the historical hosted-only instructions below. Run Android checks
through `scripts/local-limited.sh` and `scripts/check-android-local.sh`: a verified
3 GiB process-tree cap, no swap, one Gradle worker, CPU quota 200% and a 30-minute
timeout. Compiler/cache I/O uses Linux temporary storage; APK and JVM reports
are copied back into `android/app/build/`. The native check refuses an uncapped
invocation.

`scripts/check_android_ui.py` targets only `emulator-5580` with AVD name
`onepunch-qa`. It checks a new catalog exercise, logging, process recovery,
font-scale changes and completion/history using disposable data, with screenshots
and an APK hash report under `build/qa-android`. It does not target a phone.
This development debug APK and emulator evidence do not replace signed-candidate
physical-device QA or authorize publication. See `PRODUCT_QUALITY_BACKLOG.md` for
the quality priorities and experimentation loop.

## 1.0 candidate additions (pending)

- Use the exact candidate APK/hash from hosted artifacts; complete
  docs/qa/device-report.template.json after running the detailed scenarios below.
  Check clean installation, debug-backup transition, signed RC-to-final update,
  Android 8/current Android, narrow phone, large fonts and TalkBack core controls.
- Inject unreadable workspace/draft data into a disposable test installation.
  From the blocked workspace screen or blocked draft Backup e dados screen,
  export original records. Verify exact original strings, including malformed JSON,
  are present in originalPreferences, with no server setting. Cancelling/retrying,
  process recreation and output failures must never claim success incorrectly.
  This recovery document is not a normal backup: attempting normal import must
  reject it without changing state. Export must not mutate local preferences.
- Run three complete disposable workout sessions across multiple days; record
  behavior with larger history and confirm date grouping/recovery. Record actual
  device/model/OS, source, run, hashes, results and any open blockers.
- Historical 0.3.0 and d80e67e hosted passes do not validate current native changes.
  Compile/JVM checks remain hosted-only; no local emulator or APK builds.

0.3.0 (source 738a12b) passed hosted Android compilation, 54 JVM tests, four Go race
tests and 116 browser checks. Device UI QA below is still pending. Do not compile
or launch an emulator on the development PC; use the separate testing machine.
Use the fresh APK from the GitHub 0.3.0 release, not the older local build/ APK.
The temporary debug signing certificate may differ from prior APKs; use a test
device/profile and retain any existing installation with personal workout data.

## 0.3.0 device checks

1. In Histórico search by workout title, exercise, equipment and notes, including
   uppercase and unaccented Portuguese. Check empty results, clearing the query,
   and Todos/30/90-day bounds. Totals and recorded-load rows must follow the filter;
   the underlying history must remain unchanged.
2. In Backup e dados export to local storage. Cancel the picker once, retry, then
   inspect the JSON on a test device. Confirm routines, valid history, unfinished
   active strings/rest deadline and the routine draft are present. No API request
   should occur. The file is plaintext personal data; keep test records disposable.
3. Choose that file for restore. Merely selecting it must not alter any data.
   Inspect the counts/updated-routine warnings, cancel once, then confirm. Existing
   local active/draft take priority; a fresh workspace should recover both.
4. Repeat import and verify no duplicate history. Check older/newer routine edits,
   equal-date routine conflicts (copies), and an active ID already finished locally
   (must not reopen). Changed content under an existing workout ID must reject the
   entire operation, with no partial routine/workout import.
5. Export the pre-restore safety copy and confirm it describes the exact previous
   workspace and draft. It is a recovery file, not an automatic rollback action.
   Storage errors must not dismiss as success or clear local state.
6. Try malformed JSON, wrong versions, fractional set targets, duplicate IDs,
   unknown catalog entries, malformed active/draft values, invalid saved sets and
   files over 8 MiB. Expect visible errors and no local mutation. Unfinished active
   weight/repetition strings must still round-trip without being marked done.
7. Rotate/recreate the activity while a picker/file operation is open. A cancelled
   or interrupted export may need retry; it must never claim an empty/partial file
   is successful. Imports must not apply themselves after selection/interruption.
8. Check narrow phones and large font settings for top-bar data actions, history
   filters and the scrollable restore preview. Compile/tests run only on GitHub.

## Build on the testing machine

Use JDK 17 and Android SDK 36. From `android/`:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

On Windows use `gradlew.bat` instead of `./gradlew`. Install the resulting debug
APK on a test device using a disposable workspace. Never overwrite personal data
for these checks. The commands above were run on the hosted GitHub runner for 0.3.0,
not on the development PC. Repeat only when further code changes require it.

## Expanded routine examples

1. On a fresh disposable workspace expect nine routines, each with seven distinct
   exercises: Superior A/B, Inferior A/B, Empurrar, Puxar, Corpo inteiro A/B and
   Com halteres. Expect 27 catalog entries and no invented workout history.
2. Upgrade an existing workspace containing edited/archived routines, history,
   active inputs and a routine draft. All existing records must remain unchanged;
   expanded templates are available through Modelos prontos without reseeding.
3. In Treinos open Modelos prontos, search by name/exercise/equipment, inspect all
   seven exercises and their editable defaults, then use a model. Expect an unsaved
   creation draft with a fresh ID; edit and save. Repeat from the same model and
   confirm a separate ID each time. Cancel/discard and confirm no routine is added.
4. Choose a model while another draft is pending. Expect the existing draft to
   resume rather than being replaced. Unreadable-draft protection still applies.
5. Confirm the nine new catalog entries render and can be logged: cadeira
   extensora, afundo para trás, elevação pélvica, abdominal reverso, tríceps acima
   da cabeça, face pull, rosca inversa, agachamento goblet and romeno com halteres.
6. Restart the personal API using the updated catalog, then sync a saved template
   copy with new exercise IDs and view it on the web companion. Inspect the web
   Modelos prontos preview/editor path too; existing routines/history stay intact.
7. Run the added template-copy RoutineDraftTest case on the other machine. JSON
   structure/reference validation already passed here; no native/web builds or
   device/browser suites were run for this expansion.

## Routine management

1. Create a routine and change its name, note, set/repetition counts and exercise
   order. Move exercises both up and down, including the first/last boundaries.
2. Enter empty/out-of-range counts. Expect range messages and disabled save.
3. Close a changed draft with the close icon and Android Back. Expect a discard
   confirmation; continuing should retain edits. An unchanged draft closes directly.
4. Use the routine card menu to duplicate. Expect a creation draft with a copied
   name and exercises. Save after editing; confirm the original is unchanged and
   both routines appear in the active list. Cancel a copy without changing data;
   confirm it was never added. Repeat from an archived routine.
5. Archive a routine, cancel once, then confirm. Expect it under Arquivadas, absent
   from the home list, with its finished history intact. Restore it and restart
   the app; expect the restored state to persist.
6. Archive a routine while its workout is active. Resume the workout and confirm
   all inputs, completed sets and the timer remain intact.
7. Sync to a disposable API workspace and sync a second client. Confirm archive
   and restore propagate, the copy has a distinct ID, and active workouts never sync.

## Routine draft recovery

1. Start creating/editing a routine. Change its name, note, exercise order, counts
   and rest interval; leave a set/repetition field empty. Use Continuar depois.
   Expect a Retomar edição card on Home and Treinos, without changes to the saved
   routine. Resume and confirm all accepted values, including the empty field.
2. Rotate the phone, recreate the activity, and force-stop/reopen the app while
   editing. Expect the latest accepted draft to remain available from the card.
   Check new routines, existing routines, archived routines and duplicated drafts.
3. With a draft pending, request a different edit, creation or duplication. Expect
   the existing draft to resume instead of being replaced. Save it or explicitly
   discard it, then create the other draft.
4. Save a valid draft. Restart; expect the saved routine once, with no draft card.
   Discard a changed draft using close/Back and confirm the original routine stays
   unchanged and the draft does not return after restart. Continuing later must
   keep it. An unchanged editor may close and clear its unused draft directly.
5. Keep a draft, update/archive its original on a second client, then sync on this
   device. Resume the draft. Expect a conflict notice, disabled direct save and
   Salvar como nova rotina. Save a copy and confirm both versions survive locally
   and after sync, with a new ID for the active copy.
6. On a disposable test installation, inject malformed/unknown-version draft JSON
   into the `onepunch` preferences `routineDraft` key. Restart. Expect a persistent
   error, no replacement of that raw value, and blocked routine editing. Existing
   workout logging, history and sync should still work. Restore test preferences
   outside the app afterward; do not use personal data for fault injection.
7. Simulate write failures where supported. A rejected field change must not claim
   success; routine commit/discard must retain the recoverable draft on failure
   and show an error. Confirm sync never includes a `routineDraft` field.
8. Run the RoutineDraftTest JVM cases on the testing machine. They cover
   incomplete draft JSON round-trip, commit isolation/timestamps, stale originals,
   conflict copies, invalid commits, unknown versions, mismatched IDs and new-ID
   collisions. These tests have not been compiled or run on the development PC.

## Active workout corrections

1. Open an exercise's three-dot menu. Change rest to zero, then to 120 seconds.
   Complete valid sets and confirm the next timer follows the selected interval.
2. While a timer is running, change the exercise's rest interval. Expect the
   current deadline to stay unchanged; the next completed set uses the new value.
3. Restart the app mid-workout. Confirm the chosen interval, entries and timer
   recover. The saved routine's rest interval should remain unchanged.
4. Add an extra set, choose its removal from the exercise menu, cancel once, then
   confirm. Confirm the correct set disappears and remaining numbering updates.
5. Remove a completed set. Confirm a warning, reduced completed-set count/volume,
   and exclusion from the final saved workout. Existing history stays unchanged.
6. Remove all sets from one exercise. Add a set again and complete it. Saving an
   entire workout with no valid completed sets must remain blocked.
7. Save a multi-exercise workout with a note. Open its history detail and confirm
   the note appears once after the exercise summaries.

## Active exercise order and replacement

1. Start a routine with multiple exercises. Enter decimal-comma weights, empty
   fields and completed sets; start a rest timer. Move exercises up/down through
   the exercise menus. Expect the first/last boundaries disabled and every value,
   done flag and the current rest deadline retained.
2. Restart the app. Confirm order and inputs recover. Append a set to an earlier
   exercise, complete it and finish the workout. Expect history detail and replay
   to follow the visible exercise order, including that appended set.
3. Choose Substituir séries pendentes on an unstarted exercise. Existing workout
   exercises must be unavailable as replacement targets. Pick a target, cancel
   once, then choose another. No workout data may change until confirmation.
4. Confirm replacement. Expect it at the original position with fresh unchecked
   sets, the pending/planned count and original rest interval. Weights must come
   only from the replacement exercise's recorded history, or stay empty if none
   exists; never reuse weights from the original movement.
5. Complete some original sets before replacement. Expect completed sets to remain
   under the original exercise, and pending sets to become the replacement directly
   after it. Completed-set count and volume must remain unchanged. The original
   pending inputs are removed only after the confirmation. A fully completed
   source must have replacement disabled; adding another exercise still works.
6. Remove all rows from an exercise, then replace it. Expect its planned count to
   become new unchecked rows. Check that timer, workout ID/start time/note, saved
   routine and old finished history remain unchanged throughout these operations.
7. Restart after replacement, finish after explicitly completing a new set, then
   sync. Expect correct exercise IDs in history, original completed sets retained,
   and no active session in sync. Simulate a storage failure if possible: the
   replacement must not be applied, the confirmation must stay open with an error.
8. Run the eleven WorkoutEditingTest cases on the other machine. They cover move
   preservation/boundaries, replacement history isolation, completed-set retention,
   fresh unchecked rows, duplicate/stale/all-done guards, empty-row replacement,
   finished order, legacy invalid-set handling and local-only serialization.
   These cases have not been compiled or executed on this PC.

## Exercise history and repeat workout

1. Open an exercise from the library with no history. Expect an explanation with
   no invented records. Add it to a workout and confirm it appears only once.
2. Save two workouts containing that exercise and another exercise. Expect the
   exercise detail to show only its own completed valid sets, newest session first,
   maximum recorded kg and maximum per-session volume. Include zero kg, decimal
   commas and different calendar years; zero must remain valid and dates include years.
3. Open exercise history from the active workout menu. Return using Voltar ao
   treino; confirm no duplicate exercise and no changes to inputs or timer.
4. Open a session from exercise history. Expect the complete original workout,
   including its other exercises and note, rather than only the selected exercise.
   Open an exercise from the workout detail to navigate back to exercise history.
5. With no active workout, choose Repetir este treino. Expect a new start time,
   exercise order and prefilled recorded weights/reps, zero completed sets/volume,
   no copied note or timer, and the documented 90-second rest default. No new
   history record should exist until sets are explicitly completed and saved.
6. Restart during the repeated workout. Confirm entries persist, finish it after
   completing at least one set, then sync. Expect a distinct workout ID and the
   original history record unchanged on both clients.
7. With a workout active, open a different history entry. Expect Retomar treino
   em andamento; using it must resume the existing session without replacing it.
8. Run the new HistoryTest JVM cases on the testing machine: validity filtering,
   fresh unchecked IDs, original history preservation, zero-load replay, refusal
   to replace an active session, and refusal to save uncompleted replay prefills.
   These cases were written but not executed on the development PC.

## Workout names, progress and finish review

1. Rename a routine-based workout and a free workout. Confirm the active name
   persists after restarting, while the routine name and old history stay intact.
   Temporarily clear the name; restart and confirm the unfinished edit is retained.
   Finish must stay disabled until a nonblank name is entered.
2. In a new empty session expect zero progress, not completed routine targets.
   Enter valid weight/reps without checking completion; progress must not advance.
   Complete a zero-kg set and a decimal-comma set; both must count. Editing a done
   set must uncheck it and reduce progress again.
3. Open Finalizar treino with a mixture of done and pending sets. Expect a full
   scrollable review with exact included loads/reps, original row numbers, total
   volume, excluded counts by exercise, and the workout note once. Nothing may
   enter history just by opening/closing review. Back and Continuar treinando must
   return to the same inputs and timer.
4. Correct pending values and explicitly complete them, then reopen review. Expect
   refreshed totals and included sets. On a test fixture include an invalid marked
   done row: progress/review must exclude it and explain the invalid-data count.
5. Save the reviewed workout. Expect exactly the included sets in visible exercise
   order and the trimmed title, with only this active session cleared. Save/preview
   must remain blocked when there are no valid completed sets.
6. Simulate final-save failure if supported. Expect the review to remain open with
   an inline error, with all active inputs available for retry/restart. Also check
   failed discard/exercise removal: their confirmations must not dismiss as success.
7. On the testing machine run the seven WorkoutReviewTest cases: included/excluded
   counts, zero-load/decimal volume, preview/save agreement, unchanged old history,
   empty progress, legacy rows without specs, blank-name/unchecked-set rejection,
   and interrupted-name recovery. They were written but not run on this PC.

## Layout and persistence failures

Check a narrow phone and large font settings: routine filters should scroll and
card actions should remain reachable. Check the keyboard, Back navigation and
scrolling in menus/dialogs. Simulate a storage-write failure if the harness allows
it: archive, restore, set removal and rest selection must not replace in-memory
state before a successful save, and failure must be surfaced to the user.

Record device/OS, build result, JVM test result and device findings separately.

## Unreleased manual rest controls

- Start rest from an active exercise menu without completing a set; confirm the
  configured duration and unchanged sets/volume. A zero-second interval disables
  the action. Starting another rest replaces only the current deadline.
- Shorten/extend by 30s; shortening with <=30s remaining hides the timer. Try a
  click at expiry and confirm shortening cannot restart it; extension grants 30s.
- Background/restart during rest and confirm the absolute deadline survives.
  Check compact widths and large font sizes for all timer controls.

## Unreleased routine, comparison and persistence checks

- Replace a routine exercise after editing its sets/reps/rest; verify position and
  targets survive, then restart and resume the draft. Cancel the picker and check
  duplicate exercises cannot be selected. Saved routines/history stay unchanged
  until routine save. Verify storage-write errors leave the picker open.
- With fewer than two valid saved sessions for an exercise, show no comparison.
  With two, check previous/latest values and signed differences against the saved
  rows, including zero kg and differing row counts. Open original session details.
- In a disposable test profile, use malformed local version/active/sync timestamp
  fields and confirm loading is blocked while original preferences are preserved.
  Confirm normal local recovery, backup restore and versionless API sync still work.

## Assisted-load correction — pending on the next exact candidate

On disposable data, log one assisted pull-up and one assisted chin-up with nonzero
assistance. Verify explicit assistance input/accessibility labels, previous-value
and history units, persistence across restart, completion count, rest timer,
finish preview/save agreement and unchecked replay. Assistance-only volume must
be zero; a mixed session must include only ordinary lifted kg × reps. Neither
assisted exercise should appear in highest-load records or load/volume comparison.
Verify long assistance labels on a narrow phone with enlarged fonts. Export, sync
and restore must preserve the raw assistance values and IDs. Existing totals may
decrease after updating because assistance is excluded; do not rewrite history.
These scenarios have not yet been executed for this source.

## Immutable-history hardening — pending on the next exact candidate

With disposable data and a controlled test API, return a changed workout under a
saved ID at older, equal and newer timestamps, together with a new routine and
workout. Expect a specific history conflict, no local merge, unchanged sync time
and preserved active inputs. Repeat with an identical numeric-equivalent record:
expect successful, idempotent sync and preserved active state. Test a recovered
active/history ID collision: finish must show an error and retain the review,
active inputs and original history. Export and compare the records before/after.
These scenarios and the four new JVM regressions have not yet been executed.

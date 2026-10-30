# Product quality pass — 2026-10-29

Owner feedback: exercise choice is too limited, visual bugs remain, and the
existing design needs refinement through real use. Native Android is the primary
client. Preserve the current identity while improving the workout flow.

## Evidence and limits

- Confirmed in source: 27 exercises, nine routine examples, six broad muscle
  categories. Several familiar options are absent: pull-ups, conventional
  deadlift, barbell row, machine chest press, rear-delt fly and hip abduction.
- Confirmed in `App.kt`: active-set headers use 8–9sp text; previous performance
  uses 10sp inside a fixed 49dp column. These are readability concerns and likely
  large-font pressure points. Clipping has not been reproduced on a device.
- Confirmed in `ExercisePicker`: name/equipment text search and muscle chips
  exist, but there is no dedicated equipment filter or explicit empty-result row.
- Confirmed in `web/src/styles.css`: narrow-screen session metadata drops to
  8px and week metadata to 9px. Readability needs review alongside Android.
- CURRENTTASK records a signed RC with hosted compilation, JVM, web and browser
  checks passing. Real Android interaction, layout and interruption QA remains
  pending. Automated build success does not establish visual quality.
- This is a source review, not a screenshot audit. No new native build, device
  installation, browser suite or visual inspection was performed.

## Priorities

| Priority | Work | Acceptance evidence |
| --- | --- | --- |
| P0 | Test a complete workout on the exact signed candidate | Record APK hash, device/OS, screenshots and reproducible issues; cover keyboard entry, previous sets, completion, rest, finish review and restart recovery. |
| P1 | Make the active-set table comfortably readable | Readable labels and previous values on a narrow phone and at enlarged system font sizes; load/reps inputs and completion controls remain reachable with keyboard open. Prefer moving previous performance onto a second line over shrinking text. |
| P1 | Expand exercise coverage deliberately | Add common missing movement/equipment variants using stable new IDs in the shared catalog. Both clients can select/log them and the updated API accepts them. Existing routines, history and drafts remain intact. |
| P1 | Improve exercise selection as the library grows | Equipment filtering, visible result count, explicit no-results feedback and a clear way to reset filters; preserve accent-insensitive search and existing-selection behavior. |
| P2 | Refine hierarchy and consistency | Align text sizes, spacing, row actions and empty states across training, routines, library and history; verify screenshots before and after rather than replacing the visual identity. |

## Catalog expansion boundaries

Start with gaps across chest, back, shoulders, legs, arms and core, and across
barbell, dumbbell, cable, machine and bodyweight equipment. Review names and
duplicates before choosing a target count; a larger number alone is not coverage.
Keep current IDs and routine examples stable. Define whether displayed dumbbell
loads mean one dumbbell or the total before adding ambiguous variants.

The current completed-set contract records kilograms and repetitions. Timed
holds, distance/cardio and assisted-load semantics need an explicit model and
cross-client validation design before being offered as ordinary exercises.
Catalog expansion should not silently force these into kg × reps volume.

## Experimentation loop

Use a disposable workspace on a separate testing device; do not replace personal
workout data. Follow `NATIVE_QA.md` for full scenarios and exact-artifact reporting.

1. Run one free workout and one routine workout, including exercise replacement,
   long names, zero load, decimal commas, notes and a rest timer.
2. Capture narrow-phone, enlarged-font and keyboard-open states, including long
   history and empty/no-results screens. Check TalkBack names and focus order.
3. Log each issue with steps, expected/actual behavior, screenshot, build hash,
   severity and whether it prevents completing the workout.
4. Fix blockers and repeated friction first. Review screenshots of the same
   states after each change; repeat the affected interaction and recovery checks.
5. Keep hosted build results separate from device UI evidence. Source changes
   require a fresh hosted candidate before their device results count.

Local work continues with source-only checks. Native compilation stays on hosted
runners; no publication or device installation follows from this backlog.

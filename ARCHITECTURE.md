# Architecture

One repository, two independently rendered clients, one shared catalog and sync
format. Native UI is Kotlin/Jetpack Compose; the React app runs in the browser.
Go serves the compiled web assets and provides a personal sync API. Runtime data
is separate from source and the initial seed contains routines, never history.

Shared content contains 100 exercises and 17 editable routine examples, each
with seven distinct exercises and editable initial set/rep/rest values. Existing
seed IDs remain stable. Fresh workspaces receive the expanded examples; existing
workspaces are never reseeded or overwritten. Both clients expose Modelos prontos
to preview the full library and edit a new UUID copy before saving. Native copies
use the recoverable local draft flow and resume any pending draft first. The
library itself never becomes workout history. Restart the personal API with the
updated exercise catalog before syncing newly added exercise IDs.

## Layout

```text
shared/             exercise catalog, routine templates, provisional brand
web/                React/JavaScript, pure domain module, production shell cache
android/            Kotlin/Compose app and Gradle wrapper
server/             standard-library Go HTTP service and atomic JSON store
scripts/            reproducible browser checks
build/              ignored native Linux executable and Android debug APK
```

## Client state

`Workspace`: version 1, routines, workouts, active workout or null, lastSyncedAt.
An active workout contains a UUID, routine/title, start time, exercise specs,
editable set inputs, optional note, and restUntil epoch milliseconds. Inputs stay
strings while being edited (including empty values and decimal commas). Marking a
set done requires kg in [0,2000] and integer reps in [1,1000]. Editing a completed
set unchecks it. Routine sets range 1–20; rest intervals range 0–600 seconds.

Finishing copies only valid completed sets into immutable history, normalizes
numeric values, clears active, and never saves an empty session. Session volume
is sum(weight × reps) for lifted-load exercises; machine assistance is excluded.
No predicted scores, progress or training outcomes are invented. Previous-set suggestions come exclusively from actual finished history.

Native session progress counts actual set rows, not routine targets: valid done
sets are completed; unchecked sets and invalid marked-done sets remain excluded.
An empty session shows zero progress. The finish review lists the included sets
by exercise with their original active row numbers, excluded counts, volume and
note. Both preview and finish use reviewWorkout, preventing selection drift.
Active names persist while editing (including a temporarily empty value); finish
requires a nonblank name and trims it for history. Renaming never edits a routine
or old history. Failed final saves leave the review and active session in place.

Native exercise details derive session history, maximum recorded load and maximum
per-session volume from valid completed sets, grouped by exercise. These are
derived views; opening a session returns its full original workout. Latest-set
prefills use the same validity filter. No empty or predicted records are shown.

Repeating a finished workout creates a new local active ID and new unchecked set
IDs, preserving the recorded kg/reps and exercise order. It cannot replace an
existing active workout and does not modify finished history or routine templates.
Old workout notes/timestamps/timers are not carried over. Finished records do not
store rest intervals, so replay explicitly starts at 90 seconds per exercise,
editable during the new session. Storage and API formats remain version 1.

The web stores JSON in localStorage `onepunch.workspace.v1`. Android stores its
workspace in private SharedPreferences. Invalid saved data is preserved instead
of auto-reset. Native writes check commit success before replacing UI state.
Web loading checks nested record shapes and timer/sync timestamps before render;
an existing empty string is unreadable, not an absent workspace. Successful sync
responses must pass the same readability check before becoming UI/persisted state.
These are structural checks, not a substitute for the API's complete validation.
Browser storage-write failures show a persistent error. Data cleared by the OS or
user is not silently recoverable unless it has been synced to the personal API.

Native routine editing has a separate version-1 `routineDraft` SharedPreferences
entry: original routine snapshot, current draft and creation flag. Each accepted
edit persists before replacing the editor state; invalid/unfinished counts are
allowed in drafts but cannot be committed as saved routines. One draft can be
resumed from Home or Treinos after restart. Creating/editing another routine first
resumes the pending draft rather than overwriting it. Continuing later keeps it;
confirmed discard removes it. It never enters Workspace or sync payloads.

Saving a routine and removing its draft use one SharedPreferences commit. If the
saved routine differs from the draft's original snapshot (including archive/sync
changes), direct replacement is blocked; saving as a new active copy preserves
both versions. Unknown/unreadable draft records are left untouched and routine
editing is blocked, while existing workouts and sync remain usable.

The timer persists an absolute deadline, so backgrounding/reloading does not reset
it. Native exercise menus can manually start/restart the configured rest without
completing sets; zero-second intervals disable manual start. Active timers allow
30-second adjustments; shortening to zero clears the deadline, while extending
an expired deadline starts 30 seconds from click time. This build does not schedule system alarms or promise background alerts.

## Assisted-load compatibility

The stable catalog IDs `assisted-pull-up` and `assisted-chin-up` record machine
assistance in the existing weight field, labeled kg de assistência in both
clients. Valid explicitly completed assisted sets remain in history, completed-set
counts, previous-set suggestions, native replay, backup and sync. They contribute
zero to lifted-volume totals and are excluded from highest-load records and
native load/volume comparisons. No body weight or effective lifted load is inferred.

This is a derived-metric correction, not a stored-data migration: IDs, raw values,
finished records and version-1 payloads remain unchanged. Existing totals can
therefore decrease after updating; older clients still include assistance in their
metrics and should be rebuilt/updated together. The API stores raw values and
requires no schema change. Future assisted catalog IDs require explicit matching
classification in both domain helpers and regression coverage.

## Native backup and restore

Native JSON backup format is separate from sync: `format: onepunch.backup`, version
1, export time, a full local workspace and optional routine draft. Android's system
document picker grants access only to the files the user chooses; backup actions
do not send data to the personal API. The reader bounds input to 8 MiB, rejects
invalid UTF-8, unknown versions/exercises, duplicate IDs and invalid saved history,
and preserves unfinished active/draft inputs. Export is plaintext, not encrypted.

Import shows a merge preview before any write. Existing finished workout IDs must
have semantically identical records (numeric load strings are normalized) or the
whole operation is rejected. Newer routine timestamps win; equal-date conflicts
become deduplicated fresh-ID copies, including changed seed templates. A local
active workout and routine draft take priority; an imported active ID already in
finished history is never reopened. Last-sync metadata resets after a merge.

Restore saves the old full workspace/draft to `restoreSafetyBackup` and commits
the merged state/draft in one preferences edit. That safety copy can be exported,
including for manual recovery, but is not an automatic undo. An unreadable safety
copy blocks replacement and remains exportable. Merged data must remain exportable
within the same document limit. File operations and device interruption QA remain
separate from the pure merge/validation tests.

## Hosted releases

Signed candidate builds use `.github/workflows/candidate.yml`, owner-controlled
signing secrets, non-debuggable release APKs and release-variant JVM tests. The
manifest includes source, artifact hashes and certificate fingerprint. The Linux
archive is extracted and smoke-tested against disposable data on the hosted runner.
`promote.yml` checks the existing candidate and exact-artifact device QA before
uploading the same files as stable, without rebuilding. The debug workflow rejects
stable release settings. See docs/RELEASE_OPERATIONS.md for setup and handoff.

Release identity is owned by `shared/release.properties` (version, Android version
code and prerelease flag). Gradle reads it directly; hosted CI validates web package
versions and matching release notes before creating assets, tags or releases.

When local workspace/draft decoding fails, Android offers a raw recovery export
through the system picker. `onepunch.recovery` version 1 preserves original
workspace, routineDraft and restoreSafetyBackup preference values without parsing
or overwriting them. It excludes server settings and is explicitly not a normal
importable backup. This gives users a recovery copy from signed installations
without requiring debug access; device file-picker QA still applies.

## Sync API

- `GET /api/health`: service health/version.
- `GET /api/exercises`: the bundled shared catalog.
- `POST /api/sync`: JSON `{routines, workouts}` -> complete merged server snapshot
  `{routines, workouts, revision}`. Never send `active` or client-only metadata.

A routine is `{id,name,note,updatedAt,archived,exercises}`. An exercise spec is
`{exerciseId,sets,reps,restSeconds}`. Finished workouts are
`{id,routineId,title,startedAt,endedAt,updatedAt,note,sets}`. Each completed set is
`{id,exerciseId,weight,reps,done:true}` with numeric weight/reps on the wire.
All timestamps are epoch milliseconds, IDs are stable UUIDs, and weights are kg.

The store validates catalog references, limits and completed sets. History IDs
are immutable; a changed record under an existing ID produces 409. Routine
conflicts use modification timestamps, not real-time collaborative editing.
Client merge retains any local active workout and newer local routine edits.
Both clients reject differing finished-history content under an existing workout
ID before applying any response records or sync metadata. Android uses the same
numeric-equivalence comparator for sync and backup restore. Finishing a colliding
active ID rejects instead of replacing history. Numeric load/repetition
representations are normalized for comparison; identical records retain the local
copy. Sync conflicts surface as recoverable feedback without replacing local data.
Native routine management supports archive/restore using the existing archived
flag and updatedAt timestamp; archived routines stay in sync and out of the home
recommendations. Duplicating creates a new routine ID and opens an unsaved draft,
leaving the original unchanged. There is no destructive history-delete API.

Native active-workout exercise menus support removing an individual set (with
confirmation, including completed sets) and changing the rest interval for future
set completions. These changes affect only the active local session, preserve the
current timer deadline, and do not rewrite routine templates or finished history.

Active exercise order can be changed without modifying set IDs, values or done
flags. Finished set order follows the visible exercise order, including sets added
later. Replacement is confirmed and affects only pending sets: done sets remain
under the original exercise, with the replacement inserted after it. If no sets
are done, the replacement takes the same position. New sets receive fresh IDs and
unchecked prefills exclusively from the replacement exercise's history; original
pending inputs are removed. The pending count, target reps and rest interval are
used as defaults; an exercise with no rows uses its planned count. Already-present
targets and fully completed source exercises cannot be replaced. Active workout
identity, note, startedAt and current rest deadline are preserved. No schema changes.

One process owns the JSON file. A mutex serializes merges; each successful write
uses a synced temporary file and rename before updating memory. Inputs/returned
snapshots are detached from store-owned slices. The service has no external
libraries, no public authentication and no multi-tenant data model. Default bind
is 127.0.0.1. Public hosting would require an intentional identity/security design.

## Offline web shell

The Vite build emits a versioned service worker and precache list of built assets.
Navigation falls back to the cached root app shell when offline. API responses and
personal records are never cached by the worker. A first successful online load
is necessary before browser offline reload works. Native assets are bundled in
the APK and require no initial connection. Routine/catalog changes require client
rebuilds; changing the display brand preserves storage/application identity.

## Current verification boundary

RC.2 development source (uncommitted changes based on cfdc351) passed local
Android debug compilation and 77 JVM tests, plus 16 web tests, 13 release-tooling
tests, production build, Go race checks and 233 browser assertions on 2026-11-08.
The browser run used disposable real API data and isolated profiles; all 53 recorded
source hashes matched at review. Both host jobs enforced 3 GiB/no swap. See
`docs/qa/v1_0_0_rc_2_automated.json` for exact evidence and debug APK hash.
Fresh signed-release candidate and exact-artifact physical-device QA are still
required. Browser screenshots and JVM results are not Android device UI evidence.

## Unreleased native additions

Routine draft exercise replacement preserves row position and targets, including
unfinished counts, and persists through the existing draft flow. Exercise detail
comparisons use the two latest sessions containing valid completed sets for that
exercise, with deterministic timestamp-tie ordering and factual differences only.
Local workspace loading requires the version-1 envelope and explicit active field;
malformed active data and invalid sync timestamps fail without replacing storage.
Sync responses may still omit local-only fields. Debug compilation/JVM checks for
these additions passed in the 77-test run; exact signed-candidate device QA remains pending.

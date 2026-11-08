import { test } from "node:test";
import assert from "node:assert/strict";
import {
  emptyState,
  startWorkout,
  toggleSet,
  finishWorkout,
  validSet,
  volume,
  completedSets,
  mergeSynced,
  readState,
  weekDays,
  addExercise,
  latestSets,
  routineValid,
  isAssisted,
  loadUnit,
} from "./model.mjs";
const routine = {
  id: "r",
  name: "A",
  exercises: [{ exerciseId: "squat", sets: 2, reps: 10, restSeconds: 90 }],
};
test("workout persists uncompleted entries but history saves only valid completed sets", () => {
  let state = emptyState([routine]);
  state.active = startWorkout(routine, [], 1000);
  assert.throws(() => finishWorkout(state, 2000));
  state.active.sets[0].weight = "20,5";
  state.active = toggleSet(state.active, state.active.sets[0].id, 2000);
  assert.equal(state.active.restUntil, 92000);
  const restored = readState(JSON.stringify(state), []);
  const done = finishWorkout(restored, 10000);
  assert.equal(done.active, null);
  assert.equal(done.workouts[0].sets.length, 1);
  assert.equal(volume(done.workouts[0]), 205);
  assert.equal(restored.active.sets.length, 2);
  assert.equal(finishWorkout(done).workouts.length, 1);
});
test("invalid inputs never complete; zero load is valid for body weight exercises", () => {
  for (const set of [
    { weight: "", reps: 10 },
    { weight: -1, reps: 10 },
    { weight: 5, reps: 0 },
    { weight: 5, reps: 1.5 },
    { weight: "NaN", reps: 10 },
  ])
    assert.equal(validSet(set), false);
  assert.equal(validSet({ weight: 0, reps: 10 }), true);
  const active = startWorkout(routine);
  assert.equal(toggleSet(active, active.sets[0].id), active);
  assert.equal(addExercise(active, "squat"), active);
});
test("sync merges records without replacing an ongoing workout", () => {
  const state = emptyState([routine]);
  state.active = startWorkout(routine);
  const merged = mergeSynced(
    state,
    {
      routines: [{ ...routine, name: "Updated", updatedAt: 10 }],
      workouts: [],
    },
    20,
  );
  assert.equal(merged.routines[0].name, "Updated");
  assert.equal(merged.active, state.active);
  assert.equal(merged.lastSyncedAt, 20);
});
test("previous performance prefills a new session without marking sets complete", () => {
  let state = emptyState([routine]);
  state.active = startWorkout(routine, [], 1000);
  state.active.sets[0].weight = "30";
  state.active = toggleSet(state.active, state.active.sets[0].id, 2000);
  state = finishWorkout(state, 3000);
  const next = startWorkout(routine, state.workouts, 4000);
  assert.equal(next.sets[0].weight, "30");
  assert.equal(next.sets[0].done, false);
  assert.equal(
    weekDays(state.workouts, 3000).reduce((n, d) => n + d.count, 0),
    1,
  );
  assert.throws(() => readState("{broken", []));
});

test("previous performance excludes invalid completed rows and newer invalid-only sessions", () => {
  const valid = { id: "valid", exerciseId: "squat", weight: "0", reps: "12", done: true };
  const invalid = { ...valid, id: "invalid", weight: "" };
  const history = [
    { endedAt: 3000, sets: [invalid] },
    { endedAt: 2000, sets: [invalid, valid, { ...valid, id: "pending", done: false }] },
  ];
  assert.deepEqual(latestSets(history, "squat"), [valid]);
  const next = startWorkout(routine, history, 4000);
  assert.equal(next.sets[0].weight, "0");
  assert.equal(next.sets[0].reps, "12");
  assert.equal(next.sets[0].done, false);
  assert.equal(next.sets[1].weight, "");
});

test("routine rest intervals use whole seconds compatible with the API", () => {
  for (const restSeconds of [0, 90, 600, "90"])
    assert.equal(routineValid({ ...routine, exercises: [{ ...routine.exercises[0], restSeconds }] }), true);
  for (const restSeconds of [-1, 0.5, 90.5, 601, NaN, Infinity])
    assert.equal(routineValid({ ...routine, exercises: [{ ...routine.exercises[0], restSeconds }] }), false);
});

test("finishing a recovered active ID cannot replace immutable history", () => {
  const state = finishedFixture();
  state.active = startWorkout(routine, state.workouts, 4000);
  state.active.id = state.workouts[0].id;
  state.active.sets[0].weight = "50";
  state.active = toggleSet(state.active, state.active.sets[0].id, 5000);
  const before = structuredClone(state);
  assert.throws(() => finishWorkout(state, 6000), /já existe no histórico/);
  assert.deepEqual(state, before);
});

test("assistance stays in history and prefills but never contributes lifted volume", () => {
  for (const exerciseId of ["assisted-pull-up", "assisted-chin-up"]) {
    assert.equal(isAssisted(exerciseId), true);
    assert.equal(loadUnit(exerciseId), "kg de assistência");
    const assistedRoutine = { ...routine, exercises: [{ ...routine.exercises[0], exerciseId }] };
    let active = startWorkout(assistedRoutine, [], 1000);
    active.sets[0].weight = "40,5";
    active = toggleSet(active, active.sets[0].id, 2000);
    assert.equal(completedSets(active).length, 1);
    assert.equal(volume(active), 0);
    const state = finishWorkout({ ...emptyState([]), active }, 3000);
    const restored = readState(JSON.stringify(state), []);
    assert.equal(restored.workouts[0].sets[0].weight, 40.5);
    assert.equal(volume(restored.workouts[0]), 0);
    assert.equal(weekDays(restored.workouts, 3000).reduce((n, day) => n + day.count, 0), 1);
    assert.equal(weekDays(restored.workouts, 3000).reduce((n, day) => n + day.volume, 0), 0);
    const next = startWorkout(assistedRoutine, restored.workouts, 4000);
    assert.equal(next.sets[0].weight, "40.5");
    assert.equal(next.sets[0].done, false);
    const mixed = { sets: [...restored.workouts[0].sets, { id: "lifted", exerciseId: "squat", weight: "20,5", reps: "10", done: true }] };
    assert.equal(volume(mixed), 205);
  }
  assert.equal(isAssisted("pull-up"), false);
  assert.equal(loadUnit("pull-up"), "kg");
});

test("unreadable nested local records reject before they can become writable UI state", () => {
  const state = finishedFixture();
  state.active = startWorkout(routine, [], 4000);
  const malformed = [
    null, [], {}, { ...state, active: "broken" }, { ...state, active: {} },
    { ...state, active: { ...state.active, sets: null } },
    { ...state, active: { ...state.active, exercises: [null] } },
    { ...state, active: { ...state.active, restUntil: "90000" } },
    { ...state, routines: [null] }, { ...state, routines: [{ ...routine, name: null }] },
    { ...state, workouts: [{ ...state.workouts[0], sets: [null] }] },
    { ...state, workouts: [{ ...state.workouts[0], title: 123 }] },
    { ...state, lastSyncedAt: -1 }, { ...state, lastSyncedAt: "5000" },
  ];
  for (const value of malformed) {
    const raw = JSON.stringify(value);
    assert.throws(() => readState(raw, []), /Formato de dados locais/);
    assert.equal(JSON.stringify(value), raw);
  }
  assert.throws(() => readState("", []));
  assert.deepEqual(readState(null, [routine]), emptyState([routine]));
});

test("local recovery keeps unfinished inputs and timer deadlines without normalization", () => {
  const state = finishedFixture();
  state.active = startWorkout(routine, [], 4000);
  state.active.title = "";
  state.active.sets[0].weight = "20,";
  state.active.sets[0].reps = "";
  state.active.restUntil = 95000;
  assert.deepEqual(readState(JSON.stringify(state), []), state);
});

test("malformed nested sync records cannot poison the local workspace", () => {
  const state = finishedFixture();
  const before = structuredClone(state);
  for (const remote of [
    { routines: [{ ...routine, name: null, updatedAt: 10000 }], workouts: [] },
    { routines: [], workouts: [{ ...state.workouts[0], id: "remote", title: 123 }] },
    { routines: [], workouts: [{ ...state.workouts[0], id: "remote", sets: [null] }] },
  ]) {
    assert.throws(() => mergeSynced(state, remote, 10000));
    assert.deepEqual(state, before);
  }
});

function finishedFixture() {
  const state = emptyState([routine]);
  state.active = startWorkout(routine, [], 1000);
  state.active.sets[0].weight = "20,5";
  state.active = toggleSet(state.active, state.active.sets[0].id, 2000);
  return finishWorkout(state, 3000);
}

test("history conflicts reject the whole sync regardless of record timestamp", () => {
  for (const updatedAt of [2000, 3000, 4000]) {
    const state = finishedFixture();
    state.active = startWorkout(routine, state.workouts, 5000);
    const before = structuredClone(state);
    const changed = { ...state.workouts[0], title: "Different", updatedAt };
    assert.throws(() => mergeSynced(state, {
      routines: [{ ...routine, name: "Remote edit", updatedAt: 9000 }],
      workouts: [{ ...changed, id: "new-workout" }, changed],
    }, 10000), /Conflito no histórico/);
    assert.deepEqual(state, before);
  }
});

test("identical history round trips accept numeric inputs and retain local records", () => {
  const state = finishedFixture();
  const original = state.workouts[0];
  const wire = {
    ...original,
    sets: original.sets.map((set) => ({ ...set, weight: "20,50", reps: "10" })),
  };
  const remote = { routines: [], workouts: [wire, { ...original, id: "remote", endedAt: 4000 }] };
  const once = mergeSynced(state, remote, 6000);
  const twice = mergeSynced(once, remote, 7000);
  assert.equal(twice.workouts.length, 2);
  assert.equal(twice.workouts[1], original);
  assert.equal(twice.lastSyncedAt, 7000);
});

test("sync detects changes in finished set values, ordering and workout metadata", () => {
  const state = finishedFixture();
  const original = state.workouts[0];
  original.sets.push({ ...original.sets[0], id: "second-set", weight: 25 });
  for (const change of [
    { note: "Changed" }, { routineId: "different" }, { startedAt: 999 },
    { endedAt: 4000 }, { updatedAt: 4000 },
    { sets: original.sets.map((set) => ({ ...set, weight: 0 })) },
    { sets: original.sets.map((set) => ({ ...set, reps: 11 })) },
    { sets: original.sets.map((set) => ({ ...set, done: false })) },
    { sets: [...original.sets].reverse() },
  ]) {
    assert.throws(() => mergeSynced(state, {
      routines: [], workouts: [{ ...original, ...change }],
    }), /Conflito no histórico/);
  }
});

import { test } from "node:test";
import assert from "node:assert/strict";
import {
  emptyState,
  startWorkout,
  toggleSet,
  finishWorkout,
  validSet,
  volume,
  mergeSynced,
  readState,
  weekDays,
  addExercise,
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

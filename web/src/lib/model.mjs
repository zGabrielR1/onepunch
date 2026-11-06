export const STORAGE_KEY = "onepunch.workspace.v1";
export const id = () =>
  globalThis.crypto?.randomUUID?.() ||
  `id-${Date.now()}-${Math.random().toString(36).slice(2)}`;
export const emptyState = (routines) => ({
  version: 1,
  routines: structuredClone(routines),
  workouts: [],
  active: null,
  lastSyncedAt: null,
});
export const number = (value) => Number(String(value).replace(",", "."));
export const validSet = (set) =>
  String(set.weight).trim() !== "" &&
  Number.isFinite(number(set.weight)) &&
  number(set.weight) >= 0 &&
  number(set.weight) <= 2000 &&
  Number.isInteger(number(set.reps)) &&
  number(set.reps) >= 1 &&
  number(set.reps) <= 1000;
export const completedSets = (workout) =>
  (workout?.sets || []).filter((set) => set.done && validSet(set));
export const volume = (workout) =>
  Math.round(
    completedSets(workout).reduce(
      (sum, set) => sum + number(set.weight) * number(set.reps),
      0,
    ) * 100,
  ) / 100;
export const routineValid = (routine) =>
  !!routine.name.trim() &&
  routine.name.length <= 80 &&
  routine.exercises.length > 0 &&
  routine.exercises.every(
    (e) =>
      Number.isInteger(+e.sets) &&
      +e.sets >= 1 &&
      +e.sets <= 20 &&
      Number.isInteger(+e.reps) &&
      +e.reps >= 1 &&
      +e.reps <= 1000 &&
      +e.restSeconds >= 0 &&
      +e.restSeconds <= 600,
  );
export const latestSets = (workouts, exerciseId) =>
  [...workouts]
    .sort((a, b) => b.endedAt - a.endedAt)
    .find((w) => completedSets(w).some((s) => s.exerciseId === exerciseId))
    ?.sets.filter((s) => s.exerciseId === exerciseId && s.done) || [];
export function addExercise(active, exerciseId, spec = {}, workouts = []) {
  if (active.exercises.some((e) => e.exerciseId === exerciseId)) return active;
  const previous = latestSets(workouts, exerciseId);
  const exercise = {
    exerciseId,
    sets: spec.sets || 3,
    reps: spec.reps || 10,
    restSeconds: spec.restSeconds ?? 90,
  };
  return {
    ...active,
    exercises: [...active.exercises, exercise],
    sets: [
      ...active.sets,
      ...Array.from({ length: exercise.sets }, (_, i) => ({
        id: id(),
        exerciseId,
        weight: String(previous[i]?.weight ?? ""),
        reps: String(previous[i]?.reps ?? exercise.reps),
        done: false,
      })),
    ],
  };
}
export function startWorkout(routine, workouts = [], now = Date.now()) {
  let active = {
    id: id(),
    routineId: routine?.id || null,
    title: routine?.name || "Treino livre",
    startedAt: now,
    exercises: [],
    sets: [],
    restUntil: null,
    note: "",
  };
  for (const spec of routine?.exercises || [])
    active = addExercise(active, spec.exerciseId, spec, workouts);
  return active;
}
export function toggleSet(active, setId, now = Date.now()) {
  const target = active.sets.find((s) => s.id === setId);
  if (!target || (!target.done && !validSet(target))) return active;
  const rest =
    active.exercises.find((e) => e.exerciseId === target.exerciseId)
      ?.restSeconds ?? 90;
  return {
    ...active,
    sets: active.sets.map((s) =>
      s.id === setId ? { ...s, done: !s.done } : s,
    ),
    restUntil: target.done
      ? active.restUntil
      : rest > 0
        ? now + rest * 1000
        : null,
  };
}
export function finishWorkout(state, now = Date.now()) {
  if (!state.active) return state;
  const sets = completedSets(state.active);
  if (!sets.length)
    throw Error("Conclua pelo menos uma série para salvar o treino.");
  const workout = {
    id: state.active.id,
    routineId: state.active.routineId,
    title: state.active.title,
    startedAt: state.active.startedAt,
    endedAt: Math.max(now, state.active.startedAt),
    updatedAt: now,
    note: state.active.note || "",
    sets: sets.map((s) => ({
      ...s,
      weight: number(s.weight),
      reps: number(s.reps),
    })),
  };
  return {
    ...state,
    active: null,
    workouts: [workout, ...state.workouts.filter((w) => w.id !== workout.id)],
  };
}
export function mergeRecords(local, remote) {
  const all = new Map(local.map((x) => [x.id, x]));
  for (const entry of remote)
    if (
      !all.has(entry.id) ||
      entry.updatedAt > (all.get(entry.id).updatedAt || 0)
    )
      all.set(entry.id, entry);
  return [...all.values()];
}
// Compare the wire fields explicitly: JSON property order and local numeric
// input strings must not create conflicts with the API's numeric representation.
const workoutContent = (workout) => JSON.stringify([
  workout.id, workout.routineId ?? null, workout.title,
  workout.startedAt, workout.endedAt, workout.updatedAt, workout.note || "",
  workout.sets.map((set) => [
    set.id, set.exerciseId, number(set.weight), number(set.reps), set.done,
  ]),
]);
export function mergeWorkouts(local, remote) {
  const all = new Map(local.map((workout) => [workout.id, workout]));
  for (const workout of remote) {
    const prior = all.get(workout.id);
    if (prior && workoutContent(prior) !== workoutContent(workout))
      throw Error("Conflito no histórico: um treino já salvo tem conteúdo diferente. Seus dados locais foram preservados.");
    if (!prior) all.set(workout.id, workout);
  }
  return [...all.values()].sort((a, b) => b.endedAt - a.endedAt);
}
export const mergeSynced = (state, remote, now = Date.now()) => ({
  ...state,
  routines: mergeRecords(state.routines, remote.routines),
  workouts: mergeWorkouts(state.workouts, remote.workouts),
  lastSyncedAt: now,
});
export function dayKey(time) {
  const d = new Date(time);
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}
export function weekDays(workouts, now = Date.now()) {
  const today = new Date(now);
  today.setHours(0, 0, 0, 0);
  return Array.from({ length: 7 }, (_, i) => {
    const day = new Date(today);
    day.setDate(day.getDate() - 6 + i);
    const rows = workouts.filter((w) => dayKey(w.endedAt) === dayKey(day));
    return {
      time: day.getTime(),
      count: rows.length,
      volume: rows.reduce((n, w) => n + volume(w), 0),
    };
  });
}
export function readState(raw, routines) {
  if (!raw) return emptyState(routines);
  const state = JSON.parse(raw);
  if (
    state.version !== 1 ||
    !Array.isArray(state.routines) ||
    !Array.isArray(state.workouts) ||
    !("active" in state)
  )
    throw Error("Formato de dados locais não reconhecido.");
  return state;
}
export const fmt = (value) =>
  new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(value);
export function clock(seconds) {
  const value = Math.max(0, Math.floor(seconds));
  return `${String(Math.floor(value / 60)).padStart(2, "0")}:${String(value % 60).padStart(2, "0")}`;
}
export const muscleColor = (muscle) =>
  ({
    Peito: "#d9ead0",
    Costas: "#dbe4f2",
    Pernas: "#eee1cd",
    Ombros: "#e7def2",
    Braços: "#f0dcd6",
    Core: "#d5e9e6",
  })[muscle] || "#e3e6de";

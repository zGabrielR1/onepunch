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
// Stable catalog IDs: these values record machine assistance, never lifted kg.
export const isAssisted = (exerciseId) =>
  exerciseId === "assisted-pull-up" || exerciseId === "assisted-chin-up";
export const loadUnit = (exerciseId) => isAssisted(exerciseId) ? "kg de assistência" : "kg";
export const volume = (workout) =>
  Math.round(
    completedSets(workout).filter((set) => !isAssisted(set.exerciseId)).reduce(
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
      Number.isInteger(+e.restSeconds) &&
      +e.restSeconds >= 0 &&
      +e.restSeconds <= 600,
  );
export const latestSets = (workouts, exerciseId) =>
  [...workouts]
    .sort((a, b) => b.endedAt - a.endedAt)
    .find((w) => completedSets(w).some((s) => s.exerciseId === exerciseId))
    ?.sets.filter((s) => s.exerciseId === exerciseId && s.done && validSet(s)) || [];
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
  if (state.workouts.some((workout) => workout.id === state.active.id))
    throw Error("Este treino já existe no histórico. Seus dados locais foram preservados.");
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
    workouts: [workout, ...state.workouts],
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
export const mergeSynced = (state, remote, now = Date.now()) => readableState({
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
  if (raw == null) return emptyState(routines);
  return readableState(JSON.parse(raw));
}
// Check the structure before render or a sync response can make it writable.
// Input values intentionally remain strings, including unfinished local edits.
function readableState(state) {
  const record = (value) => value !== null && typeof value === "object" && !Array.isArray(value);
  const text = (value) => typeof value === "string";
  const input = (value) => text(value) || (typeof value === "number" && Number.isFinite(value));
  const timestamp = (value) => Number.isSafeInteger(value) && value > 0;
  const optionalText = (value) => value == null || text(value);
  const specs = (values) => Array.isArray(values) && values.every((value) =>
    record(value) && text(value.exerciseId) &&
    [value.sets, value.reps, value.restSeconds].every(input));
  const sets = (values) => Array.isArray(values) && values.every((value) =>
    record(value) && text(value.id) && text(value.exerciseId) &&
    input(value.weight) && input(value.reps) && typeof value.done === "boolean");
  const session = (value) => record(value) && text(value.id) && text(value.title) &&
    optionalText(value.routineId) && optionalText(value.note) && timestamp(value.startedAt) && sets(value.sets);
  if (
    !record(state) ||
    state.version !== 1 ||
    !Array.isArray(state.routines) ||
    !Array.isArray(state.workouts) ||
    !("active" in state) ||
    !state.routines.every((value) => record(value) && text(value.id) && text(value.name) &&
      optionalText(value.note) && specs(value.exercises) &&
      (value.archived == null || typeof value.archived === "boolean")) ||
    !state.workouts.every((value) => session(value) && timestamp(value.endedAt) && timestamp(value.updatedAt)) ||
    !(state.active === null || (session(state.active) && specs(state.active.exercises) &&
      (state.active.restUntil == null || timestamp(state.active.restUntil)))) ||
    !(state.lastSyncedAt == null || timestamp(state.lastSyncedAt))
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

import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { filterExercises } from "./catalog.mjs";

const catalog = JSON.parse(
  readFileSync(
    new URL("../../../shared/catalog.json", import.meta.url),
    "utf8",
  ),
);

test("catalog search combines accent-insensitive words, muscle and equipment without mutating entries", () => {
  const snapshot = JSON.stringify(catalog);
  const matches = filterExercises(
    catalog,
    "  MAQUINA   SUPINO ",
    "Peito",
    "Máquina",
  );
  assert.deepEqual(
    matches.map((e) => e.id),
    ["machine-chest-press", "smith-bench-press", "smith-incline-bench"],
  );
  assert.equal(filterExercises(catalog, "supino", "Costas").length, 0);
  assert.equal(
    filterExercises(catalog, "supino", "Peito", "Peso corporal").length,
    0,
  );
  assert.equal(filterExercises(catalog, "  ").length, catalog.length);
  assert.equal(JSON.stringify(catalog), snapshot);
});

test("expanded variants remain distinct and all original routine references resolve", () => {
  const routines = JSON.parse(
    readFileSync(
      new URL("../../../shared/routines.json", import.meta.url),
      "utf8",
    ),
  );
  const ids = new Set(catalog.map((e) => e.id));
  for (const routine of routines)
    for (const spec of routine.exercises) assert(ids.has(spec.exerciseId));
  for (const id of [
    "pull-up",
    "chin-up",
    "deadlift",
    "barbell-row",
    "hip-abduction",
    "reverse-pec-deck",
  ])
    assert(ids.has(id));
  assert.notEqual(
    filterExercises(catalog, "supino", "Peito", "Halteres").length,
    0,
  );
});

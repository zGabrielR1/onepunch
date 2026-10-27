// Source-only checks: no package installation, bundling, native compiler or browser.
import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const root = new URL("../", import.meta.url);
const readJSON = (path) => JSON.parse(readFileSync(new URL(path, root), "utf8"));
const catalog = readJSON("shared/catalog.json");
const routines = readJSON("shared/routines.json");
assert.ok(Array.isArray(catalog) && catalog.length > 0, "Empty exercise catalog");
const ids = new Set(catalog.map((exercise) => exercise.id));
assert.equal(ids.size, catalog.length, "Duplicate exercise IDs");
for (const exercise of catalog) {
  for (const field of ["id", "name", "muscle", "equipment", "description"]) {
    assert.ok(typeof exercise[field] === "string" && exercise[field].trim(), `${exercise.id}: missing ${field}`);
  }
}
assert.ok(Array.isArray(routines) && routines.length > 0, "Empty routine templates");
assert.equal(new Set(routines.map((routine) => routine.id)).size, routines.length, "Duplicate routine IDs");
for (const routine of routines) {
  assert.ok(routine.id && routine.name && routine.exercises.length, "Incomplete routine template");
  assert.equal(new Set(routine.exercises.map((exercise) => exercise.exerciseId)).size, routine.exercises.length, `${routine.id}: duplicate exercises`);
  for (const exercise of routine.exercises) {
    assert.ok(ids.has(exercise.exerciseId), `Unknown exercise: ${exercise.exerciseId}`);
    for (const [field, min, max] of [["sets", 1, 20], ["reps", 1, 1000], ["restSeconds", 0, 600]]) {
      assert.ok(Number.isInteger(exercise[field]) && exercise[field] >= min && exercise[field] <= max, `${routine.id}: invalid ${field}`);
    }
  }
}
console.log(`Shared data valid: ${catalog.length} exercises, ${routines.length} templates.`);

const tests = readdirSync(new URL("web/src/lib/", root))
  .filter((name) => name.endsWith(".test.mjs"))
  .sort()
  .map((name) => `web/src/lib/${name}`);
assert.ok(tests.length, "No domain tests found");
const result = spawnSync(process.execPath, [
  "--max-old-space-size=128", "--test", "--test-concurrency=1", ...tests,
], { cwd: fileURLToPath(root), stdio: "inherit" });
if (result.error) throw result.error;
process.exitCode = result.status ?? 1;

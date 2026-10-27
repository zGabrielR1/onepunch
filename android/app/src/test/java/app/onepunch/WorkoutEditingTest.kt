package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class WorkoutEditingTest {
    private val squat = ExerciseSpec("squat", sets = 2, reps = 8, restSeconds = 120)
    private val bench = ExerciseSpec("bench-press", sets = 1)
    private val done = WorkoutSet("done", "squat", "20,5", "8", true)
    private val pending = WorkoutSet("pending", "squat", "35", "", false)
    private val other = WorkoutSet("other", "bench-press", "0", "10", true)
    private val active = ActiveWorkout(
        id = "active", routineId = "routine", title = "Treino A", startedAt = 1000,
        exercises = listOf(squat, bench), sets = listOf(done, pending, other),
        restUntil = 120000, note = "Nota em andamento",
    )

    @Test
    fun movingPreservesEverySetInputAndTimerWhileChangingExerciseAndSetOrder() {
        val moved = moveWorkoutExercise(active, "bench-press", -1)
        assertEquals(listOf("bench-press", "squat"), moved.exercises.map { it.exerciseId })
        assertEquals(listOf(other, done, pending), moved.sets)
        assertEquals(active.restUntil, moved.restUntil)
        assertEquals(active.id, moved.id)
        assertEquals(active.startedAt, moved.startedAt)
        assertEquals(active.note, moved.note)
        assertEquals(volume(active.sets), volume(moved.sets), 0.001)
        assertEquals(active, moveWorkoutExercise(moved, "bench-press", 1))
    }

    @Test
    fun movingAtBoundariesOrMovingMissingExerciseDoesNothing() {
        assertSame(active, moveWorkoutExercise(active, "squat", -1))
        assertSame(active, moveWorkoutExercise(active, "bench-press", 1))
        assertSame(active, moveWorkoutExercise(active, "missing", 1))
    }

    @Test
    fun replacementKeepsCompletedOriginalSetsAndUsesOnlyReplacementHistory() {
        val previous = Workout("past", null, "Anterior", 100, 200, 200, "", listOf(
            WorkoutSet("old-target", "leg-press", "70", "12", true),
        ))
        val result = replaceWorkoutExercise(active, "squat", "leg-press", listOf(previous))
        assertEquals(listOf("squat", "leg-press", "bench-press"), result.exercises.map { it.exerciseId })
        assertEquals(1, result.exercises.first().sets)
        assertTrue(result.sets.contains(done))
        assertTrue(result.sets.contains(other))
        assertFalse(result.sets.any { it.id == pending.id })
        val added = result.sets.single { it.exerciseId == "leg-press" }
        assertEquals("70", added.weight)
        assertEquals("12", added.reps)
        assertFalse(added.done)
        assertNotEquals("old-target", added.id)
        assertTrue(active.sets.none { it.id == added.id })
        assertEquals(active.restUntil, result.restUntil)
        assertEquals(120, result.exercises[1].restSeconds)
        assertEquals(active.note, result.note)
        assertEquals(active.routineId, result.routineId)
        assertEquals(volume(active.sets), volume(result.sets), 0.001)
        assertEquals(listOf(done, pending, other), active.sets)
        assertTrue(previous.sets.single().done)
    }

    @Test
    fun replacingUnstartedExerciseDoesNotTransferItsWeightsToAnotherMovement() {
        val unstarted = active.copy(sets = listOf(done.copy(done = false), pending, other))
        val result = replaceWorkoutExercise(unstarted, "squat", "leg-press", emptyList())
        assertEquals(listOf("leg-press", "bench-press"), result.exercises.map { it.exerciseId })
        val added = result.sets.filter { it.exerciseId == "leg-press" }
        assertEquals(2, added.size)
        assertTrue(added.all { it.weight == "" && it.reps == "8" && !it.done })
        assertEquals(2, added.map { it.id }.distinct().size)
        assertFalse(result.sets.any { it.exerciseId == "squat" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun fullyCompletedExerciseCannotBeReplaced() {
        replaceWorkoutExercise(active.copy(sets = listOf(done, other)), "squat", "leg-press", emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun replacementCannotDuplicateAnExistingExercise() {
        replaceWorkoutExercise(active, "squat", "bench-press", emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun staleReplacementCannotTargetAnExerciseThatWasRemoved() {
        replaceWorkoutExercise(active, "missing", "leg-press", emptyList())
    }

    @Test
    fun replacingAnExerciseWithNoRemainingRowsUsesItsPlannedCount() {
        val result = replaceWorkoutExercise(active.copy(sets = listOf(other)), "squat", "leg-press", emptyList())
        assertEquals(2, result.sets.count { it.exerciseId == "leg-press" })
        assertEquals(active.restUntil, result.restUntil)
    }

    @Test
    fun finishingUsesVisibleExerciseOrderEvenWhenASetWasAppendedLater() {
        val moved = moveWorkoutExercise(active, "bench-press", -1)
        val appended = moved.copy(sets = listOf(done, other))
        val result = finishWorkout(Workspace(active = appended), 2000)
        assertEquals(listOf(other, done), result.workouts.single().sets)
    }

    @Test
    fun replacementPreservesLegacyDoneInputsButFinishStillRejectsInvalidSets() {
        val invalidDone = done.copy(weight = "")
        val result = replaceWorkoutExercise(
            active.copy(sets = listOf(invalidDone, pending, other)),
            "squat", "leg-press", emptyList(),
        )
        assertTrue(result.sets.contains(invalidDone))
        val finished = finishWorkout(Workspace(active = result), 2000)
        assertEquals(listOf(other), finished.workouts.single().sets)
    }

    @Test
    fun modifiedSessionRoundTripsLocallyWithoutEnteringSync() {
        val replaced = replaceWorkoutExercise(active, "squat", "leg-press", emptyList())
        val moved = moveWorkoutExercise(replaced, "leg-press", -1)
        val state = Workspace(active = moved)
        assertEquals(state, workspaceFrom(workspaceJson(state)))
        assertFalse(workspaceJson(state, syncing = true).has("active"))
    }
}

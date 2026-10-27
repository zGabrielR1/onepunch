package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class WorkoutReviewTest {
    private val zero = WorkoutSet("zero", "squat", "0", "10", true)
    private val valid = WorkoutSet("valid", "bench-press", "20,5", "8", true)
    private val pending = WorkoutSet("pending", "squat", "30", "10", false)
    private val invalid = WorkoutSet("invalid", "squat", "", "10", true)
    private val active = ActiveWorkout(
        id = "active", title = "  Treino de hoje  ", startedAt = 1000,
        exercises = listOf(ExerciseSpec("squat"), ExerciseSpec("bench-press")),
        sets = listOf(valid, zero, pending, invalid), restUntil = 120000,
    )

    @Test
    fun reviewSeparatesUncheckedAndInvalidDoneSetsAndKeepsZeroLoad() {
        val review = reviewWorkout(active)
        assertEquals(2, review.completedCount)
        assertEquals(1, review.pendingCount)
        assertEquals(1, review.invalidDoneCount)
        assertEquals(2, review.excludedCount)
        assertEquals(4, review.totalCount)
        assertEquals(164.0, review.totalVolume, 0.001)
        assertEquals(listOf(zero, valid), review.includedSets)
        assertEquals(2, review.exercises.first().excludedCount)
        assertEquals(listOf(zero), review.exercises.first().includedSets)
        assertEquals(listOf(valid, zero, pending, invalid), active.sets)
    }

    @Test
    fun finishSavesExactlyReviewedSetsWithTrimmedTitleAndPreservesOldHistory() {
        val old = Workout("old", null, "Anterior", 1, 2, 2, "", listOf(zero.copy(id = "old-set")))
        val state = Workspace(workouts = listOf(old), active = active)
        val result = finishWorkout(state, 2000)
        assertEquals(reviewWorkout(active).includedSets, result.workouts.first().sets)
        assertEquals("Treino de hoje", result.workouts.first().title)
        assertEquals(old, result.workouts.last())
        assertNull(result.active)
        assertEquals(active, state.active)
    }

    @Test
    fun emptyExerciseDoesNotInventTargetRowsOrProgress() {
        val review = reviewWorkout(active.copy(sets = emptyList()))
        assertEquals(0, review.totalCount)
        assertEquals(0, review.completedCount)
        assertEquals(0.0, review.totalVolume, 0.001)
        assertEquals(2, review.exercises.size)
        assertTrue(review.exercises.all { it.includedSets.isEmpty() && it.excludedCount == 0 })
    }

    @Test
    fun legacySetWithoutExerciseSpecIsStillReviewedAndSaved() {
        val orphan = zero.copy(id = "orphan", exerciseId = "leg-press")
        val changed = active.copy(sets = active.sets + orphan)
        assertEquals("leg-press", reviewWorkout(changed).exercises.last().exerciseId)
        assertTrue(finishWorkout(Workspace(active = changed), 2000).workouts.single().sets.contains(orphan))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTitleCannotBeFinishedEvenWithValidCompletedSets() {
        finishWorkout(Workspace(active = active.copy(title = "  ")), 2000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validButUncheckedInputsNeverAllowFinishing() {
        finishWorkout(Workspace(active = active.copy(sets = listOf(pending))), 2000)
    }

    @Test
    fun interruptedTitleEditPersistsLocallyAndCanBeCorrectedBeforeFinishing() {
        val editing = active.copy(title = "")
        val restored = workspaceFrom(workspaceJson(Workspace(active = editing)))
        assertEquals(editing, restored.active)
        assertFalse(workoutTitleValid(requireNotNull(restored.active).title))
        val corrected = restored.copy(active = restored.active?.copy(title = "Novo nome"))
        assertEquals("Novo nome", finishWorkout(corrected, 2000).workouts.first().title)
        assertFalse(workspaceJson(restored, syncing = true).has("active"))
    }
}

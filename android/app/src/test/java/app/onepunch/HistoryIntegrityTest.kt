package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class HistoryIntegrityTest {
    private val set = WorkoutSet("s", "squat", "20,5", "10", true)
    private val workout = Workout("w", "r", "Treino", 1000, 3000, 3000, "Nota", listOf(set))
    private val routine = Routine(id = "r", name = "Local", updatedAt = 1000)

    @Test fun conflictsRejectWholeSyncForOlderEqualAndNewerTimestamps() {
        for (updatedAt in listOf(2000L, 3000L, 4000L)) {
            val local = Workspace(listOf(routine), listOf(workout), ActiveWorkout(), 1000)
            val remote = Workspace(
                routines = listOf(routine.copy(name = "Remota", updatedAt = 9000)),
                workouts = listOf(workout.copy(id = "new"), workout.copy(title = "Outra", updatedAt = updatedAt)),
            )
            try {
                mergeWorkspace(local, remote, 10000)
                fail("Changed immutable history must reject the merge")
            } catch (expected: WorkoutHistoryConflictException) {
                assertTrue(requireNotNull(expected.message).contains("Conflito no histórico"))
            }
            assertEquals(listOf(routine), local.routines)
            assertEquals(listOf(workout), local.workouts)
            assertEquals(1000L, local.lastSyncedAt)
            assertNotNull(local.active)
        }
    }

    @Test fun numericRoundTripKeepsLocalHistoryAndActiveAcrossRepeatedSync() {
        val original = workout.copy(sets = listOf(set, set.copy(id = "zero", weight = "-0")))
        val wire = original.copy(sets = listOf(set.copy(weight = "20.50", reps = "010"), set.copy(id = "zero", weight = "0.0")))
        val local = Workspace(workouts = listOf(original), active = ActiveWorkout())
        val remote = Workspace(workouts = listOf(wire, workout.copy(id = "new", endedAt = 4000)))
        val once = mergeWorkspace(local, remote, 5000)
        val twice = mergeWorkspace(once, remote, 6000)
        assertEquals(2, twice.workouts.size)
        assertSame(original, twice.workouts.last())
        assertSame(local.active, twice.active)
        assertEquals(6000L, twice.lastSyncedAt)
    }

    @Test fun comparisonCoversAllHistoryFieldsAndSetOrder() {
        val original = workout.copy(sets = listOf(set, set.copy(id = "second", weight = "25")))
        val changes = listOf(
            original.copy(title = "Changed"), original.copy(routineId = null), original.copy(note = "Changed"),
            original.copy(startedAt = 999), original.copy(endedAt = 4000), original.copy(updatedAt = 4000),
            original.copy(sets = original.sets.reversed()),
            original.copy(sets = listOf(set.copy(id = "other"))),
            original.copy(sets = original.sets.map { it.copy(exerciseId = "bench-press") }),
            original.copy(sets = original.sets.map { it.copy(weight = "0") }),
            original.copy(sets = original.sets.map { it.copy(reps = "11") }),
            original.copy(sets = original.sets.map { it.copy(done = false) }),
        )
        for (changed in changes) assertFalse(sameWorkoutContent(original, changed))
    }

    @Test fun finishCollisionRetainsCompletedHistoryAndActiveInputs() {
        val active = ActiveWorkout(id = workout.id, sets = listOf(set.copy(weight = "50")))
        val state = Workspace(workouts = listOf(workout), active = active)
        try {
            finishWorkout(state, 5000)
            fail("Finishing cannot replace history")
        } catch (expected: IllegalArgumentException) {
            assertTrue(requireNotNull(expected.message).contains("já existe no histórico"))
        }
        assertSame(active, state.active)
        assertSame(workout, state.workouts.single())
        assertEquals("50", requireNotNull(state.active).sets.single().weight)
    }
}

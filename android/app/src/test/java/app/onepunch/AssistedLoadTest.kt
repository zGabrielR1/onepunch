package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class AssistedLoadTest {
    @Test fun assistanceIsSavedAndReplayedWithoutInflatingVolume() {
        for (exerciseId in listOf("assisted-pull-up", "assisted-chin-up")) {
            val set = WorkoutSet(exerciseId = exerciseId, weight = "40,5", reps = "10", done = true)
            val active = ActiveWorkout(startedAt = 1000, exercises = listOf(ExerciseSpec(exerciseId)), sets = listOf(set))
            val state = finishWorkout(Workspace(active = active), 3000)
            val saved = state.workouts.single()
            assertEquals(listOf(set), saved.sets)
            assertEquals(1, reviewWorkout(active).completedCount)
            assertEquals(0.0, reviewWorkout(active).totalVolume, 0.001)
            assertEquals(0.0, volume(saved.sets), 0.001)
            assertEquals(listOf(set), latestSets(state.workouts, exerciseId))
            val replay = requireNotNull(repeatWorkout(state, saved, 4000).active)
            assertEquals("40,5", replay.sets.single().weight)
            assertFalse(replay.sets.single().done)
            assertEquals(205.0, volume(saved.sets + set.copy(exerciseId = "squat", weight = "20,5")), 0.001)
        }
    }

    @Test fun assistanceHasExplicitUnitsAndNoLiftedLoadComparison() {
        for (exerciseId in listOf("assisted-pull-up", "assisted-chin-up")) {
            assertTrue(isAssisted(exerciseId))
            assertEquals("kg de assistência", loadUnit(exerciseId))
            val set = WorkoutSet(exerciseId = exerciseId, weight = "40", reps = "10", done = true)
            val first = Workout("first", null, "A", 1000, 2000, 2000, "", listOf(set))
            val second = first.copy(id = "second", endedAt = 3000, sets = listOf(set.copy(weight = "30")))
            assertNull(compareExerciseSessions(listOf(first, second), exerciseId))
            assertEquals(2, exerciseHistory(listOf(first, second), exerciseId).size)
        }
        assertFalse(isAssisted("pull-up"))
        assertEquals("kg", loadUnit("pull-up"))
    }
}

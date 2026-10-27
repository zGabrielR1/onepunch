package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class SerializationTest {
    @Test
    fun activeWorkoutAndTimerRoundTripWithoutLosingIncompleteInputs() {
        val active =
            ActiveWorkout(
                id = "active",
                title = "A",
                startedAt = 1000,
                exercises = listOf(ExerciseSpec("squat")),
                sets =
                    listOf(
                        WorkoutSet(
                            id = "set",
                            exerciseId = "squat",
                            weight = "20,5",
                            reps = "10",
                            done = true,
                        ),
                        WorkoutSet(id = "empty", exerciseId = "squat", weight = "", reps = "10"),
                    ),
                restUntil = 92000,
            )
        val restored = workspaceFrom(workspaceJson(Workspace(active = active)))
        assertEquals(active, restored.active)
    }

    @Test
    fun syncPayloadUsesSharedNumericContractAndNeverExportsActiveWorkout() {
        val workout =
            Workout(
                "w",
                null,
                "A",
                1000,
                2000,
                2000,
                "",
                listOf(
                    WorkoutSet(
                        id = "s",
                        exerciseId = "squat",
                        weight = "20,5",
                        reps = "10",
                        done = true,
                    ),
                ),
            )
        val payload =
            workspaceJson(Workspace(workouts = listOf(workout), active = ActiveWorkout()), true)
        assertFalse(payload.has("active"))
        assertFalse(payload.has("version"))
        val set =
            payload.getJSONArray("workouts").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
        assertEquals(20.5, set.getDouble("weight"), 0.001)
        assertEquals(10, set.getInt("reps"))
        assertTrue(set.getBoolean("done"))
        assertEquals("20.5", workspaceFrom(payload).workouts[0].sets[0].weight)
    }
}

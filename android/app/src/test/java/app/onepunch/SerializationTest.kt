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
        val restored = workspaceFrom(workspaceJson(Workspace(active = active)), local = true)
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

    @Test
    fun localEnvelopeRejectsUnknownOrMissingVersionsWithoutChangingInput() {
        for (version in listOf<Any>(2, 1.5, "1", org.json.JSONObject.NULL)) {
            val json = workspaceJson(Workspace(active = ActiveWorkout())).put("version", version)
            val original = json.toString()
            assertTrue(runCatching { workspaceFrom(json, local = true) }.isFailure)
            assertEquals(original, json.toString())
        }
        val missing = workspaceJson(Workspace()).also { it.remove("version") }
        assertTrue(runCatching { workspaceFrom(missing, local = true) }.isFailure)
        // The same envelope omission is intentional for the API contract.
        assertEquals(Workspace(), workspaceFrom(workspaceJson(Workspace(), syncing = true)))
    }

    @Test
    fun malformedOrMissingLocalActiveCannotSilentlyBecomeAnEmptySession() {
        for (active in listOf<Any>("unreadable", 12, org.json.JSONArray())) {
            val json = workspaceJson(Workspace()).put("active", active)
            assertTrue(runCatching { workspaceFrom(json, local = true) }.isFailure)
            assertTrue(runCatching { workspaceFrom(json) }.isFailure)
        }
        val missing = workspaceJson(Workspace()).also { it.remove("active") }
        assertTrue(runCatching { workspaceFrom(missing, local = true) }.isFailure)
        assertNull(workspaceFrom(workspaceJson(Workspace()), local = true).active)
    }

    @Test
    fun syncMetadataRejectsCoercionTruncationAndOverflow() {
        for (timestamp in listOf<Any>("123", 1.5, -1, 0, 1e20)) {
            val json = workspaceJson(Workspace()).put("lastSyncedAt", timestamp)
            assertTrue(runCatching { workspaceFrom(json, local = true) }.isFailure)
        }
        val expected = Workspace(lastSyncedAt = 123L)
        assertEquals(expected, workspaceFrom(workspaceJson(expected), local = true))
    }
}

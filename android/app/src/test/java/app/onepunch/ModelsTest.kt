package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    private val routine =
        Routine(id = "r", name = "Pernas", exercises = listOf(ExerciseSpec("squat", sets = 2)))

    @Test
    fun onlyCompletedValidSetsReachHistory() {
        var active = startWorkout(routine, emptyList(), 1000)
        active =
            active.copy(
                sets =
                    active.sets.mapIndexed { i, s -> if (i == 0) s.copy(weight = "20,5") else s },
            )
        active = toggleSet(active, active.sets[0].id, 2000)
        assertEquals(92000L, active.restUntil)
        val result = finishWorkout(Workspace(active = active), 10000)
        assertNull(result.active)
        assertEquals(1, result.workouts[0].sets.size)
        assertEquals(205.0, volume(result.workouts[0].sets), 0.001)
    }

    @Test
    fun zeroLoadIsValidButEmptyAndFractionalRepsAreNot() {
        assertTrue(validSet(WorkoutSet(exerciseId = "push-up", weight = "0", reps = "10")))
        assertFalse(validSet(WorkoutSet(exerciseId = "x", weight = "", reps = "10")))
        assertFalse(validSet(WorkoutSet(exerciseId = "x", weight = "20", reps = "2.5")))
    }

    @Test
    fun syncPreservesActiveAndNewerLocalRoutines() {
        val active = startWorkout(routine, emptyList())
        val local = Workspace(routines = listOf(routine.copy(updatedAt = 10)), active = active)
        val result =
            mergeWorkspace(
                local,
                Workspace(routines = listOf(routine.copy(name = "Old", updatedAt = 1))),
            )
        assertSame(active, result.active)
        assertEquals("Pernas", result.routines[0].name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyWorkoutCannotFinish() {
        finishWorkout(Workspace(active = startWorkout(routine, emptyList())))
    }

    @Test
    fun exerciseSearchNormalizesPortugueseAccents() {
        assertEquals("triceps", normalized("Tríceps"))
        assertEquals("elevacao lateral", normalized("ELEVAÇÃO LATERAL"))
    }
}

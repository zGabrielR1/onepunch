package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class WorkoutRestTest {
    private val active = ActiveWorkout(
        exercises = listOf(ExerciseSpec("squat", restSeconds = 120), ExerciseSpec("bench", restSeconds = 0)),
        sets = listOf(WorkoutSet("set", "squat", "20", "8", true)),
        note = "Preservar", restUntil = 100000L,
    )

    @Test fun startingRestOnlyChangesDeadline() {
        assertEquals(active.copy(restUntil = 125000L), startWorkoutRest(active, "squat", 5000L))
        assertSame(active, startWorkoutRest(active, "bench", 5000L))
        assertSame(active, startWorkoutRest(active, "missing", 5000L))
    }

    @Test fun adjustmentsPreserveSessionAndUseRemainingTime() {
        assertEquals(active.copy(restUntil = 130000L), adjustWorkoutRest(active, 30, 90000L))
        assertEquals(active.copy(restUntil = 70000L), adjustWorkoutRest(active, -30, 50000L))
        assertEquals(active.copy(restUntil = null), adjustWorkoutRest(active, -30, 90000L))
    }

    @Test fun expiredRestExtendsFromClickTimeAndCannotBeRevivedByShortening() {
        assertEquals(active.copy(restUntil = 230000L), adjustWorkoutRest(active, 30, 200000L))
        assertEquals(active.copy(restUntil = null), adjustWorkoutRest(active, -30, 200000L))
        assertEquals(active.copy(restUntil = null), adjustWorkoutRest(active, -30, 100000L))
        val stopped = active.copy(restUntil = null)
        assertSame(stopped, adjustWorkoutRest(stopped, 30, 200000L))
    }
}

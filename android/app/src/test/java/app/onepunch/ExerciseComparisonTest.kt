package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class ExerciseComparisonTest {
    private fun set(id: String, weight: String, reps: String = "10", done: Boolean = true, exercise: String = "squat") =
        WorkoutSet(id, exercise, weight, reps, done)

    private fun session(id: String, endedAt: Long, vararg sets: WorkoutSet) =
        Workout(id, null, "Treino $id", 1, endedAt, endedAt, "Nota", sets.toList())

    @Test fun comparisonUsesTwoLatestExerciseSessionsAndOnlyValidCompletedRows() {
        val older = session("older", 100, set("a", "10"))
        val previous = session("previous", 200, set("b", "20", "8"), set("c", "20", "8"))
        val latest = session("latest", 300,
            set("d", "22,5", "6"), set("e", "0", "12"),
            set("pending", "200", done = false), set("invalid", ""),
            set("other", "200", exercise = "bench"),
        )
        val unrelated = session("unrelated", 400, set("u", "100", exercise = "bench"))
        val source = listOf(latest, older, unrelated, previous)
        val comparison = requireNotNull(compareExerciseSessions(source, "squat"))
        assertEquals("latest", comparison.latest.workoutId)
        assertEquals("previous", comparison.previous.workoutId)
        assertEquals(2, comparison.latest.setCount)
        assertEquals(18, comparison.latest.totalReps)
        assertEquals(22.5, comparison.latest.maximumWeight, 0.001)
        assertEquals(135.0, comparison.latest.totalVolume, 0.001)
        assertEquals(0, comparison.setCountDifference)
        assertEquals(2, comparison.totalRepsDifference)
        assertEquals(2.5, comparison.maximumWeightDifference, 0.001)
        assertEquals(-185.0, comparison.totalVolumeDifference, 0.001)
        assertEquals(5, latest.sets.size)
        assertEquals(listOf(latest, older, unrelated, previous), source)
    }

    @Test fun comparisonRequiresTwoValidExerciseSessionsAndAcceptsZeroWeight() {
        val zero = session("zero", 100, set("a", "0"))
        val empty = session("empty", 200, set("b", "20", done = false))
        assertNull(compareExerciseSessions(emptyList(), "squat"))
        assertNull(compareExerciseSessions(listOf(zero, empty), "squat"))
        assertNull(compareExerciseSessions(listOf(zero), "missing"))
        val other = session("other", 300, set("c", "0", "8"))
        val comparison = requireNotNull(compareExerciseSessions(listOf(zero, other), "squat"))
        assertEquals(0.0, comparison.maximumWeightDifference, 0.001)
        assertEquals(0.0, comparison.totalVolumeDifference, 0.001)
        assertEquals(-2, comparison.totalRepsDifference)
    }

    @Test fun equalTimestampsHaveStableOrderingIndependentOfInputOrder() {
        val a = session("a", 100, set("s1", "10"))
        val b = session("b", 100, set("s2", "20"))
        assertEquals(compareExerciseSessions(listOf(a, b), "squat"), compareExerciseSessions(listOf(b, a), "squat"))
        val laterStart = b.copy(startedAt = 2)
        assertEquals("b", requireNotNull(compareExerciseSessions(listOf(a, laterStart), "squat")).latest.workoutId)
    }
}

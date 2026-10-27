package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class HistorySearchTest {
    private val catalog = listOf(Exercise("triceps", "Tríceps na polia", "Braços", "Polia", ""))
    private val workout = Workout("w", null, "Treino superior", 1, 100, 100, "Execução controlada",
        listOf(WorkoutSet("s", "triceps", "10", "12", true)))

    @Test fun searchNormalizesAccentsAndFindsExerciseEquipmentNotesAndTitles() {
        listOf("TRICEPS", "bracos", "polia", "execucao", "superior").forEach { query ->
            assertEquals(listOf(workout), filterWorkoutHistory(listOf(workout), catalog, query))
        }
        assertTrue(filterWorkoutHistory(listOf(workout), catalog, "pernas").isEmpty())
    }
    @Test fun dateBoundsAreInclusiveAndResultsAreNewestFirstWithoutMutatingHistory() {
        val newer = workout.copy(id = "new", endedAt = 200)
        val source = listOf(workout, newer)
        assertEquals(listOf(newer, workout), filterWorkoutHistory(source, catalog))
        assertEquals(listOf(workout), filterWorkoutHistory(source, catalog, from = 100, until = 100))
        assertTrue(filterWorkoutHistory(source, catalog, from = 201).isEmpty())
        assertEquals(listOf(workout, newer), source)
    }
    @Test fun invalidOrUncheckedExerciseRowsDoNotSupplySearchMatches() {
        val pending = workout.copy(sets = workout.sets.map { it.copy(done = false) })
        assertTrue(filterWorkoutHistory(listOf(pending), catalog, "triceps").isEmpty())
        assertEquals(listOf(pending), filterWorkoutHistory(listOf(pending), catalog, "superior"))
    }
}

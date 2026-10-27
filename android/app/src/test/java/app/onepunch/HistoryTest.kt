package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class HistoryTest {
    private fun workout(id: String, endedAt: Long, sets: List<WorkoutSet>) =
        Workout(id, "routine", "Treino A", 100, endedAt, endedAt, "Nota antiga", sets)

    private fun set(id: String, exerciseId: String, weight: String = "20,5", done: Boolean = true) =
        WorkoutSet(id = id, exerciseId = exerciseId, weight = weight, reps = "10", done = done)

    @Test
    fun exerciseHistoryExcludesIncompleteInvalidAndOtherExercisesWithoutMutatingHistory() {
        val older = workout("old", 200, listOf(set("a", "squat", weight = "0")))
        val newer = workout("new", 300, listOf(
            set("b", "squat"),
            set("c", "squat", done = false),
            set("d", "squat", weight = ""),
            set("e", "bench-press"),
        ))
        val empty = workout("empty", 400, listOf(set("f", "squat", done = false)))
        val result = exerciseHistory(listOf(older, empty, newer), "squat")
        assertEquals(listOf("new", "old"), result.map { it.id })
        assertEquals(listOf("b"), result.first().sets.map { it.id })
        assertEquals(4, newer.sets.size)
        assertEquals("b", latestSets(listOf(older, empty, newer), "squat").single().id)
        assertTrue(exerciseHistory(listOf(older), "missing").isEmpty())
    }

    @Test
    fun repeatCreatesFreshUncheckedSetsAndPreservesHistoryAndRoutines() {
        val source = workout("old", 200, listOf(
            set("a", "squat"), set("b", "bench-press", weight = "0"),
            set("c", "squat", weight = "25"), set("d", "squat", done = false),
        ))
        val state = Workspace(routines = listOf(Routine(name = "Atual")), workouts = listOf(source))
        val result = repeatWorkout(state, source, 1000)
        val active = requireNotNull(result.active)
        assertNotEquals(source.id, active.id)
        assertEquals(1000L, active.startedAt)
        assertEquals(source.title, active.title)
        assertEquals(source.routineId, active.routineId)
        assertEquals(listOf("squat", "bench-press"), active.exercises.map { it.exerciseId })
        assertEquals(listOf("20,5", "0", "25"), active.sets.map { it.weight })
        assertEquals(3, active.sets.map { it.id }.distinct().size)
        assertTrue(active.sets.none { new -> source.sets.any { it.id == new.id } })
        assertTrue(active.sets.none { it.done })
        assertEquals(0.0, volume(active.sets), 0.001)
        assertNull(active.restUntil)
        assertEquals("", active.note)
        assertTrue(active.exercises.all { it.restSeconds == 90 })
        assertSame(state.workouts, result.workouts)
        assertSame(state.routines, result.routines)
        assertNull(state.active)
    }

    @Test(expected = IllegalArgumentException::class)
    fun repeatedPrefillsCannotBeSavedAsCompletedHistory() {
        val source = workout("old", 200, listOf(set("s", "squat")))
        finishWorkout(repeatWorkout(Workspace(workouts = listOf(source)), source, 1000), 2000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun repeatCannotReplaceAnActiveSession() {
        repeatWorkout(Workspace(active = ActiveWorkout()), workout("old", 200, listOf(set("s", "squat"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun repeatRejectsHistoryWithNoCompletedValidSets() {
        repeatWorkout(Workspace(), workout("old", 200, listOf(set("s", "squat", done = false))))
    }
}

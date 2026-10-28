package app.onepunch

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RoutineDraftTest {
    private val original = Routine(
        id = "r", name = "Treino A", updatedAt = 10,
        exercises = listOf(ExerciseSpec("squat")),
    )

    private val catalog = listOf("squat", "leg-press", "bench").map {
        Exercise(it, it, "Pernas", "Máquina", "Exemplo")
    }

    @Test
    fun replacementPreservesDraftOrderTargetsAndMetadataWithoutEditingOriginal() {
        val draft = original.copy(note = "Nota", exercises = listOf(
            ExerciseSpec("bench", sets = 2),
            ExerciseSpec("squat", sets = 5, reps = 12, restSeconds = 180),
        ))
        val changed = replaceRoutineExercise(draft, "squat", "leg-press", catalog)
        assertEquals(draft.copy(exercises = listOf(
            draft.exercises.first(), draft.exercises.last().copy(exerciseId = "leg-press"),
        )), changed)
        assertEquals("squat", draft.exercises.last().exerciseId)
        assertEquals(original, RoutineDraft(original, changed).original)
    }

    @Test
    fun replacementRetainsIncompleteCountsAndRoundTripsThroughRecovery() {
        val draft = original.copy(exercises = listOf(ExerciseSpec("squat", sets = 0, reps = 0)))
        val changed = replaceRoutineExercise(draft, "squat", "leg-press", catalog)
        assertFalse(routineValid(changed))
        val session = RoutineDraft(original, changed)
        assertEquals(session, routineDraftFrom(JSONObject(routineDraftJson(session).toString())))
        assertSame(draft, replaceRoutineExercise(draft, "squat", "squat", catalog))
    }

    @Test(expected = IllegalArgumentException::class)
    fun replacementRejectsDuplicateMovement() {
        val draft = original.copy(exercises = original.exercises + ExerciseSpec("bench"))
        replaceRoutineExercise(draft, "squat", "bench", catalog)
    }

    @Test(expected = IllegalArgumentException::class)
    fun replacementRejectsMissingSource() {
        replaceRoutineExercise(original, "missing", "leg-press", catalog)
    }

    @Test(expected = IllegalArgumentException::class)
    fun replacementRejectsUnknownCatalogExercise() {
        replaceRoutineExercise(original, "squat", "unknown", catalog)
    }

    @Test
    fun templateCopiesHaveFreshIdentityAndNeverReplaceTheTemplate() {
        val copy = copyRoutineTemplate(original, now = 30)
        assertNotEquals(original.id, copy.id)
        assertEquals(original.exercises, copy.exercises)
        assertEquals(original.name, copy.name)
        assertEquals(30L, copy.updatedAt)
        assertFalse(copy.archived)
        val state = Workspace(routines = listOf(original))
        val result = commitRoutineDraft(state, RoutineDraft(copy, isNew = true), now = 31)
        assertEquals(original, result.routines.first())
        assertEquals(2, result.routines.size)
        assertEquals(copy.id, result.routines.last().id)
        assertEquals(original, state.routines.single())
    }

    @Test
    fun localDraftRoundTripPreservesIncompleteFieldsAndOriginal() {
        val value = RoutineDraft(
            original = original,
            draft = original.copy(name = "", note = "Em edição", exercises = listOf(
                ExerciseSpec("squat", sets = 0, reps = 0, restSeconds = 0),
            )),
            isNew = true,
        )
        val restored = routineDraftFrom(JSONObject(routineDraftJson(value).toString()))
        assertEquals(value, restored)
        assertEquals(original, restored.original)
        assertFalse(routineValid(restored.draft))
    }

    @Test
    fun commitUpdatesOnlyRoutineAndAdvancesTimestampEvenIfClockMovesBack() {
        val active = ActiveWorkout()
        val state = Workspace(routines = listOf(original), active = active)
        val value = RoutineDraft(original, original.copy(name = "  Editado  "))
        val result = commitRoutineDraft(state, value, now = 5)
        assertEquals("Editado", result.routines.single().name)
        assertEquals(11L, result.routines.single().updatedAt)
        assertSame(active, result.active)
        assertSame(state.workouts, result.workouts)
        assertEquals(original, state.routines.single())
    }

    @Test(expected = IllegalArgumentException::class)
    fun staleDraftCannotOverwriteNewerSyncedRoutine() {
        val state = Workspace(routines = listOf(original.copy(name = "Remota", updatedAt = 20)))
        commitRoutineDraft(state, RoutineDraft(original, original.copy(name = "Local")), now = 30)
    }

    @Test
    fun conflictCanBeSavedAsNewActiveRoutineWithoutChangingRemoteVersion() {
        val remote = original.copy(name = "Remota", updatedAt = 20, archived = true)
        val value = RoutineDraft(original, original.copy(name = "X".repeat(80), archived = true))
        val result = commitRoutineDraft(Workspace(routines = listOf(remote)), value, asCopy = true, now = 30)
        assertEquals(2, result.routines.size)
        assertEquals(remote, result.routines.first())
        val copy = result.routines.last()
        assertNotEquals(remote.id, copy.id)
        assertFalse(copy.archived)
        assertEquals(80, copy.name.length)
        assertTrue(copy.name.endsWith(" (cópia)"))
        assertEquals(30L, copy.updatedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompleteDraftCannotBecomeSavedRoutine() {
        commitRoutineDraft(Workspace(routines = listOf(original)), RoutineDraft(original, original.copy(name = "")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownDraftVersionIsRejected() {
        routineDraftFrom(routineDraftJson(RoutineDraft(original)).put("version", 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun mismatchedDraftIdentityIsRejected() {
        routineDraftFrom(routineDraftJson(RoutineDraft(original, original.copy(id = "other"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun newDraftCannotReplaceAnExistingRoutineWithTheSameId() {
        commitRoutineDraft(Workspace(routines = listOf(original)), RoutineDraft(original, isNew = true))
    }
}

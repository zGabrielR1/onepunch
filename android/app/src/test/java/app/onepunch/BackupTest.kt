package app.onepunch

import java.io.ByteArrayInputStream
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupTest {
    private val known = setOf("squat", "bench-press")
    private val routine = Routine(id = "r", name = "Pernas", updatedAt = 10, exercises = listOf(ExerciseSpec("squat")))
    private val set = WorkoutSet("s", "squat", "0", "10", true)
    private val workout = Workout("w", "r", "Treino", 1, 2, 2, "Nota", listOf(set))
    private val active = ActiveWorkout(id = "a", startedAt = 3, exercises = listOf(ExerciseSpec("squat")),
        sets = listOf(WorkoutSet("pending", "squat", "20,", "", false)), restUntil = 90000)
    private val draft = RoutineDraft(routine, routine.copy(name = "", exercises = listOf(ExerciseSpec("squat", sets = 0))))
    private val backup = WorkoutBackup(Workspace(listOf(routine), listOf(workout), active), draft, 100)

    @Test fun roundTripKeepsInterruptedInputsTimersDraftAndZeroLoadHistory() {
        val restored = decodeBackup(encodeBackup(backup), known)
        assertEquals(active, restored.workspace.active)
        assertEquals(draft, restored.routineDraft)
        assertEquals(listOf(routine), restored.workspace.routines)
        assertEquals(0.0, numeric(restored.workspace.workouts.single().sets.single().weight)!!, 0.001)
        assertEquals(100L, restored.exportedAt)
    }

    @Test fun mergeKeepsLocalActiveAndDraftAndAddsOnlyMissingHistory() {
        val localActive = active.copy(id = "local", title = "Em andamento")
        val localDraft = draft.copy(draft = draft.draft.copy(note = "Local"))
        val local = Workspace(active = localActive, lastSyncedAt = 7)
        val plan = planBackupRestore(local, localDraft, backup)
        assertSame(localActive, plan.workspace.active)
        assertSame(localDraft, plan.routineDraft)
        assertEquals(1, plan.addedRoutines)
        assertEquals(1, plan.addedWorkouts)
        assertTrue(plan.activeKept && plan.draftKept)
        assertNull(plan.workspace.lastSyncedAt)
        assertTrue(local.workouts.isEmpty())
    }

    @Test fun freshWorkspaceRecoversActiveAndDraftAndRepeatedMergeIsIdempotent() {
        val first = planBackupRestore(Workspace(), null, backup)
        assertTrue(first.activeRestored && first.draftRestored)
        val decoded = decodeBackup(encodeBackup(backup), known)
        val second = planBackupRestore(first.workspace, first.routineDraft, decoded)
        assertEquals(0, second.addedWorkouts)
        assertEquals(0, second.addedRoutines)
        assertEquals(first.workspace.workouts, second.workspace.workouts)
    }

    @Test(expected = IllegalArgumentException::class)
    fun changedImmutableHistoryRejectsTheWholePlan() {
        val changed = workout.copy(sets = listOf(set.copy(weight = "10")))
        planBackupRestore(Workspace(workouts = listOf(changed)), null, backup)
    }

    @Test fun newerRoutinesWinAndEqualDateConflictsBecomeDeduplicatedCopies() {
        val newer = routine.copy(name = "Nova", updatedAt = 20)
        val updated = planBackupRestore(Workspace(routines = listOf(routine)), null,
            backup.copy(workspace = Workspace(routines = listOf(newer))))
        assertEquals(newer, updated.workspace.routines.single())
        assertEquals(1, updated.updatedRoutines)
        val conflicting = routine.copy(name = "Outra")
        val source = backup.copy(workspace = Workspace(routines = listOf(conflicting)))
        val plan = planBackupRestore(Workspace(routines = listOf(routine)), null, source, now = 30)
        assertEquals(routine, plan.workspace.routines.first())
        assertEquals("Outra (backup)", plan.workspace.routines.last().name)
        assertEquals(1, plan.copiedRoutines)
        assertEquals(0, planBackupRestore(plan.workspace, null, source, now = 40).addedRoutines)
    }

    @Test fun alreadyFinishedLocalSessionIsNotReopenedFromBackup() {
        val finished = workout.copy(id = active.id)
        val plan = planBackupRestore(Workspace(workouts = listOf(finished)), null,
            backup.copy(workspace = Workspace(active = active)))
        assertNull(plan.workspace.active)
        assertFalse(plan.activeRestored)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownExerciseRejectsTheFile() {
        decodeBackup(encodeBackup(backup), setOf("bench-press"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun fractionalTargetCannotBeSilentlyTruncated() {
        val raw = backupJson(backup)
        raw.getJSONObject("workspace").getJSONArray("routines").getJSONObject(0)
            .getJSONArray("exercises").getJSONObject(0).put("sets", 2.5)
        decodeBackup(raw.toString(), known)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateHistoryIdsRejectTheFile() {
        val raw = backupJson(backup)
        raw.getJSONObject("workspace").put("workouts", JSONArray(listOf(workoutJson(workout), workoutJson(workout))))
        decodeBackup(raw.toString(), known)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedDocumentIsRejectedWhileReading() {
        readBackupDocument(ByteArrayInputStream(ByteArray(MAX_BACKUP_BYTES + 1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownBackupVersionIsRejected() {
        decodeBackup(backupJson(backup).put("version", 2).toString(), known)
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedActiveDataIsNotSilentlyDropped() {
        val raw = backupJson(backup)
        raw.getJSONObject("workspace").put("active", "broken")
        decodeBackup(raw.toString(), known)
    }
}

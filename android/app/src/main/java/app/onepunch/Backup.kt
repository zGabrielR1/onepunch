package app.onepunch

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

const val MAX_BACKUP_BYTES = 8 * 1024 * 1024

data class WorkoutBackup(
    val workspace: Workspace,
    val routineDraft: RoutineDraft? = null,
    val exportedAt: Long = System.currentTimeMillis(),
)

data class BackupRestorePlan(
    val workspace: Workspace,
    val routineDraft: RoutineDraft?,
    val addedRoutines: Int,
    val updatedRoutines: Int,
    val copiedRoutines: Int,
    val addedWorkouts: Int,
    val activeRestored: Boolean,
    val activeKept: Boolean,
    val draftRestored: Boolean,
    val draftKept: Boolean,
)

fun readBackupDocument(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(output.size() + count <= MAX_BACKUP_BYTES) { "O backup excede o limite de 8 MB." }
        output.write(buffer, 0, count)
    }
    return Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(output.toByteArray())).toString()
}

fun backupJson(value: WorkoutBackup): JSONObject = JSONObject()
    .put("format", "onepunch.backup")
    .put("version", 1)
    .put("exportedAt", value.exportedAt)
    .put("workspace", workspaceJson(value.workspace))
    .put("routineDraft", value.routineDraft?.let(::routineDraftJson) ?: JSONObject.NULL)

fun encodeBackup(value: WorkoutBackup): String {
    val raw = backupJson(value).toString(2)
    require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) { "Os dados excedem o limite de 8 MB para este formato de backup." }
    return raw
}

fun decodeBackup(raw: String, knownExercises: Set<String>): WorkoutBackup {
    require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) { "O backup excede o limite de 8 MB." }
    val root = JSONObject(raw)
    require(root.getString("format") == "onepunch.backup" && root.getInt("version") == 1) {
        "Este arquivo não é um backup OnePunch compatível."
    }
    val data = root.getJSONObject("workspace")
    require(data.getInt("version") == 1 && data.has("active")) { "Versão de dados incompatível." }
    require(data.isNull("active") || data.get("active") is JSONObject) { "Treino em andamento malformado." }
    require(root.has("routineDraft") && (root.isNull("routineDraft") || root.get("routineDraft") is JSONObject)) { "Rascunho malformado." }
    // JSONObject.getInt/getLong otherwise silently truncate fractional JSON numbers.
    fun integer(value: JSONObject, key: String) {
        val number = value.get(key)
        require(number is Number && number.toDouble().isFinite() && number.toDouble() == number.toLong().toDouble()) {
            "Valor inteiro inválido no campo $key."
        }
    }
    fun checkSpecs(value: JSONObject) {
        value.getJSONArray("exercises").objects().forEach { spec ->
            listOf("sets", "reps", "restSeconds").forEach { integer(spec, it) }
            require(listOf("sets", "reps", "restSeconds").all { spec.getLong(it) in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }) { "Meta fora do intervalo inteiro." }
        }
    }
    integer(root, "exportedAt")
    integer(root, "version")
    integer(data, "version")
    require(root.getLong("version") == 1L && data.getLong("version") == 1L) { "Versão de backup incompatível." }
    data.getJSONArray("routines").objects().forEach {
        integer(it, "updatedAt"); checkSpecs(it)
        require(it.get("archived") is Boolean) { "Estado de arquivamento inválido." }
    }
    data.getJSONArray("workouts").objects().forEach { workout ->
        listOf("startedAt", "endedAt", "updatedAt").forEach { integer(workout, it) }
        workout.getJSONArray("sets").objects().forEach { require(it.get("done") is Boolean) { "Estado de série inválido." } }
    }
    data.optJSONObject("active")?.let { active ->
        integer(active, "startedAt")
        if (!active.isNull("restUntil")) integer(active, "restUntil")
        checkSpecs(active)
        active.getJSONArray("sets").objects().forEach { require(it.get("done") is Boolean) { "Estado de série inválido." } }
    }
    val draft = root.optJSONObject("routineDraft")?.let {
        integer(it, "version")
        require(it.getLong("version") == 1L) { "Versão de rascunho incompatível." }
        checkSpecs(it.getJSONObject("original"))
        checkSpecs(it.getJSONObject("draft"))
        routineDraftFrom(it)
    }
    val workspace = workspaceFrom(data, local = true)
    fun ids(values: List<String>) {
        require(values.all { it.isNotBlank() && it.length <= 100 } && values.distinct().size == values.size) {
            "O backup contém identificadores vazios ou duplicados."
        }
    }
    fun specs(values: List<ExerciseSpec>, draftValues: Boolean = false) {
        ids(values.map { it.exerciseId })
        require(values.all { it.exerciseId in knownExercises }) { "O backup usa exercícios desconhecidos nesta versão." }
        if (!draftValues) require(values.all { it.sets in 1..1000 && it.reps in 1..1000 && it.restSeconds in 0..600 }) {
            "O backup contém metas de exercício inválidas."
        }
    }
    require(workspace.routines.size <= 500 && workspace.workouts.size <= 10000) { "O backup excede os limites de registros." }
    ids(workspace.routines.map { it.id })
    ids(workspace.workouts.map { it.id })
    workspace.routines.forEach {
        specs(it.exercises)
        require(routineValid(it) && it.updatedAt > 0 && it.note.length <= 200) { "O backup contém uma rotina inválida." }
    }
    workspace.workouts.forEach { workout ->
        ids(workout.sets.map { it.id })
        require(workoutTitleValid(workout.title) && workout.startedAt > 0 && workout.endedAt >= workout.startedAt &&
            workout.updatedAt > 0 && workout.sets.isNotEmpty() && workout.sets.size <= 1000 &&
            workout.note.length <= 2000 && workout.sets.all { it.exerciseId in knownExercises && it.done && validSet(it) }) {
            "O backup contém um treino concluído inválido."
        }
    }
    workspace.active?.let { active ->
        ids(listOf(active.id))
        ids(active.sets.map { it.id })
        specs(active.exercises)
        require(active.startedAt > 0 && active.sets.size <= 1000 && active.note.length <= 2000 &&
            workspace.workouts.none { it.id == active.id } && active.sets.all { set ->
                active.exercises.any { it.exerciseId == set.exerciseId }
            }) { "O backup contém um treino em andamento inválido." }
        // Unfinished weight/repetition strings and temporarily empty titles are intentional.
    }
    draft?.let {
        specs(it.original.exercises, draftValues = true)
        specs(it.draft.exercises, draftValues = true)
        require(it.draft.name.length <= 80 && it.draft.note.length <= 200) { "Rascunho de rotina inválido." }
    }
    val exportedAt = root.getLong("exportedAt")
    require(exportedAt > 0) { "Data do backup inválida." }
    return WorkoutBackup(workspace, draft, exportedAt)
}

private fun comparableWorkout(value: Workout): Workout = value.copy(sets = value.sets.map {
    it.copy(weight = numeric(it.weight)?.let { weight -> if (weight == 0.0) "0.0" else weight.toString() } ?: it.weight,
        reps = it.reps.toIntOrNull()?.toString() ?: it.reps)
})

fun planBackupRestore(local: Workspace, localDraft: RoutineDraft?, backup: WorkoutBackup, now: Long = System.currentTimeMillis()): BackupRestorePlan {
    val workouts = local.workouts.associateBy { it.id }.toMutableMap()
    var addedWorkouts = 0
    backup.workspace.workouts.forEach { imported ->
        val current = workouts[imported.id]
        require(current == null || comparableWorkout(current) == comparableWorkout(imported)) {
            "Um treino do arquivo tem o mesmo ID de um registro local, mas conteúdo diferente. Nada foi restaurado."
        }
        require(local.active?.id != imported.id) { "O arquivo conflita com o treino em andamento. Finalize-o antes de importar." }
        if (current == null) { workouts[imported.id] = imported; addedWorkouts++ }
    }
    val routines = local.routines.associateBy { it.id }.toMutableMap()
    var addedRoutines = 0
    var updatedRoutines = 0
    var copiedRoutines = 0
    backup.workspace.routines.forEach { imported ->
        val current = routines[imported.id]
        if (current == null) { routines[imported.id] = imported; addedRoutines++ }
        else if (imported.updatedAt > current.updatedAt) { routines[imported.id] = imported; updatedRoutines++ }
        else if (imported.updatedAt == current.updatedAt && current != imported) {
            val name = imported.name.take(71) + " (backup)"
            if (routines.values.none { it.name == name && it.note == imported.note && it.exercises == imported.exercises && it.archived == imported.archived }) {
                val copy = imported.copy(id = uid(), name = name, updatedAt = maxOf(now, imported.updatedAt + 1))
                routines[copy.id] = copy
                addedRoutines++
                copiedRoutines++
            }
        }
    }
    require(routines.size <= 500 && workouts.size <= 10000) { "A mesclagem excederia os limites de registros." }
    val importedActive = backup.workspace.active?.takeIf { it.id !in workouts }
    val active = local.active ?: importedActive
    return BackupRestorePlan(
        workspace = local.copy(routines = routines.values.toList(), workouts = workouts.values.sortedByDescending { it.endedAt },
            active = active, lastSyncedAt = null),
        routineDraft = localDraft ?: backup.routineDraft,
        addedRoutines = addedRoutines, updatedRoutines = updatedRoutines, copiedRoutines = copiedRoutines, addedWorkouts = addedWorkouts,
        activeRestored = local.active == null && importedActive != null,
        activeKept = local.active != null && backup.workspace.active != null,
        draftRestored = localDraft == null && backup.routineDraft != null,
        draftKept = localDraft != null && backup.routineDraft != null,
    )
}

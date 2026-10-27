package app.onepunch

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

fun specFrom(o: JSONObject) =
    ExerciseSpec(
        o.getString("exerciseId"),
        o.getInt("sets"),
        o.getInt("reps"),
        o.getInt("restSeconds"),
    )

fun routineFrom(o: JSONObject) =
    Routine(
        o.getString("id"),
        o.getString("name"),
        o.optString("note"),
        o.getJSONArray("exercises").objects().map(::specFrom),
        o.getLong("updatedAt"),
        o.optBoolean("archived"),
    )

fun setFrom(o: JSONObject) =
    WorkoutSet(
        o.getString("id"),
        o.getString("exerciseId"),
        o.get("weight").toString(),
        o.get("reps").toString(),
        o.getBoolean("done"),
    )

fun routineJson(r: Routine) =
    JSONObject()
        .put("id", r.id)
        .put("name", r.name)
        .put("note", r.note)
        .put("updatedAt", r.updatedAt)
        .put("archived", r.archived)
        .put(
            "exercises",
            JSONArray(
                r.exercises.map {
                    JSONObject()
                        .put("exerciseId", it.exerciseId)
                        .put("sets", it.sets)
                        .put("reps", it.reps)
                        .put("restSeconds", it.restSeconds)
                },
            ),
        )

fun setJson(s: WorkoutSet, complete: Boolean = false) =
    JSONObject()
        .put("id", s.id)
        .put("exerciseId", s.exerciseId)
        .put("weight", if (complete) numeric(s.weight) else s.weight)
        .put("reps", if (complete) s.reps.toInt() else s.reps)
        .put("done", s.done)

fun workoutJson(w: Workout) =
    JSONObject()
        .put("id", w.id)
        .put("routineId", w.routineId ?: JSONObject.NULL)
        .put("title", w.title)
        .put("startedAt", w.startedAt)
        .put("endedAt", w.endedAt)
        .put("updatedAt", w.updatedAt)
        .put("note", w.note)
        .put("sets", JSONArray(w.sets.map { setJson(it, true) }))

fun workspaceJson(s: Workspace, syncing: Boolean = false): JSONObject {
    val o =
        JSONObject()
            .put("routines", JSONArray(s.routines.map(::routineJson)))
            .put("workouts", JSONArray(s.workouts.map(::workoutJson)))
    if (syncing) return o
    o.put("version", 1).put("lastSyncedAt", s.lastSyncedAt ?: JSONObject.NULL)
    val a = s.active
    o.put(
        "active",
        if (a == null) JSONObject.NULL
        else
            JSONObject()
                .put("id", a.id)
                .put("routineId", a.routineId ?: JSONObject.NULL)
                .put("title", a.title)
                .put("startedAt", a.startedAt)
                .put("note", a.note)
                .put("restUntil", a.restUntil ?: JSONObject.NULL)
                .put(
                    "exercises",
                    JSONArray(
                        a.exercises.map {
                            JSONObject()
                                .put("exerciseId", it.exerciseId)
                                .put("sets", it.sets)
                                .put("reps", it.reps)
                                .put("restSeconds", it.restSeconds)
                        },
                    ),
                )
                .put("sets", JSONArray(a.sets.map { setJson(it) })),
    )
    return o
}

fun workspaceFrom(o: JSONObject): Workspace {
    val routines = o.getJSONArray("routines").objects().map(::routineFrom)
    val workouts =
        o.getJSONArray("workouts").objects().map { w ->
            Workout(
                w.getString("id"),
                w.nullableString("routineId"),
                w.getString("title"),
                w.getLong("startedAt"),
                w.getLong("endedAt"),
                w.getLong("updatedAt"),
                w.optString("note"),
                w.getJSONArray("sets").objects().map(::setFrom),
            )
        }
    val a = o.optJSONObject("active")
    val active = a?.let {
        ActiveWorkout(
            it.getString("id"),
            it.nullableString("routineId"),
            it.getString("title"),
            it.getLong("startedAt"),
            it.getJSONArray("exercises").objects().map(::specFrom),
            it.getJSONArray("sets").objects().map(::setFrom),
            if (it.isNull("restUntil")) null else it.getLong("restUntil"),
            it.optString("note"),
        )
    }
    return Workspace(
        routines,
        workouts,
        active,
        if (o.isNull("lastSyncedAt")) null else o.optLong("lastSyncedAt").takeIf { it > 0 },
    )
}

class StateStore(context: Context) {
    private val prefs = context.getSharedPreferences("onepunch", Context.MODE_PRIVATE)
    val catalog =
        JSONArray(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
            .objects()
            .map {
                Exercise(
                    it.getString("id"),
                    it.getString("name"),
                    it.getString("muscle"),
                    it.getString("equipment"),
                    it.getString("description"),
                )
            }
    val routineTemplates =
        JSONArray(context.assets.open("routines.json").bufferedReader().use { it.readText() })
            .objects()
            .map(::routineFrom)

    fun load(): Workspace =
        prefs.getString("workspace", null)?.let { workspaceFrom(JSONObject(it)) }
            ?: Workspace(routines = routineTemplates)

    fun save(s: Workspace, clearRoutineDraft: Boolean = false) {
        val edit = prefs.edit().putString("workspace", workspaceJson(s).toString())
        if (clearRoutineDraft) edit.remove("routineDraft")
        check(edit.commit()) {
            "Não foi possível salvar neste dispositivo."
        }
    }

    fun loadRoutineDraft(): RoutineDraft? =
        prefs.getString("routineDraft", null)?.let {
            routineDraftFrom(JSONObject(it)).also { value ->
                require(value.draft.exercises.all { spec -> catalog.any { it.id == spec.exerciseId } }) {
                    "O rascunho contém um exercício que não está no catálogo."
                }
            }
        }

    fun saveRoutineDraft(value: RoutineDraft) {
        check(prefs.edit().putString("routineDraft", routineDraftJson(value).toString()).commit()) {
            "Não foi possível salvar o rascunho neste dispositivo."
        }
    }

    fun clearRoutineDraft() {
        check(prefs.edit().remove("routineDraft").commit()) {
            "Não foi possível remover o rascunho neste dispositivo."
        }
    }

    var server: String
        get() = prefs.getString("server", "http://10.0.2.2:8097")!!
        set(value) {
            prefs.edit().putString("server", value).apply()
        }
}

fun syncWorkspace(server: String, state: Workspace): Workspace {
    val url = URL(server.trimEnd('/') + "/api/sync")
    require(url.protocol in listOf("http", "https")) { "Use um endereço HTTP ou HTTPS." }
    val connection =
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
    try {
        connection.outputStream.use {
            it.write(workspaceJson(state, true).toString().toByteArray())
        }
        val code = connection.responseCode
        val raw =
            (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() } ?: "{}"
        check(code in 200..299) { JSONObject(raw).optString("error", "Falha na sincronização.") }
        return workspaceFrom(JSONObject(raw))
    } finally {
        connection.disconnect()
    }
}

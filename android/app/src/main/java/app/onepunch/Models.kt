package app.onepunch

import java.util.UUID
import kotlin.math.round

data class Exercise(
    val id: String,
    val name: String,
    val muscle: String,
    val equipment: String,
    val description: String,
)

data class ExerciseSpec(
    val exerciseId: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val restSeconds: Int = 90,
)

data class Routine(
    val id: String = uid(),
    val name: String,
    val note: String = "",
    val exercises: List<ExerciseSpec> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

data class WorkoutSet(
    val id: String = uid(),
    val exerciseId: String,
    val weight: String = "",
    val reps: String = "10",
    val done: Boolean = false,
)

data class ActiveWorkout(
    val id: String = uid(),
    val routineId: String? = null,
    val title: String = "Treino livre",
    val startedAt: Long = System.currentTimeMillis(),
    val exercises: List<ExerciseSpec> = emptyList(),
    val sets: List<WorkoutSet> = emptyList(),
    val restUntil: Long? = null,
    val note: String = "",
)

data class Workout(
    val id: String,
    val routineId: String?,
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val updatedAt: Long,
    val note: String,
    val sets: List<WorkoutSet>,
)

data class Workspace(
    val routines: List<Routine> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val active: ActiveWorkout? = null,
    val lastSyncedAt: Long? = null,
)

fun uid(): String = UUID.randomUUID().toString()

fun numeric(value: String): Double? =
    value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

fun validSet(set: WorkoutSet): Boolean {
    val weight = numeric(set.weight) ?: return false
    val reps = set.reps.toIntOrNull() ?: return false
    return weight in 0.0..2000.0 && reps in 1..1000
}

fun completed(sets: List<WorkoutSet>) = sets.filter { it.done && validSet(it) }

// Stable catalog IDs: these values record machine assistance, never lifted kg.
fun isAssisted(exerciseId: String): Boolean =
    exerciseId == "assisted-pull-up" || exerciseId == "assisted-chin-up"

fun loadUnit(exerciseId: String): String = if (isAssisted(exerciseId)) "kg de assistência" else "kg"

fun volume(sets: List<WorkoutSet>): Double =
    round(
        completed(sets).filterNot { isAssisted(it.exerciseId) }
            .sumOf { (numeric(it.weight) ?: 0.0) * (it.reps.toIntOrNull() ?: 0) } * 100,
    ) / 100

fun latestSets(workouts: List<Workout>, exerciseId: String) =
    exerciseHistory(workouts, exerciseId).firstOrNull()?.sets ?: emptyList()

fun addExercise(active: ActiveWorkout, spec: ExerciseSpec, workouts: List<Workout>): ActiveWorkout {
    if (active.exercises.any { it.exerciseId == spec.exerciseId }) return active
    val previous = latestSets(workouts, spec.exerciseId)
    val sets =
        (0 until spec.sets).map { i ->
            WorkoutSet(
                exerciseId = spec.exerciseId,
                weight = previous.getOrNull(i)?.weight ?: "",
                reps = previous.getOrNull(i)?.reps ?: spec.reps.toString(),
            )
        }
    return active.copy(exercises = active.exercises + spec, sets = active.sets + sets)
}

fun startWorkout(
    routine: Routine?,
    workouts: List<Workout>,
    now: Long = System.currentTimeMillis(),
): ActiveWorkout {
    var active =
        ActiveWorkout(
            routineId = routine?.id,
            title = routine?.name ?: "Treino livre",
            startedAt = now,
        )
    routine?.exercises?.forEach { active = addExercise(active, it, workouts) }
    return active
}

fun toggleSet(
    active: ActiveWorkout,
    id: String,
    now: Long = System.currentTimeMillis(),
): ActiveWorkout {
    val set = active.sets.find { it.id == id } ?: return active
    if (!set.done && !validSet(set)) return active
    val rest = active.exercises.find { it.exerciseId == set.exerciseId }?.restSeconds ?: 90
    return active.copy(
        sets = active.sets.map { if (it.id == id) it.copy(done = !it.done) else it },
        restUntil = if (set.done) active.restUntil else if (rest > 0) now + rest * 1000L else null,
    )
}

fun finishWorkout(state: Workspace, now: Long = System.currentTimeMillis()): Workspace {
    val active = state.active ?: return state
    require(state.workouts.none { it.id == active.id }) {
        "Este treino já existe no histórico. Seus dados locais foram preservados."
    }
    val sets = reviewWorkout(active).includedSets
    require(sets.isNotEmpty()) { "Conclua ao menos uma série para salvar." }
    require(workoutTitleValid(active.title)) { "Dê um nome ao treino antes de salvar." }
    val workout =
        Workout(
            active.id,
            active.routineId,
            active.title.trim(),
            active.startedAt,
            maxOf(now, active.startedAt),
            now,
            active.note,
            sets,
        )
    return state.copy(
        active = null,
        workouts = listOf(workout) + state.workouts,
    )
}

fun mergeWorkspace(
    local: Workspace,
    remote: Workspace,
    now: Long = System.currentTimeMillis(),
): Workspace {
    val routines = local.routines.associateBy { it.id }.toMutableMap()
    remote.routines.forEach {
        if ((routines[it.id]?.updatedAt ?: 0) < it.updatedAt) routines[it.id] = it
    }
    val workouts = local.workouts.associateBy { it.id }.toMutableMap()
    remote.workouts.forEach {
        val current = workouts[it.id]
        if (current != null && !sameWorkoutContent(current, it)) throw WorkoutHistoryConflictException()
        if (current == null) workouts[it.id] = it
    }
    return local.copy(
        routines = routines.values.toList(),
        workouts = workouts.values.sortedByDescending { it.endedAt },
        lastSyncedAt = now,
    )
}

fun routineValid(routine: Routine) =
    routine.name.trim().isNotEmpty() &&
        routine.name.length <= 80 &&
        routine.exercises.isNotEmpty() &&
        routine.exercises.all { it.sets in 1..20 && it.reps in 1..1000 && it.restSeconds in 0..600 }

fun normalized(value: String): String =
    java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase(java.util.Locale.ROOT)

package app.onepunch

fun filterWorkoutHistory(
    workouts: List<Workout>, catalog: List<Exercise>, query: String = "", from: Long? = null, until: Long? = null,
): List<Workout> {
    val lookup = catalog.associateBy { it.id }
    val search = normalized(query.trim())
    return workouts.filter { workout ->
        (from == null || workout.endedAt >= from) && (until == null || workout.endedAt <= until) &&
            (search.isEmpty() || normalized(workout.title + " " + workout.note + " " +
                completed(workout.sets).joinToString(" ") { set ->
                    lookup[set.exerciseId]?.let { "${it.name} ${it.muscle} ${it.equipment}" } ?: set.exerciseId
                }).contains(search))
    }.sortedByDescending { it.endedAt }
}

// Derived views only: never replace or rewrite the original finished records.
fun exerciseHistory(workouts: List<Workout>, exerciseId: String): List<Workout> =
    workouts.mapNotNull { workout ->
        val sets = completed(workout.sets).filter { it.exerciseId == exerciseId }
        if (sets.isEmpty()) null else workout.copy(sets = sets)
    }.sortedByDescending { it.endedAt }

fun repeatWorkout(
    state: Workspace,
    workout: Workout,
    now: Long = System.currentTimeMillis(),
): Workspace {
    require(state.active == null) { "Finalize ou descarte o treino em andamento antes de repetir outro." }
    val sets = completed(workout.sets)
    require(sets.isNotEmpty()) { "Este treino não tem séries válidas para repetir." }
    val exercises = sets.groupBy { it.exerciseId }.map { (exerciseId, previous) ->
        ExerciseSpec(
            exerciseId = exerciseId,
            sets = previous.size,
            reps = previous.first().reps.toInt(),
            // Finished history does not record rest intervals. Start with 90s;
            // the user can change this locally in the active exercise menu.
            restSeconds = 90,
        )
    }
    return state.copy(
        active = ActiveWorkout(
            routineId = workout.routineId,
            title = workout.title,
            startedAt = now,
            exercises = exercises,
            sets = sets.map { it.copy(id = uid(), done = false) },
        ),
    )
}

/** A factual snapshot of valid, saved sets for one exercise in one session. */
data class ExerciseSessionSummary(
    val workoutId: String,
    val title: String,
    val endedAt: Long,
    val setCount: Int,
    val totalReps: Int,
    val maximumWeight: Double,
    val totalVolume: Double,
)

data class ExerciseSessionComparison(
    val latest: ExerciseSessionSummary,
    val previous: ExerciseSessionSummary,
) {
    val setCountDifference: Int get() = latest.setCount - previous.setCount
    val totalRepsDifference: Int get() = latest.totalReps - previous.totalReps
    val maximumWeightDifference: Double get() = latest.maximumWeight - previous.maximumWeight
    val totalVolumeDifference: Double get() = latest.totalVolume - previous.totalVolume
}

/** Raw differences, never a score or claim about training outcomes. */
fun compareExerciseSessions(workouts: List<Workout>, exerciseId: String): ExerciseSessionComparison? {
    // Timestamp ties use persisted metadata so sync/list ordering cannot flip the comparison.
    val sessions = exerciseHistory(workouts, exerciseId).sortedWith(
        compareByDescending<Workout> { it.endedAt }.thenByDescending { it.startedAt }.thenBy { it.id },
    ).take(2)
    if (sessions.size < 2) return null
    fun summary(workout: Workout) = ExerciseSessionSummary(
        workoutId = workout.id,
        title = workout.title,
        endedAt = workout.endedAt,
        setCount = workout.sets.size,
        totalReps = workout.sets.sumOf { it.reps.toInt() },
        maximumWeight = workout.sets.maxOf { requireNotNull(numeric(it.weight)) },
        totalVolume = volume(workout.sets),
    )
    return ExerciseSessionComparison(summary(sessions[0]), summary(sessions[1]))
}

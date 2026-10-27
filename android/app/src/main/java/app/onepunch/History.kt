package app.onepunch

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

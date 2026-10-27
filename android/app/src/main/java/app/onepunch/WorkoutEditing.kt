package app.onepunch

internal fun workoutSetsInExerciseOrder(sets: List<WorkoutSet>, exercises: List<ExerciseSpec>): List<WorkoutSet> {
    val order = exercises.mapIndexed { index, spec -> spec.exerciseId to index }.toMap()
    return sets.sortedBy { order[it.exerciseId] ?: Int.MAX_VALUE }
}

fun moveWorkoutExercise(active: ActiveWorkout, exerciseId: String, direction: Int): ActiveWorkout {
    require(direction == -1 || direction == 1) { "Escolha mover para cima ou para baixo." }
    val index = active.exercises.indexOfFirst { it.exerciseId == exerciseId }
    if (index == -1 || index + direction !in active.exercises.indices) return active
    val exercises = active.exercises.toMutableList()
    val other = exercises[index + direction]
    exercises[index + direction] = exercises[index]
    exercises[index] = other
    return active.copy(exercises = exercises, sets = workoutSetsInExerciseOrder(active.sets, exercises))
}

fun replaceWorkoutExercise(
    active: ActiveWorkout,
    exerciseId: String,
    replacementId: String,
    workouts: List<Workout>,
): ActiveWorkout {
    val index = active.exercises.indexOfFirst { it.exerciseId == exerciseId }
    require(index >= 0) { "O exercício original não está mais neste treino." }
    require(replacementId.isNotBlank()) { "Escolha um exercício para substituir." }
    require(active.exercises.none { it.exerciseId == replacementId }) {
        "O exercício escolhido já está neste treino."
    }
    val original = active.exercises[index]
    val oldSets = active.sets.filter { it.exerciseId == exerciseId }
    // Preserve everything explicitly marked done, even an unreadable legacy value.
    // Completion/history validation remains the responsibility of finishWorkout.
    val kept = oldSets.filter { it.done }
    val pending = oldSets.count { !it.done }
    require(pending > 0 || kept.isEmpty()) { "Este exercício não tem séries pendentes para substituir." }
    val count = if (oldSets.isEmpty()) original.sets.coerceAtLeast(1) else pending
    val replacement = original.copy(exerciseId = replacementId, sets = count)
    val exercises = active.exercises.toMutableList()
    if (kept.isEmpty()) {
        exercises[index] = replacement
    } else {
        exercises[index] = original.copy(sets = kept.size)
        exercises.add(index + 1, replacement)
    }
    // Prefill only from the replacement's own history; all new sets stay unchecked.
    val generated = addExercise(ActiveWorkout(), replacement, workouts).sets
    val sets = active.sets.filterNot { it.exerciseId == exerciseId && !it.done } + generated
    return active.copy(exercises = exercises, sets = workoutSetsInExerciseOrder(sets, exercises))
}

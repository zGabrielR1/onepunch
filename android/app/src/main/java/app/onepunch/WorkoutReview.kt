package app.onepunch

data class WorkoutExerciseReview(
    val exerciseId: String,
    val includedSets: List<WorkoutSet>,
    val excludedCount: Int,
)

data class WorkoutReview(
    val includedSets: List<WorkoutSet>,
    val pendingCount: Int,
    val invalidDoneCount: Int,
    val exercises: List<WorkoutExerciseReview>,
) {
    val completedCount: Int get() = includedSets.size
    val excludedCount: Int get() = pendingCount + invalidDoneCount
    val totalCount: Int get() = completedCount + excludedCount
    val totalVolume: Double get() = volume(includedSets)
}

fun workoutTitleValid(title: String): Boolean = title.trim().isNotEmpty()

fun reviewWorkout(active: ActiveWorkout): WorkoutReview {
    val included = workoutSetsInExerciseOrder(completed(active.sets), active.exercises)
    val allByExercise = active.sets.groupBy { it.exerciseId }
    val includedByExercise = included.groupBy { it.exerciseId }
    val order = (active.exercises.map { it.exerciseId } + allByExercise.keys).distinct()
    return WorkoutReview(
        includedSets = included,
        pendingCount = active.sets.count { !it.done },
        invalidDoneCount = active.sets.count { it.done && !validSet(it) },
        exercises = order.map { id ->
            val sets = includedByExercise[id] ?: emptyList()
            WorkoutExerciseReview(id, sets, (allByExercise[id]?.size ?: 0) - sets.size)
        },
    )
}

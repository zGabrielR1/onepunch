package app.onepunch

fun filterExercises(
    catalog: List<Exercise>,
    query: String = "",
    muscle: String = "Todos",
    equipment: String = "Todos",
): List<Exercise> {
    val terms = normalized(query).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return catalog.filter { exercise ->
        val text = normalized("${exercise.name} ${exercise.muscle} ${exercise.equipment}")
        (muscle == "Todos" || exercise.muscle == muscle) &&
            (equipment == "Todos" || exercise.equipment == equipment) &&
            terms.all { text.contains(it) }
    }
}

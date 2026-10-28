package app.onepunch

import org.json.JSONObject

// Local editor recovery only. Never include drafts in Workspace or sync payloads.
data class RoutineDraft(
    val original: Routine,
    val draft: Routine = original,
    val isNew: Boolean = false,
)

/** Change a draft's movement without losing its position or unfinished targets. */
fun replaceRoutineExercise(
    draft: Routine,
    exerciseId: String,
    replacementId: String,
    catalog: List<Exercise>,
): Routine {
    val index = draft.exercises.indexOfFirst { it.exerciseId == exerciseId }
    require(index >= 0) { "O exercício original não está mais nesta rotina." }
    require(catalog.any { it.id == replacementId }) { "Escolha um exercício do catálogo." }
    if (exerciseId == replacementId) return draft
    require(draft.exercises.none { it.exerciseId == replacementId }) {
        "O exercício escolhido já está nesta rotina."
    }
    return draft.copy(exercises = draft.exercises.mapIndexed { i, spec ->
        if (i == index) spec.copy(exerciseId = replacementId) else spec
    })
}

fun copyRoutineTemplate(template: Routine, now: Long = System.currentTimeMillis()): Routine =
    template.copy(
        id = uid(),
        archived = false,
        updatedAt = now,
        exercises = template.exercises.map { it.copy() },
    )

fun routineDraftJson(value: RoutineDraft): JSONObject =
    JSONObject()
        .put("version", 1)
        .put("original", routineJson(value.original))
        .put("draft", routineJson(value.draft))
        .put("isNew", value.isNew)

fun routineDraftFrom(value: JSONObject): RoutineDraft {
    require(value.getInt("version") == 1) { "Versão de rascunho desconhecida." }
    val original = routineFrom(value.getJSONObject("original"))
    val draft = routineFrom(value.getJSONObject("draft"))
    require(original.id.isNotBlank()) { "O rascunho não tem identificação." }
    require(original.id == draft.id) { "O rascunho não corresponde à rotina original." }
    require(draft.exercises.map { it.exerciseId }.distinct().size == draft.exercises.size) {
        "O rascunho contém exercícios duplicados."
    }
    return RoutineDraft(original, draft, value.getBoolean("isNew"))
}

fun commitRoutineDraft(
    state: Workspace,
    value: RoutineDraft,
    asCopy: Boolean = false,
    now: Long = System.currentTimeMillis(),
): Workspace {
    require(value.original.id == value.draft.id) { "O rascunho não corresponde à rotina original." }
    require(routineValid(value.draft)) { "Revise o nome, os exercícios e as séries da rotina." }
    val current = state.routines.find { it.id == value.original.id }
    val originalUnchanged = if (value.isNew) current == null else current == value.original
    require(asCopy || originalUnchanged) {
        "A rotina mudou desde que o rascunho foi aberto. Salve como nova rotina para manter as duas versões."
    }
    val saved = value.draft.copy(
        id = if (asCopy) uid() else value.draft.id,
        name = if (asCopy) value.draft.name.trim().take(72) + " (cópia)" else value.draft.name.trim(),
        archived = if (asCopy) false else value.draft.archived,
        updatedAt = maxOf(now, value.original.updatedAt + 1),
    )
    return state.copy(
        routines = if (asCopy || value.isNew) state.routines + saved
        else state.routines.map { if (it.id == saved.id) saved else it },
    )
}

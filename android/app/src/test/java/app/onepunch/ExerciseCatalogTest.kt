package app.onepunch

import org.junit.Assert.*
import org.junit.Test

class ExerciseCatalogTest {
    private val catalog = listOf(
        Exercise("machine", "Supino na máquina", "Peito", "Máquina", ""),
        Exercise("dumbbell", "Supino reto com halteres", "Peito", "Halteres", ""),
        Exercise("row", "Remada na máquina", "Costas", "Máquina", ""),
    )

    @Test fun searchCombinesWordsAccentsAndFilters() {
        assertEquals(listOf(catalog[0]), filterExercises(catalog, " MAQUINA  SUPINO ", "Peito", "Máquina"))
        assertTrue(filterExercises(catalog, "supino", "Costas").isEmpty())
        assertTrue(filterExercises(catalog, "supino", "Peito", "Peso corporal").isEmpty())
        assertEquals(catalog, filterExercises(catalog, "  "))
        assertEquals(3, catalog.size)
    }
}

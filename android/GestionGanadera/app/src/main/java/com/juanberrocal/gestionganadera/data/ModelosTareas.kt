package com.juanberrocal.gestionganadera.data

import java.time.LocalDate

/**
 * Eventos de sanidad animal.
 * @param categoriaApi valor del campo "categoria" de MongoDB (lo usa el panel web para agrupar).
 */
enum class TipoTareaEspecial(val etiqueta: String, val categoriaApi: String) {
    VACUNACION("Vacunación", "Vacunación"),
    VISITA_VETERINARIA("Visita veterinaria / Palpación", "Sanidad animal"),
    DESPARASITACION("Desparasitación / Purga", "Desparasitación"),
    TRATAMIENTO_MEDICO("Tratamiento médico", "Sanidad animal"),
    CONTROL_REPRODUCTIVO("Control reproductivo / Secado", "Reproducción"),
    OTRO("Otro manejo", "Sanidad animal");

    companion object {
        fun desde(texto: String?): TipoTareaEspecial? =
            if (texto.isNullOrBlank()) null
            else entries.firstOrNull { it.name == texto || it.etiqueta.equals(texto.trim(), true) }
    }
}

data class Tarea(
    /** id LOCAL estable (UUID de Room). "" = tarea nueva todavía sin guardar. */
    val id: String = "",
    val titulo: String,
    val descripcion: String = "",
    val fechaProgramada: LocalDate,
    val completada: Boolean = false,
    val esTareaEspecial: Boolean = false,
    /** Solo si [esTareaEspecial]. */
    val tipoTareaEspecial: TipoTareaEspecial? = null,
    /** Animal asociado; null = todo el hato. */
    val animalId: String? = null,
    /** "#008 - Luna" (copia del momento en que se programó). */
    val animalCodigoNombre: String? = null
) {
    fun vencida(hoy: LocalDate = LocalDate.now()): Boolean = !completada && fechaProgramada.isBefore(hoy)

    companion object {
        const val CATEGORIA_GENERAL = "General"
    }
}

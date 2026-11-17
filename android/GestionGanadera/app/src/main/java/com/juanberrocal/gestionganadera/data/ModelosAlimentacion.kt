package com.juanberrocal.gestionganadera.data

import com.juanberrocal.gestionganadera.logica.Formato
import java.time.LocalDate

enum class TipoInsumo(val etiqueta: String, val etiquetaCorta: String) {
    ALIMENTO("Alimento / Suplemento", "Alimento"),
    MEDICAMENTO("Medicamento / Tratamiento", "Medicamento");

    /** Categorías sugeridas en el formulario. */
    val categorias: List<String>
        get() = when (this) {
            ALIMENTO -> listOf("Concentrado", "Sal mineralizada", "Forraje / Ensilaje", "Heno", "Melaza", "Otro")
            MEDICAMENTO -> listOf(
                "Antibiótico", "Desparasitante", "Vitamina", "Antiinflamatorio", "Vacuna", "Hormonal", "Otro"
            )
        }

    companion object {
        fun desde(texto: String?): TipoInsumo =
            entries.firstOrNull { it.name == texto || it.etiqueta.equals(texto?.trim(), true) } ?: ALIMENTO
    }
}

enum class UnidadConsumo(val etiqueta: String) {
    KILOGRAMOS("kg"),
    GRAMOS("g"),
    MILILITROS("ml"),
    DOSIS("dosis");

    companion object {
        fun desde(texto: String?): UnidadConsumo =
            entries.firstOrNull { it.name == texto || it.etiqueta.equals(texto?.trim(), true) } ?: KILOGRAMOS
    }
}

enum class FrecuenciaConsumo(val etiqueta: String) {
    DIARIO("Diario"),
    CADA_12_HORAS("Cada 12 horas"),
    SEMANAL("Semanal"),
    DOSIS_UNICA("Dosis única");

    companion object {
        fun desde(texto: String?): FrecuenciaConsumo =
            entries.firstOrNull { it.name == texto || it.etiqueta.equals(texto?.trim(), true) } ?: DIARIO
    }
}

/**
 * Qué come o qué medicamento recibe un animal ([animalId]) o todo un grupo productivo ([grupoProposito]).
 *
 * Retiro de leche: mientras el tratamiento esté [activo], y durante [diasRetiro] días después de
 * [fechaFin], la leche de las vacas afectadas debe ir a "Retiro médico (Descarte)".
 */
data class RegistroAlimentacionMedicamento(
    /** id LOCAL estable (UUID de Room). "" = registro nuevo. */
    val id: String = "",
    val animalId: String? = null,
    val grupoProposito: PropositoEtapa? = null,
    /** "#008 - Luna" o "Grupo: Producción / En ordeño". */
    val objetivoLabel: String,
    val tipoRegistro: TipoInsumo,
    val categoriaDetalle: String,
    val nombreProducto: String,
    val cantidad: Double,
    val unidad: UnidadConsumo,
    val frecuencia: FrecuenciaConsumo,
    val tieneRetiroLeche: Boolean = false,
    val diasRetiro: Int = 0,
    val activo: Boolean = true,
    val fechaInicio: LocalDate = LocalDate.now(),
    /** Día en que se finalizó (activo = false). El retiro se cuenta desde aquí. */
    val fechaFin: LocalDate? = null
) {
    val esMedicamento: Boolean get() = tipoRegistro == TipoInsumo.MEDICAMENTO

    /** "4 kg · Diario" */
    val dosisTexto: String
        get() = "${Formato.numero(cantidad)} ${unidad.etiqueta} · ${frecuencia.etiqueta}"

    /** ¿Aplica a este animal, ya sea directamente o por pertenecer al grupo? */
    fun aplicaA(animal: Animal): Boolean =
        if (animalId != null) animalId == animal.id
        else grupoProposito != null && animal.proposito == grupoProposito

    /** Último día en que la leche sigue en retiro; null si no hay retiro o el tratamiento sigue activo. */
    val finRetiro: LocalDate?
        get() = if (!esMedicamento || !tieneRetiroLeche || activo) null
        else (fechaFin ?: fechaInicio).plusDays(diasRetiro.toLong())

    /** Leche en retiro hoy: tratamiento activo, o finalizado hace menos de [diasRetiro] días. */
    fun retiroVigente(hoy: LocalDate = LocalDate.now()): Boolean {
        if (!esMedicamento || !tieneRetiroLeche) return false
        if (activo) return true
        return finRetiro?.let { !hoy.isAfter(it) } == true
    }

    /** Medicamento que todavía cuenta: activo o con retiro corriendo. */
    fun medicamentoVigente(hoy: LocalDate = LocalDate.now()): Boolean = esMedicamento && (activo || retiroVigente(hoy))

    companion object {
        fun etiquetaGrupo(grupo: PropositoEtapa): String = "Grupo: ${nombreGrupo(grupo)}"

        /** "Crianza / Levante" se repite en hembras y machos: se aclara cuál es. */
        fun nombreGrupo(grupo: PropositoEtapa): String = when (grupo) {
            PropositoEtapa.CRIA_LEVANTE -> "${grupo.etiqueta} (hembras)"
            PropositoEtapa.TERNERO_LEVANTE -> "${grupo.etiqueta} (machos)"
            else -> grupo.etiqueta
        }
    }
}

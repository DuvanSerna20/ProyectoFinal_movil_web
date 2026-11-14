package com.juanberrocal.gestionganadera.data

import java.time.LocalDate

enum class JornadaOrdeno(val etiqueta: String) {
    MANANA("Mañana"),
    TARDE("Tarde");

    companion object {
        fun desde(texto: String?): JornadaOrdeno =
            if (texto?.contains("Tarde", ignoreCase = true) == true || texto == TARDE.name) TARDE else MANANA
    }
}

/**
 * A dónde va la leche ordeñada. Solo [TANQUE_VENTA] cuenta como "litros comerciales"
 * (total del día en Ordeño y en Inicio).
 */
enum class DestinoLeche(val etiqueta: String, val etiquetaCorta: String) {
    TANQUE_VENTA("Tanque / Venta", "Tanque"),
    RETIRO_MEDICO("Retiro médico (Descarte)", "Retiro médico"),
    CRIA_TERNERO("Consumo ternero", "Ternero");

    val esComercial: Boolean get() = this == TANQUE_VENTA

    companion object {
        fun desde(texto: String?): DestinoLeche =
            entries.firstOrNull { it.etiqueta.equals(texto?.trim(), true) || it.name == texto } ?: TANQUE_VENTA
    }
}

data class RegistroOrdeno(
    /** _id de MongoDB. "" = registro nuevo todavía sin guardar. */
    val id: String = "",
    /** Vaca ordeñada. null solo si es el registro general del tanque (todo el hato). */
    val animalId: String?,
    /** "#008 - Luna" o [ETIQUETA_TANQUE]. Copia del momento del registro. */
    val animalCodigoNombre: String,
    val fecha: LocalDate,
    val jornada: JornadaOrdeno,
    val litros: Double,
    val destinoLeche: DestinoLeche = DestinoLeche.TANQUE_VENTA
) {
    val esTanqueGeneral: Boolean get() = animalId == null

    companion object {
        const val ETIQUETA_TANQUE = "Todo el hato (Tanque)"
    }
}

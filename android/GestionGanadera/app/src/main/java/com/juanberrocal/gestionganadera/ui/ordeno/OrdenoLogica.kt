package com.juanberrocal.gestionganadera.ui.ordeno

import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.DestinoLeche
import com.juanberrocal.gestionganadera.data.JornadaOrdeno
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import java.time.LocalDate

data class ResumenOrdenoDia(
    /** Solo leche que va al tanque / venta. */
    val litrosComerciales: Double,
    val litrosManana: Double,
    val litrosTarde: Double,
    val litrosDescarte: Double,
    val litrosTernero: Double,
    val vacasOrdenadas: Int,
    val vacasEnProduccion: Int
) {
    val progreso: Float
        get() = if (vacasEnProduccion == 0) 0f else (vacasOrdenadas.toFloat() / vacasEnProduccion).coerceIn(0f, 1f)
}

object OrdenoLogica {

    fun registrosDelDia(registros: List<RegistroOrdeno>, dia: LocalDate): List<RegistroOrdeno> =
        registros.filter { it.fecha == dia }
            .sortedWith(compareBy<RegistroOrdeno> { it.jornada.ordinal }.thenBy { it.esTanqueGeneral }.thenBy { it.animalCodigoNombre })

    /**
     * Mañana/Tarde se desglosan sobre los litros comerciales (así suman el total).
     * "Ordeñadas" = vacas en producción con al menos un registro ese día (cualquier jornada o destino).
     */
    fun resumen(registros: List<RegistroOrdeno>, dia: LocalDate, vacasEnProduccion: List<Animal>): ResumenOrdenoDia {
        val delDia = registros.filter { it.fecha == dia }
        val comerciales = delDia.filter { it.destinoLeche.esComercial }
        val idsProduccion = vacasEnProduccion.map { it.id }.toSet()
        return ResumenOrdenoDia(
            litrosComerciales = comerciales.sumOf { it.litros },
            litrosManana = comerciales.filter { it.jornada == JornadaOrdeno.MANANA }.sumOf { it.litros },
            litrosTarde = comerciales.filter { it.jornada == JornadaOrdeno.TARDE }.sumOf { it.litros },
            litrosDescarte = delDia.filter { it.destinoLeche == DestinoLeche.RETIRO_MEDICO }.sumOf { it.litros },
            litrosTernero = delDia.filter { it.destinoLeche == DestinoLeche.CRIA_TERNERO }.sumOf { it.litros },
            vacasOrdenadas = delDia.mapNotNull { it.animalId }.filter { it in idsProduccion }.toSet().size,
            vacasEnProduccion = vacasEnProduccion.size
        )
    }

    fun litrosComerciales(registros: List<RegistroOrdeno>, dia: LocalDate): Double =
        registros.filter { it.fecha == dia && it.destinoLeche.esComercial }.sumOf { it.litros }

    /**
     * Otro registro de la misma vaca (o del tanque general, si animalId es null),
     * mismo día y misma jornada. Se excluye el registro que se está editando.
     */
    fun duplicado(
        registros: List<RegistroOrdeno>,
        animalId: String?,
        fecha: LocalDate,
        jornada: JornadaOrdeno,
        excluirId: String
    ): RegistroOrdeno? = registros.firstOrNull {
        it.id != excluirId && it.animalId == animalId && it.fecha == fecha && it.jornada == jornada
    }
}

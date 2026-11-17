package com.juanberrocal.gestionganadera.ui.potreros

import com.juanberrocal.gestionganadera.data.EstadoPotrero
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.UnidadArea
import com.juanberrocal.gestionganadera.logica.Calculos
import com.juanberrocal.gestionganadera.logica.Formato
import java.time.LocalDate
import java.util.Locale

data class ResumenPotreros(
    val total: Int,
    val ocupados: Int,
    val listos: Int,
    val enDescanso: Int,
    val areaHectareas: Double,
    val fincasActivas: Int,
    val departamentos: Int
)

/** [potreros] ya debe venir filtrado por la finca seleccionada (si hay una). */
fun resumenPotreros(potreros: List<Potrero>, fincas: List<Finca>): ResumenPotreros = ResumenPotreros(
    total = potreros.size,
    ocupados = potreros.count { it.estado == EstadoPotrero.OCUPADO },
    listos = potreros.count { it.estado == EstadoPotrero.LISTO },
    enDescanso = potreros.count { it.estado == EstadoPotrero.EN_DESCANSO },
    areaHectareas = potreros.sumOf { it.areaHectareas },
    fincasActivas = fincas.size,
    departamentos = fincas.map { it.departamento }.distinct().size
)

fun List<Potrero>.deFinca(fincaId: String?): List<Potrero> =
    if (fincaId == null) this else filter { it.fincaId == fincaId }

/** Estado del ciclo actual para la barra de progreso. null si el estado no tiene ciclo. */
data class ProgresoCiclo(
    val fraccion: Float,
    val diasTranscurridos: Long,
    val diasPlaneados: Double,
    val cumplido: Boolean,
    val esOcupacion: Boolean
)

fun Potrero.progresoCiclo(hoy: LocalDate = LocalDate.now()): ProgresoCiclo? {
    val planeado = when (estado) {
        EstadoPotrero.OCUPADO -> diasOcupacionPlaneados
        EstadoPotrero.EN_DESCANSO -> diasDescansoPlaneados.toDouble()
        else -> return null
    }
    if (planeado <= 0) return null
    val dias = Calculos.diasDesde(fechaUltimoCambio, hoy)
    return ProgresoCiclo(
        fraccion = (dias / planeado).toFloat().coerceIn(0f, 1f),
        diasTranscurridos = dias,
        diasPlaneados = planeado,
        cumplido = dias >= planeado,
        esOcupacion = estado == EstadoPotrero.OCUPADO
    )
}

/**
 * Rotación aislada por finca: solo potreros LISTOS de la MISMA finca que el origen.
 * Los potreros sin finca (antiguos) no tienen destinos.
 */
fun destinosRotacion(origen: Potrero, potreros: List<Potrero>): List<Potrero> {
    val finca = origen.fincaId ?: return emptyList()
    return potreros.filter {
        it.fincaId == finca && it.estado == EstadoPotrero.LISTO &&
            it.mongoId != null && it.mongoId != origen.mongoId
    }
}

object ValidacionPotrero {
    private val LOCALE_CO = Locale.forLanguageTag("es-CO")
    private val ESPACIOS = Regex("\\s+")

    fun nombre(texto: String): String =
        texto.trim().replace(ESPACIOS, " ").replaceFirstChar { it.titlecase(LOCALE_CO) }

    /** El nombre solo debe ser único dentro de la misma finca. */
    fun nombreRepetido(nombre: String, fincaId: String?, idActual: String, potreros: List<Potrero>): Boolean =
        potreros.any { it.id != idActual && it.fincaId == fincaId && it.nombre.equals(nombre, ignoreCase = true) }

    fun nombreFincaRepetido(nombre: String, fincas: List<Finca>): Boolean =
        fincas.any { it.nombre.equals(nombre.trim(), ignoreCase = true) }
}

/** "2,5 ha", "3 cuadras (1,92 ha)", "4.500 m² (0,45 ha)" */
fun Potrero.areaLegible(): String {
    val base = "${Formato.numero(area)} ${unidadArea.abreviatura}"
    return if (unidadArea == UnidadArea.HECTAREAS) base
    else "$base (${Formato.numero(areaHectareas)} ha)"
}

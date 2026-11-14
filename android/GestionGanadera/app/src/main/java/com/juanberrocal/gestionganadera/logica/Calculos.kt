package com.juanberrocal.gestionganadera.logica

import com.juanberrocal.gestionganadera.data.Potrero
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

object Calculos {

    const val M2_POR_HECTAREA = 10_000.0

    fun areaM2(largo: Double, ancho: Double): Double = largo * ancho

    fun hectareas(areaM2: Double): Double = areaM2 / M2_POR_HECTAREA

    /** Admite ocupaciones de medio día (0.5). */
    fun potrerosNecesarios(diasDescanso: Int, diasOcupacion: Double): Int {
        require(diasOcupacion > 0) { "Los días de ocupación deben ser mayores a 0" }
        return ceil(diasDescanso / diasOcupacion).toInt() + 1
    }

    fun tiempoTotalRegulacion(diasOcupacion: Double, numeroPotreros: Int): Double =
        diasOcupacion * numeroPotreros

    /** Con 0.5 días el cambio es el mismo día (por la tarde); se redondea hacia arriba en días. */
    fun proximoCambio(fechaIngreso: LocalDate, diasOcupacion: Double): LocalDate =
        fechaIngreso.plusDays(ceil(diasOcupacion).toLong().coerceAtLeast(0))

    /** Días transcurridos desde el último cambio de estado del potrero. */
    fun diasDesde(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(fecha, hoy).coerceAtLeast(0)
}

fun Potrero.hectareas(): Double = areaHectareas

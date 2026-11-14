package com.juanberrocal.gestionganadera.logica

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formato {

    private val locale: Locale = Locale.forLanguageTag("es-CO")
    private val fechaCorta = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val fechaLarga = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", locale)

    fun numero(valor: Double, maxDecimales: Int = 2): String {
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = maxDecimales
        return nf.format(valor)
    }

    fun plano(valor: Double): String =
        if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()

    fun litros(valor: Double): String = "${numero(valor)} L"

    fun dias(cantidad: Int): String = if (cantidad == 1) "1 día" else "$cantidad días"

    /** 0.5 -> "0,5 días", 1.0 -> "1 día", 3.0 -> "3 días" */
    fun dias(cantidad: Double): String =
        if (cantidad == 1.0) "1 día" else "${numero(cantidad, 1)} días"

    fun fecha(fecha: LocalDate): String = fecha.format(fechaCorta)

    fun fecha(iso: String): String = fecha(LocalDate.parse(iso))

    fun fechaLarga(fecha: LocalDate): String =
        fecha.format(fechaLarga).replaceFirstChar { it.uppercase(locale) }

    fun leerDecimal(texto: String?): Double? =
        texto?.trim()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it.isFinite() }

    fun leerEntero(texto: String?): Int? = texto?.trim()?.toIntOrNull()
}

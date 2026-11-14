package com.juanberrocal.gestionganadera.ui.ganado

import androidx.annotation.StringRes
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.EstadoReproductivo
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import java.util.Locale

/** Filtros rápidos de la lista. No son excluyentes: una vaca en ordeño cargada sale en ambos. */
enum class FiltroGanado(@param:StringRes val etiqueta: Int, val coincide: (Animal) -> Boolean) {
    TODOS(R.string.filtro_todos, { true }),
    EN_ORDENO(R.string.filtro_ordeno, { it.estaEnOrdeno }),
    CARGADAS(R.string.filtro_cargadas, { it.esHembraBovina && it.estadoReproductivo == EstadoReproductivo.CARGADA }),
    HORRAS(R.string.filtro_horras, { it.esHembraBovina && it.estadoReproductivo == EstadoReproductivo.HORRA }),
    CRIANZA(R.string.filtro_crianza, { it.esBovino && it.proposito in PROPOSITOS_CRIANZA }),
    CEBA_ENGORDE(R.string.filtro_ceba, { it.esBovino && it.proposito in PROPOSITOS_CEBA }),
    TOROS_REPRODUCTORES(R.string.filtro_toros, { it.esBovino && it.proposito == PropositoEtapa.TORO_REPRODUCTOR }),
    OTROS(R.string.filtro_otros, { !it.esBovino })
}

private val PROPOSITOS_CRIANZA = setOf(PropositoEtapa.CRIA_LEVANTE, PropositoEtapa.TERNERO_LEVANTE)
private val PROPOSITOS_CEBA = setOf(PropositoEtapa.ENGORDE, PropositoEtapa.TORO_CEBA)

data class ResumenGanado(
    val totalBovinos: Int,
    // Línea 1: hembras
    val enOrdeno: Int,
    val cargadas: Int,
    val horras: Int,
    val criaEngordeHembras: Int,
    // Línea 2: machos y peso
    val torosReproductores: Int,
    val machosEnCeba: Int,
    /** Promedio de los bovinos que tienen peso registrado; null si ninguno lo tiene. */
    val pesoPromedioKg: Double?,
    val otrosAnimales: Int
)

fun List<Animal>.resumen(): ResumenGanado {
    val bovinos = filter { it.esBovino }
    val hembras = bovinos.filter { it.esHembraBovina }
    val pesos = bovinos.mapNotNull { it.pesoKg }
    return ResumenGanado(
        totalBovinos = bovinos.size,
        enOrdeno = hembras.count { it.estaEnOrdeno },
        cargadas = hembras.count { it.estadoReproductivo == EstadoReproductivo.CARGADA },
        horras = hembras.count { it.estadoReproductivo == EstadoReproductivo.HORRA },
        criaEngordeHembras = hembras.count {
            it.proposito == PropositoEtapa.CRIA_LEVANTE || it.proposito == PropositoEtapa.ENGORDE
        },
        torosReproductores = bovinos.count { it.proposito == PropositoEtapa.TORO_REPRODUCTOR },
        machosEnCeba = bovinos.count { it.proposito == PropositoEtapa.TORO_CEBA },
        pesoPromedioKg = if (pesos.isEmpty()) null else pesos.average(),
        otrosAnimales = size - bovinos.size
    )
}

fun List<Animal>.filtrar(busqueda: String, filtro: FiltroGanado): List<Animal> {
    val texto = busqueda.trim().removePrefix("#")
    return filter { animal ->
        filtro.coincide(animal) && (
            texto.isEmpty() ||
                animal.nombre.contains(texto, ignoreCase = true) ||
                animal.codigo.contains(texto, ignoreCase = true)
            )
    }
}

/** Limpieza de los datos que escribe el usuario antes de guardarlos. */
object FormatoAnimal {
    private val LOCALE_CO = Locale.forLanguageTag("es-CO")
    private val ESPACIOS = Regex("\\s+")

    /** " # 0 08 " -> "008". Sin espacios ni "#" inicial, en mayúsculas. */
    fun codigo(texto: String): String =
        texto.replace(ESPACIOS, "").removePrefix("#").uppercase(LOCALE_CO)

    /** "  luna   blanca " -> "Luna blanca". */
    fun nombre(texto: String): String =
        texto.trim().replace(ESPACIOS, " ").replaceFirstChar { it.titlecase(LOCALE_CO) }

    /** ¿Ya hay otro animal (distinto al que se edita) con ese código? */
    fun codigoRepetido(codigo: String, idActual: String, existentes: List<Animal>): Boolean =
        existentes.any { it.id != idActual && it.codigo.equals(codigo, ignoreCase = true) }
}

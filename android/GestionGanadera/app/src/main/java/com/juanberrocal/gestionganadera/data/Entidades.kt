package com.juanberrocal.gestionganadera.data

import java.time.LocalDate

enum class Sexo(val etiqueta: String) {
    HEMBRA("Hembra"),
    MACHO("Macho")
}

/** Especie del animal. Solo los BOVINO cuentan como "cabezas de ganado". */
enum class Especie(val etiqueta: String) {
    BOVINO("Bovino"),
    EQUINO("Equino"),
    CANINO("Canino"),
    OTRO("Otro")
}

/**
 * Propósito / etapa del animal. Las opciones válidas dependen de la especie y el sexo:
 * use [opcionesPara] para no mezclar, por ejemplo, un macho con "Producción / En ordeño".
 */
enum class PropositoEtapa(
    val etiqueta: String,
    val etiquetaCorta: String,
    private val soloBovino: Boolean,
    private val sexo: Sexo?
) {
    PRODUCCION_ORDENO("Producción / En ordeño", "En ordeño", true, Sexo.HEMBRA),
    CRIA_LEVANTE("Crianza / Levante", "Crianza", true, Sexo.HEMBRA),
    VACA_SECA("Vaca seca / Descanso", "Seca", true, Sexo.HEMBRA),
    ENGORDE("Engorde / Ceba", "Engorde", true, Sexo.HEMBRA),
    TORO_REPRODUCTOR("Toro de reproducción", "Reproductor", true, Sexo.MACHO),
    TORO_CEBA("Toro de ceba / Engorde", "Ceba", true, Sexo.MACHO),
    TERNERO_LEVANTE("Crianza / Levante", "Crianza", true, Sexo.MACHO),
    TRABAJO_COMPANIA("Trabajo / Compañía", "Trabajo", false, null);

    /** Ceba, engorde y reproducción: el peso es obligatorio. */
    val requierePeso: Boolean get() = this == ENGORDE || this == TORO_CEBA || this == TORO_REPRODUCTOR

    companion object {
        fun opcionesPara(especie: Especie, sexo: Sexo): List<PropositoEtapa> =
            if (especie == Especie.BOVINO) entries.filter { it.soloBovino && it.sexo == sexo }
            else listOf(TRABAJO_COMPANIA)

        fun porDefecto(especie: Especie, sexo: Sexo): PropositoEtapa = opcionesPara(especie, sexo).first()
    }
}

/** Solo aplica a BOVINO + HEMBRA adulta. */
enum class EstadoReproductivo(val etiqueta: String, val etiquetaCorta: String) {
    CARGADA("Cargada / Preñada", "Cargada"),
    HORRA("Horra / Vacía", "Horra"),
    NO_APLICA("No aplica - Cría/Macho", "No aplica");

    companion object {
        /** Valor razonable al cambiar de propósito: las crías no aplican, las adultas arrancan horras. */
        fun sugerido(especie: Especie, sexo: Sexo, proposito: PropositoEtapa, actual: EstadoReproductivo?): EstadoReproductivo =
            when {
                especie != Especie.BOVINO || sexo != Sexo.HEMBRA -> NO_APLICA
                proposito == PropositoEtapa.CRIA_LEVANTE -> NO_APLICA
                actual == null || actual == NO_APLICA -> HORRA
                else -> actual
            }
    }
}

/** Razas bovinas más comunes en Colombia. La raza se guarda como texto en [Animal.raza]. */
object Razas {
    const val OTRA = "Otra / Cruce"

    val BOVINAS: List<String> = listOf(
        "Girolando",
        "Holstein",
        "Jersey",
        "Gyr",
        "Pardo Suizo",
        "Normando",
        "Ayrshire",
        "Brahman",
        "Guzerá",
        "Blanco Orejinegro (BON)",
        "Costeño con Cuernos",
        "Hartón del Valle",
        OTRA
    )

    /** Devuelve el nombre oficial de la lista si coincide (ignorando mayúsculas), o null. */
    fun bovinaOficial(raza: String): String? =
        BOVINAS.firstOrNull { it != OTRA && it.equals(raza.trim(), ignoreCase = true) }
}

data class Animal(
    /** _id de MongoDB. Vacío ("") = animal nuevo todavía sin guardar. */
    val id: String = "",
    val codigo: String,
    val nombre: String,
    val especie: Especie = Especie.BOVINO,
    val sexo: Sexo = Sexo.HEMBRA,
    val raza: String,
    val proposito: PropositoEtapa = PropositoEtapa.PRODUCCION_ORDENO,
    val estadoReproductivo: EstadoReproductivo = EstadoReproductivo.HORRA,
    /** Peso actual (kg): clave para ceba/engorde y control general. */
    val pesoKg: Double? = null,
    /** L/día. Solo tiene sentido en ordeño; se actualiza al registrar ordeños de esta vaca. */
    val produccionLitrosDia: Double? = null,
    val fincaId: String? = null,
    /** mongoId del potrero asignado. */
    val potreroId: String? = null
) {
    val esBovino: Boolean get() = especie == Especie.BOVINO
    val esHembraBovina: Boolean get() = esBovino && sexo == Sexo.HEMBRA

    /** "#008 - Luna": así se muestra el animal en Ordeño, Tareas y Alimentación. */
    val codigoNombre: String get() = "#$codigo - $nombre"

    /** Exactamente las vacas que usa el módulo de Ordeño. */
    val estaEnOrdeno: Boolean get() = esHembraBovina && proposito == PropositoEtapa.PRODUCCION_ORDENO

    /** Hembra adulta con estado reproductivo relevante (cargada u horra). */
    val muestraEstadoReproductivo: Boolean
        get() = esHembraBovina && estadoReproductivo != EstadoReproductivo.NO_APLICA
}

// ---------------------------------------------------------------------------------------------
// Fincas (una empresa ganadera puede tener varias, en distintos departamentos)
// ---------------------------------------------------------------------------------------------

enum class DepartamentoColombia(val etiqueta: String) {
    ANTIOQUIA("Antioquia"),
    ARAUCA("Arauca"),
    ATLANTICO("Atlántico"),
    BOLIVAR("Bolívar"),
    BOYACA("Boyacá"),
    CALDAS("Caldas"),
    CAQUETA("Caquetá"),
    CASANARE("Casanare"),
    CAUCA("Cauca"),
    CESAR("Cesar"),
    CHOCO("Chocó"),
    CORDOBA("Córdoba"),
    CUNDINAMARCA("Cundinamarca"),
    HUILA("Huila"),
    LA_GUAJIRA("La Guajira"),
    MAGDALENA("Magdalena"),
    META("Meta"),
    NARINO("Nariño"),
    NORTE_DE_SANTANDER("Norte de Santander"),
    QUINDIO("Quindío"),
    RISARALDA("Risaralda"),
    SANTANDER("Santander"),
    SUCRE("Sucre"),
    TOLIMA("Tolima"),
    VALLE_DEL_CAUCA("Valle del Cauca"),
    OTRO("Otro");

    companion object {
        fun desde(texto: String?): DepartamentoColombia =
            entries.firstOrNull { it.etiqueta.equals(texto?.trim(), true) || it.name == texto } ?: OTRO
    }
}

enum class PisoTermico(val etiqueta: String) {
    FRIO("Trópico Alto / Frío"),
    MEDIO("Templado / Medio"),
    CALIDO("Trópico Bajo / Cálido");

    companion object {
        fun desde(texto: String?): PisoTermico =
            entries.firstOrNull { it.etiqueta.equals(texto?.trim(), true) || it.name == texto } ?: CALIDO
    }
}

/** [id] es el _id de MongoDB: una finca solo existe en la app después de guardarse en el servidor. */
data class Finca(
    val id: String,
    val nombre: String,
    val departamento: DepartamentoColombia,
    val municipio: String,
    val pisoTermico: PisoTermico
) {
    /** "Hacienda El Roble · Risaralda (Pereira)" */
    val descripcion: String
        get() = "$nombre · ${departamento.etiqueta}" + if (municipio.isNotBlank()) " ($municipio)" else ""

    /** "Hacienda El Roble · Risaralda" */
    val etiquetaCorta: String get() = "$nombre · ${departamento.etiqueta}"
}

// ---------------------------------------------------------------------------------------------
// Potreros
// ---------------------------------------------------------------------------------------------

enum class UnidadArea(val etiqueta: String, val abreviatura: String, val m2PorUnidad: Double, val codigoApi: String) {
    HECTAREAS("Hectáreas (ha)", "ha", 10_000.0, "ha"),
    CUADRAS("Cuadras (6,400 m²)", "cuadras", 6_400.0, "cuadra"),
    METROS_CUADRADOS("Metros² (m²)", "m²", 1.0, "m2");

    companion object {
        fun desde(codigo: String?): UnidadArea =
            entries.firstOrNull { it.codigoApi == codigo || it.name == codigo } ?: METROS_CUADRADOS
    }
}

enum class TipoPasto(val etiqueta: String, val diasDescansoSugeridos: Int, val pisoIdeal: PisoTermico) {
    KIKUYO("Kikuyo", 35, PisoTermico.FRIO),
    RYEGRASS("Ryegrass", 30, PisoTermico.FRIO),
    ESTRELLA("Estrella Africana", 24, PisoTermico.MEDIO),
    BRACHIARIA("Brachiaria / Brizantha", 28, PisoTermico.CALIDO),
    GIRO_GUINEA("Mombasa / Guinea", 30, PisoTermico.CALIDO),
    IMPERIAL_KINGGRASS("King Grass / Corte", 40, PisoTermico.MEDIO),
    OTRO("Otro / Nativo", 25, PisoTermico.MEDIO);

    companion object {
        /** Acepta la etiqueta, el nombre del enum o textos antiguos como "Brachiaria". */
        fun desde(texto: String?): TipoPasto {
            val t = texto?.trim().orEmpty()
            if (t.isEmpty()) return BRACHIARIA
            return entries.firstOrNull { it.etiqueta.equals(t, true) || it.name.equals(t, true) }
                ?: entries.firstOrNull { p -> p.etiqueta.split("/").any { it.trim().equals(t, true) } }
                ?: entries.firstOrNull { it != OTRO && it.etiqueta.contains(t, true) }
                ?: OTRO
        }

        /** Primero los pastos ideales para el piso térmico, luego el resto. */
        fun ordenadosPara(piso: PisoTermico?): List<TipoPasto> =
            if (piso == null) entries.toList()
            else entries.sortedBy { if (it.pisoIdeal == piso) 0 else 1 }
    }
}

enum class EstadoPotrero(val etiqueta: String, val valorApi: String) {
    OCUPADO("En ocupación", "Ocupado"),
    EN_DESCANSO("En descanso", "En descanso"),
    LISTO("Listo para pastoreo", "Listo para pastoreo"),
    MANTENIMIENTO("En mantenimiento / Abonado", "En mantenimiento / Abonado");

    companion object {
        fun desde(texto: String?): EstadoPotrero = entries.firstOrNull {
            it.valorApi.equals(texto, true) || it.etiqueta.equals(texto, true) || it.name == texto
        } ?: EN_DESCANSO
    }
}

enum class CondicionPasto(val etiqueta: String) {
    OPTIMO("Altura óptima"),
    BAJO("Pasto bajo / Agotado"),
    ENCHARCADO("Encharcado / Invierno");

    companion object {
        fun desde(texto: String?): CondicionPasto =
            entries.firstOrNull { it.etiqueta.equals(texto, true) || it.name == texto } ?: OPTIMO
    }
}

enum class MotivoRotacion(val etiqueta: String) {
    CUMPLIO_DIAS("Cumplió días"),
    PASTO_AGOTADO("Se agotó el pasto"),
    CLIMA("Clima / Encharcamiento")
}

data class Potrero(
    val id: Long = 0L,
    /** _id de la finca. Obligatorio al crear/editar; puede ser null en potreros antiguos. */
    val fincaId: String? = null,
    val nombre: String,
    /** Área en la unidad que eligió el usuario. */
    val area: Double,
    val unidadArea: UnidadArea = UnidadArea.HECTAREAS,
    /** Solo si el área se calculó con largo x ancho (en metros). */
    val largo: Double? = null,
    val ancho: Double? = null,
    val tipoPasto: TipoPasto = TipoPasto.BRACHIARIA,
    val estado: EstadoPotrero = EstadoPotrero.EN_DESCANSO,
    val condicionPasto: CondicionPasto = CondicionPasto.OPTIMO,
    /** Permite medios días (0.5). */
    val diasOcupacionPlaneados: Double = 1.0,
    val diasDescansoPlaneados: Int = 30,
    val fechaUltimoCambio: LocalDate = LocalDate.now(),
    val cantidadAnimalesActuales: Int = 0,
    val mongoId: String? = null
) {
    val areaEnMetrosCuadrados: Double get() = area * unidadArea.m2PorUnidad
    val areaHectareas: Double get() = areaEnMetrosCuadrados / 10_000.0
}

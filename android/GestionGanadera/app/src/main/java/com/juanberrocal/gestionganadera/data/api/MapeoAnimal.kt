package com.juanberrocal.gestionganadera.data.api

import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.Especie
import com.juanberrocal.gestionganadera.data.EstadoReproductivo
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.PropositoEtapa.CRIA_LEVANTE
import com.juanberrocal.gestionganadera.data.PropositoEtapa.ENGORDE
import com.juanberrocal.gestionganadera.data.PropositoEtapa.PRODUCCION_ORDENO
import com.juanberrocal.gestionganadera.data.PropositoEtapa.TERNERO_LEVANTE
import com.juanberrocal.gestionganadera.data.PropositoEtapa.TORO_CEBA
import com.juanberrocal.gestionganadera.data.PropositoEtapa.TORO_REPRODUCTOR
import com.juanberrocal.gestionganadera.data.PropositoEtapa.TRABAJO_COMPANIA
import com.juanberrocal.gestionganadera.data.PropositoEtapa.VACA_SECA
import com.juanberrocal.gestionganadera.data.Sexo

/**
 * Conversión Animal <-> AnimalDto.
 *
 * En MongoDB se guardan las etiquetas legibles ("Bovino", "Producción / En ordeño"), igual que "sexo".
 * También se escribe el campo "estado" que muestra el panel web, derivado del propósito.
 * Lee datos de versiones anteriores ("categoria" o solo "estado").
 */
object MapeoAnimal {

    fun aDominio(dto: AnimalDto): Animal? {
        val id = dto.id ?: return null
        val sexo = if (dto.sexo.equals("Macho", ignoreCase = true)) Sexo.MACHO else Sexo.HEMBRA
        val especie = Especie.entries.firstOrNull { it.etiqueta.equals(dto.especie, true) || it.name == dto.especie }
            ?: Especie.BOVINO
        val validos = PropositoEtapa.opcionesPara(especie, sexo)
        val proposito = (desdeTexto(dto.proposito, sexo) ?: desdeCategoriaAntigua(dto.categoria, sexo) ?: desdeEstado(dto.estado, sexo))
            .takeIf { it in validos } ?: validos.first()

        val reproductivoGuardado = EstadoReproductivo.entries.firstOrNull {
            it.etiqueta.equals(dto.estadoReproductivo, true) || it.name == dto.estadoReproductivo
        } ?: if (dto.estado.equals("Gestante", true)) EstadoReproductivo.CARGADA else null
        val reproductivo = if (especie == Especie.BOVINO && sexo == Sexo.HEMBRA) {
            reproductivoGuardado ?: EstadoReproductivo.sugerido(especie, sexo, proposito, null)
        } else EstadoReproductivo.NO_APLICA

        return Animal(
            id = id,
            codigo = dto.codigo,
            nombre = dto.nombre,
            especie = especie,
            sexo = sexo,
            raza = dto.raza,
            proposito = proposito,
            estadoReproductivo = reproductivo,
            pesoKg = dto.pesoKg?.takeIf { it > 0 },
            produccionLitrosDia = dto.produccionLitrosDia?.takeIf { it > 0 },
            fincaId = dto.fincaId?.ifBlank { null },
            // OJO: aquí llega el mongoId del potrero; el repositorio lo traduce al id local de Room
            potreroId = dto.potreroId?.ifBlank { null },
            mongoId = id,
            fotoUrl = dto.fotoUrl?.ifBlank { null }
        )
    }

    fun aDto(animal: Animal, esNuevo: Boolean): AnimalDto = AnimalDto(
        codigo = animal.codigo,
        nombre = animal.nombre,
        raza = animal.raza,
        sexo = animal.sexo.etiqueta,
        pesoKg = animal.pesoKg,
        estado = estadoPanelWeb(animal),
        // Al editar no se envía para no reemplazar a quien lo registró originalmente
        registradoPor = if (esNuevo) "Android App" else null,
        especie = animal.especie.etiqueta,
        proposito = animal.proposito.etiqueta,
        estadoReproductivo = animal.estadoReproductivo.etiqueta,
        // 0 = sin producción (si deja de estar en ordeño se limpia; null no se enviaría)
        produccionLitrosDia = if (animal.estaEnOrdeno) animal.produccionLitrosDia ?: 0.0 else 0.0,
        fincaId = animal.fincaId.orEmpty(),
        potreroId = animal.potreroId.orEmpty()
    )

    /** Valor del enum "estado" del backend (models/Animal.js) que muestra el panel web. */
    fun estadoPanelWeb(a: Animal): String = when (a.proposito) {
        PRODUCCION_ORDENO -> "En producción"
        VACA_SECA -> if (a.estadoReproductivo == EstadoReproductivo.CARGADA) "Gestante" else "Seca"
        CRIA_LEVANTE, TERNERO_LEVANTE -> "Levante"
        ENGORDE, TORO_CEBA -> "Engorde"
        TORO_REPRODUCTOR -> "Reproductor"
        TRABAJO_COMPANIA -> "Trabajo / Compañía"
    }

    /** Etiqueta nueva o nombre del enum. "Crianza / Levante" se repite en hembra y macho: decide el sexo. */
    private fun desdeTexto(texto: String?, sexo: Sexo): PropositoEtapa? {
        if (texto.isNullOrBlank()) return null
        val coincidencias = PropositoEtapa.entries.filter { it.etiqueta.equals(texto.trim(), true) || it.name == texto }
        return coincidencias.firstOrNull { it in PropositoEtapa.opcionesPara(Especie.BOVINO, sexo) } ?: coincidencias.firstOrNull()
    }

    /** "categoria" de la versión anterior de la app. */
    private fun desdeCategoriaAntigua(categoria: String?, sexo: Sexo): PropositoEtapa? =
        when (categoria?.trim()?.lowercase()) {
            null, "" -> null
            "vaca en ordeño" -> PRODUCCION_ORDENO
            "vaca seca" -> VACA_SECA
            "novilla de vientre", "novilla de levante", "ternera" -> CRIA_LEVANTE
            "toro reproductor" -> TORO_REPRODUCTOR
            "torete / novillo" -> TORO_CEBA
            "ternero" -> TERNERO_LEVANTE
            "trabajo / compañía" -> TRABAJO_COMPANIA
            else -> null
        }

    /** Animales creados antes de existir "proposito": se deduce del "estado" del panel web. */
    private fun desdeEstado(estado: String?, sexo: Sexo): PropositoEtapa =
        when (estado?.trim()?.lowercase()) {
            "seca", "gestante" -> if (sexo == Sexo.HEMBRA) VACA_SECA else TORO_REPRODUCTOR
            "levante", "ternero" -> if (sexo == Sexo.HEMBRA) CRIA_LEVANTE else TERNERO_LEVANTE
            "engorde" -> if (sexo == Sexo.HEMBRA) ENGORDE else TORO_CEBA
            "reproductor" -> TORO_REPRODUCTOR
            "trabajo / compañía" -> TRABAJO_COMPANIA
            else -> if (sexo == Sexo.MACHO) TORO_REPRODUCTOR else PRODUCCION_ORDENO // "En producción"
        }
}

package com.juanberrocal.gestionganadera.data.api

import com.juanberrocal.gestionganadera.data.CondicionPasto
import com.juanberrocal.gestionganadera.data.DepartamentoColombia
import com.juanberrocal.gestionganadera.data.EstadoPotrero
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.PisoTermico
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.TipoPasto
import com.juanberrocal.gestionganadera.data.UnidadArea
import java.time.LocalDate

/** Conversión Potrero/Finca <-> DTOs del backend. */
object MapeoPotrero {

    fun aDominio(dto: PotreroDto, idLocal: Long): Potrero {
        // Potreros antiguos solo tienen largo x ancho: se expresan en m²
        val tieneArea = dto.area != null && dto.area > 0
        val unidad = if (tieneArea) UnidadArea.desde(dto.unidadArea) else UnidadArea.METROS_CUADRADOS
        val area = when {
            tieneArea -> dto.area!!
            dto.areaM2 != null -> dto.areaM2
            else -> (dto.largoMetros ?: 0.0) * (dto.anchoMetros ?: 0.0)
        }
        return Potrero(
            id = idLocal,
            fincaId = dto.fincaId?.ifBlank { null },
            nombre = dto.nombre,
            area = area,
            unidadArea = unidad,
            largo = dto.largoMetros,
            ancho = dto.anchoMetros,
            tipoPasto = TipoPasto.desde(dto.tipoPasto),
            estado = EstadoPotrero.desde(dto.estado),
            condicionPasto = CondicionPasto.desde(dto.condicionPasto),
            diasOcupacionPlaneados = dto.diasOcupacion,
            diasDescansoPlaneados = dto.diasDescanso,
            fechaUltimoCambio = dto.fechaUltimoCambio?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
                ?: LocalDate.now(),
            cantidadAnimalesActuales = dto.cantidadAnimales ?: 0,
            mongoId = dto.id
        )
    }

    fun aDto(p: Potrero, esNuevo: Boolean): PotreroDto = PotreroDto(
        fincaId = p.fincaId,
        nombre = p.nombre,
        largoMetros = p.largo,
        anchoMetros = p.ancho,
        area = p.area,
        unidadArea = p.unidadArea.codigoApi,
        areaM2 = p.areaEnMetrosCuadrados,
        areaHa = Math.round(p.areaHectareas * 100) / 100.0,
        estado = p.estado.valorApi,
        condicionPasto = p.condicionPasto.etiqueta,
        diasOcupacion = p.diasOcupacionPlaneados,
        diasDescanso = p.diasDescansoPlaneados,
        tipoPasto = p.tipoPasto.etiqueta,
        fechaUltimoCambio = p.fechaUltimoCambio.toString(),
        cantidadAnimales = p.cantidadAnimalesActuales,
        registradoPor = if (esNuevo) "Android App" else null
    )

    fun fincaADominio(dto: FincaDto): Finca? = dto.id?.let { id ->
        Finca(
            id = id,
            nombre = dto.nombre,
            departamento = DepartamentoColombia.desde(dto.departamento),
            municipio = dto.municipio.orEmpty(),
            pisoTermico = PisoTermico.desde(dto.pisoTermico)
        )
    }

    fun fincaADto(f: Finca): FincaDto = FincaDto(
        nombre = f.nombre,
        departamento = f.departamento.etiqueta,
        municipio = f.municipio,
        pisoTermico = f.pisoTermico.etiqueta
    )
}

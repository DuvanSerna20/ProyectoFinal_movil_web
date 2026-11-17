package com.juanberrocal.gestionganadera.data.local

import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.CondicionPasto
import com.juanberrocal.gestionganadera.data.DepartamentoColombia
import com.juanberrocal.gestionganadera.data.DestinoLeche
import com.juanberrocal.gestionganadera.data.Especie
import com.juanberrocal.gestionganadera.data.EstadoPotrero
import com.juanberrocal.gestionganadera.data.EstadoReproductivo
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.FrecuenciaConsumo
import com.juanberrocal.gestionganadera.data.JornadaOrdeno
import com.juanberrocal.gestionganadera.data.PisoTermico
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.data.Sexo
import com.juanberrocal.gestionganadera.data.Tarea
import com.juanberrocal.gestionganadera.data.TipoInsumo
import com.juanberrocal.gestionganadera.data.TipoPasto
import com.juanberrocal.gestionganadera.data.TipoTareaEspecial
import com.juanberrocal.gestionganadera.data.UnidadArea
import com.juanberrocal.gestionganadera.data.UnidadConsumo
import java.time.LocalDate

/**
 * Conversión dominio <-> entidades de Room.
 * Los enums se guardan por `name` (estable aunque cambie la etiqueta visible) y las fechas en ISO-8601.
 */
object MapeoLocal {

    private inline fun <reified E : Enum<E>> enumDe(nombre: String?, defecto: E): E =
        enumValues<E>().firstOrNull { it.name == nombre } ?: defecto

    private fun fecha(texto: String?): LocalDate? =
        texto?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    // ------------------------------------------------------------------------------- Animal

    fun aDominio(e: AnimalLocalEntity): Animal = Animal(
        id = e.id,
        codigo = e.codigo,
        nombre = e.nombre,
        especie = enumDe(e.especie, Especie.BOVINO),
        sexo = enumDe(e.sexo, Sexo.HEMBRA),
        raza = e.raza,
        proposito = enumDe(e.proposito, PropositoEtapa.PRODUCCION_ORDENO),
        estadoReproductivo = enumDe(e.estadoReproductivo, EstadoReproductivo.HORRA),
        pesoKg = e.pesoKg,
        produccionLitrosDia = e.produccionLitrosDia,
        fincaId = e.fincaId,
        potreroId = e.potreroId,
        mongoId = e.mongoId,
        fotoUrl = e.fotoUrl,
        fotoLocal = e.fotoLocal
    )

    fun aEntidad(
        a: Animal,
        id: String,
        mongoId: String?,
        sincronizado: Boolean,
        creadoEn: Long,
        fotoPendiente: Boolean = false
    ): AnimalLocalEntity = AnimalLocalEntity(
        id = id,
        mongoId = mongoId,
        codigo = a.codigo,
        nombre = a.nombre,
        especie = a.especie.name,
        sexo = a.sexo.name,
        raza = a.raza,
        proposito = a.proposito.name,
        estadoReproductivo = a.estadoReproductivo.name,
        pesoKg = a.pesoKg,
        produccionLitrosDia = a.produccionLitrosDia,
        fincaId = a.fincaId,
        potreroId = a.potreroId,
        fotoUrl = a.fotoUrl,
        fotoLocal = a.fotoLocal,
        fotoPendiente = fotoPendiente,
        creadoEn = creadoEn,
        sincronizado = sincronizado,
        actualizadoEn = System.currentTimeMillis()
    )

    // ------------------------------------------------------------------------------- Potrero

    fun aDominio(e: PotreroLocalEntity): Potrero = Potrero(
        id = e.id,
        fincaId = e.fincaId,
        nombre = e.nombre,
        area = e.area,
        unidadArea = enumDe(e.unidadArea, UnidadArea.HECTAREAS),
        largo = e.largo,
        ancho = e.ancho,
        tipoPasto = enumDe(e.tipoPasto, TipoPasto.OTRO),
        estado = enumDe(e.estado, EstadoPotrero.EN_DESCANSO),
        condicionPasto = enumDe(e.condicionPasto, CondicionPasto.OPTIMO),
        diasOcupacionPlaneados = e.diasOcupacion,
        diasDescansoPlaneados = e.diasDescanso,
        fechaUltimoCambio = fecha(e.fechaUltimoCambio) ?: LocalDate.now(),
        cantidadAnimalesActuales = e.cantidadAnimales,
        mongoId = e.mongoId
    )

    fun aEntidad(p: Potrero, id: String, mongoId: String?, sincronizado: Boolean): PotreroLocalEntity =
        PotreroLocalEntity(
            id = id,
            mongoId = mongoId,
            fincaId = p.fincaId,
            nombre = p.nombre,
            area = p.area,
            unidadArea = p.unidadArea.name,
            largo = p.largo,
            ancho = p.ancho,
            tipoPasto = p.tipoPasto.name,
            estado = p.estado.name,
            condicionPasto = p.condicionPasto.name,
            diasOcupacion = p.diasOcupacionPlaneados,
            diasDescanso = p.diasDescansoPlaneados,
            fechaUltimoCambio = p.fechaUltimoCambio.toString(),
            cantidadAnimales = p.cantidadAnimalesActuales,
            sincronizado = sincronizado,
            actualizadoEn = System.currentTimeMillis()
        )

    // ------------------------------------------------------------------------------- Finca

    fun aDominio(e: FincaLocalEntity): Finca = Finca(
        id = e.id,
        nombre = e.nombre,
        departamento = enumDe(e.departamento, DepartamentoColombia.OTRO),
        municipio = e.municipio,
        pisoTermico = enumDe(e.pisoTermico, PisoTermico.CALIDO)
    )

    fun aEntidad(f: Finca): FincaLocalEntity = FincaLocalEntity(
        id = f.id,
        nombre = f.nombre,
        departamento = f.departamento.name,
        municipio = f.municipio,
        pisoTermico = f.pisoTermico.name
    )

    // ------------------------------------------------------------------------------- Ordeño

    fun aDominio(e: OrdenoLocalEntity): RegistroOrdeno = RegistroOrdeno(
        id = e.id,
        animalId = e.animalId,
        animalCodigoNombre = e.animalCodigoNombre,
        fecha = fecha(e.fecha) ?: LocalDate.now(),
        jornada = enumDe(e.jornada, JornadaOrdeno.MANANA),
        litros = e.litros,
        destinoLeche = enumDe(e.destinoLeche, DestinoLeche.TANQUE_VENTA)
    )

    fun aEntidad(r: RegistroOrdeno, id: String, mongoId: String?, sincronizado: Boolean, creadoEn: Long): OrdenoLocalEntity =
        OrdenoLocalEntity(
            id = id,
            mongoId = mongoId,
            animalId = r.animalId,
            animalCodigoNombre = r.animalCodigoNombre,
            fecha = r.fecha.toString(),
            jornada = r.jornada.name,
            litros = r.litros,
            destinoLeche = r.destinoLeche.name,
            creadoEn = creadoEn,
            sincronizado = sincronizado,
            actualizadoEn = System.currentTimeMillis()
        )

    // ------------------------------------------------------------------------------- Tarea

    fun aDominio(e: TareaLocalEntity): Tarea = Tarea(
        id = e.id,
        titulo = e.titulo,
        descripcion = e.descripcion,
        fechaProgramada = fecha(e.fechaProgramada) ?: LocalDate.now(),
        completada = e.completada,
        esTareaEspecial = e.esTareaEspecial,
        tipoTareaEspecial = e.tipoTareaEspecial?.let { n -> TipoTareaEspecial.entries.firstOrNull { it.name == n } },
        animalId = e.animalId,
        animalCodigoNombre = e.animalCodigoNombre
    )

    fun aEntidad(t: Tarea, id: String, mongoId: String?, sincronizado: Boolean): TareaLocalEntity = TareaLocalEntity(
        id = id,
        mongoId = mongoId,
        titulo = t.titulo,
        descripcion = t.descripcion,
        fechaProgramada = t.fechaProgramada.toString(),
        completada = t.completada,
        esTareaEspecial = t.esTareaEspecial,
        tipoTareaEspecial = t.tipoTareaEspecial?.name,
        animalId = t.animalId,
        animalCodigoNombre = t.animalCodigoNombre,
        sincronizado = sincronizado,
        actualizadoEn = System.currentTimeMillis()
    )

    // ------------------------------------------------------------------- Alimentación / Medicamentos

    fun aDominio(e: AlimentacionLocalEntity): RegistroAlimentacionMedicamento = RegistroAlimentacionMedicamento(
        id = e.id,
        animalId = e.animalId,
        grupoProposito = e.grupoProposito?.let { n -> PropositoEtapa.entries.firstOrNull { it.name == n } },
        objetivoLabel = e.objetivoLabel,
        tipoRegistro = enumDe(e.tipoRegistro, TipoInsumo.entries.first()),
        categoriaDetalle = e.categoriaDetalle,
        nombreProducto = e.nombreProducto,
        cantidad = e.cantidad,
        unidad = enumDe(e.unidad, UnidadConsumo.entries.first()),
        frecuencia = enumDe(e.frecuencia, FrecuenciaConsumo.entries.first()),
        tieneRetiroLeche = e.tieneRetiroLeche,
        diasRetiro = e.diasRetiro,
        activo = e.activo,
        fechaInicio = fecha(e.fechaInicio) ?: LocalDate.now(),
        fechaFin = fecha(e.fechaFin)
    )

    fun aEntidad(
        r: RegistroAlimentacionMedicamento,
        id: String,
        mongoId: String?,
        sincronizado: Boolean,
        creadoEn: Long
    ): AlimentacionLocalEntity = AlimentacionLocalEntity(
        id = id,
        mongoId = mongoId,
        animalId = r.animalId,
        grupoProposito = r.grupoProposito?.name,
        objetivoLabel = r.objetivoLabel,
        tipoRegistro = r.tipoRegistro.name,
        categoriaDetalle = r.categoriaDetalle,
        nombreProducto = r.nombreProducto,
        cantidad = r.cantidad,
        unidad = r.unidad.name,
        frecuencia = r.frecuencia.name,
        tieneRetiroLeche = r.tieneRetiroLeche,
        diasRetiro = r.diasRetiro,
        activo = r.activo,
        fechaInicio = r.fechaInicio.toString(),
        fechaFin = r.fechaFin?.toString(),
        creadoEn = creadoEn,
        sincronizado = sincronizado,
        actualizadoEn = System.currentTimeMillis()
    )
}

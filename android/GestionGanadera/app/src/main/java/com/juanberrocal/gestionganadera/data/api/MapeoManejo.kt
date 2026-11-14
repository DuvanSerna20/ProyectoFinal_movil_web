package com.juanberrocal.gestionganadera.data.api

import com.juanberrocal.gestionganadera.data.DestinoLeche
import com.juanberrocal.gestionganadera.data.FrecuenciaConsumo
import com.juanberrocal.gestionganadera.data.JornadaOrdeno
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.data.Tarea
import com.juanberrocal.gestionganadera.data.TipoInsumo
import com.juanberrocal.gestionganadera.data.TipoTareaEspecial
import com.juanberrocal.gestionganadera.data.UnidadConsumo
import java.time.LocalDate

/**
 * Conversión dominio <-> DTO para Ordeño, Tareas y Alimentación/Medicamentos.
 * En MongoDB se guardan las etiquetas legibles ("Mañana", "Tanque / Venta"), como en el resto de la API.
 * Los campos opcionales se envían como "" para poder borrarlos al editar (Gson omite los null).
 */
object MapeoManejo {

    private fun fecha(texto: String?): LocalDate? =
        texto?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    // ------------------------------------------------------------------------------- Ordeño

    fun ordenoADominio(dto: OrdenoDto): RegistroOrdeno? {
        val id = dto.id ?: return null
        val animalId = dto.animalId?.ifBlank { null }
        val etiqueta = when {
            animalId == null -> RegistroOrdeno.ETIQUETA_TANQUE
            dto.animalCodigo != null && dto.animalNombre != null -> "#${dto.animalCodigo} - ${dto.animalNombre}"
            else -> dto.animalNombre ?: dto.animalCodigo?.let { "#$it" } ?: "Vaca"
        }
        return RegistroOrdeno(
            id = id,
            animalId = animalId,
            animalCodigoNombre = etiqueta,
            fecha = fecha(dto.fecha) ?: return null,
            jornada = JornadaOrdeno.desde(dto.jornada),
            litros = dto.litros,
            destinoLeche = DestinoLeche.desde(dto.destinoLeche)
        )
    }

    /** [codigo] y [nombre] se envían aparte porque así los lee el panel web. */
    fun ordenoADto(r: RegistroOrdeno, codigo: String?, nombre: String?): OrdenoDto = OrdenoDto(
        fecha = r.fecha.toString(),
        jornada = r.jornada.etiqueta,
        litros = r.litros,
        animalId = r.animalId.orEmpty(),
        animalCodigo = codigo,
        animalNombre = nombre,
        destinoLeche = r.destinoLeche.etiqueta,
        responsable = "Operario Android"
    )

    // ------------------------------------------------------------------------------- Tareas

    fun tareaADominio(dto: TareaDto): Tarea? {
        val id = dto.id ?: return null
        val tipo = TipoTareaEspecial.desde(dto.tipoTareaEspecial)
        // Tareas antiguas (sin "esTareaEspecial") son generales: la app anterior enviaba siempre
        // categoria = "Vacunación" por defecto, así que la categoría no sirve para decidirlo.
        val especial = dto.esTareaEspecial ?: (tipo != null)
        return Tarea(
            id = id,
            titulo = dto.titulo,
            descripcion = dto.descripcion.orEmpty(),
            fechaProgramada = fecha(dto.fechaLimite) ?: LocalDate.now(),
            completada = dto.completada,
            esTareaEspecial = especial,
            tipoTareaEspecial = if (especial) tipo ?: TipoTareaEspecial.OTRO else null,
            animalId = dto.animalId?.ifBlank { null },
            animalCodigoNombre = dto.animalCodigoNombre?.ifBlank { null }
        )
    }

    fun tareaADto(t: Tarea): TareaDto {
        val tipo = t.tipoTareaEspecial.takeIf { t.esTareaEspecial }
        return TareaDto(
            titulo = t.titulo,
            descripcion = t.descripcion,
            categoria = tipo?.categoriaApi ?: Tarea.CATEGORIA_GENERAL,
            fechaLimite = t.fechaProgramada.toString(),
            completada = t.completada,
            esTareaEspecial = t.esTareaEspecial,
            tipoTareaEspecial = tipo?.name.orEmpty(),
            animalId = t.animalId.takeIf { t.esTareaEspecial }.orEmpty(),
            animalCodigoNombre = t.animalCodigoNombre.takeIf { t.esTareaEspecial && t.animalId != null }.orEmpty()
        )
    }

    // ------------------------------------------------------------------- Alimentación / Medicamentos

    fun alimentacionADominio(dto: AlimentacionDto): RegistroAlimentacionMedicamento? {
        val id = dto.id ?: return null
        val tipo = TipoInsumo.desde(dto.tipoRegistro)
        val esMedicamento = tipo == TipoInsumo.MEDICAMENTO
        return RegistroAlimentacionMedicamento(
            id = id,
            animalId = dto.animalId?.ifBlank { null },
            grupoProposito = PropositoEtapa.entries.firstOrNull { it.name == dto.grupoProposito },
            objetivoLabel = dto.objetivoLabel,
            tipoRegistro = tipo,
            categoriaDetalle = dto.categoriaDetalle,
            nombreProducto = dto.nombreProducto,
            cantidad = dto.cantidad,
            unidad = UnidadConsumo.desde(dto.unidad),
            frecuencia = FrecuenciaConsumo.desde(dto.frecuencia),
            tieneRetiroLeche = esMedicamento && dto.tieneRetiroLeche,
            diasRetiro = if (esMedicamento) dto.diasRetiro.coerceAtLeast(0) else 0,
            activo = dto.activo,
            fechaInicio = fecha(dto.fechaInicio) ?: LocalDate.now(),
            fechaFin = fecha(dto.fechaFin)
        )
    }

    fun alimentacionADto(r: RegistroAlimentacionMedicamento, esNuevo: Boolean): AlimentacionDto = AlimentacionDto(
        animalId = r.animalId.orEmpty(),
        // El grupo se guarda por nombre del enum: "Crianza / Levante" se repite en hembras y machos
        grupoProposito = r.grupoProposito?.name.orEmpty(),
        objetivoLabel = r.objetivoLabel,
        tipoRegistro = r.tipoRegistro.etiqueta,
        categoriaDetalle = r.categoriaDetalle,
        nombreProducto = r.nombreProducto,
        cantidad = r.cantidad,
        unidad = r.unidad.etiqueta,
        frecuencia = r.frecuencia.etiqueta,
        tieneRetiroLeche = r.esMedicamento && r.tieneRetiroLeche,
        diasRetiro = if (r.esMedicamento && r.tieneRetiroLeche) r.diasRetiro else 0,
        activo = r.activo,
        fechaInicio = r.fechaInicio.toString(),
        fechaFin = r.fechaFin?.toString().orEmpty(),
        registradoPor = if (esNuevo) "Android App" else null
    )
}

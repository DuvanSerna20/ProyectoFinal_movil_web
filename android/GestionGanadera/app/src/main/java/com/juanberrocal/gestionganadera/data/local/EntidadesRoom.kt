package com.juanberrocal.gestionganadera.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Campos comunes de toda entidad Offline-First.
 *
 * - [id]: clave primaria LOCAL y estable (UUID generado una sola vez al crear el registro en el
 *   dispositivo, o al descargarlo por primera vez del servidor). Nunca cambia al sincronizar ni
 *   al reordenar, así que las selecciones (potrero de un animal, vaca de un ordeño...) no se pierden.
 * - [mongoId]: `_id` de MongoDB. null = el servidor todavía no conoce el registro.
 * - [sincronizado]: false = hay cambios locales pendientes de enviar.
 * - [pendienteEliminar]: borrado lógico mientras el DELETE no llega al servidor (no se muestra en la UI).
 * - [actualizadoEn]: versión local; evita marcar como sincronizada una edición hecha durante el envío.
 */
interface EntidadSincronizable {
    val id: String
    val mongoId: String?
    val sincronizado: Boolean
    val pendienteEliminar: Boolean
    val actualizadoEn: Long
}

@Entity(tableName = "animales", indices = [Index(value = ["mongoId"], unique = true), Index(value = ["codigo"])])
data class AnimalLocalEntity(
    @PrimaryKey override val id: String,
    override val mongoId: String? = null,
    val codigo: String,
    val nombre: String,
    val especie: String,
    val sexo: String,
    val raza: String,
    val proposito: String,
    val estadoReproductivo: String,
    val pesoKg: Double? = null,
    val produccionLitrosDia: Double? = null,
    val fincaId: String? = null,
    /** id LOCAL del potrero (no el mongoId). */
    val potreroId: String? = null,
    /** Ruta relativa en el servidor ("/uploads/foto-123.jpg"). */
    val fotoUrl: String? = null,
    /** Copia de la foto en el almacenamiento interno de la app. */
    val fotoLocal: String? = null,
    /** true = la foto local todavía no se ha subido (multipart "foto"). */
    val fotoPendiente: Boolean = false,
    val creadoEn: Long = System.currentTimeMillis(),
    override val sincronizado: Boolean = false,
    override val pendienteEliminar: Boolean = false,
    override val actualizadoEn: Long = System.currentTimeMillis()
) : EntidadSincronizable

@Entity(tableName = "potreros", indices = [Index(value = ["mongoId"], unique = true)])
data class PotreroLocalEntity(
    @PrimaryKey override val id: String,
    override val mongoId: String? = null,
    val fincaId: String? = null,
    val nombre: String,
    val area: Double,
    val unidadArea: String,
    val largo: Double? = null,
    val ancho: Double? = null,
    val tipoPasto: String,
    val estado: String,
    val condicionPasto: String,
    val diasOcupacion: Double,
    val diasDescanso: Int,
    /** ISO-8601 (yyyy-MM-dd). */
    val fechaUltimoCambio: String,
    val cantidadAnimales: Int = 0,
    override val sincronizado: Boolean = false,
    override val pendienteEliminar: Boolean = false,
    override val actualizadoEn: Long = System.currentTimeMillis()
) : EntidadSincronizable

/** Las fincas solo se crean en el servidor: aquí son caché de lectura (id = _id de MongoDB). */
@Entity(tableName = "fincas")
data class FincaLocalEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val departamento: String,
    val municipio: String,
    val pisoTermico: String
)

@Entity(tableName = "ordenos", indices = [Index(value = ["mongoId"], unique = true), Index(value = ["animalId"])])
data class OrdenoLocalEntity(
    @PrimaryKey override val id: String,
    override val mongoId: String? = null,
    /** id LOCAL del animal; null = tanque general. */
    val animalId: String? = null,
    val animalCodigoNombre: String,
    /** ISO-8601 (yyyy-MM-dd). */
    val fecha: String,
    val jornada: String,
    val litros: Double,
    val destinoLeche: String,
    val creadoEn: Long = System.currentTimeMillis(),
    override val sincronizado: Boolean = false,
    override val pendienteEliminar: Boolean = false,
    override val actualizadoEn: Long = System.currentTimeMillis()
) : EntidadSincronizable

@Entity(tableName = "tareas", indices = [Index(value = ["mongoId"], unique = true)])
data class TareaLocalEntity(
    @PrimaryKey override val id: String,
    override val mongoId: String? = null,
    val titulo: String,
    val descripcion: String,
    /** ISO-8601 (yyyy-MM-dd). */
    val fechaProgramada: String,
    val completada: Boolean,
    val esTareaEspecial: Boolean,
    val tipoTareaEspecial: String? = null,
    /** id LOCAL del animal. */
    val animalId: String? = null,
    val animalCodigoNombre: String? = null,
    override val sincronizado: Boolean = false,
    override val pendienteEliminar: Boolean = false,
    override val actualizadoEn: Long = System.currentTimeMillis()
) : EntidadSincronizable

@Entity(tableName = "alimentacion", indices = [Index(value = ["mongoId"], unique = true)])
data class AlimentacionLocalEntity(
    @PrimaryKey override val id: String,
    override val mongoId: String? = null,
    /** id LOCAL del animal. */
    val animalId: String? = null,
    val grupoProposito: String? = null,
    val objetivoLabel: String,
    val tipoRegistro: String,
    val categoriaDetalle: String,
    val nombreProducto: String,
    val cantidad: Double,
    val unidad: String,
    val frecuencia: String,
    val tieneRetiroLeche: Boolean,
    val diasRetiro: Int,
    val activo: Boolean,
    val fechaInicio: String,
    val fechaFin: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    override val sincronizado: Boolean = false,
    override val pendienteEliminar: Boolean = false,
    override val actualizadoEn: Long = System.currentTimeMillis()
) : EntidadSincronizable

/** Proyección (id local, mongoId) para traducir referencias entre el servidor y Room. */
data class ParIds(val id: String, val mongoId: String)

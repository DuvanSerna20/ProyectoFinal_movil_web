package com.juanberrocal.gestionganadera.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room para persistencia local de animales (Borrador y caché offline útil).
 */
@Entity(tableName = "animales_local")
data class AnimalLocalEntity(
    @PrimaryKey val id: String, // ID de MongoDB o ID temporal para borrador
    val codigo: String,
    val nombre: String,
    val raza: String,
    val sexo: String,
    val pesoKg: Double?,
    val estado: String,
    val especie: String,
    val categoria: String?,
    val proposito: String?,
    val estadoReproductivo: String?,
    val produccionLitrosDia: Double?,
    val fincaId: String?,
    val potreroId: String?,
    val fotoUrl: String?,
    val registradoPor: String,
    val fechaRegistro: String,
    val esBorrador: Boolean = false // Distingue borradores pendientes de sincronizar
)

/**
 * Entidad de Room para persistencia local de pesajes de ordeño.
 */
@Entity(tableName = "ordeno_local")
data class OrdenoLocalEntity(
    @PrimaryKey val id: String,
    val fecha: String,
    val jornada: String,
    val litros: Double,
    val animalId: String?,
    val animalCodigo: String?,
    val animalNombre: String?,
    val responsable: String,
    val horaRegistro: String,
    val destinoLeche: String?,
    val esBorrador: Boolean = false
)

/**
 * Entidad de Room para persistencia local de tareas.
 */
@Entity(tableName = "tareas_local")
data class TareaLocalEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val categoria: String,
    val fechaLimite: String,
    val completada: Boolean,
    val prioridad: String,
    val responsable: String,
    val descripcion: String?,
    val esTareaEspecial: Boolean,
    val tipoTareaEspecial: String?,
    val animalId: String?,
    val animalCodigoNombre: String?
)

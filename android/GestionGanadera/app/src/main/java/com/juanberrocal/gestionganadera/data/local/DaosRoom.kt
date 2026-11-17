package com.juanberrocal.gestionganadera.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/*
 * Convenciones comunes a todos los DAO sincronizables:
 * - obtenerTodos(): Flow observado por el ViewModel (oculta los registros con borrado pendiente).
 * - obtenerPendientes(): cambios locales sin enviar (incluye borrados pendientes).
 * - asignarMongoId(): se guarda siempre que el servidor crea el registro.
 * - marcarSincronizado(): solo si nadie editó el registro mientras se enviaba (misma versión).
 */

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animales WHERE pendienteEliminar = 0 ORDER BY creadoEn DESC")
    fun obtenerTodos(): Flow<List<AnimalLocalEntity>>

    @Query("SELECT * FROM animales")
    suspend fun obtenerTodosLista(): List<AnimalLocalEntity>

    @Query("SELECT * FROM animales WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): AnimalLocalEntity?

    @Query("SELECT * FROM animales WHERE sincronizado = 0")
    suspend fun obtenerPendientes(): List<AnimalLocalEntity>

    @Query("SELECT COUNT(*) FROM animales WHERE sincronizado = 0")
    fun contarPendientes(): Flow<Int>

    @Query("SELECT id, mongoId FROM animales WHERE mongoId IS NOT NULL")
    suspend fun pares(): List<ParIds>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM animales WHERE pendienteEliminar = 0 AND id != :excluirId " +
            "AND UPPER(codigo) = UPPER(:codigo))"
    )
    suspend fun existeCodigo(codigo: String, excluirId: String): Boolean

    @Upsert
    suspend fun upsert(animal: AnimalLocalEntity)

    @Upsert
    suspend fun upsertTodos(animales: List<AnimalLocalEntity>)

    @Query("UPDATE animales SET pendienteEliminar = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarParaEliminar(id: String, ahora: Long = System.currentTimeMillis())

    @Query("DELETE FROM animales WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM animales WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<String>)

    @Query("UPDATE animales SET mongoId = :mongoId WHERE id = :id")
    suspend fun asignarMongoId(id: String, mongoId: String?)

    @Query(
        "UPDATE animales SET sincronizado = 1, fotoPendiente = 0, fotoUrl = COALESCE(:fotoUrl, fotoUrl) " +
            "WHERE id = :id AND actualizadoEn = :version"
    )
    suspend fun marcarSincronizado(id: String, version: Long, fotoUrl: String?)

    /** Producción derivada de los ordeños: no es un cambio del usuario, no altera el estado de sincronización. */
    @Query("UPDATE animales SET produccionLitrosDia = :litros WHERE id = :id")
    suspend fun actualizarProduccion(id: String, litros: Double?)

    /** Al borrar un potrero sus animales quedan "sin potrero" (el backend hace lo mismo en MongoDB). */
    @Query("UPDATE animales SET potreroId = NULL WHERE potreroId = :potreroId")
    suspend fun quitarPotrero(potreroId: String)
}

@Dao
interface PotreroDao {
    @Query("SELECT * FROM potreros WHERE pendienteEliminar = 0 ORDER BY nombre COLLATE NOCASE ASC")
    fun obtenerTodos(): Flow<List<PotreroLocalEntity>>

    @Query("SELECT * FROM potreros")
    suspend fun obtenerTodosLista(): List<PotreroLocalEntity>

    @Query("SELECT * FROM potreros WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): PotreroLocalEntity?

    @Query("SELECT * FROM potreros WHERE sincronizado = 0")
    suspend fun obtenerPendientes(): List<PotreroLocalEntity>

    @Query("SELECT COUNT(*) FROM potreros WHERE sincronizado = 0")
    fun contarPendientes(): Flow<Int>

    @Query("SELECT id, mongoId FROM potreros WHERE mongoId IS NOT NULL")
    suspend fun pares(): List<ParIds>

    @Upsert
    suspend fun upsert(potrero: PotreroLocalEntity)

    @Upsert
    suspend fun upsertTodos(potreros: List<PotreroLocalEntity>)

    @Query("UPDATE potreros SET pendienteEliminar = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarParaEliminar(id: String, ahora: Long = System.currentTimeMillis())

    @Query("DELETE FROM potreros WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM potreros WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<String>)

    @Query("UPDATE potreros SET mongoId = :mongoId WHERE id = :id")
    suspend fun asignarMongoId(id: String, mongoId: String?)

    @Query("UPDATE potreros SET sincronizado = 1 WHERE id = :id AND actualizadoEn = :version")
    suspend fun marcarSincronizado(id: String, version: Long)
}

@Dao
interface FincaDao {
    @Query("SELECT * FROM fincas ORDER BY nombre COLLATE NOCASE ASC")
    fun obtenerTodos(): Flow<List<FincaLocalEntity>>

    @Upsert
    suspend fun upsert(finca: FincaLocalEntity)

    @Query("DELETE FROM fincas")
    suspend fun eliminarTodas()

    @Upsert
    suspend fun upsertTodas(fincas: List<FincaLocalEntity>)

    /** Las fincas son de solo lectura en el dispositivo: el servidor manda. */
    @Transaction
    suspend fun reemplazarTodas(fincas: List<FincaLocalEntity>) {
        eliminarTodas()
        upsertTodas(fincas)
    }
}

@Dao
interface OrdenoDao {
    @Query("SELECT * FROM ordenos WHERE pendienteEliminar = 0 ORDER BY fecha DESC, creadoEn DESC")
    fun obtenerTodos(): Flow<List<OrdenoLocalEntity>>

    @Query("SELECT * FROM ordenos")
    suspend fun obtenerTodosLista(): List<OrdenoLocalEntity>

    @Query("SELECT * FROM ordenos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): OrdenoLocalEntity?

    @Query("SELECT * FROM ordenos WHERE animalId = :animalId AND pendienteEliminar = 0")
    suspend fun obtenerPorAnimal(animalId: String): List<OrdenoLocalEntity>

    @Query("SELECT * FROM ordenos WHERE sincronizado = 0")
    suspend fun obtenerPendientes(): List<OrdenoLocalEntity>

    @Query("SELECT COUNT(*) FROM ordenos WHERE sincronizado = 0")
    fun contarPendientes(): Flow<Int>

    @Upsert
    suspend fun upsert(ordeno: OrdenoLocalEntity)

    @Upsert
    suspend fun upsertTodos(ordenos: List<OrdenoLocalEntity>)

    @Query("UPDATE ordenos SET pendienteEliminar = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarParaEliminar(id: String, ahora: Long = System.currentTimeMillis())

    @Query("DELETE FROM ordenos WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM ordenos WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<String>)

    @Query("UPDATE ordenos SET mongoId = :mongoId WHERE id = :id")
    suspend fun asignarMongoId(id: String, mongoId: String?)

    @Query("UPDATE ordenos SET sincronizado = 1 WHERE id = :id AND actualizadoEn = :version")
    suspend fun marcarSincronizado(id: String, version: Long)
}

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas WHERE pendienteEliminar = 0 ORDER BY completada ASC, fechaProgramada ASC")
    fun obtenerTodos(): Flow<List<TareaLocalEntity>>

    @Query("SELECT * FROM tareas")
    suspend fun obtenerTodosLista(): List<TareaLocalEntity>

    @Query("SELECT * FROM tareas WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): TareaLocalEntity?

    @Query("SELECT * FROM tareas WHERE sincronizado = 0")
    suspend fun obtenerPendientes(): List<TareaLocalEntity>

    @Query("SELECT COUNT(*) FROM tareas WHERE sincronizado = 0")
    fun contarPendientes(): Flow<Int>

    @Upsert
    suspend fun upsert(tarea: TareaLocalEntity)

    @Upsert
    suspend fun upsertTodos(tareas: List<TareaLocalEntity>)

    @Query("UPDATE tareas SET pendienteEliminar = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarParaEliminar(id: String, ahora: Long = System.currentTimeMillis())

    @Query("DELETE FROM tareas WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM tareas WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<String>)

    @Query("UPDATE tareas SET mongoId = :mongoId WHERE id = :id")
    suspend fun asignarMongoId(id: String, mongoId: String?)

    @Query("UPDATE tareas SET sincronizado = 1 WHERE id = :id AND actualizadoEn = :version")
    suspend fun marcarSincronizado(id: String, version: Long)
}

@Dao
interface AlimentacionDao {
    @Query("SELECT * FROM alimentacion WHERE pendienteEliminar = 0 ORDER BY creadoEn DESC")
    fun obtenerTodos(): Flow<List<AlimentacionLocalEntity>>

    @Query("SELECT * FROM alimentacion")
    suspend fun obtenerTodosLista(): List<AlimentacionLocalEntity>

    @Query("SELECT * FROM alimentacion WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): AlimentacionLocalEntity?

    @Query("SELECT * FROM alimentacion WHERE sincronizado = 0")
    suspend fun obtenerPendientes(): List<AlimentacionLocalEntity>

    @Query("SELECT COUNT(*) FROM alimentacion WHERE sincronizado = 0")
    fun contarPendientes(): Flow<Int>

    @Upsert
    suspend fun upsert(registro: AlimentacionLocalEntity)

    @Upsert
    suspend fun upsertTodos(registros: List<AlimentacionLocalEntity>)

    @Query("UPDATE alimentacion SET pendienteEliminar = 1, sincronizado = 0, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarParaEliminar(id: String, ahora: Long = System.currentTimeMillis())

    @Query("DELETE FROM alimentacion WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM alimentacion WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<String>)

    @Query("UPDATE alimentacion SET mongoId = :mongoId WHERE id = :id")
    suspend fun asignarMongoId(id: String, mongoId: String?)

    @Query("UPDATE alimentacion SET sincronizado = 1 WHERE id = :id AND actualizadoEn = :version")
    suspend fun marcarSincronizado(id: String, version: Long)
}

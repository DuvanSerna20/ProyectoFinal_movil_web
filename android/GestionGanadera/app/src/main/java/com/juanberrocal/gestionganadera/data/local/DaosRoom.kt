package com.juanberrocal.gestionganadera.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animales_local ORDER BY fechaRegistro DESC")
    fun obtenerTodosFlow(): Flow<List<AnimalLocalEntity>>

    @Query("SELECT * FROM animales_local ORDER BY fechaRegistro DESC")
    suspend fun obtenerTodos(): List<AnimalLocalEntity>

    @Query("SELECT * FROM animales_local WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): AnimalLocalEntity?

    @Query("SELECT * FROM animales_local WHERE esBorrador = 1")
    suspend fun obtenerBorradores(): List<AnimalLocalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(animal: AnimalLocalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(animales: List<AnimalLocalEntity>)

    @Delete
    suspend fun eliminar(animal: AnimalLocalEntity)

    @Query("DELETE FROM animales_local WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM animales_local WHERE esBorrador = 0")
    suspend fun limpiarSincronizados()
}

@Dao
interface OrdenoDao {
    @Query("SELECT * FROM ordeno_local ORDER BY fecha DESC, horaRegistro DESC")
    fun obtenerTodosFlow(): Flow<List<OrdenoLocalEntity>>

    @Query("SELECT * FROM ordeno_local ORDER BY fecha DESC")
    suspend fun obtenerTodos(): List<OrdenoLocalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ordeno: OrdenoLocalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(ordenos: List<OrdenoLocalEntity>)

    @Query("DELETE FROM ordeno_local WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM ordeno_local WHERE esBorrador = 0")
    suspend fun limpiarSincronizados()
}

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas_local ORDER BY completada ASC, fechaLimite ASC")
    fun obtenerTodosFlow(): Flow<List<TareaLocalEntity>>

    @Query("SELECT * FROM tareas_local ORDER BY fechaLimite ASC")
    suspend fun obtenerTodos(): List<TareaLocalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(tarea: TareaLocalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(tareas: List<TareaLocalEntity>)

    @Query("DELETE FROM tareas_local WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM tareas_local")
    suspend fun limpiarTodos()
}

package com.juanberrocal.gestionganadera.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Única fuente de verdad de la app (Offline-First).
 *
 * Versión 2: claves primarias UUID estables + mongoId indexado + flags de sincronización.
 * La versión 1 nunca se conectó a la UI (sus tablas estaban vacías), por eso la migración
 * es destructiva: no se pierde ningún dato real del usuario.
 */
@Database(
    entities = [
        AnimalLocalEntity::class,
        PotreroLocalEntity::class,
        FincaLocalEntity::class,
        OrdenoLocalEntity::class,
        TareaLocalEntity::class,
        AlimentacionLocalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun animalDao(): AnimalDao
    abstract fun potreroDao(): PotreroDao
    abstract fun fincaDao(): FincaDao
    abstract fun ordenoDao(): OrdenoDao
    abstract fun tareaDao(): TareaDao
    abstract fun alimentacionDao(): AlimentacionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestion_ganadera_room.db"
                )
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

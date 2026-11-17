package com.juanberrocal.gestionganadera

import android.app.Application
import com.juanberrocal.gestionganadera.data.GanaderiaRepository
import com.juanberrocal.gestionganadera.data.local.AppDatabase
import com.juanberrocal.gestionganadera.data.local.PreferenciasDataStore

/** Contenedor de dependencias de la app (una sola base de datos, un solo DataStore y un solo repositorio). */
class GanaderiaApp : Application() {
    val preferencias: PreferenciasDataStore by lazy { PreferenciasDataStore(this) }
    val repositorio: GanaderiaRepository by lazy {
        GanaderiaRepository(this, AppDatabase.getDatabase(this), preferencias)
    }
}

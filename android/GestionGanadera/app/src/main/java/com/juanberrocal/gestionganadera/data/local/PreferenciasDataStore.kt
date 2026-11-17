package com.juanberrocal.gestionganadera.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gestion_ganadera_preferences")

/** Sesión persistida. Solo cuenta como activa si hubo un login válido contra el servidor. */
data class Sesion(
    val isLoggedIn: Boolean,
    val token: String?,
    val nombre: String,
    val finca: String,
    val correo: String
) {
    val activa: Boolean get() = isLoggedIn && !token.isNullOrBlank()
}

/**
 * Preferencias, onboarding y sesión con Jetpack DataStore (reemplaza a SessionManager/SharedPreferences).
 * Todo se expone como Flow y se escribe con funciones suspend.
 */
class PreferenciasDataStore(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    companion object {
        val ONBOARDING_COMPLETADO = booleanPreferencesKey("onboarding_completado")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        /** Identificador emitido por el servidor tras un login válido. */
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USUARIO_NOMBRE = stringPreferencesKey("usuario_nombre")
        val USUARIO_FINCA = stringPreferencesKey("usuario_finca")
        val USUARIO_CORREO = stringPreferencesKey("usuario_correo")
        /** true = trabajar solo con Room, sin intentar sincronizar con Retrofit. */
        val MODO_OFFLINE_PREFERIDO = booleanPreferencesKey("modo_offline_preferido")
    }

    /** Un archivo corrupto o ilegible no debe tumbar la app: se tratan como preferencias vacías. */
    private val datos: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val onboardingCompletado: Flow<Boolean> =
        datos.map { it[ONBOARDING_COMPLETADO] ?: false }.distinctUntilChanged()

    val sesion: Flow<Sesion> = datos.map { p ->
        Sesion(
            isLoggedIn = p[IS_LOGGED_IN] ?: false,
            token = p[AUTH_TOKEN],
            nombre = p[USUARIO_NOMBRE].orEmpty(),
            finca = p[USUARIO_FINCA].orEmpty(),
            correo = p[USUARIO_CORREO].orEmpty()
        )
    }.distinctUntilChanged()

    val modoOfflinePreferido: Flow<Boolean> =
        datos.map { it[MODO_OFFLINE_PREFERIDO] ?: false }.distinctUntilChanged()

    suspend fun setOnboardingCompletado(completado: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETADO] = completado }
    }

    /** Solo debe llamarse tras una respuesta exitosa del servidor (login o registro). */
    suspend fun guardarSesion(token: String, nombre: String, finca: String, correo: String) {
        require(token.isNotBlank()) { "No se puede guardar una sesión sin token" }
        dataStore.edit {
            it[IS_LOGGED_IN] = true
            it[AUTH_TOKEN] = token
            it[USUARIO_NOMBRE] = nombre
            it[USUARIO_FINCA] = finca
            it[USUARIO_CORREO] = correo
        }
    }

    suspend fun cerrarSesion() {
        dataStore.edit {
            it[IS_LOGGED_IN] = false
            it.remove(AUTH_TOKEN)
        }
    }

    suspend fun setModoOfflinePreferido(activo: Boolean) {
        dataStore.edit { it[MODO_OFFLINE_PREFERIDO] = activo }
    }
}

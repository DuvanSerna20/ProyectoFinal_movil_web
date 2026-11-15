package com.juanberrocal.gestionganadera.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gestion_ganadera_preferences")

/**
 * Gestión de preferencias y onboarding mediante Jetpack DataStore (Requisito Rúbrica Móvil I).
 */
class PreferenciasDataStore(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETADO = booleanPreferencesKey("onboarding_completado")
        val SESION_INICIADA = booleanPreferencesKey("sesion_iniciada")
        val USUARIO_NOMBRE = stringPreferencesKey("usuario_nombre")
        val USUARIO_FINCA = stringPreferencesKey("usuario_finca")
        val USUARIO_CORREO = stringPreferencesKey("usuario_correo")
        val MODO_OFFLINE_PREFERIDO = booleanPreferencesKey("modo_offline_preferido")
    }

    val onboardingCompletadoFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETADO] ?: false
    }

    val sesionIniciadaFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SESION_INICIADA] ?: false
    }

    val usuarioNombreFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USUARIO_NOMBRE] ?: "Juan Berrocal"
    }

    val usuarioFincaFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USUARIO_FINCA] ?: "Finca La Esperanza"
    }

    val usuarioCorreoFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USUARIO_CORREO] ?: "usuario@ganaderia.com"
    }

    suspend fun setOnboardingCompletado(completado: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETADO] = completado
        }
    }

    suspend fun guardarSesion(nombre: String, finca: String, correo: String) {
        context.dataStore.edit { preferences ->
            preferences[SESION_INICIADA] = true
            preferences[USUARIO_NOMBRE] = nombre
            preferences[USUARIO_FINCA] = finca
            preferences[USUARIO_CORREO] = correo
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { preferences ->
            preferences[SESION_INICIADA] = false
        }
    }

    suspend fun isOnboardingCompletedSync(): Boolean {
        return context.dataStore.data.first()[ONBOARDING_COMPLETADO] ?: false
    }

    suspend fun isLoggedInSync(): Boolean {
        return context.dataStore.data.first()[SESION_INICIADA] ?: false
    }
}

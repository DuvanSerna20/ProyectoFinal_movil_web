package com.juanberrocal.gestionganadera.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gestion_ganadera_prefs", Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    fun saveUser(nombre: String, finca: String, correo: String) {
        prefs.edit()
            .putString("user_nombre", nombre)
            .putString("user_finca", finca)
            .putString("user_correo", correo)
            .apply()
    }

    fun getUserNombre(): String = prefs.getString("user_nombre", "Juan Berrocal") ?: "Juan Berrocal"
    fun getUserFinca(): String = prefs.getString("user_finca", "Finca El Encanto") ?: "Finca El Encanto"
    fun getUserCorreo(): String = prefs.getString("user_correo", "usuario@ganaderia.com") ?: "usuario@ganaderia.com"

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }
}

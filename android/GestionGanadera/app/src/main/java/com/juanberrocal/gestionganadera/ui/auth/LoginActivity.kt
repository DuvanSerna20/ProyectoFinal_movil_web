package com.juanberrocal.gestionganadera.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juanberrocal.gestionganadera.GanaderiaApp
import com.juanberrocal.gestionganadera.data.api.ApiErrores
import com.juanberrocal.gestionganadera.data.api.LoginRequest
import com.juanberrocal.gestionganadera.data.api.RetrofitClient
import com.juanberrocal.gestionganadera.data.local.PreferenciasDataStore
import com.juanberrocal.gestionganadera.databinding.ActivityLoginBinding
import com.juanberrocal.gestionganadera.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Inicio de sesión.
 *
 * Seguridad: SOLO se guarda sesión tras una respuesta exitosa del servidor. Si falla la red o las
 * credenciales, el usuario se queda aquí con un mensaje de error. El acceso sin conexión solo es
 * posible si ya existe una sesión válida persistida en DataStore (login previo correcto).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferencias: PreferenciasDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencias = (application as GanaderiaApp).preferencias
        binding = ActivityLoginBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            // Sesión previa válida (login correcto + token): se entra aunque no haya red
            if (preferencias.sesion.first().activa) {
                irAMain()
            } else {
                setContentView(binding.root)
                configurarFormulario()
            }
        }
    }

    private fun configurarFormulario() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                mostrarError("Por favor complete todos los campos")
                return@setOnClickListener
            }
            iniciarSesion(email, password)
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun iniciarSesion(email: String, password: String) {
        mostrarCargando(true)
        lifecycleScope.launch {
            val error: String? = try {
                val resp = RetrofitClient.apiService.login(LoginRequest(correo = email, contrasena = password))
                val usuario = resp.body()?.data
                when {
                    resp.isSuccessful && usuario != null && !usuario.id.isNullOrBlank() -> {
                        preferencias.guardarSesion(
                            token = usuario.id,
                            nombre = usuario.nombre,
                            finca = usuario.finca,
                            correo = usuario.correo
                        )
                        Toast.makeText(this@LoginActivity, "¡Bienvenido, ${usuario.nombre}!", Toast.LENGTH_SHORT).show()
                        irAMain()
                        null
                    }
                    resp.code() == 400 || resp.code() == 401 || resp.code() == 404 ->
                        "Credenciales incorrectas" + (ApiErrores.mensaje(resp)?.let { ": $it" } ?: "")
                    resp.isSuccessful -> "Respuesta inválida del servidor"
                    else -> ApiErrores.mensaje(resp) ?: "Error del servidor (${resp.code()})"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "Login sin conexión: ${e.message}")
                "Sin conexión con el servidor. Verifica tu red e inténtalo de nuevo."
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en login", e)
                "No se pudo iniciar sesión: ${e.message ?: "error inesperado"}"
            }
            if (error != null) {
                mostrarCargando(false)
                mostrarError(error)
            }
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.btnLogin.isEnabled = !cargando
        binding.progressLogin.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) binding.tvLoginError.visibility = View.GONE
    }

    private fun mostrarError(mensaje: String) {
        binding.tvLoginError.text = mensaje
        binding.tvLoginError.visibility = View.VISIBLE
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val TAG = "LoginActivity"
    }
}

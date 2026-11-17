package com.juanberrocal.gestionganadera.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juanberrocal.gestionganadera.GanaderiaApp
import com.juanberrocal.gestionganadera.data.api.ApiErrores
import com.juanberrocal.gestionganadera.data.api.RegistroRequest
import com.juanberrocal.gestionganadera.data.api.RetrofitClient
import com.juanberrocal.gestionganadera.data.local.PreferenciasDataStore
import com.juanberrocal.gestionganadera.databinding.ActivityRegisterBinding
import com.juanberrocal.gestionganadera.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Registro de cuenta. Igual que el login, la sesión solo se guarda si el servidor crea la cuenta:
 * sin conexión NO se abre sesión local (antes cualquiera podía entrar con el servidor apagado).
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var preferencias: PreferenciasDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencias = (application as GanaderiaApp).preferencias

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val finca = binding.etFinca.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            if (nombre.isEmpty() || finca.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                avisar("Por favor complete todos los campos")
                return@setOnClickListener
            }
            if (password != confirm) {
                avisar("Las contraseñas no coinciden")
                return@setOnClickListener
            }

            binding.btnRegister.isEnabled = false
            lifecycleScope.launch {
                val error: String? = try {
                    val req = RegistroRequest(
                        nombre = nombre,
                        finca = finca,
                        correo = email,
                        contrasena = password,
                        rol = "Operario de Campo",
                        dispositivo = "Android Mobile"
                    )
                    val resp = RetrofitClient.apiService.register(req)
                    val usuario = resp.body()?.data
                    if (resp.isSuccessful && usuario != null && !usuario.id.isNullOrBlank()) {
                        preferencias.guardarSesion(usuario.id, usuario.nombre, usuario.finca, usuario.correo)
                        avisar("¡Cuenta creada con éxito!")
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                        null
                    } else {
                        ApiErrores.mensaje(resp) ?: "Error al registrar la cuenta (${resp.code()})"
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    "Sin conexión con el servidor. La cuenta no se creó; inténtalo de nuevo."
                } catch (e: Exception) {
                    "No se pudo registrar: ${e.message ?: "error inesperado"}"
                }
                if (error != null) {
                    avisar(error)
                    binding.btnRegister.isEnabled = true
                }
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun avisar(mensaje: String) = Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
}

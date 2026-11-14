package com.juanberrocal.gestionganadera.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juanberrocal.gestionganadera.data.SessionManager
import com.juanberrocal.gestionganadera.data.api.RegistroRequest
import com.juanberrocal.gestionganadera.data.api.RetrofitClient
import com.juanberrocal.gestionganadera.databinding.ActivityRegisterBinding
import com.juanberrocal.gestionganadera.ui.MainActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val finca = binding.etFinca.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            if (nombre.isEmpty() || finca.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnRegister.isEnabled = false

            lifecycleScope.launch {
                try {
                    val req = RegistroRequest(
                        nombre = nombre,
                        finca = finca,
                        correo = email,
                        contrasena = password,
                        rol = "Operario de Campo",
                        dispositivo = "Android Mobile"
                    )
                    val resp = RetrofitClient.apiService.register(req)
                    if (resp.isSuccessful) {
                        sessionManager.saveUser(nombre, finca, email)
                        sessionManager.isLoggedIn = true
                        Toast.makeText(this@RegisterActivity, "¡Cuenta creada en MongoDB con éxito!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errMsg = resp.errorBody()?.string() ?: "Error al registrar cuenta"
                        Toast.makeText(this@RegisterActivity, "Aviso del servidor: $errMsg", Toast.LENGTH_LONG).show()
                        binding.btnRegister.isEnabled = true
                    }
                } catch (e: Exception) {
                    // Si el servidor está apagado o no hay red, guardar localmente para modo offline
                    sessionManager.saveUser(nombre, finca, email)
                    sessionManager.isLoggedIn = true
                    Toast.makeText(this@RegisterActivity, "Cuenta guardada localmente (Modo sin conexión)", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}

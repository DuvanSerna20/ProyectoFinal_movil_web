package com.juanberrocal.gestionganadera.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juanberrocal.gestionganadera.data.SessionManager
import com.juanberrocal.gestionganadera.data.api.LoginRequest
import com.juanberrocal.gestionganadera.data.api.RetrofitClient
import com.juanberrocal.gestionganadera.databinding.ActivityLoginBinding
import com.juanberrocal.gestionganadera.ui.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn) {
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false

            lifecycleScope.launch {
                try {
                    val req = LoginRequest(correo = email, contrasena = password)
                    val resp = RetrofitClient.apiService.login(req)
                    if (resp.isSuccessful && resp.body()?.data != null) {
                        val user = resp.body()!!.data!!
                        sessionManager.saveUser(user.nombre, user.finca, user.correo)
                        sessionManager.isLoggedIn = true
                        Toast.makeText(this@LoginActivity, "¡Bienvenido, ${user.nombre}!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    } else {
                        Toast.makeText(this@LoginActivity, "Credenciales incorrectas o usuario no encontrado", Toast.LENGTH_SHORT).show()
                        binding.btnLogin.isEnabled = true
                    }
                } catch (e: Exception) {
                    // Fallback para pruebas sin servidor
                    sessionManager.isLoggedIn = true
                    Toast.makeText(this@LoginActivity, "Ingreso en modo local (Servidor no alcanzado)", Toast.LENGTH_SHORT).show()
                    goToMain()
                }
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

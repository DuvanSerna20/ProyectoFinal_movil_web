package com.juanberrocal.gestionganadera.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.juanberrocal.gestionganadera.databinding.ActivityOnboardingBinding
import com.juanberrocal.gestionganadera.data.SessionManager

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var sessionManager: SessionManager
    private var step = 0

    private val titles = arrayOf(
        "Gestión Ganadera Inteligente",
        "Control de Ganado y Potreros",
        "Rotación, Ordeño y Tareas"
    )

    private val descriptions = arrayOf(
        "Lleva el control completo de tu ganado, potreros, rotaciones de pastoreo, ordeños y tareas diarias de forma sencilla.",
        "Registra animales con código, nombre y raza, y administra tus potreros calculando el área automáticamente.",
        "Optimiza el pastoreo rotacional con fórmulas exactas, registra los litros diarios de leche y mantén tus tareas al día."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        // TEMPORAL: Siempre mostrar onboarding para demostración
        // if (sessionManager.isOnboardingCompleted) {
        //     goToLogin()
        //     return
        // }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateStep()

        binding.btnNext.setOnClickListener {
            step++
            if (step < titles.size) {
                updateStep()
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun updateStep() {
        binding.tvTitle.text = titles[step]
        binding.tvDescription.text = descriptions[step]
        if (step == titles.size - 1) {
            binding.btnNext.text = "Comenzar"
        } else {
            binding.btnNext.text = "Siguiente"
        }
    }

    private fun finishOnboarding() {
        // sessionManager.isOnboardingCompleted = true // Comentado temporal para demostración
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

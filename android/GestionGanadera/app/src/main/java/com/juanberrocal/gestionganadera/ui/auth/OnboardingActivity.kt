package com.juanberrocal.gestionganadera.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juanberrocal.gestionganadera.GanaderiaApp
import com.juanberrocal.gestionganadera.data.local.PreferenciasDataStore
import com.juanberrocal.gestionganadera.databinding.ActivityOnboardingBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var preferencias: PreferenciasDataStore
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
        preferencias = (application as GanaderiaApp).preferencias
        binding = ActivityOnboardingBinding.inflate(layoutInflater)

        // El estado del onboarding vive en DataStore: si ya se completó, se salta directo al login
        lifecycleScope.launch {
            if (preferencias.onboardingCompletado.first()) {
                goToLogin()
            } else {
                mostrarOnboarding(savedInstanceState?.getInt(KEY_STEP) ?: 0)
            }
        }
    }

    private fun mostrarOnboarding(pasoInicial: Int) {
        step = pasoInicial.coerceIn(0, titles.lastIndex)
        setContentView(binding.root)
        updateStep()

        binding.btnNext.setOnClickListener {
            step++
            if (step < titles.size) updateStep() else finishOnboarding()
        }
        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STEP, step)
    }

    private fun updateStep() {
        binding.tvTitle.text = titles[step]
        binding.tvDescription.text = descriptions[step]
        binding.btnNext.text = if (step == titles.size - 1) "Comenzar" else "Siguiente"
    }

    private fun finishOnboarding() {
        binding.btnNext.isEnabled = false
        binding.btnSkip.isEnabled = false
        lifecycleScope.launch {
            preferencias.setOnboardingCompletado(true)
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private companion object {
        const val KEY_STEP = "onboarding_step"
    }
}

package com.juanberrocal.gestionganadera.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.SessionManager
import com.juanberrocal.gestionganadera.databinding.ActivityMainBinding
import com.juanberrocal.gestionganadera.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val navController = navHost.navController

        val destinosPrincipales = setOf(
            R.id.inicioFragment,
            R.id.ganadoFragment,
            R.id.potrerosFragment,
            R.id.ordenoFragment,
            R.id.tareasFragment
        )
        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(destinosPrincipales))
        binding.barraNavegacion.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destino, _ ->
            when (destino.id) {
                R.id.rotacionFragment -> binding.barraNavegacion.menu.findItem(R.id.potrerosFragment).isChecked = true
                R.id.alimentacionFragment -> binding.barraNavegacion.menu.findItem(R.id.inicioFragment).isChecked = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Sincronizar con MongoDB y Node.js en segundo plano
        com.juanberrocal.gestionganadera.data.GanaderiaRepository.sincronizarConServidor()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_mi_cuenta) {
            mostrarDialogoMiCuenta()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun mostrarDialogoMiCuenta() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_mi_cuenta, null)
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvNombre)
        val tvFinca = dialogView.findViewById<TextView>(R.id.tvFinca)
        val tvCorreo = dialogView.findViewById<TextView>(R.id.tvCorreo)
        val tvInitials = dialogView.findViewById<TextView>(R.id.tvProfileInitials)
        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)

        val nombre = sessionManager.getUserNombre()
        val finca = sessionManager.getUserFinca()
        val correo = sessionManager.getUserCorreo()

        tvNombre.text = "Nombre: $nombre"
        tvFinca.text = "Finca: $finca"
        tvCorreo.text = "Correo: $correo"

        val initials = if (nombre.isNotBlank()) {
            nombre.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
        } else "JB"
        tvInitials.text = initials

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    sessionManager.logout()
                    dialog.dismiss()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        dialog.show()
    }
}

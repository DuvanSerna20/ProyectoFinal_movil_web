package com.juanberrocal.gestionganadera.ui.inicio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.databinding.FragmentInicioBinding
import com.juanberrocal.gestionganadera.databinding.ItemStatBinding
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.UiState
import kotlinx.coroutines.launch
import java.time.LocalDate

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GanaderiaViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configurarTarjeta(binding.cardGanado, R.drawable.ic_ganado, R.string.inicio_cabezas, R.id.ganadoFragment)
        configurarTarjeta(binding.cardPotreros, R.drawable.ic_potreros, R.string.inicio_potreros, R.id.potrerosFragment)
        configurarTarjeta(binding.cardLeche, R.drawable.ic_ordeno, R.string.inicio_litros_hoy, R.id.ordenoFragment)
        configurarTarjeta(binding.cardTareas, R.drawable.ic_tareas, R.string.inicio_tareas_pendientes, R.id.tareasFragment)

        viewModel.totalAnimales.observe(viewLifecycleOwner) { binding.cardGanado.tvValor.text = it.toString() }
        viewModel.totalPotreros.observe(viewLifecycleOwner) { binding.cardPotreros.tvValor.text = it.toString() }
        viewModel.litrosHoy.observe(viewLifecycleOwner) { binding.cardLeche.tvValor.text = Formato.litros(it) }
        viewModel.tareasPendientes.observe(viewLifecycleOwner) { binding.cardTareas.tvValor.text = it.toString() }

        configurarSincronizacion()

        binding.btnRotacion.setOnClickListener { findNavController().navigate(R.id.rotacionFragment) }
        binding.btnAlimentacion.setOnClickListener { findNavController().navigate(R.id.alimentacionFragment) }
    }

    override fun onResume() {
        super.onResume()
        binding.tvFecha.text = Formato.fechaLarga(LocalDate.now())
        viewModel.actualizarHoy()
    }

    /**
     * Switch de MODO_OFFLINE_PREFERIDO: activado = solo Room, sin llamadas a Retrofit.
     * Al desactivarlo se envían los pendientes y se refrescan los datos.
     */
    private fun configurarSincronizacion() {
        binding.switchModoOffline.setOnCheckedChangeListener { boton, activo ->
            if (boton.isPressed) viewModel.cambiarModoOffline(activo)
        }
        binding.btnSincronizar.setOnClickListener { viewModel.sincronizar() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.modoOffline.collect { activo ->
                        if (binding.switchModoOffline.isChecked != activo) binding.switchModoOffline.isChecked = activo
                        binding.btnSincronizar.isEnabled = !activo
                    }
                }
                launch {
                    viewModel.sincronizacionState.collect { estado ->
                        binding.progresoSync.visibility = if (estado is UiState.Loading) View.VISIBLE else View.GONE
                        binding.tvEstadoSync.text = when (estado) {
                            UiState.Loading -> getString(R.string.sync_en_curso)
                            UiState.Empty -> getString(R.string.sync_sin_datos)
                            is UiState.Success ->
                                if (estado.data.motivoOmitida != null) getString(R.string.sync_modo_offline)
                                else getString(R.string.sync_ok)
                            is UiState.Error -> estado.mensaje
                        }
                    }
                }
                launch {
                    viewModel.pendientesPorSincronizar.collect { n ->
                        binding.tvPendientes.visibility = if (n > 0) View.VISIBLE else View.GONE
                        binding.tvPendientes.text = getString(R.string.sync_pendientes, n)
                    }
                }
            }
        }
    }

    private fun configurarTarjeta(
        tarjeta: ItemStatBinding,
        @DrawableRes icono: Int,
        @StringRes etiqueta: Int,
        destino: Int
    ) {
        tarjeta.ivIcono.setImageResource(icono)
        tarjeta.tvEtiqueta.setText(etiqueta)
        tarjeta.root.setOnClickListener { findNavController().navigate(destino) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

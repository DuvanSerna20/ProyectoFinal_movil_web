package com.juanberrocal.gestionganadera.ui.inicio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.databinding.FragmentInicioBinding
import com.juanberrocal.gestionganadera.databinding.ItemStatBinding
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
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

        binding.btnRotacion.setOnClickListener { findNavController().navigate(R.id.rotacionFragment) }
        binding.btnAlimentacion.setOnClickListener { findNavController().navigate(R.id.alimentacionFragment) }
    }

    override fun onResume() {
        super.onResume()
        binding.tvFecha.text = Formato.fechaLarga(LocalDate.now())
        viewModel.actualizarHoy()
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

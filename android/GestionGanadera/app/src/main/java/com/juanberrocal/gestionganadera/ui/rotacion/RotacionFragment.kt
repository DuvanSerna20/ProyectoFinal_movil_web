package com.juanberrocal.gestionganadera.ui.rotacion

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.databinding.FragmentRotacionBinding
import com.juanberrocal.gestionganadera.logica.Calculos
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.elegirFecha
import java.time.LocalDate

object RotacionArgs {
    /** id LOCAL del potrero (UUID de Room: estable aunque se sincronice o se reordene la lista). */
    const val POTRERO_ID = "potreroId"
}

class RotacionFragment : Fragment() {

    private var _binding: FragmentRotacionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GanaderiaViewModel by activityViewModels()

    private var fechaIngreso: LocalDate = LocalDate.now()
    private var potreros: List<Potrero> = emptyList()
    private var potreroElegido: Potrero? = null
    private var potreroPendiente: String? = null // potrero recibido como argumento, por seleccionar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentRotacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        potreroPendiente = savedInstanceState?.getString(RotacionArgs.POTRERO_ID)
            ?: arguments?.getString(RotacionArgs.POTRERO_ID)

        binding.etFechaIngreso.setText(Formato.fecha(fechaIngreso))
        val abrirSelector = {
            elegirFecha(fechaIngreso) {
                fechaIngreso = it
                binding.etFechaIngreso.setText(Formato.fecha(it))
            }
        }
        binding.etFechaIngreso.setOnClickListener { abrirSelector() }
        binding.tilFechaIngreso.setEndIconOnClickListener { abrirSelector() }

        viewModel.potreros.observe(viewLifecycleOwner) { lista ->
            potreros = lista
            binding.actvPotrero.setAdapter(AdaptadorSinFiltro(requireContext(), lista.map { it.nombre }))
            potreroPendiente?.let { id ->
                lista.firstOrNull { it.id == id }?.let { cargarPotrero(it) }
                potreroPendiente = null
            }
        }

        binding.actvPotrero.setOnItemClickListener { _, _, posicion, _ ->
            potreros.getOrNull(posicion)?.let { cargarPotrero(it) }
        }
        binding.btnCalcular.setOnClickListener { calcular() }
    }

    private fun cargarPotrero(potrero: Potrero) {
        potreroElegido = potrero
        binding.actvPotrero.setText(potrero.nombre, false)
        binding.etOcupacion.setText(Formato.plano(potrero.diasOcupacionPlaneados))
        binding.etDescanso.setText(potrero.diasDescansoPlaneados.toString())
    }

    private fun calcular() {
        val ocupacion = Formato.leerDecimal(binding.etOcupacion.text?.toString())
        val descanso = Formato.leerEntero(binding.etDescanso.text?.toString())

        val error = getString(R.string.error_numero_mayor_cero)
        binding.tilOcupacion.error = if (ocupacion == null || ocupacion <= 0) error else null
        binding.tilDescanso.error = if (descanso == null || descanso <= 0) error else null
        if (ocupacion == null || ocupacion <= 0 || descanso == null || descanso <= 0) {
            binding.cardResultado.visibility = View.GONE
            return
        }

        val necesarios = Calculos.potrerosNecesarios(descanso, ocupacion)
        val total = Calculos.tiempoTotalRegulacion(ocupacion, necesarios)
        val proximo = Calculos.proximoCambio(fechaIngreso, ocupacion)

        binding.tvCada.text = Formato.dias(ocupacion)
        binding.tvProximo.text = Formato.fecha(proximo)
        binding.tvNecesarios.text = necesarios.toString()
        binding.tvTiempoTotal.text = Formato.dias(total)
        // Rotación aislada por finca: solo cuentan los potreros de la finca del potrero elegido
        val totalRegistrados = potreroElegido?.fincaId
            ?.let { finca -> potreros.count { it.fincaId == finca } }
            ?: potreros.size
        binding.tvDisponibles.text = if (totalRegistrados >= necesarios) {
            getString(R.string.rotacion_alcanzan, totalRegistrados)
        } else {
            getString(R.string.rotacion_faltan, totalRegistrados, necesarios - totalRegistrados)
        }
        binding.cardResultado.visibility = View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(RotacionArgs.POTRERO_ID, potreroPendiente ?: potreroElegido?.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class AdaptadorSinFiltro(contexto: Context, private val opciones: List<String>) :
        ArrayAdapter<String>(contexto, android.R.layout.simple_dropdown_item_1line, opciones) {

        override fun getFilter(): Filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                values = opciones
                count = opciones.size
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}

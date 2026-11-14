package com.juanberrocal.gestionganadera.ui.potreros

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.avisar
import com.juanberrocal.gestionganadera.ui.rotacion.RotacionArgs
import com.juanberrocal.gestionganadera.ui.tema.GanaderiaTema

/** Se mantiene como Fragment (nav_graph y barra inferior intactos); el contenido es Compose. */
class PotrerosFragment : Fragment(), AccionesPotreros {

    private val viewModel: GanaderiaViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GanaderiaTema {
                    PotrerosRuta(viewModel = viewModel, acciones = this@PotrerosFragment)
                }
            }
        }

    override fun avisarPotreroGuardado(esNuevo: Boolean) =
        avisar(if (esNuevo) R.string.potreros_guardado else R.string.potreros_actualizado)

    override fun avisarPotreroEliminado() = avisar(R.string.potreros_eliminado)

    override fun avisarPotreroListo() = avisar(R.string.potrero_marcado_listo)

    override fun avisarFincaGuardada() = avisar(R.string.finca_guardada)

    override fun avisarRotacion(destino: String) = avisar(getString(R.string.rotar_exito, destino))

    override fun abrirCalculadoraRotacion(potrero: Potrero) {
        findNavController().navigate(R.id.rotacionFragment, bundleOf(RotacionArgs.POTRERO_ID to potrero.mongoId))
    }
}

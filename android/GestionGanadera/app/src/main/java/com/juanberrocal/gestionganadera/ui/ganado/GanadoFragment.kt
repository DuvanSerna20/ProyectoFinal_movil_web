package com.juanberrocal.gestionganadera.ui.ganado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.avisar
import com.juanberrocal.gestionganadera.ui.tema.GanaderiaTema

/**
 * Se mantiene como Fragment para no tocar nav_graph ni la barra inferior;
 * todo el contenido se dibuja con Compose.
 */
class GanadoFragment : Fragment() {

    private val viewModel: GanaderiaViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GanaderiaTema {
                    GanadoRuta(
                        viewModel = viewModel,
                        onAvisoGuardado = { esNuevo ->
                            avisar(if (esNuevo) R.string.ganado_guardado else R.string.ganado_actualizado)
                        },
                        onAvisoEliminado = { avisar(R.string.ganado_eliminado) }
                    )
                }
            }
        }
}

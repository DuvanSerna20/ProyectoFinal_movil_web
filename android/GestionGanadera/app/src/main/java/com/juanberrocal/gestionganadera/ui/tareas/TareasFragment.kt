package com.juanberrocal.gestionganadera.ui.tareas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.avisar
import com.juanberrocal.gestionganadera.ui.comun.vistaCompose

/** Se mantiene como Fragment para no tocar nav_graph ni la barra inferior; el contenido es Compose. */
class TareasFragment : Fragment() {

    private val viewModel: GanaderiaViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        vistaCompose { TareasRuta(viewModel, onAviso = { avisar(it) }) }
}

package com.juanberrocal.gestionganadera.ui.alimentacion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.avisar
import com.juanberrocal.gestionganadera.ui.comun.vistaCompose

/** Se abre desde Inicio (botón «Alimentación y Medicamentos»). */
class AlimentacionFragment : Fragment() {

    private val viewModel: GanaderiaViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        vistaCompose { AlimentacionRuta(viewModel, onAviso = { avisar(it) }) }
}

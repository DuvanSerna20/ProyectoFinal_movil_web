package com.juanberrocal.gestionganadera.ui.potreros

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import com.juanberrocal.gestionganadera.ui.comun.AvisoEstado
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.CondicionPasto
import com.juanberrocal.gestionganadera.data.DepartamentoColombia
import com.juanberrocal.gestionganadera.data.EstadoPotrero
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.MotivoRotacion
import com.juanberrocal.gestionganadera.data.PisoTermico
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.TipoPasto
import com.juanberrocal.gestionganadera.data.UnidadArea
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.tema.GanaderiaTema
import java.time.LocalDate

/** Acciones que la pantalla delega al Fragment (avisos y navegación). */
interface AccionesPotreros {
    fun avisarPotreroGuardado(esNuevo: Boolean)
    fun avisarPotreroEliminado()
    fun avisarPotreroListo()
    fun avisarFincaGuardada()
    fun avisarRotacion(destino: String)
    fun abrirCalculadoraRotacion(potrero: Potrero)
}

/** Punto de entrada con ViewModel. */
@Composable
fun PotrerosRuta(viewModel: GanaderiaViewModel, acciones: AccionesPotreros) {
    val potreros by viewModel.potreros.observeAsState(emptyList())
    val fincas by viewModel.fincas.observeAsState(emptyList())
    val fincaSeleccionadaId by viewModel.fincaSeleccionadaId.observeAsState()
    val estado by viewModel.potrerosState.collectAsState()

    Column(Modifier.fillMaxSize()) {
    AvisoEstado(estado, onReintentar = viewModel::sincronizar)
    PotrerosScreen(
        potreros = potreros,
        fincas = fincas,
        // Si la finca guardada ya no existe (p. ej. se borró desde la web), se vuelve a "Todas"
        fincaSeleccionadaId = fincaSeleccionadaId?.takeIf { id -> fincas.any { it.id == id } },
        onSeleccionarFinca = viewModel::seleccionarFinca,
        onGuardarFinca = { finca, alTerminar ->
            viewModel.guardarFinca(finca) { guardada ->
                alTerminar(guardada)
                if (guardada != null) acciones.avisarFincaGuardada()
            }
        },
        onGuardarPotrero = { potrero, alTerminar ->
            viewModel.guardarPotrero(potrero) { ok ->
                alTerminar(ok)
                if (ok) acciones.avisarPotreroGuardado(potrero.id.isEmpty())
            }
        },
        onMarcarListo = { potrero ->
            viewModel.guardarPotrero(
                potrero.copy(estado = EstadoPotrero.LISTO, fechaUltimoCambio = LocalDate.now(), cantidadAnimalesActuales = 0)
            ) { ok -> if (ok) acciones.avisarPotreroListo() }
        },
        onEliminarPotrero = { potrero ->
            viewModel.eliminarPotrero(potrero)
            acciones.avisarPotreroEliminado()
        },
        onRotar = { origen, destino, motivo, alTerminar ->
            viewModel.rotarGanado(origen, destino, motivo) { error ->
                alTerminar(error)
                if (error == null) acciones.avisarRotacion(destino.nombre)
            }
        },
        onCalcularRotacion = acciones::abrirCalculadoraRotacion
    )
    }
}

private sealed interface HojaAbierta {
    data object NuevaFinca : HojaAbierta
    data class FormPotrero(val potrero: Potrero?) : HojaAbierta
    data class Rotar(val origen: Potrero) : HojaAbierta
}

@Composable
fun PotrerosScreen(
    potreros: List<Potrero>,
    fincas: List<Finca>,
    fincaSeleccionadaId: String?,
    onSeleccionarFinca: (String?) -> Unit,
    onGuardarFinca: (Finca, (Finca?) -> Unit) -> Unit,
    onGuardarPotrero: (Potrero, (Boolean) -> Unit) -> Unit,
    onMarcarListo: (Potrero) -> Unit,
    onEliminarPotrero: (Potrero) -> Unit,
    onRotar: (Potrero, Potrero, MotivoRotacion, (String?) -> Unit) -> Unit,
    onCalcularRotacion: (Potrero) -> Unit
) {
    var hoja by remember { mutableStateOf<HojaAbierta?>(null) }
    var porEliminar by remember { mutableStateOf<Potrero?>(null) }

    val fincasPorId = remember(fincas) { fincas.associateBy { it.id } }
    val fincaActiva = fincaSeleccionadaId?.let { fincasPorId[it] }
    val visibles = remember(potreros, fincas, fincaSeleccionadaId) {
        potreros.deFinca(fincaSeleccionadaId)
            .sortedWith(compareBy({ fincasPorId[it.fincaId]?.nombre?.lowercase() ?: "￿" }, { it.nombre.lowercase() }))
    }
    val resumen = remember(visibles, fincas) { resumenPotreros(visibles, fincas) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BarraSelectorFinca(
            fincas = fincas,
            fincaSeleccionadaId = fincaSeleccionadaId,
            onSeleccion = onSeleccionarFinca,
            onNuevaFinca = { hoja = HojaAbierta.NuevaFinca }
        )

        Box(Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "resumen") {
                    TarjetaResumenPotreros(resumen, fincaActiva, Modifier.padding(bottom = 8.dp))
                }

                if (visibles.isEmpty()) {
                    item(key = "vacio") {
                        Text(
                            stringResource(
                                when {
                                    fincas.isEmpty() -> R.string.potreros_sin_fincas
                                    fincaActiva != null -> R.string.potreros_vacio_finca
                                    else -> R.string.potreros_vacio
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }

                items(visibles, key = { it.id }) { potrero ->
                    PotreroCard(
                        potrero = potrero,
                        finca = potrero.fincaId?.let { fincasPorId[it] },
                        onEditar = { hoja = HojaAbierta.FormPotrero(potrero) },
                        onEliminar = { porEliminar = potrero },
                        onRotar = { hoja = HojaAbierta.Rotar(potrero) },
                        onMarcarListo = { onMarcarListo(potrero) },
                        onCalcularRotacion = { onCalcularRotacion(potrero) },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .animateItem()
                    )
                }
            }

            // Sin fincas no se puede crear un potrero (la finca es obligatoria): el FAB abre "Nueva finca"
            ExtendedFloatingActionButton(
                onClick = {
                    hoja = if (fincas.isEmpty()) HojaAbierta.NuevaFinca else HojaAbierta.FormPotrero(null)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(stringResource(if (fincas.isEmpty()) R.string.finca_nueva else R.string.potreros_agregar))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    when (val h = hoja) {
        HojaAbierta.NuevaFinca -> FincaFormSheet(
            fincasExistentes = fincas,
            onGuardar = onGuardarFinca,
            onCerrar = { hoja = null }
        )
        is HojaAbierta.FormPotrero -> PotreroFormSheet(
            potreroInicial = h.potrero,
            fincaPreseleccionada = fincaSeleccionadaId ?: fincas.singleOrNull()?.id,
            fincas = fincas,
            potrerosExistentes = potreros,
            onGuardar = onGuardarPotrero,
            onCerrar = { hoja = null }
        )
        is HojaAbierta.Rotar -> RotarGanadoSheet(
            origen = h.origen,
            destinos = destinosRotacion(h.origen, potreros),
            finca = h.origen.fincaId?.let { fincasPorId[it] },
            onConfirmar = { destino, motivo, alTerminar -> onRotar(h.origen, destino, motivo, alTerminar) },
            onCerrar = { hoja = null }
        )
        null -> Unit
    }

    porEliminar?.let { potrero ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.eliminar_titulo)) },
            text = { Text(stringResource(R.string.potreros_confirmar_eliminar, potrero.nombre)) },
            confirmButton = {
                TextButton(onClick = { onEliminarPotrero(potrero); porEliminar = null }) {
                    Text(stringResource(R.string.eliminar_si), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PotrerosScreenPreview() {
    val roble = Finca("f1", "Hacienda El Roble", DepartamentoColombia.RISARALDA, "Pereira", PisoTermico.MEDIO)
    val manuela = Finca("f2", "Finca La Manuela", DepartamentoColombia.CALDAS, "Manizales", PisoTermico.FRIO)
    GanaderiaTema {
        PotrerosScreen(
            potreros = listOf(
                Potrero("1", "f1", "El Árbol", 2.5, UnidadArea.HECTAREAS, tipoPasto = TipoPasto.ESTRELLA,
                    estado = EstadoPotrero.OCUPADO, condicionPasto = CondicionPasto.BAJO, diasOcupacionPlaneados = 3.0,
                    fechaUltimoCambio = LocalDate.now().minusDays(2), cantidadAnimalesActuales = 18, mongoId = "p1"),
                Potrero("2", "f1", "La Loma", 3.0, UnidadArea.CUADRAS, tipoPasto = TipoPasto.BRACHIARIA,
                    estado = EstadoPotrero.LISTO, mongoId = "p2"),
                Potrero("3", "f2", "El Bajo", 4500.0, UnidadArea.METROS_CUADRADOS, tipoPasto = TipoPasto.KIKUYO,
                    estado = EstadoPotrero.EN_DESCANSO, diasDescansoPlaneados = 35,
                    fechaUltimoCambio = LocalDate.now().minusDays(20), mongoId = "p3")
            ),
            fincas = listOf(roble, manuela),
            fincaSeleccionadaId = null,
            onSeleccionarFinca = {},
            onGuardarFinca = { _, listo -> listo(null) },
            onGuardarPotrero = { _, listo -> listo(true) },
            onMarcarListo = {},
            onEliminarPotrero = {},
            onRotar = { _, _, _, listo -> listo(null) },
            onCalcularRotacion = {}
        )
    }
}

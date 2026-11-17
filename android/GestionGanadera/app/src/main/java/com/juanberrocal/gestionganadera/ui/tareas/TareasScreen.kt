package com.juanberrocal.gestionganadera.ui.tareas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import com.juanberrocal.gestionganadera.ui.comun.AvisoEstado
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.Tarea
import com.juanberrocal.gestionganadera.data.TipoTareaEspecial
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.comun.DialogoConfirmar
import com.juanberrocal.gestionganadera.ui.comun.FormaTarjetaComun
import com.juanberrocal.gestionganadera.ui.comun.MenuEditarEliminar
import com.juanberrocal.gestionganadera.ui.comun.TextoVacio
import com.juanberrocal.gestionganadera.ui.comun.bordeTarjetaComun
import com.juanberrocal.gestionganadera.ui.ganado.Etiqueta
import com.juanberrocal.gestionganadera.ui.tema.ColoresSanidad
import com.juanberrocal.gestionganadera.ui.tema.GanaderiaTema
import java.time.LocalDate

@Composable
fun TareasRuta(viewModel: GanaderiaViewModel, onAviso: (String) -> Unit) {
    val tareas by viewModel.tareas.observeAsState(emptyList())
    val animales by viewModel.animales.observeAsState(emptyList())
    val estado by viewModel.tareasState.collectAsState()

    Column(Modifier.fillMaxSize()) {
    AvisoEstado(estado, onReintentar = viewModel::sincronizar)
    TareasScreen(
        tareas = tareas,
        animales = animales,
        onGuardar = { tarea, alTerminar ->
            viewModel.guardarTarea(tarea) { ok ->
                alTerminar(ok)
                if (ok) onAviso(if (tarea.id.isEmpty()) "Tarea guardada" else "Tarea actualizada")
            }
        },
        onMarcar = viewModel::marcarTarea,
        onEliminar = { tarea ->
            viewModel.eliminarTarea(tarea) { ok -> onAviso(if (ok) "Tarea eliminada" else "No se pudo eliminar la tarea") }
        }
    )
    }
}

/** Colores del badge por tipo de evento sanitario. */
fun coloresTipo(tipo: TipoTareaEspecial): Pair<Color, Color> = when (tipo) {
    TipoTareaEspecial.VACUNACION -> ColoresSanidad.vacunaFondo to ColoresSanidad.vacunaTexto
    TipoTareaEspecial.VISITA_VETERINARIA -> ColoresSanidad.veterinariaFondo to ColoresSanidad.veterinariaTexto
    TipoTareaEspecial.DESPARASITACION -> ColoresSanidad.purgaFondo to ColoresSanidad.purgaTexto
    TipoTareaEspecial.TRATAMIENTO_MEDICO -> ColoresSanidad.retiroFondo to ColoresSanidad.retiroTexto
    TipoTareaEspecial.CONTROL_REPRODUCTIVO -> ColoresSanidad.reproductivoFondo to ColoresSanidad.reproductivoTexto
    TipoTareaEspecial.OTRO -> ColoresSanidad.neutroFondo to ColoresSanidad.neutroTexto
}

private data class FormTarea(val tarea: Tarea?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(
    tareas: List<Tarea>,
    animales: List<Animal>,
    onGuardar: (Tarea, (Boolean) -> Unit) -> Unit,
    onMarcar: (Tarea, Boolean) -> Unit,
    onEliminar: (Tarea) -> Unit
) {
    var verEspeciales by rememberSaveable { mutableStateOf(false) }
    var formulario by remember { mutableStateOf<FormTarea?>(null) }
    var porEliminar by remember { mutableStateOf<Tarea?>(null) }

    val hoy = LocalDate.now()
    val delApartado = remember(tareas, verEspeciales) { tareas.filter { it.esTareaEspecial == verEspeciales } }
    val pendientes = remember(delApartado) { delApartado.filter { !it.completada }.sortedBy { it.fechaProgramada } }
    val completadas = remember(delApartado) {
        delApartado.filter { it.completada }.sortedByDescending { it.fechaProgramada }
    }
    val animalesPorId = remember(animales) { animales.associateBy { it.id } }
    val conteo = { especial: Boolean -> tareas.count { it.esTareaEspecial == especial && !it.completada } }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Selector destacado justo debajo de la barra superior
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            ) {
                listOf(false, true).forEachIndexed { i, especial ->
                    SegmentedButton(
                        selected = verEspeciales == especial,
                        onClick = { verEspeciales = especial },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        colors = SegmentedButtonDefaults.colors(inactiveContainerColor = MaterialTheme.colorScheme.surface),
                        icon = {},
                        label = {
                            Text(
                                (if (especial) "Tareas Especiales (Sanidad Animal)" else "Tareas Generales") +
                                    " · ${conteo(especial)}",
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                fontWeight = if (verEspeciales == especial) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "pendientes") { TituloLista("Pendientes (${pendientes.size})") }
                if (delApartado.isEmpty()) {
                    item(key = "vacio") {
                        TextoVacio(
                            if (verEspeciales) "No hay tareas de sanidad animal.\nToque «Nueva tarea» para programar una vacuna, visita o tratamiento."
                            else "Aún no hay tareas.\nToque «Nueva tarea» para empezar."
                        )
                    }
                }
                items(pendientes, key = { it.id }) { tarea ->
                    TarjetaTarea(
                        tarea = tarea,
                        etiquetaAnimal = tarea.animalId?.let { animalesPorId[it]?.codigoNombre } ?: tarea.animalCodigoNombre,
                        hoy = hoy,
                        onMarcar = { onMarcar(tarea, it) },
                        onEditar = { formulario = FormTarea(tarea) },
                        onEliminar = { porEliminar = tarea }
                    )
                }
                if (completadas.isNotEmpty()) {
                    item(key = "completadas") { TituloLista("Completadas (${completadas.size})") }
                    items(completadas, key = { it.id }) { tarea ->
                        TarjetaTarea(
                            tarea = tarea,
                            etiquetaAnimal = tarea.animalId?.let { animalesPorId[it]?.codigoNombre } ?: tarea.animalCodigoNombre,
                            hoy = hoy,
                            onMarcar = { onMarcar(tarea, it) },
                            onEditar = { formulario = FormTarea(tarea) },
                            onEliminar = { porEliminar = tarea }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { formulario = FormTarea(null) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Nueva tarea") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    formulario?.let { abierto ->
        TareaFormSheet(
            inicial = abierto.tarea,
            especialPorDefecto = verEspeciales,
            animales = animales,
            onGuardar = { tarea, alTerminar ->
                onGuardar(tarea) { ok ->
                    alTerminar(ok)
                    // Si cambió de apartado al editar, se muestra donde quedó
                    if (ok) verEspeciales = tarea.esTareaEspecial
                }
            },
            onCerrar = { formulario = null }
        )
    }

    porEliminar?.let { tarea ->
        DialogoConfirmar(
            titulo = "Eliminar tarea",
            mensaje = "¿Desea eliminar la tarea «${tarea.titulo}»? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = { onEliminar(tarea); porEliminar = null },
            onCancelar = { porEliminar = null }
        )
    }
}

@Composable
private fun TituloLista(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaTarea(
    tarea: Tarea,
    etiquetaAnimal: String?,
    hoy: LocalDate,
    onMarcar: (Boolean) -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val apagada = tarea.completada
    Card(
        shape = FormaTarjetaComun,
        border = bordeTarjetaComun(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEditar,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = tarea.completada, onCheckedChange = onMarcar)
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 10.dp)
            ) {
                Text(
                    tarea.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (apagada) TextDecoration.LineThrough else null,
                    color = if (apagada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (tarea.descripcion.isNotBlank()) {
                    Text(
                        tarea.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val vencida = tarea.vencida(hoy)
                Text(
                    when {
                        vencida -> "Vencida · ${Formato.fecha(tarea.fechaProgramada)}"
                        tarea.fechaProgramada == hoy && !apagada -> "Hoy · ${Formato.fecha(tarea.fechaProgramada)}"
                        else -> Formato.fecha(tarea.fechaProgramada)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (vencida) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tarea.esTareaEspecial) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val tipo = tarea.tipoTareaEspecial ?: TipoTareaEspecial.OTRO
                        val (fondo, color) = coloresTipo(tipo)
                        Etiqueta(tipo.etiqueta, fondo, color)
                        Etiqueta(etiquetaAnimal ?: "Todo el hato", ColoresSanidad.neutroFondo, ColoresSanidad.neutroTexto)
                    }
                }
            }
            MenuEditarEliminar(onEditar = onEditar, onEliminar = onEliminar)
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun TareasScreenPreview() {
    val hoy = LocalDate.now()
    GanaderiaTema {
        TareasScreen(
            tareas = listOf(
                Tarea("t1", "Vacuna aftosa", "Ciclo nacional", hoy.plusDays(2), esTareaEspecial = true,
                    tipoTareaEspecial = TipoTareaEspecial.VACUNACION),
                Tarea("t2", "Palpación", "", hoy.minusDays(1), esTareaEspecial = true,
                    tipoTareaEspecial = TipoTareaEspecial.VISITA_VETERINARIA, animalId = "a1", animalCodigoNombre = "#008 - Luna"),
                Tarea("t3", "Revisar cercas", "Potrero Árbol", hoy)
            ),
            animales = listOf(Animal("a1", "008", "Luna", raza = "Girolando")),
            onGuardar = { _, listo -> listo(true) },
            onMarcar = { _, _ -> },
            onEliminar = {}
        )
    }
}

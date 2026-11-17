package com.juanberrocal.gestionganadera.ui.ganado

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.DepartamentoColombia
import com.juanberrocal.gestionganadera.data.EstadoReproductivo
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.PisoTermico
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.Especie
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.logica.Sanidad
import com.juanberrocal.gestionganadera.data.Sexo
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.tema.GanaderiaTema

/** Punto de entrada con ViewModel: observa los datos y delega la UI a [GanadoScreen]. */
@Composable
fun GanadoRuta(
    viewModel: GanaderiaViewModel,
    onAvisoGuardado: (esNuevo: Boolean) -> Unit,
    onAvisoEliminado: () -> Unit
) {
    val animales by viewModel.animales.observeAsState()
    val potreros by viewModel.potreros.observeAsState(emptyList())
    val fincas by viewModel.fincas.observeAsState(emptyList())
    val alimentacion by viewModel.alimentacion.observeAsState(emptyList())
    val estado by viewModel.animalesState.collectAsState()

    Column(Modifier.fillMaxSize()) {
    AvisoEstado(estado, onReintentar = viewModel::sincronizar)
    GanadoScreen(
        animales = animales,
        fincas = fincas,
        potreros = potreros,
        alimentacion = alimentacion,
        onGuardar = { animal, alTerminar ->
            viewModel.guardarAnimal(animal) { guardado ->
                alTerminar(guardado)
                if (guardado) onAvisoGuardado(animal.id.isEmpty())
            }
        },
        onEliminar = { animal ->
            viewModel.eliminarAnimal(animal)
            onAvisoEliminado()
        }
    )
    }
}

/** Envoltura del formulario abierto: `animal == null` significa "agregar". */
private data class FormularioAbierto(val animal: Animal?)

/**
 * Pantalla de Ganado sin dependencias de Android/ViewModel (fácil de previsualizar y probar).
 * @param animales null mientras Room carga la primera vez (evita mostrar "vacío" por un instante).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GanadoScreen(
    animales: List<Animal>?,
    fincas: List<Finca>,
    potreros: List<Potrero>,
    onGuardar: (Animal, (Boolean) -> Unit) -> Unit,
    onEliminar: (Animal) -> Unit,
    alimentacion: List<RegistroAlimentacionMedicamento> = emptyList()
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(FiltroGanado.TODOS) }
    var formulario by remember { mutableStateOf<FormularioAbierto?>(null) }
    var porEliminar by remember { mutableStateOf<Animal?>(null) }
    var actualizacionRapida by remember { mutableStateOf<Animal?>(null) }

    val lista = animales.orEmpty()
    val resumen = remember(lista) { lista.resumen() }
    val visibles = remember(lista, busqueda, filtro) { lista.filtrar(busqueda, filtro) }
    val potrerosPorId = remember(potreros) {
        potreros.associateBy { it.id }
    }
    val fincasPorId = remember(fincas) { fincas.associateBy { it.id } }
    val manejoPorAnimal = remember(lista, alimentacion) { Sanidad.manejoPorAnimal(lista, alimentacion) }

    /** "Finca El Roble / Potrero Árbol". Si solo tiene potrero, la finca se toma del potrero. */
    fun ubicacion(animal: Animal): String? {
        val potrero = animal.potreroId?.let { potrerosPorId[it] }
        val finca = (animal.fincaId ?: potrero?.fincaId)?.let { fincasPorId[it] }
        return listOfNotNull(finca?.nombre, potrero?.nombre).joinToString(" / ").ifEmpty { null }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "resumen") {
                TarjetaResumen(resumen, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
            }

            stickyHeader(key = "busqueda") {
                BarraBusquedaYFiltros(
                    busqueda = busqueda,
                    onBusqueda = { busqueda = it },
                    filtro = filtro,
                    onFiltro = { filtro = it },
                    conteo = { f -> lista.count(f.coincide) }
                )
            }

            if (animales != null && visibles.isEmpty()) {
                item(key = "vacio") {
                    Text(
                        stringResource(if (lista.isEmpty()) R.string.ganado_vacio else R.string.ganado_sin_resultados),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }

            items(visibles, key = { it.id }) { animal ->
                AnimalCard(
                    animal = animal,
                    ubicacion = ubicacion(animal),
                    onEditar = { formulario = FormularioAbierto(animal) },
                    onActualizarRapido = { actualizacionRapida = animal },
                    onEliminar = { porEliminar = animal },
                    manejo = manejoPorAnimal[animal.id],
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .animateItem()
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { formulario = FormularioAbierto(null) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.ganado_agregar)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    formulario?.let { abierto ->
        AnimalFormSheet(
            animalInicial = abierto.animal,
            animalesExistentes = lista,
            fincas = fincas,
            potreros = potreros,
            onGuardar = onGuardar,
            onCerrar = { formulario = null }
        )
    }

    actualizacionRapida?.let { animal ->
        DialogoActualizarRapido(
            animal = animal,
            onGuardar = onGuardar,
            onCerrar = { actualizacionRapida = null }
        )
    }

    porEliminar?.let { animal ->
        DialogoConfirmarEliminar(
            animal = animal,
            onConfirmar = {
                onEliminar(animal)
                porEliminar = null
            },
            onCancelar = { porEliminar = null }
        )
    }
}

@Composable
private fun BarraBusquedaYFiltros(
    busqueda: String,
    onBusqueda: (String) -> Unit,
    filtro: FiltroGanado,
    onFiltro: (FiltroGanado) -> Unit,
    conteo: (FiltroGanado) -> Int
) {
    val foco = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusqueda,
            placeholder = { Text(stringResource(R.string.ganado_buscar)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (busqueda.isNotEmpty()) {
                {
                    IconButton(onClick = { onBusqueda("") }) {
                        Icon(Icons.Default.Close, stringResource(R.string.ganado_limpiar_busqueda))
                    }
                }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
            shape = RoundedCornerShape(50),
            colors = coloresCampo(),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(FiltroGanado.entries, key = { it.name }) { f ->
                FilterChip(
                    selected = filtro == f,
                    onClick = { onFiltro(f) },
                    label = {
                        Text(
                            "${stringResource(f.etiqueta)} (${conteo(f)})",
                            fontWeight = if (filtro == f) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

@Composable
private fun DialogoConfirmarEliminar(animal: Animal, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.eliminar_titulo)) },
        text = {
            Text(stringResource(R.string.ganado_confirmar_eliminar, "#${animal.codigo} - ${animal.nombre}"))
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(R.string.eliminar_si), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun GanadoScreenPreview() {
    GanaderiaTema {
        GanadoScreen(
            animales = listOf(
                Animal("a1", "008", "Luna", raza = "Girolando", proposito = PropositoEtapa.PRODUCCION_ORDENO,
                    estadoReproductivo = EstadoReproductivo.CARGADA, pesoKg = 480.0, produccionLitrosDia = 18.5,
                    fincaId = "f1", potreroId = "p1"),
                Animal("a2", "012", "Canela", raza = "Jersey", proposito = PropositoEtapa.VACA_SECA,
                    estadoReproductivo = EstadoReproductivo.HORRA, pesoKg = 410.0),
                Animal("a3", "001", "Tormento", sexo = Sexo.MACHO, raza = "Gyr", proposito = PropositoEtapa.TORO_REPRODUCTOR,
                    estadoReproductivo = EstadoReproductivo.NO_APLICA, pesoKg = 720.0, potreroId = "p1"),
                Animal("a4", "C01", "Lucero", especie = Especie.EQUINO, sexo = Sexo.MACHO, raza = "Criollo",
                    proposito = PropositoEtapa.TRABAJO_COMPANIA, estadoReproductivo = EstadoReproductivo.NO_APLICA)
            ),
            fincas = listOf(Finca("f1", "El Roble", DepartamentoColombia.RISARALDA, "Pereira", PisoTermico.MEDIO)),
            potreros = listOf(Potrero(id = "p1", fincaId = "f1", nombre = "Árbol", area = 2.0, mongoId = "m1")),
            onGuardar = { _, listo -> listo(true) },
            onEliminar = {}
        )
    }
}

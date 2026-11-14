package com.juanberrocal.gestionganadera.ui.ordeno

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.DestinoLeche
import com.juanberrocal.gestionganadera.data.JornadaOrdeno
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.comun.DialogoConfirmar
import com.juanberrocal.gestionganadera.ui.comun.DialogoFecha
import com.juanberrocal.gestionganadera.ui.comun.FormaTarjetaComun
import com.juanberrocal.gestionganadera.ui.comun.MenuEditarEliminar
import com.juanberrocal.gestionganadera.ui.comun.TextoVacio
import com.juanberrocal.gestionganadera.ui.comun.bordeTarjetaComun
import com.juanberrocal.gestionganadera.ui.ganado.Etiqueta
import com.juanberrocal.gestionganadera.ui.tema.ColoresSanidad
import com.juanberrocal.gestionganadera.ui.tema.GanaderiaTema
import java.time.LocalDate

@Composable
fun OrdenoRuta(viewModel: GanaderiaViewModel, onAviso: (String) -> Unit) {
    val registros by viewModel.ordenos.observeAsState(emptyList())
    val animales by viewModel.animales.observeAsState(emptyList())
    val vacas by viewModel.vacasEnOrdenoList.observeAsState(emptyList())
    val alimentacion by viewModel.alimentacion.observeAsState(emptyList())
    val dia by viewModel.diaConsultado.observeAsState(LocalDate.now())

    OrdenoScreen(
        registros = registros,
        animales = animales,
        vacasEnProduccion = vacas,
        alimentacion = alimentacion,
        dia = dia,
        onDia = viewModel::consultarDia,
        onGuardar = { registro, alTerminar ->
            viewModel.guardarOrdeno(registro) { ok ->
                alTerminar(ok)
                if (ok) onAviso(if (registro.id.isEmpty()) "Ordeño guardado" else "Ordeño actualizado")
            }
        },
        onEliminar = { registro, avisar ->
            viewModel.eliminarOrdeno(registro) { ok ->
                if (avisar) onAviso(if (ok) "Ordeño eliminado" else "No se pudo eliminar el ordeño")
            }
        }
    )
}

private data class FormOrdeno(val registro: RegistroOrdeno?)

@Composable
fun OrdenoScreen(
    registros: List<RegistroOrdeno>,
    animales: List<Animal>,
    vacasEnProduccion: List<Animal>,
    alimentacion: List<RegistroAlimentacionMedicamento>,
    dia: LocalDate,
    onDia: (LocalDate) -> Unit,
    onGuardar: (RegistroOrdeno, (Boolean) -> Unit) -> Unit,
    onEliminar: (RegistroOrdeno, avisar: Boolean) -> Unit
) {
    var formulario by remember { mutableStateOf<FormOrdeno?>(null) }
    var porEliminar by remember { mutableStateOf<RegistroOrdeno?>(null) }

    val delDia = remember(registros, dia) { OrdenoLogica.registrosDelDia(registros, dia) }
    val resumen = remember(registros, dia, vacasEnProduccion) { OrdenoLogica.resumen(registros, dia, vacasEnProduccion) }
    val animalesPorId = remember(animales) { animales.associateBy { it.id } }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "dia") { SelectorDia(dia, onDia) }
            item(key = "resumen") { TarjetaResumenOrdeno(resumen, esHoy = dia == LocalDate.now()) }
            item(key = "titulo") {
                Text(
                    "Registros del día (${delDia.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (delDia.isEmpty()) {
                item(key = "vacio") {
                    TextoVacio("No hay ordeños registrados este día.\nToque «Registrar ordeño» para empezar.")
                }
            }
            items(delDia, key = { it.id }) { registro ->
                TarjetaRegistroOrdeno(
                    registro = registro,
                    // Nombre actual de la vaca (si la editaron en Ganado); si no, el guardado en el registro
                    etiqueta = registro.animalId?.let { animalesPorId[it]?.codigoNombre } ?: registro.animalCodigoNombre,
                    onEditar = { formulario = FormOrdeno(registro) },
                    onEliminar = { porEliminar = registro }
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { formulario = FormOrdeno(null) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Registrar ordeño") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    formulario?.let { abierto ->
        OrdenoFormSheet(
            inicial = abierto.registro,
            diaInicial = dia,
            vacasEnProduccion = vacasEnProduccion,
            animales = animales,
            registros = registros,
            alimentacion = alimentacion,
            onGuardar = onGuardar,
            onEliminarOriginal = { onEliminar(it, false) },
            onCerrar = { formulario = null }
        )
    }

    porEliminar?.let { registro ->
        DialogoConfirmar(
            titulo = "Eliminar ordeño",
            mensaje = "¿Desea eliminar el ordeño de ${registro.animalCodigoNombre} del ${Formato.fecha(registro.fecha)} " +
                "(${registro.jornada.etiqueta.lowercase()}, ${Formato.litros(registro.litros)})? Esta acción no se puede deshacer.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = { onEliminar(registro, true); porEliminar = null },
            onCancelar = { porEliminar = null }
        )
    }
}

@Composable
private fun SelectorDia(dia: LocalDate, onDia: (LocalDate) -> Unit) {
    var calendario by remember { mutableStateOf(false) }
    val hoy = LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = { onDia(dia.minusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Día anterior")
        }
        TextButton(onClick = { calendario = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.DateRange, null, modifier = Modifier.padding(end = 6.dp))
            Text(
                when (dia) {
                    hoy -> "Hoy · ${Formato.fecha(dia)}"
                    hoy.minusDays(1) -> "Ayer · ${Formato.fecha(dia)}"
                    else -> Formato.fecha(dia)
                },
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = { onDia(dia.plusDays(1)) }, enabled = dia.isBefore(hoy)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Día siguiente")
        }
    }
    if (calendario) {
        DialogoFecha(dia, onElegir = { onDia(it); calendario = false }, onCerrar = { calendario = false })
    }
}

@Composable
private fun TarjetaResumenOrdeno(resumen: ResumenOrdenoDia, esHoy: Boolean) {
    val blanco = Color.White
    val suave = MaterialTheme.colorScheme.primaryContainer
    Card(
        shape = FormaTarjetaComun,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Litros comerciales del día", color = suave, style = MaterialTheme.typography.bodyMedium)
            Text(Formato.litros(resumen.litrosComerciales), color = blanco, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                "Mañana: ${Formato.litros(resumen.litrosManana)} · Tarde: ${Formato.litros(resumen.litrosTarde)}",
                color = blanco,
                style = MaterialTheme.typography.bodyLarge
            )
            if (resumen.litrosDescarte > 0 || resumen.litrosTernero > 0) {
                Text(
                    listOfNotNull(
                        resumen.litrosDescarte.takeIf { it > 0 }?.let { "Descarte: ${Formato.litros(it)}" },
                        resumen.litrosTernero.takeIf { it > 0 }?.let { "Terneros: ${Formato.litros(it)}" }
                    ).joinToString(" · ") + " (no suman al tanque)",
                    color = suave,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "${resumen.vacasOrdenadas} de ${resumen.vacasEnProduccion} vacas en producción ordeñadas " +
                    if (esHoy) "hoy" else "ese día",
                color = blanco,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { resumen.progreso },
                color = blanco,
                trackColor = blanco.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

@Composable
private fun TarjetaRegistroOrdeno(
    registro: RegistroOrdeno,
    etiqueta: String,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        shape = FormaTarjetaComun,
        border = bordeTarjetaComun(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEditar,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    etiqueta,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (fondo, color) = if (registro.jornada == JornadaOrdeno.MANANA)
                        ColoresSanidad.ambarFondo to ColoresSanidad.ambarTexto
                    else ColoresSanidad.vacunaFondo to ColoresSanidad.vacunaTexto
                    Etiqueta(registro.jornada.etiqueta, fondo, color)
                    when (registro.destinoLeche) {
                        DestinoLeche.RETIRO_MEDICO ->
                            Etiqueta("Retiro médico", ColoresSanidad.retiroFondo, ColoresSanidad.retiroTexto)
                        DestinoLeche.CRIA_TERNERO ->
                            Etiqueta("Consumo ternero", ColoresSanidad.purgaFondo, ColoresSanidad.purgaTexto)
                        DestinoLeche.TANQUE_VENTA -> Unit
                    }
                }
            }
            Text(
                Formato.litros(registro.litros),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (registro.destinoLeche.esComercial) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
            MenuEditarEliminar(onEditar = onEditar, onEliminar = onEliminar)
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun OrdenoScreenPreview() {
    val hoy = LocalDate.now()
    val luna = Animal("a1", "008", "Luna", raza = "Girolando", produccionLitrosDia = 18.5)
    val canela = Animal("a2", "012", "Canela", raza = "Jersey", produccionLitrosDia = 12.0)
    GanaderiaTema {
        OrdenoScreen(
            registros = listOf(
                RegistroOrdeno("r1", "a1", "#008 - Luna", hoy, JornadaOrdeno.MANANA, 10.0),
                RegistroOrdeno("r2", "a2", "#012 - Canela", hoy, JornadaOrdeno.MANANA, 6.5, DestinoLeche.RETIRO_MEDICO),
                RegistroOrdeno("r3", "a1", "#008 - Luna", hoy, JornadaOrdeno.TARDE, 8.0)
            ),
            animales = listOf(luna, canela),
            vacasEnProduccion = listOf(luna, canela, Animal("a3", "020", "Mora", raza = "Holstein")),
            alimentacion = emptyList(),
            dia = hoy,
            onDia = {},
            onGuardar = { _, listo -> listo(true) },
            onEliminar = { _, _ -> }
        )
    }
}

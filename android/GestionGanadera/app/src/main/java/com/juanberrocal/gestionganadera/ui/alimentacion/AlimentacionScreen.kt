package com.juanberrocal.gestionganadera.ui.alimentacion

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.FrecuenciaConsumo
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.TipoInsumo
import com.juanberrocal.gestionganadera.data.UnidadConsumo
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.logica.Sanidad
import com.juanberrocal.gestionganadera.ui.GanaderiaViewModel
import com.juanberrocal.gestionganadera.ui.comun.AccionMenu
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
fun AlimentacionRuta(viewModel: GanaderiaViewModel, onAviso: (String) -> Unit) {
    val registros by viewModel.alimentacion.observeAsState(emptyList())
    val animales by viewModel.animales.observeAsState(emptyList())
    val resultado = { exito: String -> { ok: Boolean -> onAviso(if (ok) exito else "No se pudo completar. Revise la conexión.") } }

    AlimentacionScreen(
        registros = registros,
        animales = animales,
        onGuardar = { registro, alTerminar ->
            viewModel.guardarAlimentacion(registro) { ok ->
                alTerminar(ok)
                if (ok) onAviso(if (registro.id.isEmpty()) "Registro guardado" else "Registro actualizado")
            }
        },
        onFinalizar = { viewModel.finalizarAlimentacion(it, resultado("Finalizado")) },
        onReactivar = { viewModel.reactivarAlimentacion(it, resultado("Reactivado")) },
        onEliminar = { viewModel.eliminarAlimentacion(it, resultado("Registro eliminado")) }
    )
}

enum class FiltroAlimentacion(val etiqueta: String) {
    TODOS("Todos"),
    ALIMENTACION("Alimentación"),
    MEDICAMENTOS_ACTIVOS("Medicamentos activos");

    fun coincide(r: RegistroAlimentacionMedicamento, hoy: LocalDate): Boolean = when (this) {
        TODOS -> true
        ALIMENTACION -> !r.esMedicamento
        MEDICAMENTOS_ACTIVOS -> r.medicamentoVigente(hoy)
    }
}

private data class FormAlimentacion(val registro: RegistroAlimentacionMedicamento?)

@Composable
fun AlimentacionScreen(
    registros: List<RegistroAlimentacionMedicamento>,
    animales: List<Animal>,
    onGuardar: (RegistroAlimentacionMedicamento, (Boolean) -> Unit) -> Unit,
    onFinalizar: (RegistroAlimentacionMedicamento) -> Unit,
    onReactivar: (RegistroAlimentacionMedicamento) -> Unit,
    onEliminar: (RegistroAlimentacionMedicamento) -> Unit
) {
    var filtro by rememberSaveable { mutableStateOf(FiltroAlimentacion.TODOS) }
    var formulario by remember { mutableStateOf<FormAlimentacion?>(null) }
    var porEliminar by remember { mutableStateOf<RegistroAlimentacionMedicamento?>(null) }
    var porFinalizar by remember { mutableStateOf<RegistroAlimentacionMedicamento?>(null) }

    val hoy = LocalDate.now()
    // Activos primero, luego por fecha de inicio más reciente
    val visibles = remember(registros, filtro) {
        registros.filter { filtro.coincide(it, hoy) }
            .sortedWith(compareByDescending<RegistroAlimentacionMedicamento> { it.activo }.thenByDescending { it.fechaInicio })
    }
    val vacasEnRetiro = remember(registros, animales) {
        animales.count { it.estaEnOrdeno && Sanidad.manejoDe(it, registros, hoy).enRetiroLeche }
    }
    val animalesPorId = remember(animales) { animales.associateBy { it.id } }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "filtros") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(FiltroAlimentacion.entries, key = { it.name }) { f ->
                        FilterChip(
                            selected = filtro == f,
                            onClick = { filtro = f },
                            label = {
                                Text(
                                    "${f.etiqueta} (${registros.count { f.coincide(it, hoy) }})",
                                    fontWeight = if (filtro == f) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
            item(key = "resumen") {
                TarjetaResumenAlimentacion(
                    planes = registros.count { !it.esMedicamento && it.activo },
                    medicamentos = registros.count { it.medicamentoVigente(hoy) },
                    vacasEnRetiro = vacasEnRetiro,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (visibles.isEmpty()) {
                item(key = "vacio") {
                    TextoVacio(
                        if (registros.isEmpty()) "Aún no hay registros.\nToque «Registrar» para indicar qué come o qué medicamento recibe un animal o un grupo."
                        else "No hay registros con este filtro."
                    )
                }
            }
            items(visibles, key = { it.id }) { registro ->
                TarjetaRegistro(
                    registro = registro,
                    // Nombre actual del animal (si lo editaron en Ganado)
                    objetivo = registro.animalId?.let { animalesPorId[it]?.codigoNombre } ?: registro.objetivoLabel,
                    hoy = hoy,
                    onEditar = { formulario = FormAlimentacion(registro) },
                    onFinalizar = { porFinalizar = registro },
                    onReactivar = { onReactivar(registro) },
                    onEliminar = { porEliminar = registro },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { formulario = FormAlimentacion(null) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Registrar") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    formulario?.let { abierto ->
        AlimentacionFormSheet(
            inicial = abierto.registro,
            tipoPorDefecto = if (filtro == FiltroAlimentacion.MEDICAMENTOS_ACTIVOS) TipoInsumo.MEDICAMENTO else TipoInsumo.ALIMENTO,
            animales = animales,
            onGuardar = onGuardar,
            onCerrar = { formulario = null }
        )
    }

    porFinalizar?.let { registro ->
        val retiro = registro.esMedicamento && registro.tieneRetiroLeche
        DialogoConfirmar(
            titulo = if (registro.esMedicamento) "Finalizar tratamiento" else "Finalizar plan de alimentación",
            mensaje = "Se marcará «${registro.nombreProducto}» (${registro.objetivoLabel}) como finalizado hoy." +
                if (retiro) "\n\nLa leche seguirá en retiro ${Formato.dias(registro.diasRetiro)} más, " +
                    "hasta el ${Formato.fecha(hoy.plusDays(registro.diasRetiro.toLong()))}." else "",
            textoConfirmar = "Finalizar",
            onConfirmar = { onFinalizar(registro); porFinalizar = null },
            onCancelar = { porFinalizar = null }
        )
    }

    porEliminar?.let { registro ->
        DialogoConfirmar(
            titulo = "Eliminar registro",
            mensaje = "¿Desea eliminar «${registro.nombreProducto}» de ${registro.objetivoLabel}? " +
                "Si solo terminó el tratamiento, use «Finalizar» para conservar el historial.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = { onEliminar(registro); porEliminar = null },
            onCancelar = { porEliminar = null }
        )
    }
}

@Composable
private fun TarjetaResumenAlimentacion(planes: Int, medicamentos: Int, vacasEnRetiro: Int, modifier: Modifier = Modifier) {
    Card(
        shape = FormaTarjetaComun,
        border = bordeTarjetaComun(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metrica(planes.toString(), "Planes de\nalimentación", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            Metrica(medicamentos.toString(), "Medicamentos\nvigentes", ColoresSanidad.vacunaTexto, Modifier.weight(1f))
            Metrica(
                vacasEnRetiro.toString(), "Vacas en retiro\nde leche",
                if (vacasEnRetiro > 0) ColoresSanidad.retiroTexto else MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Metrica(valor: String, etiqueta: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaRegistro(
    registro: RegistroAlimentacionMedicamento,
    objetivo: String,
    hoy: LocalDate,
    onEditar: () -> Unit,
    onFinalizar: () -> Unit,
    onReactivar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (fondoIcono, colorIcono) = if (registro.esMedicamento) ColoresSanidad.vacunaFondo to ColoresSanidad.vacunaTexto
    else ColoresSanidad.alimentoFondo to ColoresSanidad.alimentoTexto
    val vigente = registro.activo || registro.retiroVigente(hoy)

    Card(
        shape = FormaTarjetaComun,
        border = bordeTarjetaComun(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEditar,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 2.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(if (vigente) fondoIcono else ColoresSanidad.neutroFondo, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(if (registro.esMedicamento) R.drawable.ic_medicamento else R.drawable.ic_alimentacion),
                    contentDescription = registro.tipoRegistro.etiquetaCorta,
                    tint = if (vigente) colorIcono else ColoresSanidad.neutroTexto,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    registro.nombreProducto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${registro.categoriaDetalle} · $objetivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${registro.dosisTexto} · desde ${Formato.fecha(registro.fechaInicio)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        registro.activo -> Etiqueta("Activo", ColoresSanidad.alimentoFondo, ColoresSanidad.alimentoTexto)
                        else -> Etiqueta(
                            "Finalizado" + (registro.fechaFin?.let { " ${Formato.fecha(it)}" } ?: ""),
                            ColoresSanidad.neutroFondo, ColoresSanidad.neutroTexto
                        )
                    }
                    if (registro.esMedicamento && registro.tieneRetiroLeche) {
                        val texto = when {
                            registro.activo -> "Retiro de leche · ${Formato.dias(registro.diasRetiro)} tras finalizar"
                            registro.retiroVigente(hoy) -> "Retiro de leche hasta ${Formato.fecha(registro.finRetiro!!)}"
                            else -> "Retiro cumplido"
                        }
                        val enRetiro = registro.retiroVigente(hoy)
                        Etiqueta(
                            texto,
                            if (enRetiro) ColoresSanidad.retiroFondo else ColoresSanidad.neutroFondo,
                            if (enRetiro) ColoresSanidad.retiroTexto else ColoresSanidad.neutroTexto
                        )
                    }
                }
            }
            MenuEditarEliminar(
                onEditar = onEditar,
                onEliminar = onEliminar,
                extras = listOf(
                    if (registro.activo) AccionMenu("Finalizar", Icons.Default.CheckCircle, alElegir = onFinalizar)
                    else AccionMenu("Reactivar", Icons.Default.Refresh, alElegir = onReactivar)
                )
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun AlimentacionScreenPreview() {
    val hoy = LocalDate.now()
    GanaderiaTema {
        AlimentacionScreen(
            registros = listOf(
                RegistroAlimentacionMedicamento(
                    "m1", animalId = "a1", objetivoLabel = "#008 - Luna", tipoRegistro = TipoInsumo.MEDICAMENTO,
                    categoriaDetalle = "Antibiótico", nombreProducto = "Oxitetraciclina", cantidad = 20.0,
                    unidad = UnidadConsumo.MILILITROS, frecuencia = FrecuenciaConsumo.CADA_12_HORAS,
                    tieneRetiroLeche = true, diasRetiro = 4, fechaInicio = hoy.minusDays(2)
                ),
                RegistroAlimentacionMedicamento(
                    "f1", grupoProposito = PropositoEtapa.PRODUCCION_ORDENO,
                    objetivoLabel = RegistroAlimentacionMedicamento.etiquetaGrupo(PropositoEtapa.PRODUCCION_ORDENO),
                    tipoRegistro = TipoInsumo.ALIMENTO, categoriaDetalle = "Concentrado", nombreProducto = "Lechero 18%",
                    cantidad = 4.0, unidad = UnidadConsumo.KILOGRAMOS, frecuencia = FrecuenciaConsumo.DIARIO
                )
            ),
            animales = listOf(Animal("a1", "008", "Luna", raza = "Girolando")),
            onGuardar = { _, listo -> listo(true) },
            onFinalizar = {},
            onReactivar = {},
            onEliminar = {}
        )
    }
}

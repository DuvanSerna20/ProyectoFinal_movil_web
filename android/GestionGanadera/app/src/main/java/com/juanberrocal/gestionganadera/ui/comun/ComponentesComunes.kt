package com.juanberrocal.gestionganadera.ui.comun

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.ganado.FormaCampo
import com.juanberrocal.gestionganadera.ui.ganado.coloresCampo
import com.juanberrocal.gestionganadera.ui.tema.GanaderiaTema
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

val FormaTarjetaComun = RoundedCornerShape(16.dp)

@Composable
fun bordeTarjetaComun() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

/** Fragment que dibuja todo su contenido con Compose (se mantiene nav_graph y la barra inferior). */
fun Fragment.vistaCompose(contenido: @Composable () -> Unit): View =
    ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { GanaderiaTema(contenido) }
    }

// ---------------------------------------------------------------------------------------------
// Hoja inferior de formulario
// ---------------------------------------------------------------------------------------------

/**
 * ModalBottomSheet crema con título, contenido desplazable y botones Cancelar / Guardar.
 * @param guardando deshabilita el botón mientras el servidor responde.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaFormulario(
    titulo: String,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    guardando: Boolean,
    error: String? = null,
    textoGuardar: String = "Guardar",
    contenido: @Composable ColumnScope.() -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estado,
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            contenido()
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = onGuardar, enabled = !guardando, modifier = Modifier.weight(1f)) {
                    Text(if (guardando) "Guardando…" else textoGuardar)
                }
            }
        }
    }
}

@Composable
fun TituloSeccionForm(texto: String) {
    Spacer(Modifier.height(4.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

// ---------------------------------------------------------------------------------------------
// Campos
// ---------------------------------------------------------------------------------------------

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    lineas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        shape = FormaCampo,
        colors = coloresCampo(),
        modifier = modifier.fillMaxWidth()
    )
}

/** Número decimal (acepta coma o punto) o entero, con sufijo de unidad opcional. */
@Composable
fun CampoNumero(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    sufijo: String? = null,
    error: String? = null,
    ayuda: String? = null,
    entero: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo ->
            if (nuevo.all { it.isDigit() || (!entero && (it == ',' || it == '.')) }) onCambio(nuevo)
        },
        label = { Text(etiqueta) },
        suffix = sufijo?.let { { Text(it) } },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (entero) KeyboardType.Number else KeyboardType.Decimal),
        shape = FormaCampo,
        colors = coloresCampo(),
        modifier = modifier.fillMaxWidth()
    )
}

/** Campo de solo lectura que abre un DatePickerDialog de Material 3. */
@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate,
    onFecha: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = Formato.fecha(fecha),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            singleLine = true,
            shape = FormaCampo,
            colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )
        // Capa encima del campo: un TextField de solo lectura no recibe clics por sí solo
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clickable { abierto = true }
        )
    }
    if (abierto) {
        DialogoFecha(inicial = fecha, onElegir = { onFecha(it); abierto = false }, onCerrar = { abierto = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(inicial: LocalDate, onElegir: (LocalDate) -> Unit, onCerrar: () -> Unit) {
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let {
                    onElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        DatePicker(state = estado, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
    }
}

// ---------------------------------------------------------------------------------------------
// Avisos, menús y diálogos
// ---------------------------------------------------------------------------------------------

/** Caja de aviso con icono (ámbar = advertencia, rojo = alerta sanitaria). */
@Composable
fun CajaAviso(
    texto: String,
    icono: ImageVector,
    fondo: Color,
    color: Color,
    modifier: Modifier = Modifier,
    titulo: String? = null
) {
    Surface(color = fondo, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                titulo?.let { Text(it, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Text(texto, color = color, fontSize = 13.sp)
            }
        }
    }
}

data class AccionMenu(val texto: String, val icono: ImageVector, val peligrosa: Boolean = false, val alElegir: () -> Unit)

/** Menú ⋮ con Editar / Eliminar y acciones extra opcionales (en medio). */
@Composable
fun MenuEditarEliminar(onEditar: () -> Unit, onEliminar: () -> Unit, extras: List<AccionMenu> = emptyList()) {
    var abierto by remember { mutableStateOf(false) }
    val acciones = listOf(AccionMenu("Editar", Icons.Default.Edit, alElegir = onEditar)) + extras +
        AccionMenu("Eliminar", Icons.Default.Delete, peligrosa = true, alElegir = onEliminar)
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Default.MoreVert, "Acciones", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            acciones.forEach { accion ->
                val color = if (accion.peligrosa) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                DropdownMenuItem(
                    text = { Text(accion.texto, color = if (accion.peligrosa) color else Color.Unspecified) },
                    leadingIcon = { Icon(accion.icono, null, tint = color) },
                    onClick = { abierto = false; accion.alElegir() }
                )
            }
        }
    }
}

@Composable
fun DialogoConfirmar(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    peligroso: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    textoConfirmar,
                    color = if (peligroso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Texto centrado para listas vacías. */
@Composable
fun TextoVacio(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    )
}

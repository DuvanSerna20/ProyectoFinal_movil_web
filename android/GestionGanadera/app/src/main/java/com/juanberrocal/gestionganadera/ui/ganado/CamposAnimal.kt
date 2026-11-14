package com.juanberrocal.gestionganadera.ui.ganado

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.EstadoReproductivo

/** Campo decimal (acepta coma o punto) con sufijo de unidad. Lo usan el formulario y el diálogo rápido. */
@Composable
fun CampoDecimal(
    valor: String,
    onCambio: (String) -> Unit,
    @StringRes etiqueta: Int,
    @StringRes sufijo: Int,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    error: String? = null,
    destacado: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == ',' || it == '.' }) onCambio(nuevo) },
        label = { Text(stringResource(etiqueta) + if (destacado) " *" else "") },
        suffix = { Text(stringResource(sufijo)) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = FormaCampo,
        colors = coloresCampo(),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Cargada / Horra / No aplica. Las etiquetas del botón son cortas para que quepan las tres;
 * debajo se muestra la etiqueta completa de la opción elegida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorEstadoReproductivo(seleccion: EstadoReproductivo, onSeleccion: (EstadoReproductivo) -> Unit) {
    Column {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            EstadoReproductivo.entries.forEachIndexed { i, estado ->
                SegmentedButton(
                    selected = seleccion == estado,
                    onClick = { onSeleccion(estado) },
                    shape = SegmentedButtonDefaults.itemShape(i, EstadoReproductivo.entries.size),
                    colors = SegmentedButtonDefaults.colors(inactiveContainerColor = MaterialTheme.colorScheme.surface),
                    icon = {},
                    label = { Text(estado.etiquetaCorta, maxLines = 1) }
                )
            }
        }
        Text(
            seleccion.etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
        )
    }
}

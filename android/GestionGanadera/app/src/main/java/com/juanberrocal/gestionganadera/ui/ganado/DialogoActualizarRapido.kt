package com.juanberrocal.gestionganadera.ui.ganado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.logica.Formato

/**
 * "Actualizar peso / estado": cambio rápido sin abrir todo el formulario.
 * Muestra peso siempre, estado reproductivo en hembras bovinas y producción si está en ordeño.
 */
@Composable
fun DialogoActualizarRapido(
    animal: Animal,
    onGuardar: (Animal, (Boolean) -> Unit) -> Unit,
    onCerrar: () -> Unit
) {
    var peso by remember { mutableStateOf(animal.pesoKg?.let(Formato::plano).orEmpty()) }
    var produccion by remember { mutableStateOf(animal.produccionLitrosDia?.let(Formato::plano).orEmpty()) }
    var estado by remember { mutableStateOf(animal.estadoReproductivo) }
    var errorPeso by remember { mutableStateOf(false) }
    var errorGuardar by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("#${animal.codigo} - ${animal.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoDecimal(
                    valor = peso,
                    onCambio = { peso = it; errorPeso = false },
                    etiqueta = R.string.animal_peso,
                    sufijo = R.string.unidad_kg,
                    destacado = animal.proposito.requierePeso,
                    error = if (errorPeso) stringResource(R.string.error_peso) else null
                )
                if (animal.muestraEstadoReproductivo || animal.esHembraBovina) {
                    SelectorEstadoReproductivo(estado) { estado = it }
                }
                if (animal.estaEnOrdeno) {
                    CampoDecimal(
                        valor = produccion,
                        onCambio = { produccion = it },
                        etiqueta = R.string.animal_produccion,
                        sufijo = R.string.unidad_litros_dia
                    )
                }
                if (errorGuardar) {
                    Text(
                        stringResource(R.string.error_guardar_servidor),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !guardando,
                onClick = {
                    val pesoNum = Formato.leerDecimal(peso)?.takeIf { it > 0 }
                    errorPeso = (animal.proposito.requierePeso && pesoNum == null) || (peso.isNotBlank() && pesoNum == null)
                    if (errorPeso) return@TextButton
                    guardando = true
                    errorGuardar = false
                    onGuardar(
                        animal.copy(
                            pesoKg = pesoNum,
                            estadoReproductivo = if (animal.esHembraBovina) estado else animal.estadoReproductivo,
                            produccionLitrosDia = if (animal.estaEnOrdeno) Formato.leerDecimal(produccion)?.takeIf { it > 0 }
                            else animal.produccionLitrosDia
                        )
                    ) { ok ->
                        guardando = false
                        if (ok) onCerrar() else errorGuardar = true
                    }
                }
            ) { Text(stringResource(R.string.guardar)) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) } }
    )
}

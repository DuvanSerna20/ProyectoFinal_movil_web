package com.juanberrocal.gestionganadera.ui.potreros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.DepartamentoColombia
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.PisoTermico
import com.juanberrocal.gestionganadera.ui.ganado.CampoDesplegable
import com.juanberrocal.gestionganadera.ui.ganado.FormaCampo
import com.juanberrocal.gestionganadera.ui.ganado.coloresCampo
import kotlinx.coroutines.launch

/**
 * Registro de una finca nueva.
 * @param onGuardar recibe la finca (sin id) y un callback con la finca guardada o null si falló.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FincaFormSheet(
    fincasExistentes: List<Finca>,
    onGuardar: (Finca, (Finca?) -> Unit) -> Unit,
    onCerrar: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var nombre by rememberSaveable { mutableStateOf("") }
    var departamento by rememberSaveable { mutableStateOf<DepartamentoColombia?>(null) }
    var municipio by rememberSaveable { mutableStateOf("") }
    var piso by rememberSaveable { mutableStateOf(PisoTermico.CALIDO) }
    var errorNombre by rememberSaveable { mutableStateOf<Int?>(null) }
    var errorDepartamento by rememberSaveable { mutableStateOf(false) }
    var errorGuardar by rememberSaveable { mutableStateOf(false) }
    var guardando by rememberSaveable { mutableStateOf(false) }

    fun cerrar() {
        scope.launch { estadoHoja.hide() }.invokeOnCompletion { if (!estadoHoja.isVisible) onCerrar() }
    }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estadoHoja,
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.finca_nueva),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; errorNombre = null },
                label = { Text(stringResource(R.string.finca_nombre)) },
                placeholder = { Text("Hacienda El Roble") },
                isError = errorNombre != null,
                supportingText = errorNombre?.let { { Text(stringResource(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = FormaCampo,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )

            CampoDesplegable(
                etiqueta = stringResource(R.string.finca_departamento),
                valorMostrado = departamento?.etiqueta.orEmpty(),
                opciones = DepartamentoColombia.entries,
                textoDe = { it.etiqueta },
                estaSeleccionado = { it == departamento },
                onSeleccion = { departamento = it; errorDepartamento = false },
                placeholder = stringResource(R.string.animal_seleccione),
                error = if (errorDepartamento) stringResource(R.string.error_obligatorio) else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = municipio,
                onValueChange = { municipio = it },
                label = { Text(stringResource(R.string.finca_municipio)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = FormaCampo,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                stringResource(R.string.finca_piso_termico),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            FilaChips(PisoTermico.entries, piso, { it.etiqueta }) { piso = it }

            if (errorGuardar) {
                Text(
                    stringResource(R.string.finca_error_guardar),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { cerrar() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(stringResource(R.string.cancelar)) }
                Button(
                    enabled = !guardando,
                    onClick = {
                        val limpio = ValidacionPotrero.nombre(nombre)
                        nombre = limpio
                        errorNombre = when {
                            limpio.isEmpty() -> R.string.error_obligatorio
                            ValidacionPotrero.nombreFincaRepetido(limpio, fincasExistentes) -> R.string.finca_nombre_repetido
                            else -> null
                        }
                        errorDepartamento = departamento == null
                        val depto = departamento
                        if (errorNombre != null || depto == null) return@Button

                        guardando = true
                        errorGuardar = false
                        onGuardar(
                            Finca(
                                id = "",
                                nombre = limpio,
                                departamento = depto,
                                municipio = ValidacionPotrero.nombre(municipio),
                                pisoTermico = piso
                            )
                        ) { guardada ->
                            guardando = false
                            if (guardada != null) cerrar() else errorGuardar = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(stringResource(R.string.guardar)) }
            }
        }
    }
}

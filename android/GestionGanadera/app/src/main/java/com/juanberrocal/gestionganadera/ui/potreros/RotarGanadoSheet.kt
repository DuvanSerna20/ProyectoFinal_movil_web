package com.juanberrocal.gestionganadera.ui.potreros

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.MotivoRotacion
import com.juanberrocal.gestionganadera.data.Potrero
import kotlinx.coroutines.launch

/**
 * Mover el ganado de [origen] (en ocupación) a otro potrero.
 * [destinos] ya viene filtrado: solo LISTOS de la misma finca (ver [destinosRotacion]).
 * @param onConfirmar callback con null si salió bien o el mensaje de error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotarGanadoSheet(
    origen: Potrero,
    destinos: List<Potrero>,
    finca: Finca?,
    onConfirmar: (destino: Potrero, motivo: MotivoRotacion, alTerminar: (String?) -> Unit) -> Unit,
    onCerrar: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var destinoId by rememberSaveable { mutableStateOf(destinos.singleOrNull()?.mongoId) }
    var motivo by rememberSaveable { mutableStateOf(MotivoRotacion.CUMPLIO_DIAS) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var enviando by rememberSaveable { mutableStateOf(false) }
    val nombreFinca = finca?.nombre ?: stringResource(R.string.potrero_sin_finca)

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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.rotar_titulo, origen.nombre),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.rotar_subtitulo, origen.cantidadAnimalesActuales, nombreFinca),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                stringResource(R.string.rotar_destino),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (destinos.isEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        stringResource(R.string.rotar_sin_destinos, nombreFinca),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                destinos.forEach { destino ->
                    val seleccionado = destino.mongoId == destinoId
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (seleccionado) 2.dp else 1.dp,
                            if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = seleccionado, role = Role.RadioButton) { destinoId = destino.mongoId }
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = seleccionado, onClick = null)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(destino.nombre, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${destino.areaLegible()} · Pasto: ${destino.tipoPasto.etiqueta} · ${destino.condicionPasto.etiqueta}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.rotar_motivo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            FilaChips(MotivoRotacion.entries, motivo, { it.etiqueta }) { motivo = it }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { cerrar() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(stringResource(R.string.cancelar)) }
                Button(
                    enabled = !enviando && destinos.any { it.mongoId == destinoId },
                    onClick = {
                        val destino = destinos.firstOrNull { it.mongoId == destinoId } ?: return@Button
                        enviando = true
                        error = null
                        onConfirmar(destino, motivo) { mensaje ->
                            enviando = false
                            if (mensaje == null) cerrar() else error = mensaje
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(stringResource(R.string.rotar_confirmar)) }
            }
        }
    }
}

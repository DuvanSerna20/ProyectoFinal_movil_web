package com.juanberrocal.gestionganadera.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.ui.UiState

/**
 * Franja superior según el [UiState] de la pantalla:
 * - Loading: barra de progreso.
 * - Error: mensaje + "Reintentar"; la lista de abajo sigue mostrando los datos cacheados en Room.
 * - Success / Empty: nada (cada pantalla ya dibuja su propio estado vacío).
 */
@Composable
fun AvisoEstado(estado: UiState<*>, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    when (estado) {
        UiState.Loading -> LinearProgressIndicator(modifier.fillMaxWidth())
        is UiState.Error -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    estado.mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onReintentar) { Text(stringResource(R.string.reintentar)) }
            }
        }
        is UiState.Success, UiState.Empty -> Unit
    }
}

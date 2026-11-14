package com.juanberrocal.gestionganadera.ui.ganado

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.EstadoReproductivo
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.logica.ManejoAnimal
import com.juanberrocal.gestionganadera.ui.tema.ColoresEstado
import com.juanberrocal.gestionganadera.ui.tema.ColoresSanidad

private val FormaTarjeta = RoundedCornerShape(16.dp)
val FormaCampo = RoundedCornerShape(12.dp)

@Composable
private fun bordeTarjeta() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

/** Campos de texto con fondo blanco limpio (sin tinte de M3). */
@Composable
fun coloresCampo(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)

// ---------------------------------------------------------------------------------------------
// Tarjeta de resumen (dashboard de ganado)
// ---------------------------------------------------------------------------------------------

@Composable
fun TarjetaResumen(resumen: ResumenGanado, modifier: Modifier = Modifier) {
    val pesoPromedio = resumen.pesoPromedioKg?.let { "${Formato.numero(it, 0)} kg" }
        ?: stringResource(R.string.ganado_sin_dato)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        border = bordeTarjeta(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ganado),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    stringResource(R.string.ganado_total_titulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    resumen.totalBovinos.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Línea 1: hembras
                Text(
                    stringResource(
                        R.string.ganado_resumen_hembras,
                        resumen.enOrdeno, resumen.cargadas, resumen.horras, resumen.criaEngordeHembras
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Línea 2: machos y peso
                Text(
                    stringResource(
                        R.string.ganado_resumen_machos,
                        resumen.torosReproductores, resumen.machosEnCeba, pesoPromedio
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (resumen.otrosAnimales > 0) {
                    Spacer(Modifier.height(6.dp))
                    Etiqueta(
                        texto = stringResource(R.string.ganado_otros_animales, resumen.otrosAnimales),
                        fondo = ColoresEstado.otroFondo,
                        color = ColoresEstado.otroTexto
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tarjeta de animal
// ---------------------------------------------------------------------------------------------

/**
 * @param ubicacion "Finca El Roble / Potrero Árbol" (o solo uno de los dos), null si no tiene.
 * @param manejo plan de alimentación activo y medicamentos vigentes (módulo Alimentación y Medicamentos).
 */
@Composable
fun AnimalCard(
    animal: Animal,
    ubicacion: String?,
    onEditar: () -> Unit,
    onActualizarRapido: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
    manejo: ManejoAnimal? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        border = bordeTarjeta(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEditar
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 2.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "#${animal.codigo} - ${animal.nombre}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Subtítulo 1: Raza · Finca / Potrero
                Text(
                    listOfNotNull(animal.raza.ifBlank { null }, ubicacion).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Subtítulo 2: métricas
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PildoraMetrica(
                        icono = R.drawable.ic_peso,
                        texto = animal.pesoKg?.let { stringResource(R.string.animal_metrica_peso, Formato.numero(it, 1)) }
                            ?: stringResource(R.string.animal_metrica_sin_peso),
                        apagada = animal.pesoKg == null
                    )
                    if (animal.estaEnOrdeno) {
                        PildoraMetrica(
                            icono = R.drawable.ic_ordeno,
                            texto = stringResource(
                                R.string.animal_metrica_produccion,
                                animal.produccionLitrosDia?.let { Formato.numero(it, 1) } ?: stringResource(R.string.ganado_sin_dato)
                            ),
                            apagada = animal.produccionLitrosDia == null
                        )
                    }
                }
                manejo?.takeUnless { it.vacio }?.let { SeccionManejo(it, muestraRetiro = animal.esHembraBovina) }
            }

            // Badges en la esquina derecha
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            ) {
                BadgeProposito(animal)
                if (animal.muestraEstadoReproductivo) BadgeReproductivo(animal.estadoReproductivo)
            }

            MenuAcciones(onEditar = onEditar, onActualizarRapido = onActualizarRapido, onEliminar = onEliminar)
        }
    }
}

/** Medicamentos vigentes (con alerta de retiro de leche) y plan de alimentación activo. */
@Composable
private fun SeccionManejo(manejo: ManejoAnimal, muestraRetiro: Boolean) {
    if (manejo.medicamentos.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (muestraRetiro && manejo.enRetiroLeche) {
                Etiqueta(
                    manejo.finRetiro?.let { "Retiro de leche hasta ${Formato.fecha(it)}" } ?: "Retiro de leche",
                    ColoresSanidad.retiroFondo, ColoresSanidad.retiroTexto
                )
            } else {
                Etiqueta("En tratamiento", ColoresSanidad.vacunaFondo, ColoresSanidad.vacunaTexto)
            }
        }
        LineaManejo(
            icono = R.drawable.ic_medicamento,
            texto = manejo.medicamentos.joinToString { "${it.nombreProducto} (${it.dosisTexto})" },
            color = ColoresSanidad.vacunaTexto
        )
    }
    if (manejo.alimentos.isNotEmpty()) {
        if (manejo.medicamentos.isEmpty()) Spacer(Modifier.height(6.dp))
        LineaManejo(
            icono = R.drawable.ic_alimentacion,
            texto = manejo.alimentos.joinToString { "${it.categoriaDetalle}: ${it.nombreProducto} (${it.dosisTexto})" },
            color = ColoresSanidad.alimentoTexto
        )
    }
}

@Composable
private fun LineaManejo(@DrawableRes icono: Int, texto: String, color: Color) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(painterResource(icono), null, tint = color, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BadgeProposito(animal: Animal) {
    // Verde = ordeño, azul = reproductor, ámbar = ceba/engorde, crema/gris = crianza y demás
    val (fondo, color) = when (animal.proposito) {
        PropositoEtapa.PRODUCCION_ORDENO -> ColoresEstado.ordenoFondo to ColoresEstado.ordenoTexto
        PropositoEtapa.TORO_REPRODUCTOR -> ColoresEstado.toroFondo to ColoresEstado.toroTexto
        PropositoEtapa.ENGORDE, PropositoEtapa.TORO_CEBA -> ColoresEstado.secaFondo to ColoresEstado.secaTexto
        PropositoEtapa.CRIA_LEVANTE, PropositoEtapa.TERNERO_LEVANTE -> ColoresEstado.criaFondo to ColoresEstado.criaTexto
        PropositoEtapa.VACA_SECA, PropositoEtapa.TRABAJO_COMPANIA -> ColoresEstado.otroFondo to ColoresEstado.otroTexto
    }
    // Para otras especies el propósito siempre es el mismo: es más útil mostrar la especie
    Etiqueta(if (animal.esBovino) animal.proposito.etiquetaCorta else animal.especie.etiqueta, fondo, color)
}

@Composable
private fun BadgeReproductivo(estado: EstadoReproductivo) {
    val (fondo, color) = when (estado) {
        EstadoReproductivo.CARGADA -> ColoresEstado.cargadaFondo to ColoresEstado.cargadaTexto
        else -> ColoresEstado.otroFondo to ColoresEstado.otroTexto
    }
    Etiqueta(estado.etiquetaCorta, fondo, color)
}

@Composable
private fun PildoraMetrica(@DrawableRes icono: Int, texto: String, apagada: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(icono), null,
                tint = if (apagada) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                texto,
                style = MaterialTheme.typography.labelMedium,
                color = if (apagada) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Surface(color = fondo, shape = RoundedCornerShape(50)) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun MenuAcciones(onEditar: () -> Unit, onActualizarRapido: () -> Unit, onEliminar: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.ganado_acciones),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.editar)) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { abierto = false; onEditar() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ganado_actualizar_rapido)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_peso), null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp))
                },
                onClick = { abierto = false; onActualizarRapido() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { abierto = false; onEliminar() }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Selector desplegable genérico (ExposedDropdownMenuBox de solo lectura)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CampoDesplegable(
    etiqueta: String,
    valorMostrado: String,
    opciones: List<T>,
    textoDe: (T) -> String,
    estaSeleccionado: (T) -> Boolean,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    ayuda: String? = null,
    habilitado: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido && habilitado,
        onExpandedChange = { if (habilitado) expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = valorMostrado,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            singleLine = true,
            label = { Text(etiqueta) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = error != null,
            supportingText = (error ?: ayuda)?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            shape = FormaCampo,
            colors = coloresCampo(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, habilitado)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandido && habilitado,
            onDismissRequest = { expandido = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            opciones.forEach { opcion ->
                val seleccionado = estaSeleccionado(opcion)
                DropdownMenuItem(
                    text = {
                        Text(
                            textoDe(opcion),
                            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    trailingIcon = if (seleccionado) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        onSeleccion(opcion)
                        expandido = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

package com.juanberrocal.gestionganadera.ui.potreros

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.CondicionPasto
import com.juanberrocal.gestionganadera.data.EstadoPotrero
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.ganado.CampoDesplegable
import com.juanberrocal.gestionganadera.ui.ganado.Etiqueta
import com.juanberrocal.gestionganadera.ui.tema.ColoresEstado

private val FormaTarjeta = RoundedCornerShape(16.dp)

@Composable
private fun bordeTarjeta() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun coloresTarjeta() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

/** Colores (fondo, texto) del badge de estado: verde = listo para entrar, azul = en uso. */
fun coloresEstado(estado: EstadoPotrero): Pair<Color, Color> = when (estado) {
    EstadoPotrero.LISTO -> ColoresEstado.ordenoFondo to ColoresEstado.ordenoTexto
    EstadoPotrero.OCUPADO -> ColoresEstado.toroFondo to ColoresEstado.toroTexto
    EstadoPotrero.EN_DESCANSO -> ColoresEstado.secaFondo to ColoresEstado.secaTexto
    EstadoPotrero.MANTENIMIENTO -> ColoresEstado.criaFondo to ColoresEstado.criaTexto
}

// ---------------------------------------------------------------------------------------------
// Barra selectora de finca (fija debajo del TopAppBar)
// ---------------------------------------------------------------------------------------------

@Composable
fun BarraSelectorFinca(
    fincas: List<Finca>,
    fincaSeleccionadaId: String?,
    onSeleccion: (String?) -> Unit,
    onNuevaFinca: () -> Unit
) {
    val todas = stringResource(R.string.finca_todas)
    val seleccionada = fincas.firstOrNull { it.id == fincaSeleccionadaId }
    val opciones: List<Finca?> = listOf<Finca?>(null) + fincas

    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CampoDesplegable(
                etiqueta = stringResource(R.string.potrero_finca),
                valorMostrado = seleccionada?.descripcion ?: todas,
                opciones = opciones,
                textoDe = { it?.descripcion ?: todas },
                estaSeleccionado = { it?.id == fincaSeleccionadaId },
                onSeleccion = { onSeleccion(it?.id) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onNuevaFinca,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.finca_nueva), maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Resumen
// ---------------------------------------------------------------------------------------------

@Composable
fun TarjetaResumenPotreros(resumen: ResumenPotreros, finca: Finca?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        border = bordeTarjeta(),
        colors = coloresTarjeta(),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_potreros), null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    if (finca != null) stringResource(R.string.potreros_resumen_finca, resumen.total, finca.nombre)
                    else stringResource(R.string.potreros_resumen_empresa, resumen.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (finca == null) {
                    Text(
                        stringResource(R.string.potreros_resumen_fincas, resumen.fincasActivas, resumen.departamentos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    stringResource(R.string.potreros_resumen_estados, resumen.ocupados, resumen.listos, resumen.enDescanso),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.potreros_resumen_area, Formato.numero(resumen.areaHectareas)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tarjeta de potrero
// ---------------------------------------------------------------------------------------------

@Composable
fun PotreroCard(
    potrero: Potrero,
    finca: Finca?,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onRotar: () -> Unit,
    onMarcarListo: () -> Unit,
    onCalcularRotacion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (fondoEstado, textoEstado) = coloresEstado(potrero.estado)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjeta,
        border = bordeTarjeta(),
        colors = coloresTarjeta(),
        elevation = CardDefaults.cardElevation(0.dp),
        onClick = onEditar
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp)) {
            // Encabezado: nombre + badge de estado + menú
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    potrero.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Etiqueta(potrero.estado.etiqueta, fondoEstado, textoEstado)
                Spacer(Modifier.weight(1f))
                MenuPotrero(
                    puedeMarcarListo = potrero.estado == EstadoPotrero.EN_DESCANSO ||
                        potrero.estado == EstadoPotrero.MANTENIMIENTO,
                    onEditar = onEditar,
                    onMarcarListo = onMarcarListo,
                    onCalcularRotacion = onCalcularRotacion,
                    onEliminar = onEliminar
                )
            }

            Column(Modifier.padding(end = 12.dp)) {
                // Finca · Departamento
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        finca?.etiquetaCorta ?: stringResource(R.string.potrero_sin_finca),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (finca != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${potrero.areaLegible()} · Pasto: ${potrero.tipoPasto.etiqueta} · " +
                        stringResource(R.string.potrero_animales_cuenta, potrero.cantidadAnimalesActuales),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                potrero.progresoCiclo()?.let { progreso ->
                    Spacer(Modifier.height(10.dp))
                    BarraProgresoCiclo(progreso)
                }

                when (potrero.condicionPasto) {
                    CondicionPasto.BAJO -> Alerta(
                        icono = Icons.Default.Warning,
                        texto = stringResource(R.string.potrero_alerta_pasto_bajo),
                        fondo = ColoresEstado.secaFondo,
                        color = ColoresEstado.secaTexto
                    )
                    CondicionPasto.ENCHARCADO -> Alerta(
                        icono = Icons.Default.Info,
                        texto = stringResource(R.string.potrero_alerta_encharcado),
                        fondo = ColoresEstado.toroFondo,
                        color = ColoresEstado.toroTexto
                    )
                    CondicionPasto.OPTIMO -> Unit
                }

                if (potrero.estado == EstadoPotrero.OCUPADO) {
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onRotar,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.potrero_rotar))
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraProgresoCiclo(progreso: ProgresoCiclo) {
    val transcurridos = Formato.numero(progreso.diasTranscurridos.toDouble(), 0)
    val planeados = Formato.numero(progreso.diasPlaneados, 1)
    val texto = when {
        progreso.esOcupacion && progreso.cumplido -> stringResource(R.string.potrero_progreso_ocupacion_cumplida)
        progreso.esOcupacion -> stringResource(R.string.potrero_progreso_ocupacion, transcurridos, planeados)
        progreso.cumplido -> stringResource(R.string.potrero_progreso_descanso_cumplido)
        else -> stringResource(R.string.potrero_progreso_descanso, transcurridos, planeados)
    }
    // Ocupación cumplida = ámbar (hay que rotar). Descanso cumplido = verde (ya puede entrar ganado).
    val color = when {
        progreso.esOcupacion && progreso.cumplido -> ColoresEstado.secaTexto
        progreso.esOcupacion -> ColoresEstado.toroTexto
        else -> MaterialTheme.colorScheme.primary
    }
    LinearProgressIndicator(
        progress = { progreso.fraccion },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    )
    Spacer(Modifier.height(4.dp))
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = if (progreso.cumplido) color else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (progreso.cumplido) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Composable
private fun Alerta(icono: ImageVector, texto: String, fondo: Color, color: Color) {
    Spacer(Modifier.height(8.dp))
    Surface(color = fondo, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MenuPotrero(
    puedeMarcarListo: Boolean,
    onEditar: () -> Unit,
    onMarcarListo: () -> Unit,
    onCalcularRotacion: () -> Unit,
    onEliminar: () -> Unit
) {
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
            if (puedeMarcarListo) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.potrero_marcar_listo)) },
                    leadingIcon = { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { abierto = false; onMarcarListo() }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.potrero_calcular_rotacion)) },
                leadingIcon = { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { abierto = false; onCalcularRotacion() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { abierto = false; onEliminar() }
            )
        }
    }
}

/** Fila de "chip" seleccionable reutilizada en formularios (estado, condición, motivo, piso). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FilaChips(
    opciones: List<T>,
    seleccion: T?,
    texto: (T) -> String,
    onSeleccion: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccion,
                onClick = { onSeleccion(opcion) },
                label = { Text(texto(opcion)) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

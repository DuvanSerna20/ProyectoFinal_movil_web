package com.juanberrocal.gestionganadera.ui.ordeno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.DestinoLeche
import com.juanberrocal.gestionganadera.data.JornadaOrdeno
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.logica.Sanidad
import com.juanberrocal.gestionganadera.ui.comun.CajaAviso
import com.juanberrocal.gestionganadera.ui.comun.CampoFecha
import com.juanberrocal.gestionganadera.ui.comun.CampoNumero
import com.juanberrocal.gestionganadera.ui.comun.DialogoConfirmar
import com.juanberrocal.gestionganadera.ui.comun.HojaFormulario
import com.juanberrocal.gestionganadera.ui.ganado.CampoDesplegable
import com.juanberrocal.gestionganadera.ui.tema.ColoresSanidad
import java.time.LocalDate
import java.time.LocalTime

/**
 * Registrar ([inicial] = null) o editar un ordeño.
 *
 * @param vacasEnProduccion BOVINO + HEMBRA + PRODUCCION_ORDENO (ver Animal.estaEnOrdeno).
 * @param onGuardar recibe el registro y un callback con `true` si el servidor lo guardó.
 * @param onEliminarOriginal al editar un registro y fusionarlo con otro duplicado, se borra el original.
 */
@Composable
fun OrdenoFormSheet(
    inicial: RegistroOrdeno?,
    diaInicial: LocalDate,
    vacasEnProduccion: List<Animal>,
    animales: List<Animal>,
    registros: List<RegistroOrdeno>,
    alimentacion: List<RegistroAlimentacionMedicamento>,
    onGuardar: (RegistroOrdeno, (Boolean) -> Unit) -> Unit,
    onEliminarOriginal: (RegistroOrdeno) -> Unit,
    onCerrar: () -> Unit
) {
    // Opciones: vacas en producción (+ la del registro aunque ya no esté en ordeño) y el tanque al final
    val vacas = remember(vacasEnProduccion, inicial) {
        val extra = inicial?.animalId
            ?.takeIf { id -> vacasEnProduccion.none { it.id == id } }
            ?.let { id -> animales.firstOrNull { it.id == id } }
        listOfNotNull(extra) + vacasEnProduccion
    }
    val opciones: List<Animal?> = vacas + listOf(null)

    var fecha by remember { mutableStateOf(inicial?.fecha ?: diaInicial) }
    var jornada by remember {
        mutableStateOf(inicial?.jornada ?: if (LocalTime.now().hour >= 12) JornadaOrdeno.TARDE else JornadaOrdeno.MANANA)
    }
    val retiroEn = { animal: Animal?, dia: LocalDate ->
        animal?.let { Sanidad.manejoDe(it, alimentacion, dia) }?.takeIf { it.enRetiroLeche }
    }

    // Vaca por defecto al registrar: la primera que aún no tiene ordeño en esa fecha y jornada
    var vaca by remember {
        mutableStateOf(
            if (inicial != null) vacas.firstOrNull { it.id == inicial.animalId }
            else vacas.firstOrNull { v -> OrdenoLogica.duplicado(registros, v.id, fecha, jornada, "") == null }
                ?: vacas.firstOrNull()
        )
    }
    var destino by remember {
        mutableStateOf(
            inicial?.destinoLeche
                ?: if (retiroEn(vaca, fecha) != null) DestinoLeche.RETIRO_MEDICO else DestinoLeche.TANQUE_VENTA
        )
    }
    var litros by remember { mutableStateOf(inicial?.litros?.let { Formato.plano(it) }.orEmpty()) }
    var errorLitros by remember { mutableStateOf<String?>(null) }
    var errorGuardar by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var duplicado by remember { mutableStateOf<Pair<RegistroOrdeno, RegistroOrdeno>?>(null) }

    val retiro = retiroEn(vaca, fecha)

    /** Al elegir una vaca en retiro se preselecciona "Retiro médico"; al salir de ella se vuelve al tanque. */
    fun elegirVaca(nueva: Animal?) {
        vaca = nueva
        destino = when {
            retiroEn(nueva, fecha) != null -> DestinoLeche.RETIRO_MEDICO
            destino == DestinoLeche.RETIRO_MEDICO -> DestinoLeche.TANQUE_VENTA
            else -> destino
        }
    }

    fun enviar(registro: RegistroOrdeno, original: RegistroOrdeno?) {
        guardando = true
        errorGuardar = null
        onGuardar(registro) { ok ->
            guardando = false
            if (ok) {
                // Si se editó un registro y se fusionó con otro existente, el original sobra
                if (original != null && original.id.isNotEmpty() && original.id != registro.id) onEliminarOriginal(original)
                onCerrar()
            } else {
                errorGuardar = "No se pudo guardar. Revise la conexión con el servidor."
            }
        }
    }

    fun guardar() {
        val valor = Formato.leerDecimal(litros)
        errorLitros = if (valor == null || valor <= 0) "Ingrese los litros (mayor a 0)" else null
        if (valor == null || valor <= 0) return
        val registro = RegistroOrdeno(
            id = inicial?.id.orEmpty(),
            animalId = vaca?.id,
            animalCodigoNombre = vaca?.codigoNombre ?: RegistroOrdeno.ETIQUETA_TANQUE,
            fecha = fecha,
            jornada = jornada,
            litros = valor,
            destinoLeche = destino
        )
        val existente = OrdenoLogica.duplicado(registros, registro.animalId, fecha, jornada, excluirId = registro.id)
        if (existente != null) duplicado = existente to registro else enviar(registro, null)
    }

    HojaFormulario(
        titulo = if (inicial == null) "Registrar ordeño" else "Editar ordeño",
        onCerrar = onCerrar,
        onGuardar = ::guardar,
        guardando = guardando,
        error = errorGuardar
    ) {
        CampoFecha("Fecha", fecha, onFecha = { fecha = it; elegirVaca(vaca) })

        Text("Jornada", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectorJornada(jornada, onSeleccion = { jornada = it })

        if (vacasEnProduccion.isEmpty()) {
            CajaAviso(
                titulo = "No hay vacas en producción",
                texto = "En Ganado, marque las vacas con propósito «Producción / En ordeño» para registrar " +
                    "el ordeño de cada una. Mientras tanto puede registrar el total del tanque.",
                icono = Icons.Default.Info,
                fondo = ColoresSanidad.ambarFondo,
                color = ColoresSanidad.ambarTexto
            )
        }

        CampoDesplegable(
            etiqueta = "Vaca",
            valorMostrado = vaca?.let { etiquetaVaca(it, retiroEn(it, fecha) != null) } ?: RegistroOrdeno.ETIQUETA_TANQUE,
            opciones = opciones,
            textoDe = { it?.let { v -> etiquetaVaca(v, retiroEn(v, fecha) != null) } ?: RegistroOrdeno.ETIQUETA_TANQUE },
            estaSeleccionado = { it?.id == vaca?.id },
            onSeleccion = ::elegirVaca,
            ayuda = if (vaca != null) "Al guardar se actualiza la producción diaria de la vaca en Ganado"
            else "Total del hato: no se asigna a ninguna vaca"
        )

        retiro?.let { manejo ->
            val productos = manejo.retiros.joinToString { it.nombreProducto }
            val hasta = manejo.finRetiro?.let { "hasta el ${Formato.fecha(it)}" } ?: "mientras dure el tratamiento"
            CajaAviso(
                titulo = "Leche en retiro médico",
                texto = "${vaca?.nombre} está en tratamiento con $productos. Su leche no debe ir al tanque $hasta.",
                icono = Icons.Default.Warning,
                fondo = ColoresSanidad.retiroFondo,
                color = ColoresSanidad.retiroTexto
            )
        }

        CampoNumero(
            valor = litros,
            onCambio = { litros = it; errorLitros = null },
            etiqueta = "Litros",
            sufijo = "L",
            error = errorLitros
        )

        Text("Destino de la leche", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectorDestino(destino, onSeleccion = { destino = it })
        if (retiro != null && destino.esComercial) {
            CajaAviso(
                texto = "Esta vaca tiene retiro de leche vigente: enviar su leche al tanque puede contaminar la venta.",
                icono = Icons.Default.Warning,
                fondo = ColoresSanidad.ambarFondo,
                color = ColoresSanidad.ambarTexto
            )
        }
    }

    duplicado?.let { (existente, nuevo) ->
        DialogoConfirmar(
            titulo = "Ordeño ya registrado",
            mensaje = "Ya existe un registro de ${existente.animalCodigoNombre} el ${Formato.fecha(existente.fecha)} " +
                "en la jornada de la ${existente.jornada.etiqueta.lowercase()} (${Formato.litros(existente.litros)}).\n\n" +
                "¿Desea actualizar ese registro con ${Formato.litros(nuevo.litros)}?",
            textoConfirmar = "Actualizar",
            onConfirmar = {
                duplicado = null
                enviar(nuevo.copy(id = existente.id), original = inicial)
            },
            onCancelar = { duplicado = null }
        )
    }
}

/** "#008 - Luna (Prom: 18,5 L)" + aviso si está en retiro. */
private fun etiquetaVaca(vaca: Animal, enRetiro: Boolean): String {
    val promedio = vaca.produccionLitrosDia?.let { Formato.litros(it) } ?: "— L"
    return "${vaca.codigoNombre} (Prom: $promedio)" + if (enRetiro) " · ⚠ Retiro" else ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorJornada(seleccion: JornadaOrdeno, onSeleccion: (JornadaOrdeno) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        JornadaOrdeno.entries.forEachIndexed { i, j ->
            SegmentedButton(
                selected = seleccion == j,
                onClick = { onSeleccion(j) },
                shape = SegmentedButtonDefaults.itemShape(i, JornadaOrdeno.entries.size),
                colors = SegmentedButtonDefaults.colors(inactiveContainerColor = MaterialTheme.colorScheme.surface),
                label = { Text(j.etiqueta) }
            )
        }
    }
}

@Composable
private fun SelectorDestino(seleccion: DestinoLeche, onSeleccion: (DestinoLeche) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        DestinoLeche.entries.forEach { d ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = seleccion == d, onClick = { onSeleccion(d) }, role = Role.RadioButton)
            ) {
                RadioButton(selected = seleccion == d, onClick = null, modifier = Modifier.padding(8.dp))
                Text(d.etiqueta, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

package com.juanberrocal.gestionganadera.ui.alimentacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.FrecuenciaConsumo
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.TipoInsumo
import com.juanberrocal.gestionganadera.data.UnidadConsumo
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.comun.CajaAviso
import com.juanberrocal.gestionganadera.ui.comun.CampoFecha
import com.juanberrocal.gestionganadera.ui.comun.CampoNumero
import com.juanberrocal.gestionganadera.ui.comun.CampoTexto
import com.juanberrocal.gestionganadera.ui.comun.HojaFormulario
import com.juanberrocal.gestionganadera.ui.comun.TituloSeccionForm
import com.juanberrocal.gestionganadera.ui.ganado.CampoDesplegable
import com.juanberrocal.gestionganadera.ui.tema.ColoresSanidad
import java.time.LocalDate

/** Grupos productivos en el orden en que se piensan en finca. */
private val GRUPOS = listOf(
    PropositoEtapa.PRODUCCION_ORDENO, PropositoEtapa.VACA_SECA, PropositoEtapa.CRIA_LEVANTE,
    PropositoEtapa.ENGORDE, PropositoEtapa.TORO_REPRODUCTOR, PropositoEtapa.TORO_CEBA,
    PropositoEtapa.TERNERO_LEVANTE, PropositoEtapa.TRABAJO_COMPANIA
)

/**
 * Registrar ([inicial] = null) o editar lo que consume un animal o un grupo productivo.
 * @param tipoPorDefecto al crear, según el filtro activo de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlimentacionFormSheet(
    inicial: RegistroAlimentacionMedicamento?,
    tipoPorDefecto: TipoInsumo,
    animales: List<Animal>,
    onGuardar: (RegistroAlimentacionMedicamento, (Boolean) -> Unit) -> Unit,
    onCerrar: () -> Unit
) {
    var tipo by remember { mutableStateOf(inicial?.tipoRegistro ?: tipoPorDefecto) }
    var porGrupo by remember { mutableStateOf(inicial?.grupoProposito != null) }
    var animal by remember { mutableStateOf(animales.firstOrNull { it.id == inicial?.animalId }) }
    var grupo by remember { mutableStateOf(inicial?.grupoProposito ?: PropositoEtapa.PRODUCCION_ORDENO) }
    var categoria by remember { mutableStateOf(inicial?.categoriaDetalle ?: tipo.categorias.first()) }
    var producto by remember { mutableStateOf(inicial?.nombreProducto.orEmpty()) }
    var cantidad by remember { mutableStateOf(inicial?.cantidad?.let { Formato.plano(it) }.orEmpty()) }
    var unidad by remember {
        mutableStateOf(inicial?.unidad ?: if (tipo == TipoInsumo.MEDICAMENTO) UnidadConsumo.MILILITROS else UnidadConsumo.KILOGRAMOS)
    }
    var frecuencia by remember {
        mutableStateOf(inicial?.frecuencia ?: if (tipo == TipoInsumo.MEDICAMENTO) FrecuenciaConsumo.DOSIS_UNICA else FrecuenciaConsumo.DIARIO)
    }
    var fechaInicio by remember { mutableStateOf(inicial?.fechaInicio ?: LocalDate.now()) }
    var retiro by remember { mutableStateOf(inicial?.tieneRetiroLeche ?: false) }
    var diasRetiro by remember { mutableStateOf(inicial?.diasRetiro?.takeIf { it > 0 }?.toString().orEmpty()) }

    var errorAnimal by remember { mutableStateOf<String?>(null) }
    var errorProducto by remember { mutableStateOf<String?>(null) }
    var errorCantidad by remember { mutableStateOf<String?>(null) }
    var errorDias by remember { mutableStateOf<String?>(null) }
    var errorGuardar by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val opcionesAnimal = remember(animales) { animales.sortedWith(compareBy<Animal> { !it.esBovino }.thenBy { it.codigo }) }
    val esMedicamento = tipo == TipoInsumo.MEDICAMENTO

    fun cambiarTipo(nuevo: TipoInsumo) {
        if (nuevo == tipo) return
        tipo = nuevo
        if (categoria !in nuevo.categorias) categoria = nuevo.categorias.first()
        // Valores típicos al cambiar: medicamentos en ml / dosis única, alimentos en kg / diario
        if (inicial == null) {
            unidad = if (nuevo == TipoInsumo.MEDICAMENTO) UnidadConsumo.MILILITROS else UnidadConsumo.KILOGRAMOS
            frecuencia = if (nuevo == TipoInsumo.MEDICAMENTO) FrecuenciaConsumo.DOSIS_UNICA else FrecuenciaConsumo.DIARIO
        }
    }

    fun guardar() {
        val valor = Formato.leerDecimal(cantidad)
        val dias = Formato.leerEntero(diasRetiro)
        errorAnimal = if (!porGrupo && animal == null) "Elija el animal" else null
        errorProducto = if (producto.isBlank()) "Escriba el nombre del producto" else null
        errorCantidad = if (valor == null || valor <= 0) "Ingrese una cantidad mayor a 0" else null
        errorDias = if (esMedicamento && retiro && (dias == null || dias < 1)) "Indique los días de retiro (mínimo 1)" else null
        if (listOf(errorAnimal, errorProducto, errorCantidad, errorDias).any { it != null }) return

        val registro = RegistroAlimentacionMedicamento(
            id = inicial?.id.orEmpty(),
            animalId = if (porGrupo) null else animal!!.id,
            grupoProposito = if (porGrupo) grupo else null,
            objetivoLabel = if (porGrupo) RegistroAlimentacionMedicamento.etiquetaGrupo(grupo) else animal!!.codigoNombre,
            tipoRegistro = tipo,
            categoriaDetalle = categoria,
            nombreProducto = producto.trim(),
            cantidad = valor!!,
            unidad = unidad,
            frecuencia = frecuencia,
            tieneRetiroLeche = esMedicamento && retiro,
            diasRetiro = if (esMedicamento && retiro) dias!! else 0,
            activo = inicial?.activo ?: true,
            fechaInicio = fechaInicio,
            fechaFin = inicial?.fechaFin
        )
        guardando = true
        errorGuardar = null
        onGuardar(registro) { ok ->
            guardando = false
            if (ok) onCerrar() else errorGuardar = "No se pudo guardar. Revise la conexión con el servidor."
        }
    }

    HojaFormulario(
        titulo = if (inicial == null) "Registrar alimento o medicamento" else "Editar registro",
        onCerrar = onCerrar,
        onGuardar = ::guardar,
        guardando = guardando,
        error = errorGuardar
    ) {
        Segmentos(TipoInsumo.entries, tipo, { it.etiquetaCorta }, ::cambiarTipo)

        TituloSeccionForm("¿A quién se le aplica?")
        Segmentos(listOf(false, true), porGrupo, { if (it) "Grupo productivo" else "Animal específico" }, { porGrupo = it })
        if (porGrupo) {
            CampoDesplegable(
                etiqueta = "Grupo",
                valorMostrado = RegistroAlimentacionMedicamento.nombreGrupo(grupo),
                opciones = GRUPOS,
                textoDe = { g -> "${RegistroAlimentacionMedicamento.nombreGrupo(g)} (${animales.count { it.proposito == g }})" },
                estaSeleccionado = { it == grupo },
                onSeleccion = { grupo = it },
                ayuda = "Aplica a los ${animales.count { it.proposito == grupo }} animales que hoy están en este grupo"
            )
        } else {
            CampoDesplegable(
                etiqueta = "Animal",
                valorMostrado = animal?.codigoNombre ?: inicial?.objetivoLabel?.takeIf { inicial.animalId != null }.orEmpty(),
                placeholder = "Elija un animal de Ganado",
                opciones = opcionesAnimal,
                textoDe = { if (it.esBovino) it.codigoNombre else "${it.codigoNombre} (${it.especie.etiqueta})" },
                estaSeleccionado = { it.id == animal?.id },
                onSeleccion = { animal = it; errorAnimal = null },
                error = errorAnimal
            )
        }

        TituloSeccionForm("Producto")
        CampoDesplegable(
            etiqueta = "Categoría",
            valorMostrado = categoria,
            opciones = tipo.categorias,
            textoDe = { it },
            estaSeleccionado = { it == categoria },
            onSeleccion = { categoria = it }
        )
        CampoTexto(
            valor = producto,
            onCambio = { producto = it; errorProducto = null },
            etiqueta = if (esMedicamento) "Nombre del medicamento" else "Nombre del alimento / suplemento",
            error = errorProducto
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            CampoNumero(
                valor = cantidad,
                onCambio = { cantidad = it; errorCantidad = null },
                etiqueta = "Cantidad",
                error = errorCantidad,
                modifier = Modifier.weight(1f)
            )
            CampoDesplegable(
                etiqueta = "Unidad",
                valorMostrado = unidad.etiqueta,
                opciones = UnidadConsumo.entries,
                textoDe = { it.etiqueta },
                estaSeleccionado = { it == unidad },
                onSeleccion = { unidad = it },
                modifier = Modifier.weight(1f)
            )
        }
        CampoDesplegable(
            etiqueta = "Frecuencia",
            valorMostrado = frecuencia.etiqueta,
            opciones = FrecuenciaConsumo.entries,
            textoDe = { it.etiqueta },
            estaSeleccionado = { it == frecuencia },
            onSeleccion = { frecuencia = it }
        )
        CampoFecha("Fecha de inicio", fechaInicio, onFecha = { fechaInicio = it })

        if (esMedicamento) {
            TituloSeccionForm("Retiro de leche")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Implica retiro de leche", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "La leche de las vacas tratadas no puede ir al tanque",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = retiro, onCheckedChange = { retiro = it; errorDias = null })
            }
            if (retiro) {
                CampoNumero(
                    valor = diasRetiro,
                    onCambio = { diasRetiro = it; errorDias = null },
                    etiqueta = "Días de retiro después del tratamiento",
                    sufijo = "días",
                    entero = true,
                    error = errorDias
                )
                CajaAviso(
                    texto = "Mientras el tratamiento esté activo, y durante los días de retiro después de finalizarlo, " +
                        "Ordeño preseleccionará «Retiro médico (Descarte)» para estas vacas.",
                    icono = Icons.Default.Info,
                    fondo = ColoresSanidad.ambarFondo,
                    color = ColoresSanidad.ambarTexto
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmentos(opciones: List<T>, seleccion: T, texto: (T) -> String, onSeleccion: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, opcion ->
            SegmentedButton(
                selected = opcion == seleccion,
                onClick = { onSeleccion(opcion) },
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                colors = SegmentedButtonDefaults.colors(inactiveContainerColor = MaterialTheme.colorScheme.surface),
                label = { Text(texto(opcion), maxLines = 1) }
            )
        }
    }
}

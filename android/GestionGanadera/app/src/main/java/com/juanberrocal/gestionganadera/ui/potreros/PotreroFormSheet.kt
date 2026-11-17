package com.juanberrocal.gestionganadera.ui.potreros

import androidx.annotation.StringRes
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.CondicionPasto
import com.juanberrocal.gestionganadera.data.EstadoPotrero
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.TipoPasto
import com.juanberrocal.gestionganadera.data.UnidadArea
import com.juanberrocal.gestionganadera.logica.Formato
import com.juanberrocal.gestionganadera.ui.ganado.CampoDesplegable
import com.juanberrocal.gestionganadera.ui.ganado.FormaCampo
import com.juanberrocal.gestionganadera.ui.ganado.coloresCampo
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Agregar ([potreroInicial] = null) o editar un potrero.
 * @param fincaPreseleccionada finca activa en pantalla (se usa al crear).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotreroFormSheet(
    potreroInicial: Potrero?,
    fincaPreseleccionada: String?,
    fincas: List<Finca>,
    potrerosExistentes: List<Potrero>,
    onGuardar: (Potrero, (Boolean) -> Unit) -> Unit,
    onCerrar: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val form = remember(potreroInicial?.id) { EstadoFormularioPotrero(potreroInicial, fincaPreseleccionada) }
    val fincaElegida = fincas.firstOrNull { it.id == form.fincaId }

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
                stringResource(if (potreroInicial == null) R.string.potreros_agregar else R.string.potreros_editar),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Finca
            CampoDesplegable(
                etiqueta = stringResource(R.string.potrero_finca),
                valorMostrado = fincaElegida?.descripcion.orEmpty(),
                opciones = fincas,
                textoDe = { it.descripcion },
                estaSeleccionado = { it.id == form.fincaId },
                onSeleccion = { form.fincaId = it.id; form.errorFinca = false; form.errorNombre = null },
                placeholder = stringResource(R.string.animal_seleccione),
                error = if (form.errorFinca) stringResource(R.string.error_obligatorio) else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Nombre
            OutlinedTextField(
                value = form.nombre,
                onValueChange = { form.nombre = it; form.errorNombre = null },
                label = { Text(stringResource(R.string.potrero_nombre)) },
                isError = form.errorNombre != null,
                supportingText = form.errorNombre?.let { { Text(stringResource(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = FormaCampo,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )

            // Área: directa o largo x ancho
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.potrero_usar_largo_ancho),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = form.usarLargoAncho, onCheckedChange = { form.usarLargoAncho = it; form.errorArea = false })
            }
            if (form.usarLargoAncho) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero(form.largo, { form.largo = it; form.errorArea = false }, R.string.potrero_largo,
                        error = form.errorArea, modifier = Modifier.weight(1f))
                    CampoNumero(form.ancho, { form.ancho = it; form.errorArea = false }, R.string.potrero_ancho,
                        error = form.errorArea, modifier = Modifier.weight(1f))
                }
                val m2 = form.areaM2DesdeLargoAncho()
                Text(
                    if (m2 != null) stringResource(R.string.potrero_area_calculada, Formato.numero(m2), Formato.numero(m2 / 10_000))
                    else stringResource(R.string.potrero_area_vacia),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (form.errorArea) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero(form.area, { form.area = it; form.errorArea = false }, R.string.potrero_area,
                        error = form.errorArea, modifier = Modifier.weight(1f))
                    CampoDesplegable(
                        etiqueta = stringResource(R.string.potrero_unidad),
                        valorMostrado = form.unidad.abreviatura,
                        opciones = UnidadArea.entries,
                        textoDe = { it.etiqueta },
                        estaSeleccionado = { it == form.unidad },
                        onSeleccion = { form.unidad = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tipo de pasto: primero los recomendados para el piso térmico de la finca
            val piso = fincaElegida?.pisoTermico
            val pastos = TipoPasto.ordenadosPara(piso)
            val contexto = LocalContext.current
            Spacer(Modifier.height(4.dp))
            CampoDesplegable(
                etiqueta = stringResource(R.string.potrero_tipo_pasto),
                valorMostrado = form.tipoPasto.etiqueta,
                opciones = pastos,
                textoDe = { pasto ->
                    if (piso != null && pasto.pisoIdeal == piso) contexto.getString(R.string.potrero_pasto_recomendado, pasto.etiqueta)
                    else pasto.etiqueta
                },
                estaSeleccionado = { it == form.tipoPasto },
                onSeleccion = { form.elegirPasto(it) },
                ayuda = stringResource(
                    R.string.potrero_pasto_sugerencia,
                    form.tipoPasto.diasDescansoSugeridos,
                    form.tipoPasto.pisoIdeal.etiqueta
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Ocupación / Descanso: etiquetas cortas + sufijo "días" para que no se corten
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoNumero(
                    form.ocupacion, { form.ocupacion = it; form.errorOcupacion = false },
                    R.string.potrero_ocupacion,
                    sufijo = R.string.potrero_sufijo_dias,
                    ayuda = R.string.potrero_ocupacion_ayuda,
                    error = form.errorOcupacion,
                    modifier = Modifier.weight(1f)
                )
                CampoNumero(
                    form.descanso, { form.descanso = it; form.errorDescanso = false },
                    R.string.potrero_descanso,
                    sufijo = R.string.potrero_sufijo_dias,
                    decimal = false,
                    error = form.errorDescanso,
                    modifier = Modifier.weight(1f)
                )
            }

            // Estado y condición
            TituloSeccion(R.string.potrero_estado)
            FilaChips(EstadoPotrero.entries, form.estado, { it.etiqueta }) { form.estado = it }
            if (form.estado == EstadoPotrero.OCUPADO) {
                CampoNumero(
                    form.animales, { form.animales = it },
                    R.string.potrero_animales,
                    decimal = false,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
            TituloSeccion(R.string.potrero_condicion)
            FilaChips(CondicionPasto.entries, form.condicion, { it.etiqueta }) { form.condicion = it }

            if (form.errorGuardar) {
                Text(
                    stringResource(R.string.potreros_error_guardar),
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
                    enabled = !form.guardando,
                    onClick = {
                        val potrero = form.validar(potrerosExistentes) ?: return@Button
                        form.guardando = true
                        form.errorGuardar = false
                        onGuardar(potrero) { ok ->
                            form.guardando = false
                            if (ok) cerrar() else form.errorGuardar = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(stringResource(R.string.guardar)) }
            }
        }
    }
}

@Composable
private fun TituloSeccion(@StringRes texto: Int) {
    Text(
        stringResource(texto),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun CampoNumero(
    valor: String,
    onCambio: (String) -> Unit,
    @StringRes etiqueta: Int,
    modifier: Modifier = Modifier,
    @StringRes sufijo: Int? = null,
    @StringRes ayuda: Int? = null,
    decimal: Boolean = true,
    error: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || (decimal && (it == ',' || it == '.')) }) onCambio(nuevo) },
        label = { Text(stringResource(etiqueta), maxLines = 1) },
        suffix = sufijo?.let { { Text(stringResource(it)) } },
        isError = error,
        supportingText = when {
            error -> { { Text(stringResource(R.string.error_numero_mayor_cero)) } }
            ayuda != null -> { { Text(stringResource(ayuda)) } }
            else -> null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = FormaCampo,
        colors = coloresCampo(),
        modifier = modifier
    )
}

/** Estado y validación del formulario de potrero. */
@Stable
private class EstadoFormularioPotrero(private val inicial: Potrero?, fincaPreseleccionada: String?) {
    var fincaId by mutableStateOf(inicial?.fincaId ?: fincaPreseleccionada)
    var nombre by mutableStateOf(inicial?.nombre.orEmpty())
    var usarLargoAncho by mutableStateOf(inicial?.largo != null && inicial.ancho != null)
    var area by mutableStateOf(inicial?.area?.let(Formato::plano).orEmpty())
    var unidad by mutableStateOf(inicial?.unidadArea ?: UnidadArea.HECTAREAS)
    var largo by mutableStateOf(inicial?.largo?.let(Formato::plano).orEmpty())
    var ancho by mutableStateOf(inicial?.ancho?.let(Formato::plano).orEmpty())
    var tipoPasto by mutableStateOf(inicial?.tipoPasto ?: TipoPasto.BRACHIARIA)
    var ocupacion by mutableStateOf(inicial?.diasOcupacionPlaneados?.let(Formato::plano) ?: "1")
    var descanso by mutableStateOf(
        (inicial?.diasDescansoPlaneados ?: TipoPasto.BRACHIARIA.diasDescansoSugeridos).toString()
    )
    var estado by mutableStateOf(inicial?.estado ?: EstadoPotrero.EN_DESCANSO)
    var condicion by mutableStateOf(inicial?.condicionPasto ?: CondicionPasto.OPTIMO)
    var animales by mutableStateOf((inicial?.cantidadAnimalesActuales ?: 0).toString())

    var errorFinca by mutableStateOf(false)
    var errorNombre by mutableStateOf<Int?>(null)
    var errorArea by mutableStateOf(false)
    var errorOcupacion by mutableStateOf(false)
    var errorDescanso by mutableStateOf(false)
    var errorGuardar by mutableStateOf(false)
    var guardando by mutableStateOf(false)

    /** Al elegir el pasto se autocompletan los días de descanso sugeridos. */
    fun elegirPasto(pasto: TipoPasto) {
        tipoPasto = pasto
        descanso = pasto.diasDescansoSugeridos.toString()
        errorDescanso = false
    }

    fun areaM2DesdeLargoAncho(): Double? {
        val l = Formato.leerDecimal(largo) ?: return null
        val a = Formato.leerDecimal(ancho) ?: return null
        return if (l > 0 && a > 0) l * a else null
    }

    fun validar(existentes: List<Potrero>): Potrero? {
        val nombreLimpio = ValidacionPotrero.nombre(nombre)
        nombre = nombreLimpio
        val id = inicial?.id.orEmpty()

        errorFinca = fincaId == null
        errorNombre = when {
            nombreLimpio.isEmpty() -> R.string.error_obligatorio
            ValidacionPotrero.nombreRepetido(nombreLimpio, fincaId, id, existentes) -> R.string.potrero_nombre_repetido
            else -> null
        }

        val largoNum = Formato.leerDecimal(largo)
        val anchoNum = Formato.leerDecimal(ancho)
        val areaFinal: Double? = if (usarLargoAncho) {
            // Se guarda en la unidad elegida para mostrarlo igual que antes
            areaM2DesdeLargoAncho()?.let { it / unidad.m2PorUnidad }
        } else {
            Formato.leerDecimal(area)?.takeIf { it > 0 }
        }
        errorArea = areaFinal == null

        val ocupacionNum = Formato.leerDecimal(ocupacion)?.takeIf { it >= 0.5 }
        errorOcupacion = ocupacionNum == null
        val descansoNum = Formato.leerEntero(descanso)?.takeIf { it > 0 }
        errorDescanso = descansoNum == null

        if (errorFinca || errorNombre != null || areaFinal == null || ocupacionNum == null || descansoNum == null) return null

        // La fecha del ciclo se reinicia si es nuevo o si cambió el estado
        val fecha = if (inicial == null || inicial.estado != estado) LocalDate.now() else inicial.fechaUltimoCambio

        return (inicial ?: Potrero(nombre = "", area = 0.0)).copy(
            fincaId = fincaId,
            nombre = nombreLimpio,
            area = areaFinal,
            unidadArea = unidad,
            largo = if (usarLargoAncho) largoNum else null,
            ancho = if (usarLargoAncho) anchoNum else null,
            tipoPasto = tipoPasto,
            estado = estado,
            condicionPasto = condicion,
            diasOcupacionPlaneados = ocupacionNum,
            diasDescansoPlaneados = descansoNum,
            fechaUltimoCambio = fecha,
            cantidadAnimalesActuales = if (estado == EstadoPotrero.OCUPADO) Formato.leerEntero(animales) ?: 0 else 0
        )
    }
}

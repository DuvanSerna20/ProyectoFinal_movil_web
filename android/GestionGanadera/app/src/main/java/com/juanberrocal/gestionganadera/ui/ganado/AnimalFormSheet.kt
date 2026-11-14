package com.juanberrocal.gestionganadera.ui.ganado

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.juanberrocal.gestionganadera.R
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.Especie
import com.juanberrocal.gestionganadera.data.EstadoReproductivo
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.Razas
import com.juanberrocal.gestionganadera.data.Sexo
import com.juanberrocal.gestionganadera.logica.Formato
import kotlinx.coroutines.launch

/**
 * Hoja inferior para agregar ([animalInicial] = null) o editar un animal, en 4 bloques.
 * @param onGuardar recibe el animal validado y un callback con `true` si se guardó.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalFormSheet(
    animalInicial: Animal?,
    animalesExistentes: List<Animal>,
    fincas: List<Finca>,
    potreros: List<Potrero>,
    onGuardar: (animal: Animal, alTerminar: (guardado: Boolean) -> Unit) -> Unit,
    onCerrar: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val form = remember(animalInicial?.id) { EstadoFormularioAnimal(animalInicial) }

    fun cerrarConAnimacion() {
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
                stringResource(if (animalInicial == null) R.string.ganado_agregar else R.string.ganado_editar),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ---------------------------------------------------------------- 1. Identificación
            TituloBloque(R.string.animal_seccion_identificacion)
            OutlinedTextField(
                value = form.codigo,
                onValueChange = { form.codigo = it; form.errorCodigo = null },
                label = { Text(stringResource(R.string.animal_codigo_chapeta)) },
                isError = form.errorCodigo != null,
                supportingText = { Text(stringResource(form.errorCodigo ?: R.string.animal_codigo_ayuda)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                shape = FormaCampo,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.nombre,
                onValueChange = { form.nombre = it; form.errorNombre = null },
                label = { Text(stringResource(R.string.animal_nombre)) },
                isError = form.errorNombre != null,
                supportingText = form.errorNombre?.let { { Text(stringResource(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                shape = FormaCampo,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )

            // Finca y potrero (opcionales). El potrero se filtra por la finca elegida.
            val sinFinca = stringResource(R.string.animal_sin_finca)
            val sinPotrero = stringResource(R.string.animal_sin_potrero)
            val potrerosServidor = potreros.filter { it.mongoId != null }
            val potrerosDeFinca = form.fincaId?.let { f -> potrerosServidor.filter { it.fincaId == f } } ?: potrerosServidor
            val nombreFinca = { id: String? -> fincas.firstOrNull { it.id == id }?.nombre }

            CampoDesplegable(
                etiqueta = stringResource(R.string.animal_finca),
                valorMostrado = nombreFinca(form.fincaId) ?: sinFinca,
                opciones = listOf<Finca?>(null) + fincas,
                textoDe = { it?.descripcion ?: sinFinca },
                estaSeleccionado = { it?.id == form.fincaId },
                onSeleccion = { form.elegirFinca(it?.id, potrerosServidor) },
                habilitado = fincas.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            CampoDesplegable(
                etiqueta = stringResource(R.string.animal_potrero),
                valorMostrado = potrerosServidor.firstOrNull { it.mongoId == form.potreroId }?.nombre ?: sinPotrero,
                opciones = listOf<Potrero?>(null) + potrerosDeFinca,
                textoDe = { p ->
                    when {
                        p == null -> sinPotrero
                        form.fincaId == null -> listOfNotNull(p.nombre, nombreFinca(p.fincaId)).joinToString(" · ")
                        else -> p.nombre
                    }
                },
                estaSeleccionado = { it?.mongoId == form.potreroId },
                onSeleccion = { p -> form.potreroId = p?.mongoId; if (p?.fincaId != null) form.fincaId = p.fincaId },
                habilitado = potrerosDeFinca.isNotEmpty(),
                ayuda = if (potrerosDeFinca.isEmpty()) stringResource(R.string.animal_sin_potreros_registrados) else null,
                modifier = Modifier.fillMaxWidth()
            )

            // ---------------------------------------------------------------- 2. Clasificación
            TituloBloque(R.string.animal_seccion_clasificacion)
            Subtitulo(R.string.animal_especie)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Especie.entries.forEach { especie ->
                    FilterChip(
                        selected = form.especie == especie,
                        onClick = { form.cambiarEspecie(especie) },
                        label = { Text(especie.etiqueta) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
            Subtitulo(R.string.animal_sexo)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Sexo.entries.forEachIndexed { i, sexo ->
                    SegmentedButton(
                        selected = form.sexo == sexo,
                        onClick = { form.cambiarSexo(sexo) },
                        shape = SegmentedButtonDefaults.itemShape(i, Sexo.entries.size),
                        colors = SegmentedButtonDefaults.colors(inactiveContainerColor = MaterialTheme.colorScheme.surface),
                        label = { Text(sexo.etiqueta) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (form.usaListaRazas) {
                CampoDesplegable(
                    etiqueta = stringResource(R.string.animal_raza),
                    valorMostrado = form.razaLista.orEmpty(),
                    opciones = Razas.BOVINAS,
                    textoDe = { it },
                    estaSeleccionado = { it == form.razaLista },
                    onSeleccion = { form.razaLista = it; form.errorRaza = null },
                    placeholder = stringResource(R.string.animal_seleccione),
                    error = form.errorRaza?.takeIf { !form.muestraRazaManual }?.let { stringResource(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (form.muestraRazaManual) {
                OutlinedTextField(
                    value = form.razaManual,
                    onValueChange = { form.razaManual = it; form.errorRaza = null },
                    label = {
                        Text(stringResource(if (form.usaListaRazas) R.string.animal_raza_cruce else R.string.animal_raza_manual))
                    },
                    isError = form.errorRaza != null,
                    supportingText = form.errorRaza?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = FormaCampo,
                    colors = coloresCampo(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---------------------------------------------------------------- 3. Propósito y reproducción
            TituloBloque(R.string.animal_seccion_proposito)
            CampoDesplegable(
                etiqueta = stringResource(R.string.animal_proposito),
                valorMostrado = form.proposito.etiqueta,
                opciones = form.propositos,
                textoDe = { it.etiqueta },
                estaSeleccionado = { it == form.proposito },
                onSeleccion = { form.cambiarProposito(it) },
                habilitado = form.propositos.size > 1,
                modifier = Modifier.fillMaxWidth()
            )
            if (form.esHembraBovina) {
                Subtitulo(R.string.animal_estado_reproductivo)
                SelectorEstadoReproductivo(form.estadoReproductivo) { form.estadoReproductivo = it }
            }

            // ---------------------------------------------------------------- 4. Productivo y peso
            TituloBloque(R.string.animal_seccion_productivo)
            CampoDecimal(
                valor = form.peso,
                onCambio = { form.peso = it; form.errorPeso = false },
                etiqueta = R.string.animal_peso,
                sufijo = R.string.unidad_kg,
                destacado = form.proposito.requierePeso,
                error = if (form.errorPeso) stringResource(R.string.error_peso) else null,
                ayuda = stringResource(
                    if (form.proposito.requierePeso) R.string.animal_peso_obligatorio else R.string.animal_peso_ayuda
                )
            )
            if (form.enOrdeno) {
                CampoDecimal(
                    valor = form.produccion,
                    onCambio = { form.produccion = it },
                    etiqueta = R.string.animal_produccion,
                    sufijo = R.string.unidad_litros_dia,
                    ayuda = stringResource(R.string.animal_produccion_ayuda)
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { cerrarConAnimacion() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(stringResource(R.string.cancelar)) }

                Button(
                    onClick = {
                        val animal = form.validar(animalesExistentes) ?: return@Button
                        form.guardando = true // evita guardar dos veces con un doble toque
                        onGuardar(animal) { guardado ->
                            form.guardando = false
                            if (guardado) cerrarConAnimacion() else form.errorCodigo = R.string.error_guardar_servidor
                        }
                    },
                    enabled = !form.guardando,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text(stringResource(R.string.guardar)) }
            }
        }
    }
}

@Composable
private fun TituloBloque(@StringRes texto: Int) {
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Text(
        stringResource(texto),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun Subtitulo(@StringRes texto: Int) {
    Text(
        stringResource(texto),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/** Estado y validación del formulario, separado de la UI. */
@Stable
private class EstadoFormularioAnimal(private val inicial: Animal?) {
    var codigo by mutableStateOf(inicial?.codigo.orEmpty())
    var nombre by mutableStateOf(inicial?.nombre.orEmpty())
    var fincaId by mutableStateOf(inicial?.fincaId)
    var potreroId by mutableStateOf(inicial?.potreroId)
    var especie by mutableStateOf(inicial?.especie ?: Especie.BOVINO)
        private set
    var sexo by mutableStateOf(inicial?.sexo ?: Sexo.HEMBRA)
        private set
    var proposito by mutableStateOf(inicial?.proposito ?: PropositoEtapa.porDefecto(especie, sexo))
        private set
    var estadoReproductivo by mutableStateOf(inicial?.estadoReproductivo ?: EstadoReproductivo.HORRA)

    /** Raza elegida en la lista (solo bovinos). */
    var razaLista by mutableStateOf<String?>(null)
    /** Raza escrita a mano (otras especies, o bovino con "Otra / Cruce"). */
    var razaManual by mutableStateOf("")
    var peso by mutableStateOf(inicial?.pesoKg?.let(Formato::plano).orEmpty())
    var produccion by mutableStateOf(inicial?.produccionLitrosDia?.let(Formato::plano).orEmpty())

    var errorCodigo by mutableStateOf<Int?>(null)
    var errorNombre by mutableStateOf<Int?>(null)
    var errorRaza by mutableStateOf<Int?>(null)
    var errorPeso by mutableStateOf(false)
    var guardando by mutableStateOf(false)

    val propositos: List<PropositoEtapa> get() = PropositoEtapa.opcionesPara(especie, sexo)
    val esHembraBovina: Boolean get() = especie == Especie.BOVINO && sexo == Sexo.HEMBRA
    val enOrdeno: Boolean get() = esHembraBovina && proposito == PropositoEtapa.PRODUCCION_ORDENO
    val usaListaRazas: Boolean get() = especie == Especie.BOVINO
    val muestraRazaManual: Boolean get() = !usaListaRazas || razaLista == Razas.OTRA

    init {
        val raza = inicial?.raza.orEmpty()
        if (especie == Especie.BOVINO) {
            val oficial = Razas.bovinaOficial(raza)
            when {
                oficial != null -> razaLista = oficial
                raza.isNotBlank() -> { razaLista = Razas.OTRA; razaManual = raza }
            }
        } else {
            razaManual = raza
        }
        ajustarProposito()
    }

    /** Al cambiar de finca se quita el potrero si no pertenece a la nueva finca. */
    fun elegirFinca(id: String?, potreros: List<Potrero>) {
        fincaId = id
        val potrero = potreros.firstOrNull { it.mongoId == potreroId }
        if (id != null && potrero != null && potrero.fincaId != id) potreroId = null
    }

    fun cambiarEspecie(nueva: Especie) {
        especie = nueva
        errorRaza = null
        ajustarProposito()
    }

    fun cambiarSexo(nuevo: Sexo) {
        sexo = nuevo
        ajustarProposito()
    }

    fun cambiarProposito(nuevo: PropositoEtapa) {
        proposito = nuevo
        errorPeso = false
        estadoReproductivo = EstadoReproductivo.sugerido(especie, sexo, proposito, estadoReproductivo)
    }

    /** Si el propósito no aplica a la nueva especie/sexo, se toma el primero válido. */
    private fun ajustarProposito() {
        if (proposito !in propositos) proposito = propositos.first()
        estadoReproductivo = EstadoReproductivo.sugerido(especie, sexo, proposito, estadoReproductivo)
    }

    /** Devuelve el animal listo para guardar, o null (y marca los errores) si algo falta. */
    fun validar(existentes: List<Animal>): Animal? {
        val codigoLimpio = FormatoAnimal.codigo(codigo)
        val nombreLimpio = FormatoAnimal.nombre(nombre)
        val raza = if (usaListaRazas && razaLista != Razas.OTRA) razaLista.orEmpty()
        else FormatoAnimal.nombre(razaManual)
        val id = inicial?.id.orEmpty()
        val pesoNum = Formato.leerDecimal(peso)?.takeIf { it > 0 }
        val produccionNum = Formato.leerDecimal(produccion)?.takeIf { it > 0 }

        errorCodigo = when {
            codigoLimpio.isEmpty() -> R.string.error_obligatorio
            FormatoAnimal.codigoRepetido(codigoLimpio, id, existentes) -> R.string.error_codigo_repetido
            else -> null
        }
        errorNombre = if (nombreLimpio.isEmpty()) R.string.error_obligatorio else null
        errorRaza = when {
            raza.isNotEmpty() -> null
            usaListaRazas && razaLista == null -> R.string.error_seleccione_raza
            else -> R.string.error_obligatorio
        }
        // Peso: obligatorio en ceba/engorde/reproducción; si se escribe, debe ser > 0
        errorPeso = (proposito.requierePeso && pesoNum == null) || (peso.isNotBlank() && pesoNum == null)

        codigo = codigoLimpio
        nombre = nombreLimpio

        if (errorCodigo != null || errorNombre != null || errorRaza != null || errorPeso) return null

        return Animal(
            id = id,
            codigo = codigoLimpio,
            nombre = nombreLimpio,
            especie = especie,
            sexo = sexo,
            raza = raza,
            proposito = proposito,
            estadoReproductivo = if (esHembraBovina) estadoReproductivo else EstadoReproductivo.NO_APLICA,
            pesoKg = pesoNum,
            produccionLitrosDia = if (enOrdeno) produccionNum else null,
            fincaId = fincaId,
            potreroId = potreroId
        )
    }
}

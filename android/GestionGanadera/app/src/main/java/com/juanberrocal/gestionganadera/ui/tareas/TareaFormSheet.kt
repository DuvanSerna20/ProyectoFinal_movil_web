package com.juanberrocal.gestionganadera.ui.tareas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.Tarea
import com.juanberrocal.gestionganadera.data.TipoTareaEspecial
import com.juanberrocal.gestionganadera.ui.comun.CampoFecha
import com.juanberrocal.gestionganadera.ui.comun.CampoTexto
import com.juanberrocal.gestionganadera.ui.comun.HojaFormulario
import com.juanberrocal.gestionganadera.ui.comun.TituloSeccionForm
import com.juanberrocal.gestionganadera.ui.ganado.CampoDesplegable
import java.time.LocalDate

private const val TODO_EL_HATO = "Todo el hato"

/**
 * Crear ([inicial] = null) o editar una tarea.
 * @param especialPorDefecto al crear, la pestaña desde la que se abrió (General / Especial).
 */
@Composable
fun TareaFormSheet(
    inicial: Tarea?,
    especialPorDefecto: Boolean,
    animales: List<Animal>,
    onGuardar: (Tarea, (Boolean) -> Unit) -> Unit,
    onCerrar: () -> Unit
) {
    var titulo by remember { mutableStateOf(inicial?.titulo.orEmpty()) }
    var descripcion by remember { mutableStateOf(inicial?.descripcion.orEmpty()) }
    var fecha by remember { mutableStateOf(inicial?.fechaProgramada ?: LocalDate.now()) }
    var especial by remember { mutableStateOf(inicial?.esTareaEspecial ?: especialPorDefecto) }
    var tipo by remember { mutableStateOf(inicial?.tipoTareaEspecial ?: TipoTareaEspecial.VACUNACION) }
    var animal by remember { mutableStateOf(animales.firstOrNull { it.id == inicial?.animalId }) }
    var errorTitulo by remember { mutableStateOf<String?>(null) }
    var errorGuardar by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    // Bovinos primero y por código; si el animal guardado ya no existe se conserva su etiqueta
    val opcionesAnimal: List<Animal?> = remember(animales) {
        listOf<Animal?>(null) + animales.sortedWith(compareBy<Animal> { !it.esBovino }.thenBy { it.codigo })
    }
    // Animal ya eliminado de Ganado: se conserva su etiqueta hasta que el usuario elija otra opción
    var etiquetaGuardada by remember {
        mutableStateOf(inicial?.animalCodigoNombre?.takeIf { inicial.animalId != null && animales.none { it.id == inicial.animalId } })
    }

    fun guardar() {
        val limpio = titulo.trim()
        // En una tarea especial sin título se usa el tipo de evento ("Vacunación")
        val tituloFinal = limpio.ifEmpty { if (especial) tipo.etiqueta else "" }
        errorTitulo = if (tituloFinal.isEmpty()) "Escriba qué hay que hacer" else null
        if (tituloFinal.isEmpty()) return

        val conservarAnimal = etiquetaGuardada != null
        val tarea = Tarea(
            id = inicial?.id.orEmpty(),
            titulo = tituloFinal,
            descripcion = descripcion.trim(),
            fechaProgramada = fecha,
            completada = inicial?.completada ?: false,
            esTareaEspecial = especial,
            tipoTareaEspecial = if (especial) tipo else null,
            animalId = if (!especial) null else animal?.id ?: inicial?.animalId?.takeIf { conservarAnimal },
            animalCodigoNombre = if (!especial) null else animal?.codigoNombre ?: etiquetaGuardada
        )
        guardando = true
        errorGuardar = null
        onGuardar(tarea) { ok ->
            guardando = false
            if (ok) onCerrar() else errorGuardar = "No se pudo guardar. Revise la conexión con el servidor."
        }
    }

    HojaFormulario(
        titulo = if (inicial == null) "Nueva tarea" else "Editar tarea",
        onCerrar = onCerrar,
        onGuardar = ::guardar,
        guardando = guardando,
        error = errorGuardar
    ) {
        CampoTexto(
            valor = titulo,
            onCambio = { titulo = it; errorTitulo = null },
            etiqueta = "Título",
            error = errorTitulo,
            ayuda = if (especial) "Si lo deja vacío se usa el tipo de evento" else null
        )
        CampoTexto(descripcion, { descripcion = it }, "Descripción (opcional)", lineas = 3)
        CampoFecha("Fecha programada", fecha, onFecha = { fecha = it })

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Tarea especial", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Sanidad animal: vacunas, veterinario, purgas, tratamientos…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = especial, onCheckedChange = { especial = it })
        }

        if (especial) {
            TituloSeccionForm("Sanidad animal")
            CampoDesplegable(
                etiqueta = "Tipo de evento",
                valorMostrado = tipo.etiqueta,
                opciones = TipoTareaEspecial.entries,
                textoDe = { it.etiqueta },
                estaSeleccionado = { it == tipo },
                onSeleccion = { tipo = it }
            )
            CampoDesplegable(
                etiqueta = "Animal",
                valorMostrado = animal?.codigoNombre ?: etiquetaGuardada ?: TODO_EL_HATO,
                opciones = opcionesAnimal,
                textoDe = { it?.let { a -> if (a.esBovino) a.codigoNombre else "${a.codigoNombre} (${a.especie.etiqueta})" } ?: TODO_EL_HATO },
                estaSeleccionado = { it?.id == animal?.id },
                onSeleccion = { animal = it; etiquetaGuardada = null },
                ayuda = if (animal == null && etiquetaGuardada == null) "Se aplica a todo el hato" else null
            )
        }
    }
}

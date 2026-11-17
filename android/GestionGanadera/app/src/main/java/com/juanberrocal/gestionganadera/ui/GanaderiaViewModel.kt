package com.juanberrocal.gestionganadera.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.juanberrocal.gestionganadera.GanaderiaApp
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.EstadoSync
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.MotivoOmision
import com.juanberrocal.gestionganadera.data.MotivoRotacion
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.data.Resultado
import com.juanberrocal.gestionganadera.data.ResumenSync
import com.juanberrocal.gestionganadera.data.RutaSync
import com.juanberrocal.gestionganadera.data.Tarea
import com.juanberrocal.gestionganadera.ui.ordeno.OrdenoLogica
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Estado de una operación/lista para las pantallas de Compose.
 * [Error] conserva los datos ya cacheados en Room: el error es recuperable y no vacía la pantalla.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error<T>(val mensaje: String, val datosCacheados: T? = null) : UiState<T>
}

class GanaderiaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as GanaderiaApp).repositorio

    // ============================================================ Datos (Flows de Room -> LiveData para las pantallas)

    val animales: LiveData<List<Animal>> = repo.animales.asLiveData()
    // Solo bovinos: es lo que se muestra como "cabezas de ganado" (Inicio y Ganado)
    val totalAnimales: LiveData<Int> = animales.map { lista -> lista.count { it.esBovino } }

    /**
     * Conexión Ganado -> Ordeño: exactamente las vacas con
     * especie == BOVINO && sexo == HEMBRA && proposito == PRODUCCION_ORDENO (ver Animal.estaEnOrdeno).
     */
    val vacasEnOrdenoList: LiveData<List<Animal>> =
        animales.map { lista -> lista.filter { it.estaEnOrdeno }.sortedBy { it.codigo } }

    val potreros: LiveData<List<Potrero>> = repo.potreros.asLiveData()
    val totalPotreros: LiveData<Int> = potreros.map { it.size }

    val fincas: LiveData<List<Finca>> = repo.fincas.asLiveData()

    /** Finca elegida en la pantalla de Potreros; null = "Todas las fincas". */
    private val _fincaSeleccionadaId = MutableLiveData<String?>(null)
    val fincaSeleccionadaId: LiveData<String?> = _fincaSeleccionadaId

    fun seleccionarFinca(id: String?) {
        _fincaSeleccionadaId.value = id
    }

    val ordenos: LiveData<List<RegistroOrdeno>> = repo.ordenos.asLiveData()

    val tareas: LiveData<List<Tarea>> = repo.tareas.asLiveData()
    val tareasPendientes: LiveData<Int> = tareas.map { it.count { t -> !t.completada } }

    val alimentacion: LiveData<List<RegistroAlimentacionMedicamento>> = repo.alimentacion.asLiveData()

    /** Día consultado en la pantalla de Ordeño (se conserva al cambiar de pestaña). */
    private val _diaConsultado = MutableLiveData(LocalDate.now())
    val diaConsultado: LiveData<LocalDate> = _diaConsultado

    fun consultarDia(dia: LocalDate) {
        _diaConsultado.value = dia
    }

    /** Litros comerciales (tanque / venta) de hoy para Inicio: no cuenta descarte ni consumo de terneros. */
    private val _hoy = MutableLiveData(LocalDate.now())
    val litrosHoy: LiveData<Double> = _hoy.switchMap { dia ->
        ordenos.map { lista -> OrdenoLogica.litrosComerciales(lista, dia) }
    }

    fun actualizarHoy() {
        _hoy.value = LocalDate.now()
    }

    // ============================================================ Estados (UiState) por operación

    val animalesState: StateFlow<UiState<List<Animal>>> = estadoDe(repo.animales, RutaSync.GANADO)
    val potrerosState: StateFlow<UiState<List<Potrero>>> = estadoDe(repo.potreros, RutaSync.POTREROS, RutaSync.FINCAS)
    val ordenosState: StateFlow<UiState<List<RegistroOrdeno>>> = estadoDe(repo.ordenos, RutaSync.ORDENOS)
    val tareasState: StateFlow<UiState<List<Tarea>>> = estadoDe(repo.tareas, RutaSync.TAREAS)
    val alimentacionState: StateFlow<UiState<List<RegistroAlimentacionMedicamento>>> =
        estadoDe(repo.alimentacion, RutaSync.ALIMENTACION)

    /** Estado de la última sincronización (Inicio). */
    val sincronizacionState: StateFlow<UiState<ResumenSync>> = repo.estadoSync.map { estado ->
        when (estado) {
            EstadoSync.Inactivo -> UiState.Empty
            EstadoSync.EnCurso -> UiState.Loading
            is EstadoSync.Terminado -> {
                val r = estado.resumen
                when {
                    r.exitosa || r.motivoOmitida == MotivoOmision.MODO_OFFLINE -> UiState.Success(r)
                    r.motivoOmitida != null -> UiState.Error(r.motivoOmitida.mensaje, r)
                    else -> UiState.Error(mensajeErrores(r), r)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Empty)

    val pendientesPorSincronizar: StateFlow<Int> =
        repo.pendientesPorSincronizar.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val modoOffline: StateFlow<Boolean> =
        repo.modoOffline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Combina los datos de Room con el estado de la sincronización:
     * - Loading: Room aún no emite, o se sincroniza por primera vez sin nada en caché.
     * - Error: falló la ruta (o no hay red), pero los datos cacheados siguen disponibles.
     * - Empty: la consulta respondió y de verdad no hay registros.
     */
    private fun <T> estadoDe(datos: Flow<List<T>>, vararg rutas: RutaSync): StateFlow<UiState<List<T>>> =
        combine(datos, repo.estadoSync) { lista, sync ->
            val resumen = (sync as? EstadoSync.Terminado)?.resumen
            val error = resumen?.errores?.let { e -> rutas.firstNotNullOfOrNull { e[it] } }
                ?: resumen?.motivoOmitida?.takeIf { it == MotivoOmision.SIN_RED }?.mensaje
            when {
                sync is EstadoSync.EnCurso && lista.isEmpty() -> UiState.Loading
                error != null -> UiState.Error(error, lista)
                lista.isEmpty() -> UiState.Empty
                else -> UiState.Success(lista)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun mensajeErrores(r: ResumenSync): String =
        "No se sincronizó: " + r.errores.entries.joinToString("; ") { (ruta, msg) -> "${ruta.etiqueta} ($msg)" }

    // ============================================================ Sincronización y modo offline

    fun sincronizar() {
        viewModelScope.launch { repo.sincronizarConServidor() }
    }

    fun cambiarModoOffline(activo: Boolean) {
        viewModelScope.launch { repo.cambiarModoOffline(activo) }
    }

    // ============================================================ Escrituras (Room primero, luego el servidor)

    private fun ejecutar(alTerminar: (Boolean) -> Unit, accion: suspend () -> Resultado) {
        viewModelScope.launch { alTerminar(accion() is Resultado.Ok) }
    }

    // el ganado
    fun guardarAnimal(animal: Animal, alTerminar: (Boolean) -> Unit) = ejecutar(alTerminar) { repo.guardarAnimal(animal) }

    fun eliminarAnimal(animal: Animal) {
        viewModelScope.launch { repo.eliminarAnimal(animal) }
    }

    // potreros
    fun guardarPotrero(potrero: Potrero, alTerminar: (Boolean) -> Unit = {}) =
        ejecutar(alTerminar) { repo.guardarPotrero(potrero) }

    /** Guarda la finca (requiere conexión) y la deja seleccionada. */
    fun guardarFinca(finca: Finca, alTerminar: (Finca?) -> Unit) {
        viewModelScope.launch {
            val (guardada, _) = repo.agregarFinca(finca)
            if (guardada != null) _fincaSeleccionadaId.value = guardada.id
            alTerminar(guardada)
        }
    }

    fun rotarGanado(origen: Potrero, destino: Potrero, motivo: MotivoRotacion, alTerminar: (String?) -> Unit) {
        viewModelScope.launch { alTerminar(repo.rotarGanado(origen, destino, motivo)) }
    }

    fun eliminarPotrero(potrero: Potrero) {
        viewModelScope.launch { repo.eliminarPotrero(potrero) }
    }

    // ordeño
    fun guardarOrdeno(registro: RegistroOrdeno, alTerminar: (Boolean) -> Unit) =
        ejecutar(alTerminar) { repo.guardarOrdeno(registro) }

    fun eliminarOrdeno(registro: RegistroOrdeno, alTerminar: (Boolean) -> Unit = {}) =
        ejecutar(alTerminar) { repo.eliminarOrdeno(registro) }

    // tareas
    fun guardarTarea(tarea: Tarea, alTerminar: (Boolean) -> Unit) = ejecutar(alTerminar) { repo.guardarTarea(tarea) }

    fun eliminarTarea(tarea: Tarea, alTerminar: (Boolean) -> Unit = {}) = ejecutar(alTerminar) { repo.eliminarTarea(tarea) }

    fun marcarTarea(tarea: Tarea, completada: Boolean) {
        viewModelScope.launch { repo.marcarTarea(tarea, completada) }
    }

    // alimentación y medicamentos
    fun guardarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit) =
        ejecutar(alTerminar) { repo.guardarAlimentacion(registro) }

    fun eliminarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit = {}) =
        ejecutar(alTerminar) { repo.eliminarAlimentacion(registro) }

    /** Finaliza el tratamiento/plan hoy (si tiene retiro de leche, el retiro empieza a contar desde hoy). */
    fun finalizarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit = {}) =
        guardarAlimentacion(registro.copy(activo = false, fechaFin = LocalDate.now()), alTerminar)

    fun reactivarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit = {}) =
        guardarAlimentacion(registro.copy(activo = true, fechaFin = null), alTerminar)
}

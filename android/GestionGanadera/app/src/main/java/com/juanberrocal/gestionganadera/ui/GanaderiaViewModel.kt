package com.juanberrocal.gestionganadera.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.Finca
import com.juanberrocal.gestionganadera.data.MotivoRotacion
import com.juanberrocal.gestionganadera.data.GanaderiaRepository
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.data.Tarea
import com.juanberrocal.gestionganadera.ui.ordeno.OrdenoLogica
import java.time.LocalDate

class GanaderiaViewModel(app: Application) : AndroidViewModel(app) {

    // Consultas desde el repositorio en memoria (preparado para Node.js / MongoDB backend)
    val animales: LiveData<List<Animal>> = GanaderiaRepository.animales
    // Solo bovinos: es lo que se muestra como "cabezas de ganado" (Inicio y Ganado)
    val totalAnimales: LiveData<Int> = GanaderiaRepository.animales.map { lista -> lista.count { it.esBovino } }

    /**
     * Conexión Ganado -> Ordeño: exactamente las vacas con
     * especie == BOVINO && sexo == HEMBRA && proposito == PRODUCCION_ORDENO (ver Animal.estaEnOrdeno).
     */
    val vacasEnOrdenoList: LiveData<List<Animal>> =
        GanaderiaRepository.animales.map { lista -> lista.filter { it.estaEnOrdeno }.sortedBy { it.codigo } }

    val potreros: LiveData<List<Potrero>> = GanaderiaRepository.potreros
    val totalPotreros: LiveData<Int> = GanaderiaRepository.potreros.map { it.size }

    val fincas: LiveData<List<Finca>> = GanaderiaRepository.fincas

    /** Finca elegida en la pantalla de Potreros; null = "Todas las fincas". */
    private val _fincaSeleccionadaId = MutableLiveData<String?>(null)
    val fincaSeleccionadaId: LiveData<String?> = _fincaSeleccionadaId

    fun seleccionarFinca(id: String?) {
        _fincaSeleccionadaId.value = id
    }

    val ordenos: LiveData<List<RegistroOrdeno>> = GanaderiaRepository.ordenos

    val tareas: LiveData<List<Tarea>> = GanaderiaRepository.tareas
    val tareasPendientes: LiveData<Int> = GanaderiaRepository.tareas.map { it.count { t -> !t.completada } }

    val alimentacion: LiveData<List<RegistroAlimentacionMedicamento>> = GanaderiaRepository.alimentacion

    /** Día consultado en la pantalla de Ordeño (se conserva al cambiar de pestaña). */
    private val _diaConsultado = MutableLiveData(LocalDate.now())
    val diaConsultado: LiveData<LocalDate> = _diaConsultado

    fun consultarDia(dia: LocalDate) {
        _diaConsultado.value = dia
    }

    /** Litros comerciales (tanque / venta) de hoy para Inicio: no cuenta descarte ni consumo de terneros. */
    private val _hoy = MutableLiveData(LocalDate.now())
    val litrosHoy: LiveData<Double> = _hoy.switchMap { dia ->
        GanaderiaRepository.ordenos.map { lista -> OrdenoLogica.litrosComerciales(lista, dia) }
    }

    fun actualizarHoy() {
        _hoy.value = LocalDate.now()
    }

    // el ganado
    fun guardarAnimal(animal: Animal, alTerminar: (Boolean) -> Unit) {
        if (GanaderiaRepository.existeCodigoAnimal(animal.codigo, animal.id)) {
            alTerminar(false)
            return
        }
        if (animal.id.isEmpty()) {
            GanaderiaRepository.agregarAnimal(animal) { exito ->
                alTerminar(exito)
            }
        } else {
            GanaderiaRepository.actualizarAnimal(animal) { exito ->
                alTerminar(exito)
            }
        }
    }

    fun eliminarAnimal(animal: Animal) {
        GanaderiaRepository.eliminarAnimal(animal)
    }

    // potreros
    fun guardarPotrero(potrero: Potrero, alTerminar: (Boolean) -> Unit = {}) {
        if (potrero.id == 0L) {
            GanaderiaRepository.agregarPotrero(potrero, alTerminar)
        } else {
            GanaderiaRepository.actualizarPotrero(potrero, alTerminar)
        }
    }

    /** Guarda la finca y la deja seleccionada. */
    fun guardarFinca(finca: Finca, alTerminar: (Finca?) -> Unit) {
        GanaderiaRepository.agregarFinca(finca) { guardada ->
            if (guardada != null) _fincaSeleccionadaId.value = guardada.id
            alTerminar(guardada)
        }
    }

    fun rotarGanado(origen: Potrero, destino: Potrero, motivo: MotivoRotacion, alTerminar: (String?) -> Unit) {
        GanaderiaRepository.rotarGanado(origen, destino, motivo, alTerminar)
    }

    fun eliminarPotrero(potrero: Potrero) {
        GanaderiaRepository.eliminarPotrero(potrero)
    }

    // ordeño
    fun guardarOrdeno(registro: RegistroOrdeno, alTerminar: (Boolean) -> Unit) =
        GanaderiaRepository.guardarOrdeno(registro, alTerminar)

    fun eliminarOrdeno(registro: RegistroOrdeno, alTerminar: (Boolean) -> Unit = {}) =
        GanaderiaRepository.eliminarOrdeno(registro, alTerminar)

    // tareas
    fun guardarTarea(tarea: Tarea, alTerminar: (Boolean) -> Unit) = GanaderiaRepository.guardarTarea(tarea, alTerminar)

    fun eliminarTarea(tarea: Tarea, alTerminar: (Boolean) -> Unit = {}) =
        GanaderiaRepository.eliminarTarea(tarea, alTerminar)

    fun marcarTarea(tarea: Tarea, completada: Boolean) = GanaderiaRepository.marcarTarea(tarea, completada)

    // alimentación y medicamentos
    fun guardarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit) =
        GanaderiaRepository.guardarAlimentacion(registro, alTerminar)

    fun eliminarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit = {}) =
        GanaderiaRepository.eliminarAlimentacion(registro, alTerminar)

    /** Finaliza el tratamiento/plan hoy (si tiene retiro de leche, el retiro empieza a contar desde hoy). */
    fun finalizarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit = {}) =
        GanaderiaRepository.guardarAlimentacion(registro.copy(activo = false, fechaFin = LocalDate.now()), alTerminar)

    fun reactivarAlimentacion(registro: RegistroAlimentacionMedicamento, alTerminar: (Boolean) -> Unit = {}) =
        GanaderiaRepository.guardarAlimentacion(registro.copy(activo = true, fechaFin = null), alTerminar)
}

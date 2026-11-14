package com.juanberrocal.gestionganadera.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.juanberrocal.gestionganadera.data.api.MapeoAnimal
import com.juanberrocal.gestionganadera.data.api.MapeoManejo
import com.juanberrocal.gestionganadera.data.api.MapeoPotrero
import com.juanberrocal.gestionganadera.data.api.RotacionRequest
import com.juanberrocal.gestionganadera.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

object GanaderiaRepository {

    private const val TAG = "GanaderiaRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _animales = MutableLiveData<List<Animal>>(emptyList())
    val animales: LiveData<List<Animal>> get() = _animales

    private val _potreros = MutableLiveData<List<Potrero>>(emptyList())
    val potreros: LiveData<List<Potrero>> get() = _potreros

    private val _fincas = MutableLiveData<List<Finca>>(emptyList())
    val fincas: LiveData<List<Finca>> get() = _fincas

    private val _ordenos = MutableLiveData<List<RegistroOrdeno>>(emptyList())
    val ordenos: LiveData<List<RegistroOrdeno>> get() = _ordenos

    private val _tareas = MutableLiveData<List<Tarea>>(emptyList())
    val tareas: LiveData<List<Tarea>> get() = _tareas

    private val _alimentacion = MutableLiveData<List<RegistroAlimentacionMedicamento>>(emptyList())
    val alimentacion: LiveData<List<RegistroAlimentacionMedicamento>> get() = _alimentacion

    init {

        sincronizarConServidor()
    }

    fun sincronizarConServidor(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                // 1. Cargar Ganado
                val respAnimales = RetrofitClient.apiService.getAnimales()
                if (respAnimales.isSuccessful && respAnimales.body()?.data != null) {
                    val lista = respAnimales.body()!!.data!!.mapNotNull { MapeoAnimal.aDominio(it) }
                    _animales.postValue(lista)
                }

                // 2. Cargar Fincas (en su propio try: un backend sin /fincas no debe frenar el resto)
                try {
                    val respFincas = RetrofitClient.apiService.getFincas()
                    if (respFincas.isSuccessful && respFincas.body()?.data != null) {
                        _fincas.postValue(respFincas.body()!!.data!!.mapNotNull { MapeoPotrero.fincaADominio(it) })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al cargar fincas: ${e.message}", e)
                }

                // 2b. Cargar Potreros
                val respPotreros = RetrofitClient.apiService.getPotreros()
                if (respPotreros.isSuccessful && respPotreros.body()?.data != null) {
                    val lista = respPotreros.body()!!.data!!.mapIndexed { index, dto ->
                        MapeoPotrero.aDominio(dto, idLocal = (index + 1).toLong())
                    }
                    _potreros.postValue(lista)
                }

                // 3. Cargar Ordeños
                val respOrdenos = RetrofitClient.apiService.getOrdenos()
                if (respOrdenos.isSuccessful && respOrdenos.body()?.data != null) {
                    _ordenos.postValue(respOrdenos.body()!!.data!!.mapNotNull { MapeoManejo.ordenoADominio(it) })
                }

                // 4. Cargar Tareas
                val respTareas = RetrofitClient.apiService.getTareas()
                if (respTareas.isSuccessful && respTareas.body()?.data != null) {
                    _tareas.postValue(respTareas.body()!!.data!!.mapNotNull { MapeoManejo.tareaADominio(it) })
                }

                // 5. Alimentación y medicamentos (try propio: un backend sin /alimentacion no frena el resto)
                try {
                    val resp = RetrofitClient.apiService.getAlimentacion()
                    if (resp.isSuccessful && resp.body()?.data != null) {
                        _alimentacion.postValue(resp.body()!!.data!!.mapNotNull { MapeoManejo.alimentacionADominio(it) })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al cargar alimentación: ${e.message}", e)
                }

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al sincronizar con el servidor: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
            }
        }
    }

    // --- Animales CRUD conectado a MongoDB ---
    fun agregarAnimal(animal: Animal, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.createAnimal(MapeoAnimal.aDto(animal, esNuevo = true))
                val guardado = resp.body()?.data?.takeIf { resp.isSuccessful }?.let { MapeoAnimal.aDominio(it) }
                if (guardado != null) {
                    _animales.postValue(listOf(guardado) + _animales.value.orEmpty())
                }
                withContext(Dispatchers.Main) { onComplete?.invoke(guardado != null) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear animal: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    fun actualizarAnimal(animal: Animal, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                if (animal.id.isEmpty()) return@launch withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                val resp = RetrofitClient.apiService.updateAnimal(animal.id, MapeoAnimal.aDto(animal, esNuevo = false))
                if (resp.isSuccessful) {
                    val actualizado = resp.body()?.data?.let { MapeoAnimal.aDominio(it) } ?: animal
                    _animales.postValue(_animales.value.orEmpty().map { if (it.id == animal.id) actualizado else it })
                }
                withContext(Dispatchers.Main) { onComplete?.invoke(resp.isSuccessful) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar animal: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    fun eliminarAnimal(animal: Animal) {
        scope.launch {
            try {
                if (animal.id.isNotEmpty()) RetrofitClient.apiService.deleteAnimal(animal.id)
                _animales.postValue(_animales.value.orEmpty().filterNot { it.id == animal.id })
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar animal: ${e.message}", e)
            }
        }
    }

    fun existeCodigoAnimal(codigo: String, excluirId: String = ""): Boolean =
        _animales.value.orEmpty().any { it.codigo.equals(codigo, ignoreCase = true) && it.id != excluirId }

    // --- Fincas ---
    fun agregarFinca(finca: Finca, onComplete: ((Finca?) -> Unit)? = null) {
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.createFinca(MapeoPotrero.fincaADto(finca))
                val guardada = resp.body()?.data?.takeIf { resp.isSuccessful }?.let { MapeoPotrero.fincaADominio(it) }
                if (guardada != null) {
                    _fincas.postValue((_fincas.value.orEmpty() + guardada).sortedBy { it.nombre.lowercase() })
                }
                withContext(Dispatchers.Main) { onComplete?.invoke(guardada) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear finca: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(null) }
            }
        }
    }

    // --- Potreros CRUD conectado a MongoDB ---
    fun agregarPotrero(potrero: Potrero, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.createPotrero(MapeoPotrero.aDto(potrero, esNuevo = true))
                if (resp.isSuccessful && resp.body()?.data != null) {
                    val nuevoPotrero = potrero.copy(
                        id = (System.currentTimeMillis() % 100000),
                        mongoId = resp.body()!!.data!!.id
                    )
                    _potreros.postValue(_potreros.value.orEmpty() + nuevoPotrero)
                    withContext(Dispatchers.Main) { onComplete?.invoke(true) }
                } else {
                    withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al agregar potrero: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    fun actualizarPotrero(potrero: Potrero, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                val mongoId = potrero.mongoId
                    ?: return@launch withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                val resp = RetrofitClient.apiService.updatePotrero(mongoId, MapeoPotrero.aDto(potrero, esNuevo = false))
                if (resp.isSuccessful) {
                    _potreros.postValue(_potreros.value.orEmpty().map { if (it.id == potrero.id) potrero else it })
                }
                withContext(Dispatchers.Main) { onComplete?.invoke(resp.isSuccessful) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar potrero: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    fun eliminarPotrero(potrero: Potrero) {
        scope.launch {
            try {
                potrero.mongoId?.let { RetrofitClient.apiService.deletePotrero(it) }
                val lista = _potreros.value.orEmpty().toMutableList()
                lista.removeAll { it.id == potrero.id || (potrero.mongoId != null && it.mongoId == potrero.mongoId) }
                _potreros.postValue(lista)
                // Los animales de ese potrero quedan "sin potrero" (el backend hace lo mismo en MongoDB)
                potrero.mongoId?.let { id ->
                    _animales.postValue(_animales.value.orEmpty().map {
                        if (it.potreroId == id) it.copy(potreroId = null) else it
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar potrero: ${e.message}", e)
            }
        }
    }

    /**
     * Mueve el ganado de [origen] (en ocupación) a [destino] (listo, misma finca).
     * El servidor valida las reglas y mueve también los animales asignados; luego se resincroniza.
     * @param onComplete null si salió bien, o el mensaje de error.
     */
    fun rotarGanado(origen: Potrero, destino: Potrero, motivo: MotivoRotacion, onComplete: (String?) -> Unit) {
        scope.launch {
            val error = try {
                val resp = RetrofitClient.apiService.rotarGanado(
                    RotacionRequest(
                        origenId = origen.mongoId ?: error("Potrero sin sincronizar"),
                        destinoId = destino.mongoId ?: error("Potrero sin sincronizar"),
                        motivo = motivo.etiqueta,
                        fecha = LocalDate.now().toString()
                    )
                )
                if (resp.isSuccessful) null
                else mensajeError(resp.errorBody()?.string()) ?: "El servidor rechazó la rotación"
            } catch (e: Exception) {
                Log.e(TAG, "Error al rotar ganado: ${e.message}", e)
                "Sin conexión con el servidor"
            }
            withContext(Dispatchers.Main) { onComplete(error) }
            if (error == null) sincronizarConServidor()
        }
    }

    private fun mensajeError(cuerpo: String?): String? =
        cuerpo?.let { Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.get(1) }

    // --- Ordeños CRUD conectado a MongoDB ---
    // Si el ordeño es de una vaca, su produccionLitrosDia = Mañana + Tarde del último día con registros.
    // Se recalcula aquí al instante y el backend hace lo mismo en MongoDB (luego se recargan los animales).

    private suspend fun main(bloque: () -> Unit) = withContext(Dispatchers.Main) { bloque() }

    /** Recarga solo los animales (p. ej. para ver la producción recalculada de una vaca). */
    private suspend fun refrescarAnimales() {
        try {
            val resp = RetrofitClient.apiService.getAnimales()
            resp.body()?.data?.takeIf { resp.isSuccessful }?.let { lista ->
                _animales.postValue(lista.mapNotNull { MapeoAnimal.aDominio(it) })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al recargar animales: ${e.message}", e)
        }
    }

    /** Misma regla que el backend: litros del último día con ordeños de esa vaca (todos los destinos). */
    private fun recalcularProduccionLocal(animalIds: Set<String?>, ordenos: List<RegistroOrdeno>) {
        val ids = animalIds.filterNotNull().toSet()
        if (ids.isEmpty()) return
        val animales = _animales.value.orEmpty().map { animal ->
            if (animal.id !in ids) return@map animal
            val propios = ordenos.filter { it.animalId == animal.id }
            val ultimoDia = propios.maxOfOrNull { it.fecha } ?: return@map animal
            animal.copy(produccionLitrosDia = propios.filter { it.fecha == ultimoDia }.sumOf { it.litros })
        }
        _animales.postValue(animales)
    }

    private fun ordenoADto(r: RegistroOrdeno) = _animales.value.orEmpty()
        .firstOrNull { it.id == r.animalId }
        .let { vaca -> MapeoManejo.ordenoADto(r, vaca?.codigo, vaca?.nombre) }

    /** Crea ([registro].id vacío) o actualiza un ordeño. */
    fun guardarOrdeno(registro: RegistroOrdeno, onComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                val esNuevo = registro.id.isEmpty()
                val anterior = _ordenos.value.orEmpty().firstOrNull { it.id == registro.id }
                val resp = if (esNuevo) RetrofitClient.apiService.createOrdeno(ordenoADto(registro))
                else RetrofitClient.apiService.updateOrdeno(registro.id, ordenoADto(registro))
                val guardado = resp.body()?.data?.takeIf { resp.isSuccessful }
                    ?.let { MapeoManejo.ordenoADominio(it) }
                    ?.copy(animalCodigoNombre = registro.animalCodigoNombre)
                if (guardado == null) return@launch main { onComplete(false) }

                val lista = if (esNuevo) listOf(guardado) + _ordenos.value.orEmpty()
                else _ordenos.value.orEmpty().map { if (it.id == guardado.id) guardado else it }
                _ordenos.postValue(lista)
                main { onComplete(true) }

                val afectadas = setOf(guardado.animalId, anterior?.animalId)
                if (afectadas.any { it != null }) {
                    recalcularProduccionLocal(afectadas, lista)
                    refrescarAnimales()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar ordeño: ${e.message}", e)
                main { onComplete(false) }
            }
        }
    }

    fun eliminarOrdeno(registro: RegistroOrdeno, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.deleteOrdeno(registro.id)
                if (!resp.isSuccessful) return@launch main { onComplete(false) }
                val lista = _ordenos.value.orEmpty().filterNot { it.id == registro.id }
                _ordenos.postValue(lista)
                main { onComplete(true) }
                if (registro.animalId != null) {
                    recalcularProduccionLocal(setOf(registro.animalId), lista)
                    refrescarAnimales()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar ordeño: ${e.message}", e)
                main { onComplete(false) }
            }
        }
    }

    // --- Tareas CRUD conectado a MongoDB ---

    fun guardarTarea(tarea: Tarea, onComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                val dto = MapeoManejo.tareaADto(tarea)
                val esNueva = tarea.id.isEmpty()
                val resp = if (esNueva) RetrofitClient.apiService.createTarea(dto)
                else RetrofitClient.apiService.updateTarea(tarea.id, dto)
                val guardada = resp.body()?.data?.takeIf { resp.isSuccessful }?.let { MapeoManejo.tareaADominio(it) }
                if (guardada == null) return@launch main { onComplete(false) }
                _tareas.postValue(
                    if (esNueva) _tareas.value.orEmpty() + guardada
                    else _tareas.value.orEmpty().map { if (it.id == guardada.id) guardada else it }
                )
                main { onComplete(true) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar tarea: ${e.message}", e)
                main { onComplete(false) }
            }
        }
    }

    /** Cambio optimista: se marca al instante y se revierte si el servidor falla. */
    fun marcarTarea(tarea: Tarea, completada: Boolean) {
        if (tarea.completada == completada) return
        fun aplicar(valor: Boolean) = _tareas.value.orEmpty().map { if (it.id == tarea.id) it.copy(completada = valor) else it }
        _tareas.value = aplicar(completada)
        scope.launch {
            val ok = try {
                // /toggle (y no PUT) para que el backend deje el registro en Auditoría, como antes
                RetrofitClient.apiService.toggleTarea(tarea.id).isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error al marcar tarea: ${e.message}", e)
                false
            }
            if (!ok) _tareas.postValue(aplicar(tarea.completada))
        }
    }

    fun eliminarTarea(tarea: Tarea, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.deleteTarea(tarea.id)
                if (resp.isSuccessful) _tareas.postValue(_tareas.value.orEmpty().filterNot { it.id == tarea.id })
                main { onComplete(resp.isSuccessful) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar tarea: ${e.message}", e)
                main { onComplete(false) }
            }
        }
    }

    // --- Alimentación y Medicamentos CRUD conectado a MongoDB ---

    fun guardarAlimentacion(registro: RegistroAlimentacionMedicamento, onComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                val esNuevo = registro.id.isEmpty()
                val dto = MapeoManejo.alimentacionADto(registro, esNuevo)
                val resp = if (esNuevo) RetrofitClient.apiService.createAlimentacion(dto)
                else RetrofitClient.apiService.updateAlimentacion(registro.id, dto)
                val guardado = resp.body()?.data?.takeIf { resp.isSuccessful }
                    ?.let { MapeoManejo.alimentacionADominio(it) }
                if (guardado == null) return@launch main { onComplete(false) }
                _alimentacion.postValue(
                    if (esNuevo) listOf(guardado) + _alimentacion.value.orEmpty()
                    else _alimentacion.value.orEmpty().map { if (it.id == guardado.id) guardado else it }
                )
                main { onComplete(true) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar alimentación/medicamento: ${e.message}", e)
                main { onComplete(false) }
            }
        }
    }

    fun eliminarAlimentacion(registro: RegistroAlimentacionMedicamento, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.deleteAlimentacion(registro.id)
                if (resp.isSuccessful) {
                    _alimentacion.postValue(_alimentacion.value.orEmpty().filterNot { it.id == registro.id })
                }
                main { onComplete(resp.isSuccessful) }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar alimentación/medicamento: ${e.message}", e)
                main { onComplete(false) }
            }
        }
    }
}

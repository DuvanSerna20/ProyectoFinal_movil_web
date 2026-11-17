package com.juanberrocal.gestionganadera.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.room.withTransaction
import com.juanberrocal.gestionganadera.data.api.AnimalDto
import com.juanberrocal.gestionganadera.data.api.ApiErrores
import com.juanberrocal.gestionganadera.data.api.ApiResponse
import com.juanberrocal.gestionganadera.data.api.ApiService
import com.juanberrocal.gestionganadera.data.api.MapeoAnimal
import com.juanberrocal.gestionganadera.data.api.MapeoManejo
import com.juanberrocal.gestionganadera.data.api.MapeoPotrero
import com.juanberrocal.gestionganadera.data.api.RetrofitClient
import com.juanberrocal.gestionganadera.data.api.RotacionRequest
import com.juanberrocal.gestionganadera.data.local.AnimalLocalEntity
import com.juanberrocal.gestionganadera.data.local.AppDatabase
import com.juanberrocal.gestionganadera.data.local.EntidadSincronizable
import com.juanberrocal.gestionganadera.data.local.MapeoLocal
import com.juanberrocal.gestionganadera.data.local.PreferenciasDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Rutas independientes de la API: el fallo de una no frena a las demás. */
enum class RutaSync(val etiqueta: String) {
    POTREROS("Potreros"),
    FINCAS("Fincas"),
    GANADO("Ganado"),
    ORDENOS("Ordeños"),
    TAREAS("Tareas"),
    ALIMENTACION("Alimentación")
}

/**
 * Resultado de una sincronización.
 * @param errores mensaje del primer error de cada ruta que falló (las demás sí se sincronizaron).
 * @param motivoOmitida si no se intentó sincronizar (modo offline, sin red, sin sesión).
 */
data class ResumenSync(
    val errores: Map<RutaSync, String> = emptyMap(),
    val motivoOmitida: MotivoOmision? = null
) {
    val exitosa: Boolean get() = motivoOmitida == null && errores.isEmpty()
}

enum class MotivoOmision(val mensaje: String, val corto: String) {
    MODO_OFFLINE("Modo sin conexión activado: los cambios se guardan solo en el dispositivo", "modo sin conexión activado"),
    SIN_RED("Sin conexión a internet: mostrando los datos guardados en el dispositivo", "sin internet"),
    SIN_SESION("Inicia sesión para sincronizar", "sesión no iniciada")
}

sealed interface EstadoSync {
    data object Inactivo : EstadoSync
    data object EnCurso : EstadoSync
    data class Terminado(val resumen: ResumenSync) : EstadoSync
}

/** Resultado de una escritura local. */
sealed interface Resultado {
    data object Ok : Resultado
    data class Error(val mensaje: String) : Resultado
}

/**
 * Repositorio Offline-First: Room es la ÚNICA fuente de verdad.
 *
 * - Lecturas: Flows de Room (los datos sobreviven a cerrar la app y a no tener red).
 * - Escrituras: primero en Room con `sincronizado = false`; luego se intenta enviar a Retrofit y,
 *   si el servidor responde bien, se guarda su `mongoId` y se marca `sincronizado = true`.
 * - Identidad: clave primaria UUID local estable + `mongoId` para la identidad del servidor.
 *   Las referencias entre entidades (potrero de un animal, vaca de un ordeño...) usan SIEMPRE el id
 *   local; se traducen a mongoId solo al hablar con la API.
 */
class GanaderiaRepository(
    context: Context,
    private val db: AppDatabase,
    private val preferencias: PreferenciasDataStore,
    private val api: () -> ApiService = { RetrofitClient.apiService }
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Evita enviar dos veces el mismo pendiente (p. ej. guardado + sincronización simultáneos). */
    private val mutexSubida = Mutex()
    private val mutexSync = Mutex()

    private val animalDao = db.animalDao()
    private val potreroDao = db.potreroDao()
    private val fincaDao = db.fincaDao()
    private val ordenoDao = db.ordenoDao()
    private val tareaDao = db.tareaDao()
    private val alimentacionDao = db.alimentacionDao()

    // ============================================================================ Lecturas (Room)

    val animales: Flow<List<Animal>> = animalDao.obtenerTodos().map { l -> l.map { MapeoLocal.aDominio(it) } }
    val potreros: Flow<List<Potrero>> = potreroDao.obtenerTodos().map { l -> l.map { MapeoLocal.aDominio(it) } }
    val fincas: Flow<List<Finca>> = fincaDao.obtenerTodos().map { l -> l.map { MapeoLocal.aDominio(it) } }
    val ordenos: Flow<List<RegistroOrdeno>> = ordenoDao.obtenerTodos().map { l -> l.map { MapeoLocal.aDominio(it) } }
    val tareas: Flow<List<Tarea>> = tareaDao.obtenerTodos().map { l -> l.map { MapeoLocal.aDominio(it) } }
    val alimentacion: Flow<List<RegistroAlimentacionMedicamento>> =
        alimentacionDao.obtenerTodos().map { l -> l.map { MapeoLocal.aDominio(it) } }

    /** Total de cambios locales que aún no llegan al servidor. */
    val pendientesPorSincronizar: Flow<Int> = combine(
        animalDao.contarPendientes(),
        potreroDao.contarPendientes(),
        ordenoDao.contarPendientes(),
        tareaDao.contarPendientes(),
        alimentacionDao.contarPendientes()
    ) { a, p, o, t, al -> a + p + o + t + al }

    val modoOffline: Flow<Boolean> = preferencias.modoOfflinePreferido

    private val _estadoSync = MutableStateFlow<EstadoSync>(EstadoSync.Inactivo)
    val estadoSync: StateFlow<EstadoSync> = _estadoSync.asStateFlow()

    init {
        // Al recuperar la red se envían los pendientes (salvo que el usuario prefiera el modo offline).
        runCatching {
            appContext.getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        scope.launch { sincronizarConServidor() }
                    }
                })
        }.onFailure { Log.w(TAG, "No se pudo observar la conectividad", it) }
    }

    // ============================================================================ Red y preferencias

    fun hayRed(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun motivoParaNoSincronizar(): MotivoOmision? = when {
        !preferencias.sesion.first().activa -> MotivoOmision.SIN_SESION
        preferencias.modoOfflinePreferido.first() -> MotivoOmision.MODO_OFFLINE
        !hayRed() -> MotivoOmision.SIN_RED
        else -> null
    }

    suspend fun cambiarModoOffline(activo: Boolean) {
        preferencias.setModoOfflinePreferido(activo)
        if (!activo) sincronizarConServidor()
    }

    // ============================================================================ Sincronización

    /**
     * 1) Sube los pendientes locales (en orden de dependencias: potreros -> animales -> el resto).
     * 2) Descarga cada ruta y la fusiona en Room.
     * Cada ruta corre aislada (supervisorScope + captura propia): si /fincas falla, ganado, potreros,
     * ordeños y tareas se sincronizan igual y el error queda reportado solo para esa ruta.
     */
    suspend fun sincronizarConServidor(): ResumenSync = withContext(Dispatchers.IO) {
        if (mutexSync.isLocked) {
            // Ya hay una en curso: se espera a que termine y se devuelve su resultado
            mutexSync.withLock { }
            return@withContext (estadoSync.value as? EstadoSync.Terminado)?.resumen ?: ResumenSync()
        }
        mutexSync.withLock {
            motivoParaNoSincronizar()?.let { motivo ->
                return@withLock ResumenSync(motivoOmitida = motivo).also { _estadoSync.value = EstadoSync.Terminado(it) }
            }
            _estadoSync.value = EstadoSync.EnCurso
            val errores = ConcurrentHashMap<RutaSync, String>()

            mutexSubida.withLock { subirPendientes(errores) }

            supervisorScope {
                listOf(
                    async { aislar(RutaSync.POTREROS, errores) { descargarPotreros() } },
                    async { aislar(RutaSync.FINCAS, errores) { descargarFincas() } }
                ).awaitAll()
                // Los animales necesitan los potreros ya mapeados (id local <-> mongoId)
                aislar(RutaSync.GANADO, errores) { descargarAnimales() }
                // Ordeños, tareas y alimentación necesitan los animales ya mapeados
                listOf(
                    async { aislar(RutaSync.ORDENOS, errores) { descargarOrdenos() } },
                    async { aislar(RutaSync.TAREAS, errores) { descargarTareas() } },
                    async { aislar(RutaSync.ALIMENTACION, errores) { descargarAlimentacion() } }
                ).awaitAll()
            }

            ResumenSync(errores = errores.toMap()).also {
                if (it.errores.isNotEmpty()) Log.w(TAG, "Sincronización parcial: ${it.errores}")
                _estadoSync.value = EstadoSync.Terminado(it)
            }
        }
    }

    /** Ejecuta una ruta capturando su error sin propagarlo a las demás. */
    private suspend fun aislar(ruta: RutaSync, errores: MutableMap<RutaSync, String>, bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error en la ruta ${ruta.etiqueta}: ${e.message}", e)
            errores.putIfAbsent(ruta, mensajeDe(e))
        }
    }

    /** Envía en segundo plano lo pendiente (tras cada escritura local). */
    private fun programarSubida() {
        scope.launch {
            if (motivoParaNoSincronizar() != null) return@launch
            val errores = ConcurrentHashMap<RutaSync, String>()
            mutexSubida.withLock { subirPendientes(errores) }
            if (errores.isNotEmpty()) Log.w(TAG, "Pendientes sin enviar: $errores")
        }
    }

    // ---------------------------------------------------------------------------- Subida

    private suspend fun subirPendientes(errores: MutableMap<RutaSync, String>) {
        aislar(RutaSync.POTREROS, errores) {
            subirRuta(
                RutaSync.POTREROS, errores, potreroDao.obtenerPendientes(),
                enviar = { e ->
                    val dto = MapeoPotrero.aDto(MapeoLocal.aDominio(e), esNuevo = e.mongoId == null)
                    e.mongoId?.let { api().updatePotrero(it, dto) } ?: api().createPotrero(dto)
                },
                idDe = { it.id },
                eliminarRemoto = { api().deletePotrero(it) },
                confirmar = { e, mongoId, _ ->
                    potreroDao.asignarMongoId(e.id, mongoId)
                    potreroDao.marcarSincronizado(e.id, e.actualizadoEn)
                },
                eliminarLocal = { potreroDao.eliminarPorId(it) },
                olvidarMongoId = { potreroDao.asignarMongoId(it, null) }
            )
        }
        aislar(RutaSync.GANADO, errores) {
            subirRuta(
                RutaSync.GANADO, errores, animalDao.obtenerPendientes(),
                enviar = { e -> enviarAnimal(e) },
                idDe = { it.id },
                eliminarRemoto = { api().deleteAnimal(it) },
                confirmar = { e, mongoId, dto ->
                    animalDao.asignarMongoId(e.id, mongoId)
                    animalDao.marcarSincronizado(e.id, e.actualizadoEn, dto?.fotoUrl?.ifBlank { null })
                },
                eliminarLocal = { id ->
                    animalDao.obtenerPorId(id)?.fotoLocal?.let { borrarFotoInterna(it) }
                    animalDao.eliminarPorId(id)
                },
                olvidarMongoId = { animalDao.asignarMongoId(it, null) }
            )
        }
        aislar(RutaSync.ORDENOS, errores) {
            subirRuta(
                RutaSync.ORDENOS, errores, ordenoDao.obtenerPendientes(),
                enviar = { e ->
                    val vaca = resolverAnimal(e.animalId) ?: return@subirRuta null
                    val dto = MapeoManejo.ordenoADto(
                        MapeoLocal.aDominio(e).copy(animalId = vaca.mongoId),
                        vaca.entidad?.codigo, vaca.entidad?.nombre
                    )
                    e.mongoId?.let { api().updateOrdeno(it, dto) } ?: api().createOrdeno(dto)
                },
                idDe = { it.id },
                eliminarRemoto = { api().deleteOrdeno(it) },
                confirmar = { e, mongoId, _ ->
                    ordenoDao.asignarMongoId(e.id, mongoId)
                    ordenoDao.marcarSincronizado(e.id, e.actualizadoEn)
                },
                eliminarLocal = { ordenoDao.eliminarPorId(it) },
                olvidarMongoId = { ordenoDao.asignarMongoId(it, null) }
            )
        }
        aislar(RutaSync.TAREAS, errores) {
            subirRuta(
                RutaSync.TAREAS, errores, tareaDao.obtenerPendientes(),
                enviar = { e ->
                    val animal = resolverAnimal(e.animalId) ?: return@subirRuta null
                    val dto = MapeoManejo.tareaADto(MapeoLocal.aDominio(e).copy(animalId = animal.mongoId))
                    e.mongoId?.let { api().updateTarea(it, dto) } ?: api().createTarea(dto)
                },
                idDe = { it.id },
                eliminarRemoto = { api().deleteTarea(it) },
                confirmar = { e, mongoId, _ ->
                    tareaDao.asignarMongoId(e.id, mongoId)
                    tareaDao.marcarSincronizado(e.id, e.actualizadoEn)
                },
                eliminarLocal = { tareaDao.eliminarPorId(it) },
                olvidarMongoId = { tareaDao.asignarMongoId(it, null) }
            )
        }
        aislar(RutaSync.ALIMENTACION, errores) {
            subirRuta(
                RutaSync.ALIMENTACION, errores, alimentacionDao.obtenerPendientes(),
                enviar = { e ->
                    val animal = resolverAnimal(e.animalId) ?: return@subirRuta null
                    val dto = MapeoManejo.alimentacionADto(
                        MapeoLocal.aDominio(e).copy(animalId = animal.mongoId),
                        esNuevo = e.mongoId == null
                    )
                    e.mongoId?.let { api().updateAlimentacion(it, dto) } ?: api().createAlimentacion(dto)
                },
                idDe = { it.id },
                eliminarRemoto = { api().deleteAlimentacion(it) },
                confirmar = { e, mongoId, _ ->
                    alimentacionDao.asignarMongoId(e.id, mongoId)
                    alimentacionDao.marcarSincronizado(e.id, e.actualizadoEn)
                },
                eliminarLocal = { alimentacionDao.eliminarPorId(it) },
                olvidarMongoId = { alimentacionDao.asignarMongoId(it, null) }
            )
        }
    }

    /**
     * Envía cada pendiente de una ruta. Un registro que falla no detiene a los demás.
     * @param enviar POST (sin mongoId) o PUT; devuelve null si depende de algo aún no sincronizado.
     */
    private suspend fun <E : EntidadSincronizable, D> subirRuta(
        ruta: RutaSync,
        errores: MutableMap<RutaSync, String>,
        pendientes: List<E>,
        enviar: suspend (E) -> Response<ApiResponse<D>>?,
        idDe: (D) -> String?,
        eliminarRemoto: suspend (String) -> Response<ApiResponse<Any>>,
        confirmar: suspend (E, mongoId: String, D?) -> Unit,
        eliminarLocal: suspend (String) -> Unit,
        olvidarMongoId: suspend (String) -> Unit
    ) {
        for (e in pendientes) {
            try {
                if (e.pendienteEliminar) {
                    val mongoId = e.mongoId
                    if (mongoId == null) {
                        eliminarLocal(e.id)
                    } else {
                        val resp = eliminarRemoto(mongoId)
                        // 404 = ya no existe en el servidor: el borrado está cumplido
                        if (resp.isSuccessful || resp.code() == 404) eliminarLocal(e.id)
                        else errores.putIfAbsent(ruta, mensajeHttp(resp))
                    }
                    continue
                }
                val resp = enviar(e) ?: continue
                val datos = resp.body()?.data
                val mongoId = datos?.let(idDe) ?: e.mongoId
                when {
                    resp.isSuccessful && mongoId != null -> confirmar(e, mongoId, datos)
                    resp.isSuccessful -> errores.putIfAbsent(ruta, "El servidor no devolvió el identificador del registro")
                    // Se borró en el servidor mientras aquí se editaba: se volverá a crear en el próximo envío
                    resp.code() == 404 && e.mongoId != null -> olvidarMongoId(e.id)
                    else -> errores.putIfAbsent(ruta, mensajeHttp(resp))
                }
            } catch (c: CancellationException) {
                throw c
            } catch (ex: Exception) {
                Log.e(TAG, "Error al subir ${ruta.etiqueta} ${e.id}: ${ex.message}", ex)
                errores.putIfAbsent(ruta, mensajeDe(ex))
            }
        }
    }

    /** Animal referido, ya traducido a mongoId. null = el animal aún no existe en el servidor (esperar). */
    private class RefAnimal(val mongoId: String?, val entidad: AnimalLocalEntity?)

    private suspend fun resolverAnimal(idLocal: String?): RefAnimal? {
        if (idLocal == null) return RefAnimal(null, null)
        val local = animalDao.obtenerPorId(idLocal)
            // Referencia a un animal que no está en Room (p. ej. borrado): se envía tal cual
            ?: return RefAnimal(idLocal, null)
        return local.mongoId?.let { RefAnimal(it, local) }
    }

    private suspend fun enviarAnimal(e: AnimalLocalEntity): Response<ApiResponse<AnimalDto>>? {
        val potreroMongoId: String? = e.potreroId?.let { idLocal ->
            val potrero = potreroDao.obtenerPorId(idLocal) ?: return@let idLocal // sin mapear: se envía tal cual
            potrero.mongoId ?: return null // el potrero aún no está en el servidor
        }
        val animal = MapeoLocal.aDominio(e).copy(potreroId = potreroMongoId)
        val dto = MapeoAnimal.aDto(animal, esNuevo = e.mongoId == null)
        val foto = if (e.fotoPendiente) e.fotoLocal?.let { uriAMultipart(Uri.fromFile(File(it))) } else null

        val mongoId = e.mongoId
        return if (mongoId == null) {
            api().createAnimal(
                codigo = texto(dto.codigo)!!, nombre = texto(dto.nombre)!!, raza = texto(dto.raza)!!, sexo = texto(dto.sexo)!!,
                especie = texto(dto.especie), proposito = texto(dto.proposito),
                estadoReproductivo = texto(dto.estadoReproductivo), estado = texto(dto.estado),
                pesoKg = texto(dto.pesoKg), produccionLitrosDia = texto(dto.produccionLitrosDia),
                fincaId = texto(dto.fincaId), potreroId = texto(dto.potreroId),
                registradoPor = texto(dto.registradoPor), foto = foto
            )
        } else {
            api().updateAnimal(
                id = mongoId,
                codigo = texto(dto.codigo)!!, nombre = texto(dto.nombre)!!, raza = texto(dto.raza)!!, sexo = texto(dto.sexo)!!,
                especie = texto(dto.especie), proposito = texto(dto.proposito),
                estadoReproductivo = texto(dto.estadoReproductivo), estado = texto(dto.estado),
                pesoKg = texto(dto.pesoKg), produccionLitrosDia = texto(dto.produccionLitrosDia),
                fincaId = texto(dto.fincaId), potreroId = texto(dto.potreroId),
                registradoPor = texto(dto.registradoPor), foto = foto
            )
        }
    }

    // ---------------------------------------------------------------------------- Descarga

    /** Cuerpo de una respuesta exitosa o excepción con el mensaje del servidor. */
    private fun <T> datosDe(resp: Response<ApiResponse<T>>): T {
        if (!resp.isSuccessful) throw ErrorApi(mensajeHttp(resp))
        return resp.body()?.data ?: throw ErrorApi("Respuesta vacía del servidor")
    }

    /** El servidor respondió, pero con error (a diferencia de una IOException de red). */
    private class ErrorApi(mensaje: String) : Exception(mensaje)

    private suspend fun descargarPotreros() {
        val lista = datosDe(api().getPotreros())
        db.withTransaction {
            val locales = potreroDao.obtenerTodosLista()
            val porMongo = locales.filter { it.mongoId != null }.associateBy { it.mongoId!! }
            val nuevos = lista.mapNotNull { dto ->
                val mongoId = dto.id ?: return@mapNotNull null
                val local = porMongo[mongoId]
                if (local != null && !local.sincronizado) return@mapNotNull null // gana el cambio local pendiente
                val id = local?.id ?: UUID.randomUUID().toString()
                MapeoLocal.aEntidad(MapeoPotrero.aDominio(dto, idLocal = id), id, mongoId, sincronizado = true)
            }
            potreroDao.upsertTodos(nuevos)
            borrarAusentes(locales, lista.mapNotNull { it.id }.toSet()) { potreroDao.eliminarPorIds(it) }
        }
    }

    private suspend fun descargarFincas() {
        val lista = datosDe(api().getFincas())
        fincaDao.reemplazarTodas(lista.mapNotNull { MapeoPotrero.fincaADominio(it) }.map { MapeoLocal.aEntidad(it) })
    }

    private suspend fun descargarAnimales() {
        val lista = datosDe(api().getAnimales())
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            val potrerosLocales = potreroDao.pares().associate { it.mongoId to it.id }
            val locales = animalDao.obtenerTodosLista()
            val porMongo = locales.filter { it.mongoId != null }.associateBy { it.mongoId!! }
            val nuevos = lista.mapIndexedNotNull { i, dto ->
                val remoto = MapeoAnimal.aDominio(dto) ?: return@mapIndexedNotNull null
                val local = porMongo[remoto.mongoId]
                if (local != null && !local.sincronizado) return@mapIndexedNotNull null
                val id = local?.id ?: UUID.randomUUID().toString()
                val animal = remoto.copy(
                    id = id,
                    potreroId = remoto.potreroId?.let { potrerosLocales[it] ?: it },
                    // La copia local de la foto sirve mientras sea la misma que tiene el servidor
                    fotoLocal = local?.fotoLocal?.takeIf { local.fotoUrl == remoto.fotoUrl }
                )
                // El servidor devuelve primero los más recientes: se conserva ese orden
                MapeoLocal.aEntidad(animal, id, remoto.mongoId, sincronizado = true, creadoEn = local?.creadoEn ?: (ahora - i))
            }
            animalDao.upsertTodos(nuevos)
            borrarAusentes(locales, lista.mapNotNull { it.id }.toSet()) { animalDao.eliminarPorIds(it) }
        }
    }

    private suspend fun descargarOrdenos() {
        val lista = datosDe(api().getOrdenos())
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            val animalesLocales = animalDao.pares().associate { it.mongoId to it.id }
            val locales = ordenoDao.obtenerTodosLista()
            val porMongo = locales.filter { it.mongoId != null }.associateBy { it.mongoId!! }
            val nuevos = lista.mapIndexedNotNull { i, dto ->
                val remoto = MapeoManejo.ordenoADominio(dto) ?: return@mapIndexedNotNull null
                val local = porMongo[remoto.id]
                if (local != null && !local.sincronizado) return@mapIndexedNotNull null
                val id = local?.id ?: UUID.randomUUID().toString()
                val registro = remoto.copy(animalId = remoto.animalId?.let { animalesLocales[it] ?: it })
                MapeoLocal.aEntidad(registro, id, remoto.id, sincronizado = true, creadoEn = local?.creadoEn ?: (ahora - i))
            }
            ordenoDao.upsertTodos(nuevos)
            borrarAusentes(locales, lista.mapNotNull { it.id }.toSet()) { ordenoDao.eliminarPorIds(it) }
        }
    }

    private suspend fun descargarTareas() {
        val lista = datosDe(api().getTareas())
        db.withTransaction {
            val animalesLocales = animalDao.pares().associate { it.mongoId to it.id }
            val locales = tareaDao.obtenerTodosLista()
            val porMongo = locales.filter { it.mongoId != null }.associateBy { it.mongoId!! }
            val nuevos = lista.mapNotNull { dto ->
                val remoto = MapeoManejo.tareaADominio(dto) ?: return@mapNotNull null
                val local = porMongo[remoto.id]
                if (local != null && !local.sincronizado) return@mapNotNull null
                val id = local?.id ?: UUID.randomUUID().toString()
                val tarea = remoto.copy(animalId = remoto.animalId?.let { animalesLocales[it] ?: it })
                MapeoLocal.aEntidad(tarea, id, remoto.id, sincronizado = true)
            }
            tareaDao.upsertTodos(nuevos)
            borrarAusentes(locales, lista.mapNotNull { it.id }.toSet()) { tareaDao.eliminarPorIds(it) }
        }
    }

    private suspend fun descargarAlimentacion() {
        val lista = datosDe(api().getAlimentacion())
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            val animalesLocales = animalDao.pares().associate { it.mongoId to it.id }
            val locales = alimentacionDao.obtenerTodosLista()
            val porMongo = locales.filter { it.mongoId != null }.associateBy { it.mongoId!! }
            val nuevos = lista.mapIndexedNotNull { i, dto ->
                val remoto = MapeoManejo.alimentacionADominio(dto) ?: return@mapIndexedNotNull null
                val local = porMongo[remoto.id]
                if (local != null && !local.sincronizado) return@mapIndexedNotNull null
                val id = local?.id ?: UUID.randomUUID().toString()
                val registro = remoto.copy(animalId = remoto.animalId?.let { animalesLocales[it] ?: it })
                MapeoLocal.aEntidad(registro, id, remoto.id, sincronizado = true, creadoEn = local?.creadoEn ?: (ahora - i))
            }
            alimentacionDao.upsertTodos(nuevos)
            borrarAusentes(locales, lista.mapNotNull { it.id }.toSet()) { alimentacionDao.eliminarPorIds(it) }
        }
    }

    /** Borra lo que estaba sincronizado y ya no existe en el servidor (eliminado desde la web). */
    private suspend fun borrarAusentes(
        locales: List<EntidadSincronizable>,
        idsServidor: Set<String>,
        eliminar: suspend (List<String>) -> Unit
    ) {
        locales
            .filter { it.sincronizado && it.mongoId != null && it.mongoId !in idsServidor }
            .map { it.id }
            .chunked(500)
            .forEach { eliminar(it) }
    }

    // ============================================================================ Ganado

    /**
     * Guarda en Room y luego intenta enviarlo. Si [animal].fotoLocal es una Uri recién elegida
     * (content:// o file://), se copia comprimida al almacenamiento interno para poder subirla
     * más tarde aunque ahora no haya red.
     */
    suspend fun guardarAnimal(animal: Animal): Resultado = withContext(Dispatchers.IO) {
        val esNuevo = animal.id.isEmpty()
        val id = if (esNuevo) UUID.randomUUID().toString() else animal.id
        if (animalDao.existeCodigo(animal.codigo, excluirId = id)) {
            return@withContext Resultado.Error("Ya existe un animal con el código ${animal.codigo}")
        }
        val anterior = if (esNuevo) null else animalDao.obtenerPorId(id)

        val fotoNueva = animal.fotoLocal?.takeIf { it != anterior?.fotoLocal && esUri(it) }
        val rutaFoto = fotoNueva?.let {
            copiarFotoInterna(Uri.parse(it), id) ?: return@withContext Resultado.Error("No se pudo leer la imagen seleccionada")
        }
        if (rutaFoto != null) anterior?.fotoLocal?.let { borrarFotoInterna(it) }

        val entidad = MapeoLocal.aEntidad(
            animal.copy(id = id, fotoLocal = rutaFoto ?: anterior?.fotoLocal, fotoUrl = anterior?.fotoUrl ?: animal.fotoUrl),
            id = id,
            mongoId = anterior?.mongoId,
            sincronizado = false,
            creadoEn = anterior?.creadoEn ?: System.currentTimeMillis(),
            fotoPendiente = rutaFoto != null || (anterior?.fotoPendiente ?: false)
        )
        animalDao.upsert(entidad)
        programarSubida()
        Resultado.Ok
    }

    suspend fun eliminarAnimal(animal: Animal) = withContext(Dispatchers.IO) {
        val local = animalDao.obtenerPorId(animal.id) ?: return@withContext
        if (local.mongoId == null) {
            local.fotoLocal?.let { borrarFotoInterna(it) }
            animalDao.eliminarPorId(local.id)
        } else {
            animalDao.marcarParaEliminar(local.id)
            programarSubida()
        }
    }

    // ============================================================================ Fincas

    /** Las fincas se crean en el servidor (su id es el _id de MongoDB); luego quedan en caché en Room. */
    suspend fun agregarFinca(finca: Finca): Pair<Finca?, String?> = withContext(Dispatchers.IO) {
        motivoParaNoSincronizar()?.let { return@withContext null to "Crear una finca requiere conexión con el servidor (${it.corto})" }
        try {
            val resp = api().createFinca(MapeoPotrero.fincaADto(finca))
            val guardada = resp.body()?.data?.takeIf { resp.isSuccessful }?.let { MapeoPotrero.fincaADominio(it) }
                ?: return@withContext null to mensajeHttp(resp)
            fincaDao.upsert(MapeoLocal.aEntidad(guardada))
            guardada to null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear finca: ${e.message}", e)
            null to mensajeDe(e)
        }
    }

    // ============================================================================ Potreros

    suspend fun guardarPotrero(potrero: Potrero): Resultado = withContext(Dispatchers.IO) {
        val id = potrero.id.ifEmpty { UUID.randomUUID().toString() }
        val anterior = potreroDao.obtenerPorId(id)
        potreroDao.upsert(MapeoLocal.aEntidad(potrero, id, anterior?.mongoId, sincronizado = false))
        programarSubida()
        Resultado.Ok
    }

    suspend fun eliminarPotrero(potrero: Potrero) = withContext(Dispatchers.IO) {
        val local = potreroDao.obtenerPorId(potrero.id) ?: return@withContext
        db.withTransaction {
            // Sus animales quedan "sin potrero" (el backend hace lo mismo en MongoDB)
            animalDao.quitarPotrero(local.id)
            if (local.mongoId == null) potreroDao.eliminarPorId(local.id) else potreroDao.marcarParaEliminar(local.id)
        }
        if (local.mongoId != null) programarSubida()
    }

    /**
     * Mueve el ganado de [origen] (en ocupación) a [destino] (listo, misma finca). Lo valida el servidor,
     * así que necesita conexión. @return null si salió bien, o el mensaje de error.
     */
    suspend fun rotarGanado(origen: Potrero, destino: Potrero, motivo: MotivoRotacion): String? =
        withContext(Dispatchers.IO) {
            motivoParaNoSincronizar()?.let { return@withContext "La rotación requiere conexión con el servidor (${it.corto})" }
            val origenId = origen.mongoId ?: return@withContext "El potrero de origen aún no se ha sincronizado"
            val destinoId = destino.mongoId ?: return@withContext "El potrero de destino aún no se ha sincronizado"
            val error = try {
                val resp = api().rotarGanado(
                    RotacionRequest(origenId, destinoId, motivo.etiqueta, LocalDate.now().toString())
                )
                if (resp.isSuccessful) null else ApiErrores.mensaje(resp) ?: "El servidor rechazó la rotación"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al rotar ganado: ${e.message}", e)
                mensajeDe(e)
            }
            if (error == null) scope.launch { sincronizarConServidor() }
            error
        }

    // ============================================================================ Ordeños

    /** Crea ([registro].id vacío) o actualiza un ordeño y recalcula la producción de la vaca. */
    suspend fun guardarOrdeno(registro: RegistroOrdeno): Resultado = withContext(Dispatchers.IO) {
        val id = registro.id.ifEmpty { UUID.randomUUID().toString() }
        val anterior = ordenoDao.obtenerPorId(id)
        ordenoDao.upsert(
            MapeoLocal.aEntidad(registro, id, anterior?.mongoId, sincronizado = false, creadoEn = anterior?.creadoEn ?: System.currentTimeMillis())
        )
        recalcularProduccion(setOf(registro.animalId, anterior?.animalId))
        programarSubida()
        Resultado.Ok
    }

    suspend fun eliminarOrdeno(registro: RegistroOrdeno): Resultado = withContext(Dispatchers.IO) {
        val local = ordenoDao.obtenerPorId(registro.id) ?: return@withContext Resultado.Error("El ordeño ya no existe")
        if (local.mongoId == null) ordenoDao.eliminarPorId(local.id) else ordenoDao.marcarParaEliminar(local.id)
        recalcularProduccion(setOf(local.animalId))
        if (local.mongoId != null) programarSubida()
        Resultado.Ok
    }

    /** Misma regla que el backend: litros del último día con ordeños de esa vaca (todos los destinos). */
    private suspend fun recalcularProduccion(animalIds: Set<String?>) {
        animalIds.filterNotNull().forEach { id ->
            val propios = ordenoDao.obtenerPorAnimal(id)
            val ultimoDia = propios.maxOfOrNull { it.fecha } ?: return@forEach // ISO-8601: el orden de texto es cronológico
            animalDao.actualizarProduccion(id, propios.filter { it.fecha == ultimoDia }.sumOf { it.litros })
        }
    }

    // ============================================================================ Tareas

    suspend fun guardarTarea(tarea: Tarea): Resultado = withContext(Dispatchers.IO) {
        val id = tarea.id.ifEmpty { UUID.randomUUID().toString() }
        val anterior = tareaDao.obtenerPorId(id)
        tareaDao.upsert(MapeoLocal.aEntidad(tarea, id, anterior?.mongoId, sincronizado = false))
        programarSubida()
        Resultado.Ok
    }

    /**
     * Se marca al instante en Room. Si la tarea no tenía otros cambios pendientes se usa /toggle
     * (para que el backend lo deje en Auditoría); si no, viaja en el PUT normal de la cola.
     */
    suspend fun marcarTarea(tarea: Tarea, completada: Boolean) = withContext(Dispatchers.IO) {
        if (tarea.completada == completada) return@withContext
        val anterior = tareaDao.obtenerPorId(tarea.id) ?: return@withContext
        val nueva = MapeoLocal.aEntidad(
            MapeoLocal.aDominio(anterior).copy(completada = completada), anterior.id, anterior.mongoId, sincronizado = false
        )
        tareaDao.upsert(nueva)
        val mongoId = anterior.mongoId
        if (!anterior.sincronizado || mongoId == null) {
            programarSubida()
            return@withContext
        }
        scope.launch {
            if (motivoParaNoSincronizar() != null) return@launch
            mutexSubida.withLock {
                val actual = tareaDao.obtenerPorId(nueva.id)
                if (actual == null || actual.sincronizado || actual.actualizadoEn != nueva.actualizadoEn) return@withLock
                val ok = try {
                    val resp = api().toggleTarea(mongoId)
                    resp.isSuccessful && resp.body()?.data?.completada == completada
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error al marcar tarea: ${e.message}", e)
                    false
                }
                // Si /toggle falla, queda pendiente y se enviará con PUT en la próxima sincronización
                if (ok) tareaDao.marcarSincronizado(nueva.id, nueva.actualizadoEn)
            }
        }
    }

    suspend fun eliminarTarea(tarea: Tarea): Resultado = withContext(Dispatchers.IO) {
        val local = tareaDao.obtenerPorId(tarea.id) ?: return@withContext Resultado.Error("La tarea ya no existe")
        if (local.mongoId == null) tareaDao.eliminarPorId(local.id) else {
            tareaDao.marcarParaEliminar(local.id)
            programarSubida()
        }
        Resultado.Ok
    }

    // ============================================================================ Alimentación y medicamentos

    suspend fun guardarAlimentacion(registro: RegistroAlimentacionMedicamento): Resultado = withContext(Dispatchers.IO) {
        val id = registro.id.ifEmpty { UUID.randomUUID().toString() }
        val anterior = alimentacionDao.obtenerPorId(id)
        alimentacionDao.upsert(
            MapeoLocal.aEntidad(registro, id, anterior?.mongoId, sincronizado = false, creadoEn = anterior?.creadoEn ?: System.currentTimeMillis())
        )
        programarSubida()
        Resultado.Ok
    }

    suspend fun eliminarAlimentacion(registro: RegistroAlimentacionMedicamento): Resultado = withContext(Dispatchers.IO) {
        val local = alimentacionDao.obtenerPorId(registro.id) ?: return@withContext Resultado.Error("El registro ya no existe")
        if (local.mongoId == null) alimentacionDao.eliminarPorId(local.id) else {
            alimentacionDao.marcarParaEliminar(local.id)
            programarSubida()
        }
        Resultado.Ok
    }

    // ============================================================================ Imágenes (multipart)

    /**
     * Convierte la Uri de una imagen del dispositivo (galería, cámara o archivo interno) en la parte
     * multipart que espera `upload.single("foto")` del backend. El nombre de archivo lleva extensión
     * porque el filtro de multer valida extensión y mimetype (jpg, png, webp).
     */
    fun uriAMultipart(uri: Uri, nombreCampo: String = CAMPO_FOTO): MultipartBody.Part? {
        val resolver = appContext.contentResolver
        val mime = resolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
            ?: "image/jpeg"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer la imagen $uri: ${e.message}", e)
            null
        } ?: return null
        return MultipartBody.Part.createFormData(nombreCampo, "foto.$extension", bytes.toRequestBody(mime.toMediaTypeOrNull()))
    }

    /** Copia la imagen elegida (redimensionada a JPEG, < 5 MB que admite el backend) a files/fotos. */
    private fun copiarFotoInterna(uri: Uri, animalId: String): String? = try {
        val resolver = appContext.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        var muestreo = 1
        while (maxOf(limites.outWidth, limites.outHeight) / muestreo > LADO_MAXIMO_FOTO) muestreo *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo })
        } ?: throw IOException("Imagen ilegible")
        val carpeta = File(appContext.filesDir, "fotos").apply { mkdirs() }
        val destino = File(carpeta, "$animalId-${System.currentTimeMillis()}.jpg")
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        destino.absolutePath
    } catch (e: Exception) {
        Log.e(TAG, "Error al copiar la foto: ${e.message}", e)
        null
    }

    private fun borrarFotoInterna(ruta: String) {
        val archivo = File(ruta)
        if (archivo.parentFile?.name == "fotos" && archivo.absolutePath.startsWith(appContext.filesDir.absolutePath)) {
            archivo.delete()
        }
    }

    private fun esUri(texto: String) = texto.startsWith("content://") || texto.startsWith("file://")

    // ============================================================================ Utilidades

    private fun texto(valor: Any?): RequestBody? = valor?.toString()?.toRequestBody(TEXTO_PLANO)

    private fun mensajeHttp(resp: Response<*>): String =
        ApiErrores.mensaje(resp) ?: "Error del servidor (${resp.code()})"

    private fun mensajeDe(e: Exception): String = when (e) {
        is IOException -> "Sin conexión con el servidor"
        else -> e.message ?: e.javaClass.simpleName
    }

    companion object {
        private const val TAG = "GanaderiaRepository"
        const val CAMPO_FOTO = "foto"
        private const val LADO_MAXIMO_FOTO = 1600
        private val TEXTO_PLANO = "text/plain".toMediaType()
    }
}

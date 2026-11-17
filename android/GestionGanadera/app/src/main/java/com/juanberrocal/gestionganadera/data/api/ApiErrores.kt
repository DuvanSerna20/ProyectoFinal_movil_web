package com.juanberrocal.gestionganadera.data.api

import retrofit2.Response

/** Extrae el "message" que devuelve el backend en sus respuestas de error. */
object ApiErrores {
    private val MENSAJE = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")

    fun mensaje(cuerpo: String?): String? = cuerpo?.let { MENSAJE.find(it)?.groupValues?.get(1) }

    fun mensaje(resp: Response<*>): String? = runCatching { mensaje(resp.errorBody()?.string()) }.getOrNull()
}

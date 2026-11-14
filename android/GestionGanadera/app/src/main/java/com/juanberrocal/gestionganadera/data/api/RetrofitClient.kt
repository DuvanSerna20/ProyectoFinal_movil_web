package com.juanberrocal.gestionganadera.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // En el emulador de Android Studio, 10.0.2.2 apunta al localhost de la máquina anfitriona
    // Si pruebas en un dispositivo físico conectado a la misma red WiFi, cambia esto por tu IP local (ej: http://192.168.1.X:3000/api/)
    var BASE_URL: String = "http://10.0.2.2:3000/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var retrofit: Retrofit? = null

    val apiService: ApiService
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!.create(ApiService::class.java)
        }

    fun actualizarBaseUrl(nuevaUrl: String) {
        val urlFinal = if (nuevaUrl.endsWith("/")) nuevaUrl else "$nuevaUrl/"
        BASE_URL = urlFinal
        retrofit = Retrofit.Builder()
            .baseUrl(urlFinal)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

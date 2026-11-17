package com.juanberrocal.gestionganadera.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Ganado ---
    @GET("animales")
    suspend fun getAnimales(): Response<ApiResponse<List<AnimalDto>>>

    /*
     * Alta y edición en multipart/form-data, alineado con `upload.single("foto")` de
     * backend/src/routes/animalRoutes.js. Los campos van como texto plano (multer los deja en req.body)
     * y la imagen en la parte "foto". Retrofit omite las partes null (igual que Gson omitía los null).
     */
    @Multipart
    @POST("animales")
    suspend fun createAnimal(
        @Part("codigo") codigo: RequestBody,
        @Part("nombre") nombre: RequestBody,
        @Part("raza") raza: RequestBody,
        @Part("sexo") sexo: RequestBody,
        @Part("especie") especie: RequestBody?,
        @Part("proposito") proposito: RequestBody?,
        @Part("estadoReproductivo") estadoReproductivo: RequestBody?,
        @Part("estado") estado: RequestBody?,
        @Part("pesoKg") pesoKg: RequestBody?,
        @Part("produccionLitrosDia") produccionLitrosDia: RequestBody?,
        @Part("fincaId") fincaId: RequestBody?,
        @Part("potreroId") potreroId: RequestBody?,
        @Part("registradoPor") registradoPor: RequestBody?,
        @Part foto: MultipartBody.Part?
    ): Response<ApiResponse<AnimalDto>>

    @Multipart
    @PUT("animales/{id}")
    suspend fun updateAnimal(
        @Path("id") id: String,
        @Part("codigo") codigo: RequestBody,
        @Part("nombre") nombre: RequestBody,
        @Part("raza") raza: RequestBody,
        @Part("sexo") sexo: RequestBody,
        @Part("especie") especie: RequestBody?,
        @Part("proposito") proposito: RequestBody?,
        @Part("estadoReproductivo") estadoReproductivo: RequestBody?,
        @Part("estado") estado: RequestBody?,
        @Part("pesoKg") pesoKg: RequestBody?,
        @Part("produccionLitrosDia") produccionLitrosDia: RequestBody?,
        @Part("fincaId") fincaId: RequestBody?,
        @Part("potreroId") potreroId: RequestBody?,
        @Part("registradoPor") registradoPor: RequestBody?,
        @Part foto: MultipartBody.Part?
    ): Response<ApiResponse<AnimalDto>>

    @DELETE("animales/{id}")
    suspend fun deleteAnimal(@Path("id") id: String): Response<ApiResponse<Any>>

    // --- Potreros ---
    @GET("potreros")
    suspend fun getPotreros(): Response<ApiResponse<List<PotreroDto>>>

    @POST("potreros")
    suspend fun createPotrero(@Body potrero: PotreroDto): Response<ApiResponse<PotreroDto>>

    @PUT("potreros/{id}")
    suspend fun updatePotrero(@Path("id") id: String, @Body potrero: PotreroDto): Response<ApiResponse<PotreroDto>>

    @DELETE("potreros/{id}")
    suspend fun deletePotrero(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("potreros/rotar")
    suspend fun rotarGanado(@Body request: RotacionRequest): Response<ApiResponse<Any>>

    // --- Fincas ---
    @GET("fincas")
    suspend fun getFincas(): Response<ApiResponse<List<FincaDto>>>

    @POST("fincas")
    suspend fun createFinca(@Body finca: FincaDto): Response<ApiResponse<FincaDto>>

    // --- Ordeño ---
    @GET("ordeno")
    suspend fun getOrdenos(): Response<ApiResponse<List<OrdenoDto>>>

    @POST("ordeno")
    suspend fun createOrdeno(@Body ordeno: OrdenoDto): Response<ApiResponse<OrdenoDto>>

    @PUT("ordeno/{id}")
    suspend fun updateOrdeno(@Path("id") id: String, @Body ordeno: OrdenoDto): Response<ApiResponse<OrdenoDto>>

    @DELETE("ordeno/{id}")
    suspend fun deleteOrdeno(@Path("id") id: String): Response<ApiResponse<Any>>

    // --- Tareas ---
    @GET("tareas")
    suspend fun getTareas(): Response<ApiResponse<List<TareaDto>>>

    @POST("tareas")
    suspend fun createTarea(@Body tarea: TareaDto): Response<ApiResponse<TareaDto>>

    @PUT("tareas/{id}")
    suspend fun updateTarea(@Path("id") id: String, @Body tarea: TareaDto): Response<ApiResponse<TareaDto>>

    @PATCH("tareas/{id}/toggle")
    suspend fun toggleTarea(@Path("id") id: String): Response<ApiResponse<TareaDto>>

    @DELETE("tareas/{id}")
    suspend fun deleteTarea(@Path("id") id: String): Response<ApiResponse<Any>>

    // --- Alimentación y Medicamentos ---
    @GET("alimentacion")
    suspend fun getAlimentacion(): Response<ApiResponse<List<AlimentacionDto>>>

    @POST("alimentacion")
    suspend fun createAlimentacion(@Body registro: AlimentacionDto): Response<ApiResponse<AlimentacionDto>>

    @PUT("alimentacion/{id}")
    suspend fun updateAlimentacion(@Path("id") id: String, @Body registro: AlimentacionDto): Response<ApiResponse<AlimentacionDto>>

    @DELETE("alimentacion/{id}")
    suspend fun deleteAlimentacion(@Path("id") id: String): Response<ApiResponse<Any>>

    // --- Autenticación y Usuarios ---
    @POST("usuarios")
    suspend fun register(@Body request: RegistroRequest): Response<ApiResponse<UsuarioDto>>

    @POST("usuarios/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<UsuarioDto>>
}

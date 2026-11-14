package com.juanberrocal.gestionganadera.data.api

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Ganado ---
    @GET("animales")
    suspend fun getAnimales(): Response<ApiResponse<List<AnimalDto>>>

    @POST("animales")
    suspend fun createAnimal(@Body animal: AnimalDto): Response<ApiResponse<AnimalDto>>

    @PUT("animales/{id}")
    suspend fun updateAnimal(@Path("id") id: String, @Body animal: AnimalDto): Response<ApiResponse<AnimalDto>>

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

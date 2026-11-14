package com.juanberrocal.gestionganadera.data.api

import com.google.gson.annotations.SerializedName

// Contenedor estándar de respuestas del backend Node.js
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("data") val data: T? = null
)

// DTOs para Ganado
data class AnimalDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("raza") val raza: String,
    @SerializedName("sexo") val sexo: String,
    @SerializedName("pesoKg") val pesoKg: Double? = null,
    @SerializedName("estado") val estado: String? = "En producción",
    @SerializedName("fotoUrl") val fotoUrl: String? = null,
    @SerializedName("registradoPor") val registradoPor: String? = "Android App",
    @SerializedName("fechaRegistro") val fechaRegistro: String? = null,
    @SerializedName("especie") val especie: String? = null,
    /** Solo lectura: campo de la versión anterior de la app. */
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("proposito") val proposito: String? = null,
    @SerializedName("estadoReproductivo") val estadoReproductivo: String? = null,
    @SerializedName("produccionLitrosDia") val produccionLitrosDia: Double? = null,
    // "" = sin finca / sin potrero (Gson omite los null, y así se pueden quitar al editar)
    @SerializedName("fincaId") val fincaId: String? = null,
    @SerializedName("potreroId") val potreroId: String? = null
)

// DTOs para Potreros
data class PotreroDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("fincaId") val fincaId: String? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("largoMetros") val largoMetros: Double? = null,
    @SerializedName("anchoMetros") val anchoMetros: Double? = null,
    @SerializedName("area") val area: Double? = null,
    @SerializedName("unidadArea") val unidadArea: String? = null,
    @SerializedName("areaM2") val areaM2: Double? = null,
    @SerializedName("areaHa") val areaHa: Double? = null,
    @SerializedName("estado") val estado: String = "En descanso",
    @SerializedName("condicionPasto") val condicionPasto: String? = null,
    @SerializedName("diasOcupacion") val diasOcupacion: Double = 3.0,
    @SerializedName("diasDescanso") val diasDescanso: Int = 30,
    @SerializedName("tipoPasto") val tipoPasto: String? = "Brachiaria",
    @SerializedName("fechaUltimoCambio") val fechaUltimoCambio: String? = null,
    @SerializedName("cantidadAnimales") val cantidadAnimales: Int? = null,
    @SerializedName("registradoPor") val registradoPor: String? = null
)

// DTOs para Fincas
data class FincaDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("departamento") val departamento: String,
    @SerializedName("municipio") val municipio: String? = "",
    @SerializedName("pisoTermico") val pisoTermico: String? = null,
    @SerializedName("registradoPor") val registradoPor: String? = "Android App"
)

data class RotacionRequest(
    @SerializedName("origenId") val origenId: String,
    @SerializedName("destinoId") val destinoId: String,
    @SerializedName("motivo") val motivo: String,
    @SerializedName("fecha") val fecha: String
)

// DTOs para Ordeño
data class OrdenoDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("jornada") val jornada: String,
    @SerializedName("litros") val litros: Double,
    @SerializedName("animalId") val animalId: String? = null,
    @SerializedName("animalCodigo") val animalCodigo: String? = null,
    @SerializedName("animalNombre") val animalNombre: String? = null,
    @SerializedName("responsable") val responsable: String = "Operario Android",
    @SerializedName("horaRegistro") val horaRegistro: String? = null,
    @SerializedName("destinoLeche") val destinoLeche: String? = null
)

// DTOs para Tareas
data class TareaDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("categoria") val categoria: String = "Vacunación",
    @SerializedName("fechaLimite") val fechaLimite: String,
    @SerializedName("completada") val completada: Boolean = false,
    @SerializedName("prioridad") val prioridad: String = "Media",
    @SerializedName("responsable") val responsable: String = "Operario Android",
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("esTareaEspecial") val esTareaEspecial: Boolean? = null,
    // "" = sin tipo / sin animal (Gson omite los null y así se pueden quitar al editar)
    @SerializedName("tipoTareaEspecial") val tipoTareaEspecial: String? = null,
    @SerializedName("animalId") val animalId: String? = null,
    @SerializedName("animalCodigoNombre") val animalCodigoNombre: String? = null
)

// DTOs para Alimentación y Medicamentos
data class AlimentacionDto(
    @SerializedName("_id") val id: String? = null,
    // "" = no aplica (Gson omite los null)
    @SerializedName("animalId") val animalId: String? = null,
    @SerializedName("grupoProposito") val grupoProposito: String? = null,
    @SerializedName("objetivoLabel") val objetivoLabel: String,
    @SerializedName("tipoRegistro") val tipoRegistro: String,
    @SerializedName("categoriaDetalle") val categoriaDetalle: String,
    @SerializedName("nombreProducto") val nombreProducto: String,
    @SerializedName("cantidad") val cantidad: Double,
    @SerializedName("unidad") val unidad: String,
    @SerializedName("frecuencia") val frecuencia: String,
    @SerializedName("tieneRetiroLeche") val tieneRetiroLeche: Boolean = false,
    @SerializedName("diasRetiro") val diasRetiro: Int = 0,
    @SerializedName("activo") val activo: Boolean = true,
    @SerializedName("fechaInicio") val fechaInicio: String? = null,
    @SerializedName("fechaFin") val fechaFin: String? = null,
    @SerializedName("registradoPor") val registradoPor: String? = null
)

// DTOs para Usuarios / Autenticación
data class UsuarioDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("finca") val finca: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("rol") val rol: String? = "Operario de Campo",
    @SerializedName("dispositivo") val dispositivo: String? = "Android Mobile",
    @SerializedName("estado") val estado: String? = "Activo"
)

data class RegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("finca") val finca: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String,
    @SerializedName("rol") val rol: String = "Operario de Campo",
    @SerializedName("dispositivo") val dispositivo: String = "Android Mobile"
)

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String
)

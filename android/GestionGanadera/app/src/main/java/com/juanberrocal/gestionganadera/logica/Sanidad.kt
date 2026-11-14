package com.juanberrocal.gestionganadera.logica

import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import java.time.LocalDate

/**
 * Lo que consume un animal hoy: su plan de alimentación activo y los medicamentos vigentes.
 * Lo usan la tarjeta de Ganado y el formulario de Ordeño (alerta de retiro de leche).
 */
data class ManejoAnimal(
    val alimentos: List<RegistroAlimentacionMedicamento>,
    val medicamentos: List<RegistroAlimentacionMedicamento>,
    /** Medicamentos que obligan a descartar la leche hoy. */
    val retiros: List<RegistroAlimentacionMedicamento>
) {
    val enRetiroLeche: Boolean get() = retiros.isNotEmpty()
    val vacio: Boolean get() = alimentos.isEmpty() && medicamentos.isEmpty()

    /**
     * Último día de retiro entre todos los medicamentos; null si alguno sigue activo
     * (el retiro no termina hasta finalizar el tratamiento).
     */
    val finRetiro: LocalDate?
        get() = if (retiros.any { it.activo }) null else retiros.mapNotNull { it.finRetiro }.maxOrNull()

    companion object {
        val VACIO = ManejoAnimal(emptyList(), emptyList(), emptyList())
    }
}

object Sanidad {

    fun manejoDe(
        animal: Animal,
        registros: List<RegistroAlimentacionMedicamento>,
        hoy: LocalDate = LocalDate.now()
    ): ManejoAnimal {
        val propios = registros.filter { it.aplicaA(animal) }
        if (propios.isEmpty()) return ManejoAnimal.VACIO
        return ManejoAnimal(
            alimentos = propios.filter { !it.esMedicamento && it.activo },
            medicamentos = propios.filter { it.medicamentoVigente(hoy) },
            retiros = propios.filter { it.retiroVigente(hoy) }
        )
    }

    /** Por id de animal; solo incluye los que tienen algo registrado. */
    fun manejoPorAnimal(
        animales: List<Animal>,
        registros: List<RegistroAlimentacionMedicamento>,
        hoy: LocalDate = LocalDate.now()
    ): Map<String, ManejoAnimal> =
        if (registros.isEmpty()) emptyMap()
        else animales.associate { it.id to manejoDe(it, registros, hoy) }.filterValues { !it.vacio }
}

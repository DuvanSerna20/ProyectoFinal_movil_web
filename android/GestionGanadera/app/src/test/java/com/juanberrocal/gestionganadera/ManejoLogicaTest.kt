package com.juanberrocal.gestionganadera

import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.DestinoLeche
import com.juanberrocal.gestionganadera.data.FrecuenciaConsumo
import com.juanberrocal.gestionganadera.data.JornadaOrdeno
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.RegistroAlimentacionMedicamento
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.data.TipoInsumo
import com.juanberrocal.gestionganadera.data.UnidadConsumo
import com.juanberrocal.gestionganadera.logica.Sanidad
import com.juanberrocal.gestionganadera.ui.ordeno.OrdenoLogica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ManejoLogicaTest {

    private val hoy = LocalDate.of(2026, 11, 14)
    private val luna = Animal("a1", "008", "Luna", raza = "Girolando")
    private val canela = Animal("a2", "012", "Canela", raza = "Jersey")
    private val mora = Animal("a3", "020", "Mora", raza = "Holstein")

    private fun ordeno(id: String, animal: Animal?, jornada: JornadaOrdeno, litros: Double, destino: DestinoLeche = DestinoLeche.TANQUE_VENTA, fecha: LocalDate = hoy) =
        RegistroOrdeno(id, animal?.id, animal?.codigoNombre ?: RegistroOrdeno.ETIQUETA_TANQUE, fecha, jornada, litros, destino)

    private fun medicamento(activo: Boolean, diasRetiro: Int, fechaFin: LocalDate? = null, animalId: String? = "a1", grupo: PropositoEtapa? = null) =
        RegistroAlimentacionMedicamento(
            id = "m", animalId = animalId, grupoProposito = grupo, objetivoLabel = "x",
            tipoRegistro = TipoInsumo.MEDICAMENTO, categoriaDetalle = "Antibiótico", nombreProducto = "Oxitetraciclina",
            cantidad = 20.0, unidad = UnidadConsumo.MILILITROS, frecuencia = FrecuenciaConsumo.CADA_12_HORAS,
            tieneRetiroLeche = diasRetiro > 0, diasRetiro = diasRetiro, activo = activo,
            fechaInicio = hoy.minusDays(5), fechaFin = fechaFin
        )

    @Test
    fun resumen_soloCuentaLecheComercialYVacasDistintas() {
        val registros = listOf(
            ordeno("1", luna, JornadaOrdeno.MANANA, 10.0),
            ordeno("2", luna, JornadaOrdeno.TARDE, 8.0),
            ordeno("3", canela, JornadaOrdeno.MANANA, 6.0, DestinoLeche.RETIRO_MEDICO),
            ordeno("4", null, JornadaOrdeno.TARDE, 4.0, DestinoLeche.CRIA_TERNERO),
            ordeno("5", mora, JornadaOrdeno.MANANA, 50.0, fecha = hoy.minusDays(1))
        )
        val r = OrdenoLogica.resumen(registros, hoy, listOf(luna, canela, mora))
        assertEquals(18.0, r.litrosComerciales, 0.001)
        assertEquals(10.0, r.litrosManana, 0.001)
        assertEquals(8.0, r.litrosTarde, 0.001)
        assertEquals(6.0, r.litrosDescarte, 0.001)
        assertEquals(4.0, r.litrosTernero, 0.001)
        assertEquals(2, r.vacasOrdenadas) // Luna y Canela; Mora fue ayer
        assertEquals(3, r.vacasEnProduccion)
    }

    @Test
    fun duplicado_mismaVacaFechaYJornada_excluyeElQueSeEdita() {
        val registros = listOf(ordeno("1", luna, JornadaOrdeno.MANANA, 10.0))
        assertEquals("1", OrdenoLogica.duplicado(registros, "a1", hoy, JornadaOrdeno.MANANA, "")?.id)
        assertNull(OrdenoLogica.duplicado(registros, "a1", hoy, JornadaOrdeno.TARDE, ""))
        assertNull(OrdenoLogica.duplicado(registros, "a1", hoy, JornadaOrdeno.MANANA, excluirId = "1"))
        assertNull(OrdenoLogica.duplicado(registros, null, hoy, JornadaOrdeno.MANANA, ""))
    }

    @Test
    fun retiro_vigenteMientrasActivoYDuranteLosDiasDeRetiro() {
        assertTrue(medicamento(activo = true, diasRetiro = 3).retiroVigente(hoy))
        val finalizado = medicamento(activo = false, diasRetiro = 3, fechaFin = hoy.minusDays(3))
        assertEquals(hoy, finalizado.finRetiro)
        assertTrue(finalizado.retiroVigente(hoy))
        assertFalse(finalizado.retiroVigente(hoy.plusDays(1)))
        assertFalse(medicamento(activo = true, diasRetiro = 0).retiroVigente(hoy))
    }

    @Test
    fun manejo_aplicaPorAnimalOPorGrupo() {
        val porGrupo = medicamento(activo = true, diasRetiro = 2, animalId = null, grupo = PropositoEtapa.PRODUCCION_ORDENO)
        assertTrue(Sanidad.manejoDe(canela, listOf(porGrupo), hoy).enRetiroLeche)
        val seca = canela.copy(proposito = PropositoEtapa.VACA_SECA)
        assertFalse(Sanidad.manejoDe(seca, listOf(porGrupo), hoy).enRetiroLeche)

        val soloLuna = medicamento(activo = true, diasRetiro = 2)
        assertTrue(Sanidad.manejoDe(luna, listOf(soloLuna), hoy).enRetiroLeche)
        assertFalse(Sanidad.manejoDe(canela, listOf(soloLuna), hoy).enRetiroLeche)
        assertNull(Sanidad.manejoDe(luna, listOf(soloLuna), hoy).finRetiro) // activo: sin fecha de fin
    }
}

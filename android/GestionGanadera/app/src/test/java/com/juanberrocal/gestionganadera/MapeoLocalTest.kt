package com.juanberrocal.gestionganadera

import com.juanberrocal.gestionganadera.data.Animal
import com.juanberrocal.gestionganadera.data.DestinoLeche
import com.juanberrocal.gestionganadera.data.EstadoPotrero
import com.juanberrocal.gestionganadera.data.JornadaOrdeno
import com.juanberrocal.gestionganadera.data.Potrero
import com.juanberrocal.gestionganadera.data.PropositoEtapa
import com.juanberrocal.gestionganadera.data.RegistroOrdeno
import com.juanberrocal.gestionganadera.data.Sexo
import com.juanberrocal.gestionganadera.data.Tarea
import com.juanberrocal.gestionganadera.data.TipoTareaEspecial
import com.juanberrocal.gestionganadera.data.UnidadArea
import com.juanberrocal.gestionganadera.data.local.MapeoLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/** Ida y vuelta dominio -> Room -> dominio: nada se pierde y los ids locales no cambian. */
class MapeoLocalTest {

    @Test
    fun animalConservaIdLocalMongoIdYPotrero() {
        val id = UUID.randomUUID().toString()
        val potreroLocal = UUID.randomUUID().toString()
        val animal = Animal(
            id = id, codigo = "008", nombre = "Luna", raza = "Girolando", sexo = Sexo.HEMBRA,
            proposito = PropositoEtapa.PRODUCCION_ORDENO, pesoKg = 480.0, produccionLitrosDia = 12.5,
            fincaId = "finca-mongo", potreroId = potreroLocal, mongoId = "animal-mongo",
            fotoUrl = "/uploads/foto-1.jpg", fotoLocal = "/data/fotos/x.jpg"
        )
        val entidad = MapeoLocal.aEntidad(animal, id, animal.mongoId, sincronizado = false, creadoEn = 1L)

        assertFalse(entidad.sincronizado)
        assertEquals(animal, MapeoLocal.aDominio(entidad))
    }

    @Test
    fun potreroIdaYVuelta() {
        val potrero = Potrero(
            id = "p-uuid", fincaId = "f1", nombre = "La Loma", area = 3.0, unidadArea = UnidadArea.CUADRAS,
            estado = EstadoPotrero.LISTO, diasOcupacionPlaneados = 1.5, diasDescansoPlaneados = 28,
            fechaUltimoCambio = LocalDate.of(2026, 3, 1), mongoId = "p-mongo"
        )
        val entidad = MapeoLocal.aEntidad(potrero, potrero.id, potrero.mongoId, sincronizado = true)
        assertEquals(potrero, MapeoLocal.aDominio(entidad))
    }

    @Test
    fun ordenoYTareaIdaYVuelta() {
        val ordeno = RegistroOrdeno(
            id = "o-uuid", animalId = "a-uuid", animalCodigoNombre = "#008 - Luna",
            fecha = LocalDate.of(2026, 11, 17), jornada = JornadaOrdeno.TARDE, litros = 7.5,
            destinoLeche = DestinoLeche.RETIRO_MEDICO
        )
        assertEquals(ordeno, MapeoLocal.aDominio(MapeoLocal.aEntidad(ordeno, ordeno.id, null, false, 0L)))

        val tarea = Tarea(
            id = "t-uuid", titulo = "Vacuna aftosa", fechaProgramada = LocalDate.of(2026, 12, 1),
            esTareaEspecial = true, tipoTareaEspecial = TipoTareaEspecial.VACUNACION, animalId = "a-uuid",
            animalCodigoNombre = "#008 - Luna"
        )
        assertEquals(tarea, MapeoLocal.aDominio(MapeoLocal.aEntidad(tarea, tarea.id, "t-mongo", true)))
    }
}

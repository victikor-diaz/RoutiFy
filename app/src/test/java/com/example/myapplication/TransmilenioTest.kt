package com.example.myapplication

import com.example.myapplication.data.Anden
import com.example.myapplication.data.OpcionViaje
import com.example.myapplication.data.Paso
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.data.Troncal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.TimeZone

/** Instante en hora de Bogotá, p. ej. instante(20261026, 8) = lunes 26 de octubre de 2026, 8:00 a. m. */
fun instante(fecha: Int, hora: Int, minuto: Int = 0): Long =
    Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")).apply {
        clear()
        set(fecha / 10_000, fecha / 100 % 100 - 1, fecha % 100, hora, minuto)
    }.timeInMillis

/** Carga el mismo troncal.txt que lleva la app (los tests corren desde la carpeta app/). */
fun cargarRed() = Transmilenio.inicializar { File("src/main/assets/troncal.txt").inputStream() }

class TransmilenioTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun cargar() = cargarRed()

        private val LUNES_7AM = instante(20261026, 7)
        private val LUNES_8AM = instante(20261026, 8)
        private val LUNES_MEDIODIA = instante(20261026, 12)
        private val LUNES_5PM = instante(20261026, 17)
    }

    private val estaciones get() = Transmilenio.estaciones
    private fun estacion(nombre: String) = Transmilenio.estacion(nombre)

    private fun revisar(opcion: OpcionViaje) {
        val tramos = opcion.tramos
        assertTrue("${opcion.etiqueta}: sin tramos", tramos.isNotEmpty())
        assertEquals(opcion.origen.nombre, tramos.first().desde)
        assertEquals(opcion.destino.nombre, tramos.last().hasta)
        opcion.pasos.forEach { assertTrue("$it tiene minutos negativos", it.minutos >= 0) }
        tramos.forEach { assertTrue(it.esperaMin in 0..it.minutos) }
        tramos.zipWithNext().forEach { (antes, despues) -> assertEquals(antes.hasta, despues.desde) }
        // Los pasos suman exactamente la diferencia entre la salida y la llegada
        assertEquals(Math.round((opcion.llegada - opcion.salida) / 60_000.0).toInt(), opcion.duracionMin)
    }

    @Test
    fun cargaLaRedTroncalCompleta() {
        assertEquals(143, estaciones.size)
        assertEquals(estaciones.size, estaciones.map { it.nombre }.toSet().size)
        assertEquals(3550, Transmilenio.TARIFA)
        assertEquals("31/12/2026", Transmilenio.vigenteHasta)
    }

    @Test
    fun lasEstacionesTienenSuTroncalOficial() {
        assertEquals(Troncal.B, estacion("Calle 100 - Marketmedios").troncal)
        assertEquals(Troncal.E, estacion("Ricaurte").troncal)
        assertEquals(Troncal.K, estacion("Universidades CityU").troncal)
        assertEquals(Troncal.M, estacion("Museo Nacional").troncal)
        assertEquals(Troncal.Z, estacion("Tibanica - Primavera").troncal)
    }

    @Test
    fun todasLasParejasTienenViajeUnLunes() {
        // Algunas rutas solo corren en hora pico (la A61 sale de Tibanica de 4:30 a 10:00 y la Z61
        // vuelve de 15:00 a 21:00), así que lo que no sale al mediodía debe salir a las 7 o a las 17
        val sinViaje = mutableListOf<String>()
        estaciones.forEach { origen ->
            estaciones.filter { it != origen }.forEach { destino ->
                val opciones = listOf(LUNES_MEDIODIA, LUNES_7AM, LUNES_5PM).asSequence()
                    .map { Transmilenio.planearViaje(origen, destino, it) }
                    .firstOrNull { it.isNotEmpty() }
                if (opciones == null) sinViaje += "${origen.nombre} → ${destino.nombre}"
                opciones?.forEach(::revisar)
            }
        }
        assertTrue("Sin viaje (${sinViaje.size}): ${sinViaje.take(20)}", sinViaje.isEmpty())
    }

    @Test
    fun laLineaZSoloCorreEnHoraPico() {
        val tibanica = estacion("Tibanica - Primavera")
        val ricaurte = estacion("Ricaurte")
        assertTrue(Transmilenio.planearViaje(tibanica, ricaurte, LUNES_7AM).isNotEmpty())
        assertTrue(Transmilenio.planearViaje(tibanica, ricaurte, LUNES_MEDIODIA).isEmpty())
        assertTrue(Transmilenio.planearViaje(ricaurte, tibanica, LUNES_5PM).isNotEmpty())
    }

    @Test
    fun laMasRapidaLlegaPrimero() {
        val origen = estacion("Portal Américas")
        estaciones.filter { it != origen }.forEach { destino ->
            val opciones = Transmilenio.planearViaje(origen, destino, LUNES_8AM)
            val rapida = opciones.firstOrNull { it.etiqueta == "Más rápida" } ?: return@forEach // la línea Z a esta hora
            opciones.forEach { assertTrue("${destino.nombre}: ${it.etiqueta} llega antes", it.llegada >= rapida.llegada) }
        }
    }

    @Test
    fun laCaminataSeSumaAlViaje() {
        val sin = Transmilenio.planearViaje(estacion("Calle 100 - Marketmedios"), estacion("Ricaurte"), LUNES_8AM)
        val con = Transmilenio.planearViaje(estacion("Calle 100 - Marketmedios"), estacion("Ricaurte"), LUNES_8AM, 7)
        val caminar = con.first().pasos.first()
        assertTrue(caminar is Paso.Caminar && caminar.minutos == 7)
        assertTrue(sin.first().pasos.none { it is Paso.Caminar })
        assertTrue(con.first().llegada >= sin.first().llegada)
        con.forEach(::revisar)
    }

    @Test
    fun elTransbordoDiceAQueVagonIr() {
        val opcion = Transmilenio.planearViaje(estacion("CL 38A Sur - Centro Mayor"), estacion("Ricaurte"), LUNES_8AM)
            .first { it.transbordos == 1 }
        val transbordo = opcion.pasos.filterIsInstance<Paso.Transbordo>().single()
        assertEquals(opcion.tramos[1].desde, transbordo.estacion)
        assertTrue(transbordo.indicacion, transbordo.indicacion.startsWith("Sin salir de la estación, busca el vagón "))
    }

    @Test
    fun calle100APortalNorteNoNecesitaTransbordo() {
        val opciones = Transmilenio.planearViaje(estacion("Calle 100 - Marketmedios"), estacion("Portal Norte - Unicervantes"), LUNES_8AM)
        assertEquals(0, opciones.minOf { it.transbordos })
        assertTrue(opciones.first().duracionMin < 40)
    }

    @Test
    fun deMadrugadaNoHayBusesYALasTresSeOfreceElPrimero() {
        val calle100 = estacion("Calle 100 - Marketmedios")
        val madrugada = instante(20261026, 2, 30)
        assertTrue(Transmilenio.planearViaje(calle100, estacion("Ricaurte"), madrugada).isEmpty())
        assertTrue(Transmilenio.proximasSalidas(calle100, madrugada).isEmpty())
        val primero = Transmilenio.planearViaje(calle100, estacion("Portal Norte - Unicervantes"), instante(20261026, 3, 30))
        assertTrue(primero.isNotEmpty())
        assertTrue(primero.first().tramos.first().esperaMin <= 90)
    }

    @Test
    fun losFestivosUsanElServicioDeDomingo() {
        val red = Transmilenio.red
        assertEquals(setOf("1"), red.serviciosActivos(20261026, 0)) // lunes normal
        assertEquals(setOf("4"), red.serviciosActivos(20261012, 0)) // lunes festivo
        assertEquals(setOf("2"), red.serviciosActivos(20261031, 5)) // sábado
        assertEquals(setOf("4"), red.serviciosActivos(20261101, 6)) // domingo
        assertEquals(emptySet<String>(), red.serviciosActivos(20270104, 0)) // fuera de la vigencia del GTFS
    }

    @Test
    fun proximasSalidasDeCalle100() {
        val salidas = Transmilenio.proximasSalidas(estacion("Calle 100 - Marketmedios"), LUNES_8AM)
        assertEquals(4, salidas.size)
        assertEquals(salidas.sortedBy { it.enMinutos }, salidas)
        salidas.forEach {
            assertTrue(it.enMinutos in 0..60)
            assertTrue(it.anden != null)
        }
    }

    @Test
    fun laEstacionMasCercanaAUnaEstacionEsElla() {
        estaciones.forEach { assertEquals(it, Transmilenio.masCercana(it.latitud, it.longitud).estacion) }
        val cercania = Transmilenio.masCercana(4.6845, -74.0565) // a unas cuadras de la Calle 100
        assertEquals("Calle 100 - Marketmedios", cercania.estacion.nombre)
        assertTrue(cercania.metros in 100..1000)
    }

    @Test
    fun buscarIgnoraTildesMayusculasYAbreviaturas() {
        assertTrue(Transmilenio.buscar("toberin").any { it.nombre == "Toberín" })
        assertTrue(Transmilenio.buscar("AV. JIMENEZ").any { it.nombre == "Avenida Jiménez" })
        assertTrue(Transmilenio.buscar("avenida 39").any { it.nombre == "Av. 39" })
        assertTrue(Transmilenio.buscar("calle 100").any { it.nombre == "Calle 100 - Marketmedios" })
        assertTrue(Transmilenio.buscar("   ").isEmpty())
    }

    @Test
    fun losAndenesSeLeenComoVagonOPlataforma() {
        assertEquals("el vagón C, puertas 2 ó 5", Anden.leer("C:2 ó 5")?.enFrase)
        assertEquals("Vagón A · puerta 4", Anden.leer("A:4")?.detalle)
        assertEquals("la plataforma T1", Anden.leer("T1")?.enFrase)
        assertEquals(null, Anden.leer(""))
    }
}

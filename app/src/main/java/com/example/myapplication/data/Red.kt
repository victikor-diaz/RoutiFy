package com.example.myapplication.data

import java.io.InputStream
import java.util.Calendar
import java.util.TimeZone

/*
 * La red troncal cargada desde assets/troncal.txt, que genera tools/gtfs/exportar_troncal.py
 * a partir del GTFS oficial de TransMilenio. Todos los horarios son programados, no en vivo.
 */

/** Tiempo mínimo para cambiar de vagón dentro de una estación. */
internal const val TRANSBORDO_SEG = 180
private const val MAX_TRAMOS = 4

// Si el próximo bus sale en más de hora y media, no se ofrece: la app planea "saliendo ahora"
private const val MAX_ESPERA_SEG = 90 * 60
private const val DIA_SEG = 86_400

internal val bogota: TimeZone = TimeZone.getTimeZone("America/Bogota")

/** Un instante visto como día de servicio: fecha AAAAMMDD, día de la semana (0 = lunes) y segundos. */
internal class Momento(val fecha: Int, val diaSemana: Int, val segundos: Int, val ayerFecha: Int, val ayerDiaSemana: Int)

internal fun momento(milis: Long): Momento {
    fun cal(m: Long) = Calendar.getInstance(bogota).apply { timeInMillis = m }
    fun fecha(c: Calendar) = c.get(Calendar.YEAR) * 10_000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    fun dia(c: Calendar) = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val hoy = cal(milis)
    val ayer = cal(milis - DIA_SEG * 1000L)
    val segundos = hoy.get(Calendar.HOUR_OF_DAY) * 3600 + hoy.get(Calendar.MINUTE) * 60 + hoy.get(Calendar.SECOND)
    return Momento(fecha(hoy), dia(hoy), segundos, fecha(ayer), dia(ayer))
}

internal class Servicio(
    private val dias: String,
    private val desde: Int,
    private val hasta: Int,
    private val agregadas: Set<Int>,
    private val quitadas: Set<Int>,
) {
    fun activo(fecha: Int, diaSemana: Int): Boolean = when (fecha) {
        in quitadas -> false
        in agregadas -> true
        else -> fecha in desde..hasta && dias[diaSemana] == '1'
    }
}

/** Una ruta con una secuencia fija de andenes. Los viajes comparten perfiles de tiempo según la hora. */
internal class Patron(
    val indice: Int,
    val ruta: Ruta,
    val estaciones: IntArray,
    val andenes: List<Anden?>,
    val perfiles: List<IntArray>,
    /** Por servicio: salida desde el primer andén (segundos) y perfil de cada viaje, ordenados por salida. */
    val viajes: Map<String, Pair<IntArray, IntArray>>,
) {
    val minimo = IntArray(estaciones.size) { i -> perfiles.minOf { it[i] } }
    val maximo = IntArray(estaciones.size) { i -> perfiles.maxOf { it[i] } }
}

/** Los viajes de un patrón que corren en un día concreto, incluidos los de anoche que pasan de las 24:00. */
internal class ViajesDelDia(private val patron: Patron, private val salidas: IntArray, private val perfil: IntArray) {

    fun hora(viaje: Int, parada: Int): Int = salidas[viaje] + patron.perfiles[perfil[viaje]][parada]

    /** El viaje que pasa primero por [parada] a partir de [desde], o -1. */
    fun primero(parada: Int, desde: Int): Int {
        var bajo = 0
        var alto = salidas.size
        val umbral = desde - patron.maximo[parada]
        while (bajo < alto) {
            val medio = (bajo + alto) ushr 1
            if (salidas[medio] < umbral) bajo = medio + 1 else alto = medio
        }
        var elegido = -1
        var mejor = Int.MAX_VALUE
        var j = bajo
        while (j < salidas.size && salidas[j] + patron.minimo[parada] < mejor) {
            val t = hora(j, parada)
            if (t in desde until mejor) {
                mejor = t
                elegido = j
            }
            j++
        }
        return elegido
    }
}

internal class TramoRed(val patron: Patron, val sube: Int, val baja: Int, val sale: Int, val llega: Int, val ronda: Int)

internal class Red(
    val fuente: String,
    val vigenteHasta: Int,
    val tarifa: Int,
    val estaciones: List<Estacion>,
    val patrones: List<Patron>,
    private val servicios: Map<String, Servicio>,
) {
    val indiceDe: Map<String, Int> = estaciones.withIndex().associate { (i, e) -> e.nombre to i }

    /** Para cada estación, los patrones que paran en ella y en qué posición. */
    val paradasEn: List<List<Pair<Patron, Int>>> = List(estaciones.size) { mutableListOf<Pair<Patron, Int>>() }.also { lista ->
        patrones.forEach { p -> p.estaciones.forEachIndexed { i, s -> (lista[s] as MutableList) += p to i } }
    }

    private var cache: Pair<Int, List<ViajesDelDia>>? = null

    @Synchronized
    fun viajesDelDia(m: Momento): List<ViajesDelDia> {
        cache?.let { (fecha, viajes) -> if (fecha == m.fecha) return viajes }
        val hoy = servicios.filterValues { it.activo(m.fecha, m.diaSemana) }.keys
        val ayer = servicios.filterValues { it.activo(m.ayerFecha, m.ayerDiaSemana) }.keys
        val viajes = patrones.map { p ->
            val pares = mutableListOf<Long>()
            p.viajes.forEach { (servicio, salidasYPerfiles) ->
                val (salidas, perfiles) = salidasYPerfiles
                for (j in salidas.indices) {
                    if (servicio in hoy) pares += salidas[j].toLong() shl 32 or perfiles[j].toLong()
                    // Un viaje de anoche a las 24:30 corre hoy a las 00:30
                    if (servicio in ayer && salidas[j] + p.maximo.last() >= DIA_SEG) {
                        pares += (salidas[j] - DIA_SEG).toLong() shl 32 or perfiles[j].toLong()
                    }
                }
            }
            pares.sort()
            ViajesDelDia(
                p,
                IntArray(pares.size) { (pares[it] shr 32).toInt() },
                IntArray(pares.size) { (pares[it] and 0xFFFFFFFFL).toInt() },
            )
        }
        cache = m.fecha to viajes
        return viajes
    }

    fun serviciosActivos(fecha: Int, diaSemana: Int): Set<String> =
        servicios.filterValues { it.activo(fecha, diaSemana) }.keys

    /**
     * Búsqueda por rondas (RAPTOR): la ronda k encuentra los mejores viajes con k buses.
     * Devuelve, de menos a más transbordos, cada viaje que llega antes que todos los de menos buses.
     */
    fun planear(origen: Int, destino: Int, m: Momento, listo: Int, excluir: String? = null): List<List<TramoRed>> {
        val dia = viajesDelDia(m)
        val n = estaciones.size
        val sin = Int.MAX_VALUE
        val llegada = Array(MAX_TRAMOS + 1) { IntArray(n) { sin } }
        val rastro = Array(MAX_TRAMOS + 1) { arrayOfNulls<TramoRed>(n) }
        val mejor = IntArray(n) { sin }
        llegada[0][origen] = listo
        mejor[origen] = listo
        var marcadas = setOf(origen)
        val resultado = mutableListOf<List<TramoRed>>()

        for (k in 1..MAX_TRAMOS) {
            llegada[k] = llegada[k - 1].copyOf()
            rastro[k] = rastro[k - 1].copyOf()
            val cola = HashMap<Patron, Int>()
            for (s in marcadas) {
                for ((p, i) in paradasEn[s]) {
                    if (p.ruta.codigo == excluir || i == p.estaciones.lastIndex) continue
                    if (i < (cola[p] ?: Int.MAX_VALUE)) cola[p] = i
                }
            }
            val nuevas = HashSet<Int>()
            for ((p, inicio) in cola) {
                val viajes = dia[p.indice]
                var viaje = -1
                var sube = -1
                for (i in inicio until p.estaciones.size) {
                    val s = p.estaciones[i]
                    if (viaje >= 0) {
                        val t = viajes.hora(viaje, i)
                        if (t < minOf(mejor[s], mejor[destino])) {
                            llegada[k][s] = t
                            mejor[s] = t
                            rastro[k][s] = TramoRed(p, sube, i, viajes.hora(viaje, sube), t, k)
                            nuevas += s
                        }
                    }
                    val antes = llegada[k - 1][s]
                    if (antes == sin) continue
                    val desde = if (rastro[k - 1][s] == null) antes else antes + TRANSBORDO_SEG
                    if (viaje >= 0 && desde > viajes.hora(viaje, i)) continue
                    val otro = viajes.primero(i, desde)
                    if (otro < 0 || viajes.hora(otro, i) - desde > MAX_ESPERA_SEG) continue
                    if (viaje < 0 || viajes.hora(otro, i) < viajes.hora(viaje, i)) {
                        viaje = otro
                        sube = i
                    }
                }
            }
            if (destino in nuevas) resultado += reconstruir(rastro, k, destino)
            if (nuevas.isEmpty()) break
            marcadas = nuevas
        }
        return resultado
    }

    private fun reconstruir(rastro: Array<Array<TramoRed?>>, ronda: Int, destino: Int): List<TramoRed> {
        val tramos = mutableListOf<TramoRed>()
        var s = destino
        var k = ronda
        while (true) {
            val tramo = rastro[k][s] ?: break
            tramos += tramo
            s = tramo.patron.estaciones[tramo.sube]
            k = tramo.ronda - 1
        }
        return tramos.reversed()
    }

    companion object {
        fun leer(entrada: InputStream): Red {
            var fuente = ""
            var vigenteHasta = 0
            var tarifa = 0
            val servicios = HashMap<String, Servicio>()
            val crudas = mutableListOf<List<String>>()
            val patrones = mutableListOf<MutableList<List<String>>>()
            entrada.bufferedReader(Charsets.UTF_8).forEachLine { linea ->
                if (linea.isBlank()) return@forEachLine
                val c = linea.split('|')
                when (c[0]) {
                    "ROUTIFY" -> check(c[1] == "1") { "Formato de troncal.txt desconocido: ${c[1]}" }
                    "FUENTE" -> {
                        fuente = c[1]
                        vigenteHasta = c[3].toInt()
                    }
                    "TARIFA" -> tarifa = c[1].toInt()
                    "SERVICIO" -> servicios[c[1]] = Servicio(
                        c[2], c[3].toInt(), c[4].toInt(), fechas(c[5]), fechas(c[6]),
                    )
                    "TRONCAL" -> check(Troncal.entries.any { it.name == c[1] && it.nombre == c[2] }) {
                        "La troncal ${c[1]} (${c[2]}) no coincide con el enum Troncal"
                    }
                    "ESTACION" -> crudas += c
                    "PATRON" -> patrones += mutableListOf(c)
                    "PERFIL", "VIAJES" -> patrones.last() += c
                }
            }
            val estaciones = crudas.map { c ->
                Estacion(c[1], Troncal.valueOf(c[5]), c[4] == "1", c[2].toDouble(), c[3].toDouble())
            }
            return Red(
                fuente, vigenteHasta, tarifa, estaciones,
                patrones.mapIndexed { i, lineas -> patron(i, lineas, estaciones) }, servicios,
            )
        }

        private fun fechas(texto: String): Set<Int> =
            if (texto.isEmpty()) emptySet() else texto.split(',').map { it.toInt() }.toSet()

        private fun patron(indice: Int, lineas: List<List<String>>, estaciones: List<Estacion>): Patron {
            val (_, codigo, paradas, andenes) = lineas.first()
            val secuencia = paradas.split(',').map { it.toInt() }.toIntArray()
            val ultima = estaciones[secuencia.last()]
            val ruta = Ruta(
                codigo = codigo,
                troncal = Troncal.entries.firstOrNull { it.name == codigo.take(1) } ?: ultima.troncal,
                hacia = ultima.nombre,
                expreso = codigo.first().isLetter(),
            )
            val perfiles = lineas.filter { it[0] == "PERFIL" }.map { c -> c[1].split(',').map { it.toInt() }.toIntArray() }
            val viajes = lineas.filter { it[0] == "VIAJES" }.associate { c ->
                val pares = c[2].split(',')
                c[1] to Pair(
                    IntArray(pares.size) { pares[it].substringBefore('.').toInt() },
                    IntArray(pares.size) { pares[it].substringAfter('.').toInt() },
                )
            }
            return Patron(indice, ruta, secuencia, andenes.split(';').map(Anden::leer), perfiles, viajes)
        }
    }
}

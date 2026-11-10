package com.example.myapplication.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.theme.Amarillo
import com.example.myapplication.ui.theme.Ambar
import com.example.myapplication.ui.theme.Azul
import com.example.myapplication.ui.theme.Coral
import com.example.myapplication.ui.theme.Lima
import com.example.myapplication.ui.theme.Morado
import com.example.myapplication.ui.theme.Turquesa
import com.example.myapplication.ui.theme.Verde
import java.io.InputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot

/*
 * Estaciones, rutas y horarios reales: salen del GTFS oficial de TransMilenio (ver Red.kt).
 * Los tiempos son los programados, no en vivo. Las alertas siguen siendo de ejemplo porque
 * TransMilenio no las publica en datos abiertos.
 */

/** Letras y nombres oficiales de TransMilenio. Los colores son los de la marca RoutiFy, no los oficiales. */
enum class Troncal(val nombre: String, val color: Color) {
    A("Caracas", Azul),
    B("Autopista Norte", Verde),
    C("Suba", Ambar),
    D("Calle 80", Morado),
    E("NQS Central", Turquesa),
    F("Américas", Coral),
    G("NQS Sur", Turquesa),
    H("Caracas Sur", Amarillo),
    J("Eje Ambiental", Lima),
    K("Calle 26", Lima),
    L("Carrera 10", Ambar),
    M("Carrera 7", Morado),
    Z("Avenida Ciudad de Cali", Coral);

    val letra: String get() = name
}

data class Estacion(
    val nombre: String,
    val troncal: Troncal,
    val accesible: Boolean,
    val latitud: Double,
    val longitud: Double,
)

/** Dónde esperar el bus: un vagón con sus puertas ("C", "2 ó 5") o una plataforma de portal ("T1"). */
data class Anden(val vagon: String, val puertas: String?) {
    val nombre: String get() = if (puertas == null) "plataforma $vagon" else "vagón $vagon"

    /** "el vagón C, puertas 2 ó 5" o "la plataforma T1", para usar dentro de una frase. */
    val enFrase: String
        get() = (if (puertas == null) "la " else "el ") + nombre + (puertas?.let { ", ${puertasTexto(it)}" } ?: "")

    /** "Vagón C · puertas 2 ó 5" o "Plataforma T1", para usar solo. */
    val detalle: String
        get() = nombre.replaceFirstChar { it.uppercase() } + (puertas?.let { " · ${puertasTexto(it)}" } ?: "")

    private fun puertasTexto(p: String) = if (p.all { it.isDigit() }) "puerta $p" else "puertas $p"

    companion object {
        fun leer(texto: String): Anden? = when {
            texto.isEmpty() -> null
            ':' in texto -> Anden(texto.substringBefore(':'), texto.substringAfter(':'))
            else -> Anden(texto, null)
        }
    }
}

enum class Ocupacion(val etiqueta: String, val nivel: Int) {
    BAJA("Con sillas", 1),
    MEDIA("Medio lleno", 2),
    ALTA("Muy lleno", 3),
}

/** [expreso]: para solo en algunas estaciones. Las rutas fáciles (solo números) paran en todas. */
data class Ruta(val codigo: String, val troncal: Troncal, val hacia: String, val expreso: Boolean)

sealed interface Paso {
    val minutos: Int

    data class Caminar(override val minutos: Int, val indicacion: String) : Paso

    data class Abordar(
        val ruta: Ruta,
        val desde: String,
        val paradas: List<String>,
        val anden: Anden?,
        val esperaMin: Int,
        override val minutos: Int,
    ) : Paso {
        val hasta: String get() = paradas.last()
    }

    data class Transbordo(
        override val minutos: Int,
        val estacion: String,
        val indicacion: String,
    ) : Paso
}

/** Un viaje planeado al pedirlo en [salida] (milisegundos); llega en [llegada]. */
data class OpcionViaje(
    val etiqueta: String,
    val origen: Estacion,
    val destino: Estacion,
    val salida: Long,
    val llegada: Long,
    val pasos: List<Paso>,
    val accesible: Boolean,
) {
    val duracionMin: Int get() = pasos.sumOf { it.minutos }
    val tramos: List<Paso.Abordar> get() = pasos.filterIsInstance<Paso.Abordar>()
    val transbordos: Int get() = (tramos.size - 1).coerceAtLeast(0)
    val caminataMin: Int get() = pasos.filterIsInstance<Paso.Caminar>().sumOf { it.minutos }
}

/** La próxima salida programada de una ruta desde una estación. */
data class Salida(val ruta: Ruta, val enMinutos: Int, val anden: Anden?)

/** La estación más cercana al usuario y la distancia en línea recta. */
data class Cercania(val estacion: Estacion, val metros: Int) {
    // A 75 m/min, con un 25 % extra porque las calles no van en línea recta
    val minutosCaminando: Int get() = ceil(metros * 1.25 / 75).toInt().coerceAtLeast(1)
}

enum class Gravedad { INFO, AVISO, CRITICA }

data class Alerta(
    val titulo: String,
    val detalle: String,
    val hace: String,
    val gravedad: Gravedad,
    val troncal: Troncal,
    val deLaComunidad: Boolean = false,
)

data class Favorito(val etiqueta: String, val estacion: Estacion)

object Transmilenio {

    private const val ARCHIVO = "troncal.txt"
    private var fuente: (() -> InputStream)? = null

    /** Hora de la app. En depuración se puede mover para probar horarios (ver MainActivity). */
    var reloj: () -> Long = System::currentTimeMillis

    fun inicializar(contexto: Context) = inicializar { contexto.assets.open(ARCHIVO) }

    fun inicializar(abrir: () -> InputStream) {
        if (fuente == null) fuente = abrir
    }

    internal val red: Red by lazy {
        checkNotNull(fuente) { "Llama a Transmilenio.inicializar antes de usar los datos" }().use { Red.leer(it) }
    }

    val TARIFA: Int get() = red.tarifa

    /** Último día que cubren los horarios cargados, como "31/12/2026". */
    val vigenteHasta: String
        get() = red.vigenteHasta.let { "%02d/%02d/%d".format(it % 100, it / 100 % 100, it / 10_000) }

    val estaciones: List<Estacion> get() = red.estaciones

    fun estacion(nombre: String): Estacion = red.estaciones[red.indiceDe.getValue(nombre)]

    val favoritos: List<Favorito> by lazy {
        listOf(
            Favorito("Casa", estacion("Portal Norte - Unicervantes")),
            Favorito("Trabajo", estacion("Calle 72 - Areandina")),
            Favorito("U", estacion("Ciudad Universitaria")),
        )
    }

    val recientes: List<Estacion> by lazy {
        listOf(estacion("Portal Américas"), estacion("CAN - British Council"), estacion("Ricaurte"))
    }

    val alertas = listOf(
        Alerta(
            "Desvío en la troncal Américas",
            "Por obras, las rutas F no paran en Banderas sentido oriente. Usa la estación Marsella.",
            "Hace 12 min", Gravedad.CRITICA, Troncal.F,
        ),
        Alerta(
            "Portal Norte muy lleno",
            "23 personas reportaron filas largas para abordar la B74.",
            "Hace 5 min", Gravedad.AVISO, Troncal.B, deLaComunidad = true,
        ),
        Alerta(
            "Ascensor fuera de servicio",
            "Estación Calle 100: el ascensor del costado occidental está en mantenimiento.",
            "Hace 40 min", Gravedad.AVISO, Troncal.B,
        ),
        Alerta(
            "Puerta del vagón 2 dañada",
            "Reportan que en Universidades la puerta del vagón 2 no abre. Usa el vagón 1 o 3.",
            "Hace 18 min", Gravedad.INFO, Troncal.K, deLaComunidad = true,
        ),
        Alerta(
            "Servicio normal en Suba",
            "Todas las rutas de la troncal operan con su frecuencia habitual.",
            "Hace 1 h", Gravedad.INFO, Troncal.C,
        ),
    )

    fun buscar(texto: String): List<Estacion> {
        val consulta = normalizar(texto)
        if (consulta.isEmpty()) return emptyList()
        return estaciones.filter { normalizar(it.nombre).contains(consulta) }
    }

    /** Sin tildes, mayúsculas ni puntuación, y con "avenida" y "carrera" abreviadas como en los letreros. */
    private fun normalizar(texto: String) =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\bavenida\\b"), "av")
            .replace(Regex("\\b(carrera|kr)\\b"), "cra")
            .trim()

    fun masCercana(latitud: Double, longitud: Double): Cercania {
        val estacion = estaciones.minBy { metros(latitud, longitud, it.latitud, it.longitud) }
        return Cercania(estacion, metros(latitud, longitud, estacion.latitud, estacion.longitud).toInt())
    }

    // Aproximación plana: sobra para distancias dentro de Bogotá
    private fun metros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val x = Math.toRadians(lon2 - lon1) * cos(Math.toRadians((lat1 + lat2) / 2))
        return 6_371_000 * hypot(x, Math.toRadians(lat2 - lat1))
    }

    /** Las próximas salidas programadas desde [estacion], una por ruta y destino, dentro de la próxima hora. */
    fun proximasSalidas(estacion: Estacion, ahora: Long = reloj(), cuantas: Int = 4): List<Salida> {
        val m = momento(ahora)
        val dia = red.viajesDelDia(m)
        return red.paradasEn[red.indiceDe.getValue(estacion.nombre)]
            .filter { (p, i) -> i < p.estaciones.lastIndex }
            .mapNotNull { (p, i) ->
                val viajes = dia[p.indice]
                val viaje = viajes.primero(i, m.segundos)
                if (viaje < 0) return@mapNotNull null
                Salida(p.ruta, (viajes.hora(viaje, i) - m.segundos) / 60, p.andenes[i])
            }
            .filter { it.enMinutos <= 60 }
            .groupBy { it.ruta.codigo to it.ruta.hacia }
            .map { (_, iguales) -> iguales.minBy { it.enMinutos } }
            .sortedBy { it.enMinutos }
            .take(cuantas)
    }

    /**
     * Opciones para ir de [origen] a [destino] saliendo en [salida] (milisegundos), caminando
     * antes [caminataMin] minutos hasta la estación. Para los mismos datos, siempre da lo mismo.
     */
    fun planearViaje(origen: Estacion, destino: Estacion, salida: Long, caminataMin: Int = 0): List<OpcionViaje> {
        if (origen == destino) return emptyList()
        val m = momento(salida)
        val desde = red.indiceDe.getValue(origen.nombre)
        val hasta = red.indiceDe.getValue(destino.nombre)
        val listo = m.segundos + caminataMin * 60
        val viajes = red.planear(desde, hasta, m, listo)
        if (viajes.isEmpty()) return emptyList()

        val porLlegada = compareBy<List<TramoRed>>({ it.last().llega }, { it.size })
        val rapido = viajes.minWith(porLlegada)
        val elegidos = mutableListOf("Más rápida" to rapido)
        val directo = viajes.first()
        if (directo.size < rapido.size) {
            elegidos += (if (directo.size == 1) "Sin transbordos" else "Menos transbordos") to directo
        }
        val otra = red.planear(desde, hasta, m, listo, excluir = rapido.first().patron.ruta.codigo).minWithOrNull(porLlegada)
        if (otra != null && otra.last().llega - rapido.last().llega <= 30 * 60 &&
            elegidos.none { (_, v) -> v.map { it.patron.ruta.codigo } == otra.map { it.patron.ruta.codigo } }
        ) {
            elegidos += "Otra ruta" to otra
        }
        return elegidos.map { (etiqueta, tramos) -> armar(etiqueta, origen, destino, salida, m.segundos, caminataMin, tramos) }
    }

    private fun armar(
        etiqueta: String,
        origen: Estacion,
        destino: Estacion,
        salida: Long,
        inicio: Int,
        caminataMin: Int,
        tramos: List<TramoRed>,
    ): OpcionViaje {
        // Los minutos de cada paso salen de redondear la hora acumulada, así suman exactamente el total
        fun minuto(segundos: Int) = Math.round((segundos - inicio) / 60.0).toInt()
        val nombres = red.estaciones.map { it.nombre }
        val pasos = mutableListOf<Paso>()
        var listo = inicio + caminataMin * 60
        if (caminataMin > 0) {
            pasos += Paso.Caminar(caminataMin, "Camina hasta la estación ${origen.nombre} y valida tu TuLlave.")
        }
        tramos.forEachIndexed { n, tramo ->
            val anden = tramo.patron.andenes[tramo.sube]
            if (n > 0) {
                val llegaAntes = tramos[n - 1].llega
                listo = llegaAntes + TRANSBORDO_SEG
                pasos += Paso.Transbordo(
                    minuto(listo) - minuto(llegaAntes),
                    nombres[tramo.patron.estaciones[tramo.sube]],
                    "Sin salir de la estación, busca ${anden?.enFrase ?: "el ${tramo.patron.ruta.codigo}"}. " +
                        "No pagas otro pasaje.",
                )
            }
            pasos += Paso.Abordar(
                ruta = tramo.patron.ruta,
                desde = nombres[tramo.patron.estaciones[tramo.sube]],
                paradas = (tramo.sube + 1..tramo.baja).map { nombres[tramo.patron.estaciones[it]] },
                anden = anden,
                esperaMin = minuto(tramo.sale) - minuto(listo),
                minutos = minuto(tramo.llega) - minuto(listo),
            )
        }
        val usadas = listOf(origen, destino) + tramos.drop(1).map { red.estaciones[it.patron.estaciones[it.sube]] }
        return OpcionViaje(
            etiqueta = etiqueta,
            origen = origen,
            destino = destino,
            salida = salida,
            llegada = salida + (tramos.last().llega - inicio) * 1000L,
            pasos = pasos,
            accesible = usadas.all { it.accesible },
        )
    }

    /** Mueve el reloj de la app a [hora] ("HH:MM") de hoy en Bogotá. Solo para probar en depuración. */
    fun fijarHora(hora: String) {
        val (h, m) = hora.split(':').map { it.toInt() }
        val objetivo = Calendar.getInstance(bogota).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val desfase = objetivo - System.currentTimeMillis()
        reloj = { System.currentTimeMillis() + desfase }
    }
}

private val colombia: Locale = Locale.forLanguageTag("es-CO")

/** "8:05 a. m." en hora de Bogotá. */
fun hora(milis: Long): String =
    SimpleDateFormat("h:mm a", colombia).apply { timeZone = bogota }.format(milis)

fun pesos(valor: Int): String = "$" + String.format(colombia, "%,d", valor)

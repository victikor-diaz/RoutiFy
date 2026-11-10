package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.data.Estacion
import com.example.myapplication.data.OpcionViaje
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.ui.screens.AlertasScreen
import com.example.myapplication.ui.screens.BuscarScreen
import com.example.myapplication.ui.screens.DetalleScreen
import com.example.myapplication.ui.screens.EnViajeScreen
import com.example.myapplication.ui.screens.InicioScreen
import com.example.myapplication.ui.screens.PerfilScreen
import com.example.myapplication.ui.screens.ResultadosScreen

sealed interface Pantalla {
    data object Inicio : Pantalla
    data object Alertas : Pantalla
    data object Perfil : Pantalla
    data object Buscar : Pantalla

    /** [salida]: cuándo se pidió el viaje; los horarios se calculan desde ese instante. */
    data class Resultados(val origen: Estacion, val destino: Estacion, val salida: Long, val caminataMin: Int) : Pantalla
    data class Detalle(val opcion: OpcionViaje) : Pantalla
    data class EnViaje(val opcion: OpcionViaje) : Pantalla
}

data class Preferencias(
    val priorizarAccesibles: Boolean = false,
    val avisoAntesDeBajar: Boolean = true,
    val leerEnVozAlta: Boolean = false,
)

// Android recrea la actividad al girar el teléfono o cambiar el tema. Para no perder la
// pantalla actual, cada una se guarda como texto; como el planificador es determinista,
// basta con las estaciones, la hora de salida y la caminata para reconstruir la opción de viaje.
internal fun Pantalla.codificar(): String = when (this) {
    Pantalla.Inicio -> "inicio"
    Pantalla.Alertas -> "alertas"
    Pantalla.Perfil -> "perfil"
    Pantalla.Buscar -> "buscar"
    is Pantalla.Resultados -> listOf("resultados", origen.nombre, destino.nombre, salida, caminataMin).joinToString("|")
    is Pantalla.Detalle -> (listOf("detalle") + opcion.claves()).joinToString("|")
    is Pantalla.EnViaje -> (listOf("enviaje") + opcion.claves()).joinToString("|")
}

private fun OpcionViaje.claves() = listOf(origen.nombre, destino.nombre, salida, caminataMin, etiqueta)

internal fun decodificarPantalla(texto: String): Pantalla {
    val partes = texto.split("|")
    fun resultados() = Pantalla.Resultados(
        Transmilenio.estacion(partes[1]), Transmilenio.estacion(partes[2]), partes[3].toLong(), partes[4].toInt(),
    )
    fun opcion() = resultados().let { Transmilenio.planearViaje(it.origen, it.destino, it.salida, it.caminataMin) }
        .first { it.etiqueta == partes[5] }
    return when (partes[0]) {
        "alertas" -> Pantalla.Alertas
        "perfil" -> Pantalla.Perfil
        "buscar" -> Pantalla.Buscar
        "resultados" -> resultados()
        "detalle" -> Pantalla.Detalle(opcion())
        "enviaje" -> Pantalla.EnViaje(opcion())
        else -> Pantalla.Inicio
    }
}

private val PilaSaver = listSaver<SnapshotStateList<Pantalla>, String>(
    save = { pila -> pila.map { it.codificar() } },
    restore = { guardada -> guardada.map(::decodificarPantalla).toMutableStateList() },
)

private val PreferenciasSaver = listSaver<Preferencias, Boolean>(
    save = { listOf(it.priorizarAccesibles, it.avisoAntesDeBajar, it.leerEnVozAlta) },
    restore = { Preferencias(it[0], it[1], it[2]) },
)

private class Pestana(val pantalla: Pantalla, val etiqueta: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana(Pantalla.Inicio, "Inicio", Icons.Filled.Home),
    Pestana(Pantalla.Alertas, "Alertas", Icons.Filled.Notifications),
    Pestana(Pantalla.Perfil, "Perfil", Icons.Filled.Person),
)

/**
 * Navegación del prototipo con una pila simple de pantallas, sin dependencias extra.
 * Cuando crezca, conviene pasar a Navigation Compose y a un ViewModel.
 */
@Composable
fun RoutiFyApp() {
    val pila = rememberSaveable(saver = PilaSaver) { mutableStateListOf<Pantalla>(Pantalla.Inicio) }
    var preferencias by rememberSaveable(stateSaver = PreferenciasSaver) { mutableStateOf(Preferencias()) }
    var saldo by rememberSaveable { mutableIntStateOf(12_400) }
    val actual = pila.last()

    val ubicacion = rememberUbicacion()
    var permisoPedido by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (ubicacion.ubicacion == Ubicacion.SinPermiso && !permisoPedido) {
            permisoPedido = true
            ubicacion.pedirPermiso()
        }
    }
    val lugar = ubicacion.ubicacion
    val cercania = remember(lugar) { (lugar as? Ubicacion.Lista)?.let { Transmilenio.masCercana(it.latitud, it.longitud) } }

    fun ir(destino: Pantalla) {
        pila.add(destino)
    }

    fun planear(origen: Estacion, destino: Estacion) {
        val caminata = if (origen == cercania?.estacion) cercania.minutosCaminando else 0
        ir(Pantalla.Resultados(origen, destino, Transmilenio.reloj(), caminata))
    }

    fun volver() {
        if (pila.size > 1) pila.removeAt(pila.lastIndex)
    }

    fun irAPestana(pestana: Pantalla) {
        pila.clear()
        pila.add(pestana)
    }

    BackHandler(enabled = pila.size > 1) { volver() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (pestanas.any { it.pantalla == actual }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    pestanas.forEach { pestana ->
                        NavigationBarItem(
                            selected = actual == pestana.pantalla,
                            onClick = { irAPestana(pestana.pantalla) },
                            icon = { Icon(pestana.icono, contentDescription = null) },
                            label = { Text(pestana.etiqueta) },
                        )
                    }
                }
            }
        },
    ) { relleno ->
        AnimatedContent(
            targetState = actual,
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .consumeWindowInsets(relleno),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "pantalla",
        ) { pantalla ->
            when (pantalla) {
                Pantalla.Inicio -> InicioScreen(
                    saldo = saldo,
                    ubicacion = lugar,
                    cercania = cercania,
                    alPedirUbicacion = ubicacion.pedirPermiso,
                    alBuscar = { ir(Pantalla.Buscar) },
                    alIrA = { destino ->
                        val origen = cercania?.estacion
                        if (origen != null && origen != destino) planear(origen, destino) else ir(Pantalla.Buscar)
                    },
                    alVerAlertas = { irAPestana(Pantalla.Alertas) },
                    alVerPerfil = { irAPestana(Pantalla.Perfil) },
                )

                Pantalla.Alertas -> AlertasScreen()

                Pantalla.Perfil -> PerfilScreen(
                    saldo = saldo,
                    preferencias = preferencias,
                    alCambiarPreferencias = { preferencias = it },
                    alRecargar = { saldo += 10_000 },
                )

                Pantalla.Buscar -> BuscarScreen(
                    cercana = cercania?.estacion,
                    alVolver = { volver() },
                    alPlanear = { origen, destino -> planear(origen, destino) },
                )

                is Pantalla.Resultados -> ResultadosScreen(
                    origen = pantalla.origen,
                    destino = pantalla.destino,
                    salida = pantalla.salida,
                    caminataMin = pantalla.caminataMin,
                    priorizarAccesibles = preferencias.priorizarAccesibles,
                    alVolver = { volver() },
                    alElegir = { ir(Pantalla.Detalle(it)) },
                )

                is Pantalla.Detalle -> DetalleScreen(
                    opcion = pantalla.opcion,
                    avisoAntesDeBajar = preferencias.avisoAntesDeBajar,
                    alVolver = { volver() },
                    alIniciar = { ir(Pantalla.EnViaje(pantalla.opcion)) },
                )

                is Pantalla.EnViaje -> EnViajeScreen(
                    opcion = pantalla.opcion,
                    preferencias = preferencias,
                    alVolver = { volver() },
                    alTerminar = { irAPestana(Pantalla.Inicio) },
                )
            }
        }
    }
}

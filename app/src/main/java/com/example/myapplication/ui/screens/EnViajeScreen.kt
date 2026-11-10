package com.example.myapplication.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Ocupacion
import com.example.myapplication.data.OpcionViaje
import com.example.myapplication.data.Paso
import com.example.myapplication.ui.Preferencias
import com.example.myapplication.ui.components.BarraSuperior
import com.example.myapplication.ui.components.FranjaMarca
import com.example.myapplication.ui.components.InsigniaRuta
import com.example.myapplication.ui.components.Tarjeta
import com.example.myapplication.ui.theme.Ambar
import com.example.myapplication.ui.theme.Tinta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private data class ParadaEnRuta(val nombre: String, val tramo: Int, val finDeTramo: Boolean)

/**
 * Acompañamiento durante el viaje. En el prototipo el bus avanza solo; en la app real
 * la posición vendría del GPS del teléfono comparado con el trazado de la ruta.
 */
@Composable
fun EnViajeScreen(
    opcion: OpcionViaje,
    preferencias: Preferencias,
    alVolver: () -> Unit,
    alTerminar: () -> Unit,
) {
    val tramos = opcion.tramos
    val recorrido = remember(opcion) {
        tramos.flatMapIndexed { t, tramo ->
            tramo.paradas.mapIndexed { i, nombre -> ParadaEnRuta(nombre, t, i == tramo.paradas.lastIndex) }
        }
    }
    // -1: todavía esperando el bus en la estación de origen
    var indice by rememberSaveable(opcion) { mutableIntStateOf(-1) }
    LaunchedEffect(opcion) {
        delay(3_000)
        while (indice < recorrido.lastIndex) {
            indice++
            delay(if (recorrido[indice].finDeTramo) 4_500 else 2_500)
        }
    }

    val llego = indice == recorrido.lastIndex
    val actual = recorrido.getOrNull(indice)
    val enTransbordo = actual != null && actual.finDeTramo && !llego
    val numTramo = when {
        actual == null -> 0
        enTransbordo -> actual.tramo + 1
        else -> actual.tramo
    }
    val tramo = tramos[numTramo]
    val restantes = recorrido.withIndex().count { (i, parada) -> i > indice && parada.tramo == numTramo }
    val faltaUna = actual != null && !enTransbordo && !llego && restantes == 1

    val enAnden = tramo.anden?.let { " en ${it.enFrase}" } ?: ""
    val mensaje = when {
        llego -> "Llegaste a ${opcion.destino.nombre}. Buen viaje."
        actual == null -> "Espera el ${tramo.ruta.codigo}$enAnden."
        enTransbordo -> "Bájate en ${actual.nombre} y haz transbordo al ${tramo.ruta.codigo}$enAnden."
        faltaUna -> "Prepárate. Te bajas en la próxima parada: ${tramo.hasta}."
        else -> "Próxima parada: ${recorrido[indice + 1].nombre}."
    }

    val voz = rememberVoz(preferencias.leerEnVozAlta)
    LaunchedEffect(mensaje, voz) {
        voz?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "routify")
    }
    val haptica = LocalHapticFeedback.current
    LaunchedEffect(faltaUna, enTransbordo, llego) {
        if (preferencias.avisoAntesDeBajar && (faltaUna || enTransbordo || llego)) {
            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val progreso by animateFloatAsState((indice + 1f) / recorrido.size, label = "progreso")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior(
                titulo = "En viaje",
                alVolver = alVolver,
                subtitulo = "${opcion.origen.nombre} → ${opcion.destino.nombre}",
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PanelPrincipal(
                    tramo = tramo,
                    titulo = when {
                        llego -> "Llegaste"
                        actual == null -> "Espera en ${opcion.origen.nombre}"
                        enTransbordo -> "Transbordo"
                        else -> "Próxima parada"
                    },
                    principal = when {
                        llego -> opcion.destino.nombre
                        actual == null -> tramo.anden?.nombre?.replaceFirstChar { it.uppercase() } ?: tramo.ruta.codigo
                        enTransbordo -> actual.nombre
                        else -> recorrido[indice + 1].nombre
                    },
                    secundario = when {
                        llego -> "Viaje de unos ${opcion.duracionMin} min"
                        actual == null -> "El ${tramo.ruta.codigo} llega en ${tramo.esperaMin} min"
                        enTransbordo -> "Busca el ${tramo.ruta.codigo}$enAnden"
                        else -> "Te bajas en ${tramo.hasta} · " +
                            if (restantes == 1) "es la siguiente" else "faltan $restantes paradas"
                    },
                    progreso = progreso,
                    origen = opcion.origen.nombre,
                    destino = opcion.destino.nombre,
                )
                if (faltaUna) AvisoBajada(tramo.hasta)
                if (!llego) ParadasDelTramo(tramo, recorrido, indice, numTramo)
                if (actual != null && !enTransbordo && !llego) {
                    ReporteOcupacion(tramo.ruta.codigo) {
                        alcance.launch {
                            snackbar.showSnackbar("Gracias. Tu reporte ayuda a quienes esperan el ${tramo.ruta.codigo}.")
                        }
                    }
                }
                Text(
                    "Simulación: el bus avanza solo cada pocos segundos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
                val modificador = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
                if (llego) {
                    Button(onClick = alTerminar, modifier = modificador, shape = RoundedCornerShape(16.dp)) {
                        Text("Terminar viaje", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    OutlinedButton(onClick = alTerminar, modifier = modificador, shape = RoundedCornerShape(16.dp)) {
                        Text("Salir del viaje", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )
    }
}

@Composable
private fun rememberVoz(activa: Boolean): TextToSpeech? {
    val contexto = LocalContext.current
    var voz by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(activa) {
        if (!activa) return@DisposableEffect onDispose { }
        var tts: TextToSpeech? = null
        tts = TextToSpeech(contexto) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.forLanguageTag("es-CO"))
                voz = tts
            }
        }
        onDispose {
            tts?.shutdown()
            voz = null
        }
    }
    return voz
}

@Composable
private fun PanelPrincipal(
    tramo: Paso.Abordar,
    titulo: String,
    principal: String,
    secundario: String,
    progreso: Float,
    origen: String,
    destino: String,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        val tenue = LocalContentColor.current.copy(alpha = 0.7f)
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InsigniaRuta(tramo.ruta, grande = true)
                Spacer(Modifier.width(12.dp))
                Text(
                    "hacia ${tramo.ruta.hacia}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(titulo.uppercase(), style = MaterialTheme.typography.labelLarge, color = tenue)
            Text(principal, style = MaterialTheme.typography.headlineMedium)
            Text(secundario, style = MaterialTheme.typography.bodyMedium, color = tenue)
            Spacer(Modifier.height(20.dp))
            BarraProgreso(progreso)
            Spacer(Modifier.height(8.dp))
            Row {
                Text(origen, style = MaterialTheme.typography.bodySmall, color = tenue, modifier = Modifier.weight(1f))
                Text(destino, style = MaterialTheme.typography.bodySmall, color = tenue)
            }
        }
    }
}

/** Barra de avance pintada con las franjas del logo. */
@Composable
private fun BarraProgreso(progreso: Float) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(LocalContentColor.current.copy(alpha = 0.15f))
    ) {
        val ancho = maxWidth
        Box(
            Modifier
                .fillMaxHeight()
                .width(ancho * progreso.coerceIn(0f, 1f))
                .clipToBounds()
        ) {
            FranjaMarca(
                Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .width(ancho),
                alto = 8.dp,
            )
        }
    }
}

@Composable
private fun AvisoBajada(parada: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Ambar, contentColor = Tinta) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("¡Prepárate para bajar!", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tu parada, $parada, es la siguiente. Acércate a la puerta.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ParadasDelTramo(tramo: Paso.Abordar, recorrido: List<ParadaEnRuta>, indice: Int, numTramo: Int) {
    Tarjeta {
        Text("Paradas del ${tramo.ruta.codigo}", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        recorrido.forEachIndexed { i, parada ->
            if (parada.tramo != numTramo) return@forEachIndexed
            val pasada = i < indice
            val aqui = i == indice
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(if (aqui || parada.finDeTramo) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                aqui || parada.finDeTramo -> tramo.ruta.troncal.color
                                pasada -> MaterialTheme.colorScheme.outlineVariant
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    parada.nombre + if (parada.finDeTramo) " · te bajas aquí" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (aqui || parada.finDeTramo) FontWeight.Bold else FontWeight.Normal,
                    color = if (pasada) MaterialTheme.colorScheme.outline else Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun ReporteOcupacion(codigo: String, alReportar: (Ocupacion) -> Unit) {
    var elegida by remember(codigo) { mutableStateOf<Ocupacion?>(null) }
    Tarjeta {
        Text("¿Cómo va el $codigo?", style = MaterialTheme.typography.titleSmall)
        Text(
            "Con un toque ayudas a quienes lo están esperando.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ocupacion.entries.forEach { ocupacion ->
                val contenido: @Composable () -> Unit = {
                    Text(ocupacion.etiqueta, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                val alPulsar = {
                    elegida = ocupacion
                    alReportar(ocupacion)
                }
                val relleno = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                if (elegida == ocupacion) {
                    Button(onClick = alPulsar, Modifier.weight(1f), contentPadding = relleno) { contenido() }
                } else {
                    OutlinedButton(onClick = alPulsar, Modifier.weight(1f), contentPadding = relleno) { contenido() }
                }
            }
        }
    }
}

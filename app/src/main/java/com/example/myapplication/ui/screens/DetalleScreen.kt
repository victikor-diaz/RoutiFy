package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.OpcionViaje
import com.example.myapplication.data.Paso
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.data.hora
import com.example.myapplication.ui.components.BarraSuperior
import com.example.myapplication.ui.components.InsigniaRuta
import com.example.myapplication.ui.components.Tarjeta
import com.example.myapplication.ui.components.textoEncima
import com.example.myapplication.ui.theme.RoutiFyTheme

@Composable
fun DetalleScreen(
    opcion: OpcionViaje,
    avisoAntesDeBajar: Boolean,
    alVolver: () -> Unit,
    alIniciar: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = "${opcion.duracionMin} min · llegas ${hora(opcion.llegada)}",
            alVolver = alVolver,
            subtitulo = "${opcion.origen.nombre} → ${opcion.destino.nombre}",
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        ) {
            items(opcion.pasos) { FilaPaso(it) }
            item { FilaLlegada(opcion.destino.nombre) }
            item {
                Spacer(Modifier.height(20.dp))
                Consejos(opcion, avisoAntesDeBajar)
            }
        }
        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
            Button(
                onClick = alIniciar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Iniciar viaje", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Una fila de la línea de tiempo: ícono y trazo a la izquierda, indicaciones a la derecha. */
@Composable
private fun FilaPaso(paso: Paso) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            Modifier
                .width(40.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconoPaso(paso)
            if (paso is Paso.Abordar) {
                Box(
                    Modifier
                        .weight(1f)
                        .width(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(paso.ruta.troncal.color)
                )
            } else {
                LineaPunteada(Modifier.weight(1f), MaterialTheme.colorScheme.outline)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 6.dp, bottom = 24.dp)
        ) {
            when (paso) {
                is Paso.Caminar -> {
                    Text("Camina ${paso.minutos} min", style = MaterialTheme.typography.titleMedium)
                    Text(
                        paso.indicacion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is Paso.Transbordo -> {
                    Text("Transbordo en ${paso.estacion}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        paso.indicacion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is Paso.Abordar -> ContenidoAbordar(paso)
            }
        }
    }
}

@Composable
private fun IconoPaso(paso: Paso) {
    val (fondo, icono: Painter) = when (paso) {
        is Paso.Caminar -> MaterialTheme.colorScheme.surfaceVariant to painterResource(R.drawable.ic_caminar)
        is Paso.Abordar -> paso.ruta.troncal.color to painterResource(R.drawable.ic_bus)
        is Paso.Transbordo -> MaterialTheme.colorScheme.primary to painterResource(R.drawable.ic_intercambiar)
    }
    val tinte = when (paso) {
        is Paso.Caminar -> MaterialTheme.colorScheme.onSurfaceVariant
        is Paso.Abordar -> fondo.textoEncima()
        is Paso.Transbordo -> MaterialTheme.colorScheme.onPrimary
    }
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, Modifier.size(20.dp), tint = tinte)
    }
}

@Composable
private fun LineaPunteada(modifier: Modifier, color: Color) {
    Canvas(modifier.width(4.dp)) {
        val x = size.width / 2
        drawLine(
            color = color,
            start = Offset(x, 6.dp.toPx()),
            end = Offset(x, size.height),
            strokeWidth = size.width,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 9.dp.toPx())),
        )
    }
}

@Composable
private fun ContenidoAbordar(paso: Paso.Abordar) {
    var verParadas by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        InsigniaRuta(paso.ruta, grande = true)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("hacia ${paso.ruta.hacia}", style = MaterialTheme.typography.titleMedium)
            Text(
                if (paso.ruta.expreso) "Expreso" else "Ruta fácil · para en todas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Sube en ${paso.desde}", style = MaterialTheme.typography.bodyMedium)
    paso.anden?.let { Text(it.detalle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
    Text(
        "Próximo bus en ${paso.esperaMin} min",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.SemiBold,
    )
    TextButton(onClick = { verParadas = !verParadas }, contentPadding = PaddingValues(0.dp)) {
        val cuantas = if (paso.paradas.size == 1) "1 parada" else "${paso.paradas.size} paradas"
        Text("$cuantas · bájate en ${paso.hasta}")
        Icon(
            if (verParadas) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (verParadas) "Ocultar paradas" else "Ver paradas",
        )
    }
    AnimatedVisibility(verParadas) {
        Column {
            paso.paradas.forEachIndexed { i, parada ->
                val ultima = i == paso.paradas.lastIndex
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (ultima) paso.ruta.troncal.color else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        parada,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (ultima) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaLlegada(destino: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text("Llegas a $destino", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Consejos(opcion: OpcionViaje, avisoAntesDeBajar: Boolean) {
    val primerTramo = opcion.tramos.first()
    val consejos = buildList {
        add("Ten tu TuLlave a mano antes de llegar al torniquete.")
        primerTramo.anden?.let { add("Ubícate en ${it.enFrase}: ahí para el ${primerTramo.ruta.codigo}.") }
        if (opcion.transbordos > 0) add("En el transbordo no sales de la estación ni pagas de nuevo.")
        if (avisoAntesDeBajar) add("Te avisaremos una parada antes de bajarte.")
    }
    Tarjeta {
        Text("Consejos para este viaje", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        consejos.forEach {
            Text(
                "•  $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DetallePreview() {
    Transmilenio.inicializar(LocalContext.current)
    val opcion = Transmilenio.planearViaje(
        Transmilenio.estacion("Portal Norte - Unicervantes"),
        Transmilenio.estacion("Universidades CityU"),
        Transmilenio.reloj(),
    ).firstOrNull() ?: return
    RoutiFyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DetalleScreen(opcion, avisoAntesDeBajar = true, alVolver = {}, alIniciar = {})
        }
    }
}

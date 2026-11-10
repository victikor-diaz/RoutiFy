@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.Gravedad
import com.example.myapplication.data.OpcionViaje
import com.example.myapplication.data.Ruta
import com.example.myapplication.data.Troncal
import com.example.myapplication.ui.theme.Ambar
import com.example.myapplication.ui.theme.Coral
import com.example.myapplication.ui.theme.FranjasMarca
import com.example.myapplication.ui.theme.Tinta
import com.example.myapplication.ui.theme.Verde

/** Texto negro o blanco, el que mejor contraste tenga sobre este color. */
fun Color.textoEncima(): Color = if (luminance() > 0.18f) Tinta else Color.White

fun colorDeGravedad(gravedad: Gravedad): Color = when (gravedad) {
    Gravedad.INFO -> Verde
    Gravedad.AVISO -> Ambar
    Gravedad.CRITICA -> Coral
}

/** Las franjas del logo, firma visual de RoutiFy. */
@Composable
fun FranjaMarca(modifier: Modifier = Modifier, alto: Dp = 6.dp) {
    Row(
        modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(alto / 2))
    ) {
        FranjasMarca.forEach { color ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
fun LogoRoutiFy(modifier: Modifier = Modifier, tamano: Dp = 48.dp) {
    Surface(
        modifier = modifier.size(tamano),
        shape = RoundedCornerShape(tamano * 0.28f),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Image(
            painter = painterResource(R.drawable.routify_logo),
            contentDescription = "Logo de RoutiFy",
            modifier = Modifier.padding(tamano * 0.1f),
        )
    }
}

/** Código de ruta: relleno para expresos, solo borde para rutas fáciles. */
@Composable
fun InsigniaRuta(ruta: Ruta, modifier: Modifier = Modifier, grande: Boolean = false) {
    val color = ruta.troncal.color
    val forma = RoundedCornerShape(if (grande) 10.dp else 6.dp)
    Box(
        modifier = modifier
            .clip(forma)
            .background(if (ruta.expreso) color else MaterialTheme.colorScheme.surface)
            .then(if (ruta.expreso) Modifier else Modifier.border(2.dp, color, forma))
            .padding(horizontal = if (grande) 12.dp else 8.dp, vertical = if (grande) 6.dp else 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ruta.codigo,
            color = if (ruta.expreso) color.textoEncima() else MaterialTheme.colorScheme.onSurface,
            style = if (grande) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun InsigniaTroncal(troncal: Troncal, modifier: Modifier = Modifier, tamano: Dp = 28.dp) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(troncal.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = troncal.letra,
            color = troncal.color.textoEncima(),
            fontWeight = FontWeight.Black,
            fontSize = (tamano.value * 0.5f).sp,
        )
    }
}

/** Caminar → rutas → destino, en una sola línea. */
@Composable
fun CadenaDeRuta(opcion: OpcionViaje, modifier: Modifier = Modifier) {
    val tenue = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        if (opcion.caminataMin > 0) {
            Icon(painterResource(R.drawable.ic_caminar), contentDescription = "Caminar", Modifier.size(20.dp), tint = tenue)
        }
        opcion.tramos.forEachIndexed { i, tramo ->
            if (i > 0 || opcion.caminataMin > 0) Flecha()
            InsigniaRuta(tramo.ruta)
        }
        Flecha()
        Icon(Icons.Filled.Place, contentDescription = "Destino", Modifier.size(20.dp))
    }
}

@Composable
private fun Flecha() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.outline,
    )
}

@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.primary,
    color: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Text(
        text = texto,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    alPulsar: (() -> Unit)? = null,
    destacada: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(20.dp)
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val borde = if (destacada) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    if (alPulsar != null) {
        Card(onClick = alPulsar, modifier = modifier.fillMaxWidth(), shape = forma, colors = colores, border = borde) {
            Column(Modifier.padding(16.dp)) { contenido() }
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = forma, colors = colores, border = borde) {
            Column(Modifier.padding(16.dp)) { contenido() }
        }
    }
}

@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    alAccion: () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (accion != null) {
            TextButton(onClick = alAccion) { Text(accion) }
        }
    }
}

@Composable
fun BarraSuperior(
    titulo: String,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = alVolver) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitulo != null) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

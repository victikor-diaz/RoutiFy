@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.Cercania
import com.example.myapplication.data.Estacion
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.data.bogota
import com.example.myapplication.data.pesos
import com.example.myapplication.ui.Ubicacion
import com.example.myapplication.ui.components.Etiqueta
import com.example.myapplication.ui.components.FranjaMarca
import com.example.myapplication.ui.components.InsigniaRuta
import com.example.myapplication.ui.components.InsigniaTroncal
import com.example.myapplication.ui.components.LogoRoutiFy
import com.example.myapplication.ui.components.Tarjeta
import com.example.myapplication.ui.components.TituloSeccion
import com.example.myapplication.ui.components.colorDeGravedad
import com.example.myapplication.ui.theme.RoutiFyTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

@Composable
fun InicioScreen(
    saldo: Int,
    ubicacion: Ubicacion,
    cercania: Cercania?,
    alPedirUbicacion: () -> Unit,
    alBuscar: () -> Unit,
    alIrA: (Estacion) -> Unit,
    alVerAlertas: () -> Unit,
    alVerPerfil: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Encabezado(alVerAlertas) }
        item { Buscador(alBuscar) }
        item { MisLugares(alIrA) }
        item { CercaDeTi(ubicacion, cercania, alPedirUbicacion) }
        item { EstadoDelServicio(alVerAlertas) }
        item { SaldoTuLlave(saldo, alVerPerfil) }
    }
}

// Con el reloj de la app, para que coincida con la hora fingida en las pruebas (intent "hora")
private fun saludo(): String {
    val hora = Calendar.getInstance(bogota).apply { timeInMillis = Transmilenio.reloj() }.get(Calendar.HOUR_OF_DAY)
    return when (hora) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
private fun Encabezado(alVerAlertas: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoRoutiFy(tamano = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("RoutiFy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "Tu ruta en TransMilenio, sin enredos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Sin contador: las alertas todavía son de ejemplo
            IconButton(onClick = alVerAlertas) {
                Icon(Icons.Filled.Notifications, contentDescription = "Alertas")
            }
        }
        Spacer(Modifier.height(14.dp))
        FranjaMarca()
    }
}

@Composable
private fun Buscador(alBuscar: () -> Unit) {
    Column {
        Text(saludo(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("¿A dónde vas hoy?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(14.dp))
        Surface(
            onClick = alBuscar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Busca tu estación de destino",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painterResource(R.drawable.ic_bus),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MisLugares(alIrA: (Estacion) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Transmilenio.favoritos.forEach { favorito ->
            Surface(
                onClick = { alIrA(favorito.estacion) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InsigniaTroncal(favorito.estacion.troncal, tamano = 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(favorito.etiqueta, style = MaterialTheme.typography.labelLarge)
                        Text(
                            favorito.estacion.nombre,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CercaDeTi(ubicacion: Ubicacion, cercania: Cercania?, alPedirUbicacion: () -> Unit) {
    Column {
        TituloSeccion("Cerca de ti")
        Spacer(Modifier.height(8.dp))
        Tarjeta {
            if (cercania == null) SinUbicacion(ubicacion, alPedirUbicacion) else EstacionCercana(cercania)
        }
    }
}

@Composable
private fun EstacionCercana(cercania: Cercania) {
    val estacion = cercania.estacion
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaTroncal(estacion.troncal)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Estación ${estacion.nombre}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${distancia(cercania.metros)} · ${cercania.minutosCaminando} min caminando",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painterResource(R.drawable.ic_caminar),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        // Se recalcula cada 30 s para que los minutos no se queden quietos
        var ahora by remember { mutableLongStateOf(Transmilenio.reloj()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30_000)
                ahora = Transmilenio.reloj()
            }
        }
        val salidas = remember(estacion, ahora) { Transmilenio.proximasSalidas(estacion, ahora) }
        if (salidas.isEmpty()) {
            Text(
                "No hay salidas programadas en la próxima hora.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        salidas.forEachIndexed { i, salida ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InsigniaRuta(salida.ruta, Modifier.widthIn(min = 52.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("hacia ${salida.ruta.hacia}", style = MaterialTheme.typography.bodyMedium)
                    salida.anden?.let {
                        Text(
                            it.detalle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    if (salida.enMinutos == 0) "Ya" else "${salida.enMinutos} min",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Text(
            "Según el horario programado de TransMilenio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun SinUbicacion(ubicacion: Ubicacion, alPedirUbicacion: () -> Unit) {
    val texto = when (ubicacion) {
        Ubicacion.Buscando -> "Buscando tu ubicación…"
        Ubicacion.NoDisponible -> "No pudimos saber dónde estás. Revisa que la ubicación del teléfono esté activa."
        else -> "Activa tu ubicación para ver la estación más cercana y sus próximos buses."
    }
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (ubicacion == Ubicacion.SinPermiso) {
        TextButton(onClick = alPedirUbicacion, contentPadding = PaddingValues(0.dp)) {
            Text("Usar mi ubicación")
        }
    }
}

private fun distancia(metros: Int): String =
    if (metros < 1000) "${(metros + 5) / 10 * 10} m" else String.format(Locale.forLanguageTag("es-CO"), "%.1f km", metros / 1000.0)

@Composable
private fun EstadoDelServicio(alVerAlertas: () -> Unit) {
    Column {
        TituloSeccion("Estado del servicio", accion = "Ver todo", alAccion = alVerAlertas)
        Etiqueta(
            "Datos de ejemplo",
            fondo = MaterialTheme.colorScheme.secondaryContainer,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.height(8.dp))
        Tarjeta(alPulsar = alVerAlertas) {
            Transmilenio.alertas.take(2).forEachIndexed { i, alerta ->
                if (i > 0) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row {
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorDeGravedad(alerta.gravedad))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(alerta.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(
                            alerta.hace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaldoTuLlave(saldo: Int, alVerPerfil: () -> Unit) {
    Tarjeta(alPulsar = alVerPerfil) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Saldo TuLlave",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(pesos(saldo), style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Te alcanza para ${saldo / Transmilenio.TARIFA} viajes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun InicioPreview() {
    RoutiFyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Transmilenio.inicializar(LocalContext.current)
            val calle100 = Transmilenio.estacion("Calle 100 - Marketmedios")
            InicioScreen(
                saldo = 12_400,
                ubicacion = Ubicacion.Lista(calle100.latitud, calle100.longitud),
                cercania = Cercania(calle100, 350),
                alPedirUbicacion = {},
                alBuscar = {},
                alIrA = {},
                alVerAlertas = {},
                alVerPerfil = {},
            )
        }
    }
}

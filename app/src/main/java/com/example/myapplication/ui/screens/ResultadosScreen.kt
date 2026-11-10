@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Estacion
import com.example.myapplication.data.OpcionViaje
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.data.hora
import com.example.myapplication.ui.components.BarraSuperior
import com.example.myapplication.ui.components.CadenaDeRuta
import com.example.myapplication.ui.components.Etiqueta
import com.example.myapplication.ui.components.Tarjeta
import com.example.myapplication.ui.theme.RoutiFyTheme

private enum class Criterio(val etiqueta: String) {
    RAPIDA("Más rápida"),
    TRANSBORDOS("Menos transbordos"),
    SALE_PRIMERO("Sale primero"),
}

@Composable
fun ResultadosScreen(
    origen: Estacion,
    destino: Estacion,
    salida: Long,
    caminataMin: Int,
    priorizarAccesibles: Boolean,
    alVolver: () -> Unit,
    alElegir: (OpcionViaje) -> Unit,
) {
    val opciones = remember(origen, destino, salida, caminataMin) {
        Transmilenio.planearViaje(origen, destino, salida, caminataMin)
    }
    var criterio by rememberSaveable { mutableStateOf(Criterio.RAPIDA) }
    val ordenadas = remember(opciones, criterio, priorizarAccesibles) {
        val porCriterio = when (criterio) {
            Criterio.RAPIDA -> opciones.sortedBy { it.llegada }
            Criterio.TRANSBORDOS -> opciones.sortedWith(compareBy({ it.transbordos }, { it.llegada }))
            Criterio.SALE_PRIMERO -> opciones.sortedWith(compareBy({ it.tramos.first().esperaMin }, { it.llegada }))
        }
        if (priorizarAccesibles) porCriterio.sortedByDescending { it.accesible } else porCriterio
    }

    Column(Modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = "${origen.nombre} → ${destino.nombre}",
            alVolver = alVolver,
            subtitulo = "Saliendo a las ${hora(salida)} · " +
                if (opciones.size == 1) "1 opción" else "${opciones.size} opciones",
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Criterio.entries.forEach { opcion ->
                val elegido = criterio == opcion
                FilterChip(
                    selected = elegido,
                    onClick = { criterio = opcion },
                    label = { Text(opcion.etiqueta) },
                    leadingIcon = if (elegido) {
                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (opciones.isEmpty()) {
                item {
                    Tarjeta {
                        Text("No encontramos un viaje saliendo ahora", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A esta hora no hay buses troncales programados entre estas estaciones. " +
                                "El servicio troncal suele operar entre las 4:00 a. m. y las 11:00 p. m.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            itemsIndexed(ordenadas) { i, opcion ->
                TarjetaOpcion(opcion, recomendada = i == 0, alPulsar = { alElegir(opcion) })
            }
            item {
                Text(
                    "Horarios programados del GTFS oficial de TransMilenio, vigente hasta el " +
                        "${Transmilenio.vigenteHasta}. No son tiempos en vivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaOpcion(opcion: OpcionViaje, recomendada: Boolean, alPulsar: () -> Unit) {
    Tarjeta(alPulsar = alPulsar, destacada = recomendada) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    opcion.etiqueta.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${opcion.duracionMin}", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        " min",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Text(
                    "Llegas ${hora(opcion.llegada)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (recomendada) Etiqueta("Recomendada")
        }
        Spacer(Modifier.height(14.dp))
        CadenaDeRuta(opcion)
        Spacer(Modifier.height(14.dp))
        Text(
            when (opcion.transbordos) {
                0 -> "Sin transbordos"
                1 -> "1 transbordo"
                else -> "${opcion.transbordos} transbordos"
            } + if (opcion.caminataMin > 0) " · ${opcion.caminataMin} min a pie" else "",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Próximo bus en ${opcion.tramos.first().esperaMin} min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!opcion.accesible) {
            Text(
                "Incluye una estación sin rampa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ResultadosPreview() {
    RoutiFyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Transmilenio.inicializar(LocalContext.current)
            ResultadosScreen(
                origen = Transmilenio.estacion("Portal Norte - Unicervantes"),
                destino = Transmilenio.estacion("Universidades CityU"),
                salida = Transmilenio.reloj(),
                caminataMin = 5,
                priorizarAccesibles = false,
                alVolver = {},
                alElegir = {},
            )
        }
    }
}

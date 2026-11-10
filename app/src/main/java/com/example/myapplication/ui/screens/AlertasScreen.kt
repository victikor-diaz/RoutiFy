@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Alerta
import com.example.myapplication.data.Gravedad
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.ui.components.Etiqueta
import com.example.myapplication.ui.components.InsigniaTroncal
import com.example.myapplication.ui.components.Tarjeta
import com.example.myapplication.ui.components.colorDeGravedad
import com.example.myapplication.ui.theme.RoutiFyTheme

private enum class FiltroAlertas(val etiqueta: String) {
    TODAS("Todas"),
    OFICIALES("Oficiales"),
    COMUNIDAD("Comunidad"),
}

@Composable
fun AlertasScreen() {
    var filtro by rememberSaveable { mutableStateOf(FiltroAlertas.TODAS) }
    val visibles = Transmilenio.alertas.filter {
        when (filtro) {
            FiltroAlertas.TODAS -> true
            FiltroAlertas.OFICIALES -> !it.deLaComunidad
            FiltroAlertas.COMUNIDAD -> it.deLaComunidad
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Alertas", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Estado del servicio y reportes de otros viajeros",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(Modifier.padding(14.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Estas alertas son datos de ejemplo. TransMilenio no publica el estado del servicio " +
                            "en sus datos abiertos; los horarios y rutas de la app sí son reales.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroAlertas.entries.forEach { opcion ->
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = { Text(opcion.etiqueta) },
                    )
                }
            }
        }
        items(visibles) { TarjetaAlerta(it) }
    }
}

@Composable
private fun TarjetaAlerta(alerta: Alerta) {
    val color = colorDeGravedad(alerta.gravedad)
    Tarjeta {
        Row {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when (alerta.gravedad) {
                        Gravedad.INFO -> if (alerta.deLaComunidad) Icons.Filled.Info else Icons.Filled.CheckCircle
                        Gravedad.AVISO, Gravedad.CRITICA -> Icons.Filled.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alerta.titulo, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    alerta.detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InsigniaTroncal(alerta.troncal, tamano = 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${alerta.troncal.nombre} · ${alerta.hace}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (alerta.deLaComunidad) {
                        Etiqueta(
                            "Comunidad",
                            fondo = MaterialTheme.colorScheme.secondaryContainer,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AlertasPreview() {
    RoutiFyTheme {
        Surface(color = MaterialTheme.colorScheme.background) { AlertasScreen() }
    }
}

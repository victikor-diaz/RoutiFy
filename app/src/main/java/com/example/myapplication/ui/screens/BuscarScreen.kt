@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.Estacion
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.data.Troncal
import com.example.myapplication.ui.components.BarraSuperior
import com.example.myapplication.ui.components.InsigniaTroncal
import com.example.myapplication.ui.components.Tarjeta

private enum class Campo { ORIGEN, DESTINO }

// Las estaciones se guardan por nombre para sobrevivir a la recreación de la actividad
private val EstacionOpcionalSaver = Saver<Estacion?, String>(
    save = { it?.nombre },
    restore = { Transmilenio.estacion(it) },
)

/** [cercana]: la estación más cercana según la ubicación; sin ubicación el usuario elige el origen. */
@Composable
fun BuscarScreen(cercana: Estacion?, alVolver: () -> Unit, alPlanear: (Estacion, Estacion) -> Unit) {
    var origen by rememberSaveable(stateSaver = EstacionOpcionalSaver) { mutableStateOf(cercana) }
    var destino by rememberSaveable(stateSaver = EstacionOpcionalSaver) { mutableStateOf(null) }
    var campo by rememberSaveable { mutableStateOf(Campo.DESTINO) }
    var texto by rememberSaveable { mutableStateOf("") }
    val enfoque = remember { FocusRequester() }
    LaunchedEffect(Unit) { enfoque.requestFocus() }

    fun elegir(estacion: Estacion) {
        texto = ""
        if (campo == Campo.ORIGEN) {
            origen = estacion
            campo = Campo.DESTINO
        } else {
            destino = estacion
            if (origen == null) campo = Campo.ORIGEN
        }
        val desde = origen
        val hasta = destino
        if (desde != null && hasta != null && desde != hasta) alPlanear(desde, hasta)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        BarraSuperior("Planear viaje", alVolver)
        Tarjeta(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CampoRuta(
                        etiqueta = "Desde",
                        valor = origen.let {
                            when {
                                it == null -> "¿Desde qué estación sales?"
                                it == cercana -> "Mi ubicación · ${it.nombre}"
                                else -> it.nombre
                            }
                        },
                        troncal = origen?.troncal,
                        activo = campo == Campo.ORIGEN,
                    ) {
                        campo = Campo.ORIGEN
                        texto = ""
                    }
                    HorizontalDivider(
                        Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    CampoRuta(
                        etiqueta = "Hasta",
                        valor = destino?.nombre ?: "¿A dónde vas?",
                        troncal = destino?.troncal,
                        activo = campo == Campo.DESTINO,
                    ) {
                        campo = Campo.DESTINO
                        texto = ""
                    }
                }
                IconButton(
                    onClick = {
                        val antes = origen
                        origen = destino
                        destino = antes
                    },
                    enabled = origen != null || destino != null,
                ) {
                    Icon(painterResource(R.drawable.ic_intercambiar), contentDescription = "Intercambiar origen y destino")
                }
            }
        }

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(enfoque),
            placeholder = {
                Text(if (campo == Campo.ORIGEN) "¿Desde qué estación sales?" else "Escribe una estación, ej. Calle 72")
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (texto.isNotEmpty()) {
                    IconButton(onClick = { texto = "" }) { Icon(Icons.Filled.Close, contentDescription = "Borrar") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        if (destino != null && destino == origen) {
            Text(
                "El origen y el destino son la misma estación.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        val resultados = remember(texto) { Transmilenio.buscar(texto) }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (texto.isBlank()) {
                item { EncabezadoLista("Mis lugares") }
                items(Transmilenio.favoritos) { FilaEstacion(it.estacion, it.etiqueta) { elegir(it.estacion) } }
                item { EncabezadoLista("Recientes") }
                items(Transmilenio.recientes) { FilaEstacion(it) { elegir(it) } }
            } else if (resultados.isEmpty()) {
                item {
                    Text(
                        "No encontramos “$texto”. Prueba con otro nombre.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(resultados) { FilaEstacion(it) { elegir(it) } }
            }
        }
    }
}

@Composable
private fun CampoRuta(
    etiqueta: String,
    valor: String,
    troncal: Troncal?,
    activo: Boolean,
    alPulsar: () -> Unit,
) {
    Surface(
        onClick = alPulsar,
        shape = RoundedCornerShape(12.dp),
        color = if (activo) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (troncal != null) {
                InsigniaTroncal(troncal, tamano = 24.dp)
            } else {
                Surface(
                    Modifier.size(24.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                ) {}
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    valor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun EncabezadoLista(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilaEstacion(estacion: Estacion, etiqueta: String? = null, alPulsar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = alPulsar)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InsigniaTroncal(estacion.troncal)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (etiqueta != null) "$etiqueta · ${estacion.nombre}" else estacion.nombre,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                buildString {
                    append("Troncal ${estacion.troncal.nombre}")
                    if (!estacion.accesible) append(" · sin rampa")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

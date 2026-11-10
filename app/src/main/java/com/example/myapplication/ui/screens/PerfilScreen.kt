package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.data.pesos
import com.example.myapplication.ui.Preferencias
import com.example.myapplication.ui.components.FranjaMarca
import com.example.myapplication.ui.components.InsigniaTroncal
import com.example.myapplication.ui.components.LogoRoutiFy
import com.example.myapplication.ui.components.Tarjeta
import com.example.myapplication.ui.components.TituloSeccion
import com.example.myapplication.ui.theme.RoutiFyTheme

@Composable
fun PerfilScreen(
    saldo: Int,
    preferencias: Preferencias,
    alCambiarPreferencias: (Preferencias) -> Unit,
    alRecargar: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text("Perfil", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Tu tarjeta, tus lugares y cómo quieres que te guiemos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { TarjetaTuLlave(saldo, alRecargar) }
        item {
            TituloSeccion("Mis lugares")
            Spacer(Modifier.height(8.dp))
            Tarjeta {
                Transmilenio.favoritos.forEachIndexed { i, favorito ->
                    if (i > 0) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InsigniaTroncal(favorito.estacion.troncal)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(favorito.etiqueta, style = MaterialTheme.typography.titleSmall)
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
        item {
            TituloSeccion("Accesibilidad y avisos")
            Spacer(Modifier.height(8.dp))
            Tarjeta {
                OpcionInterruptor(
                    titulo = "Priorizar estaciones accesibles",
                    detalle = "Muestra primero las rutas con rampas y ascensores",
                    activo = preferencias.priorizarAccesibles,
                ) { alCambiarPreferencias(preferencias.copy(priorizarAccesibles = it)) }
                OpcionInterruptor(
                    titulo = "Avisarme antes de bajar",
                    detalle = "El teléfono vibra una parada antes de tu destino",
                    activo = preferencias.avisoAntesDeBajar,
                ) { alCambiarPreferencias(preferencias.copy(avisoAntesDeBajar = it)) }
                OpcionInterruptor(
                    titulo = "Leer indicaciones en voz alta",
                    detalle = "Útil con baja visión o con el teléfono en el bolsillo",
                    activo = preferencias.leerEnVozAlta,
                ) { alCambiarPreferencias(preferencias.copy(leerEnVozAlta = it)) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoRoutiFy(tamano = 32.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "RoutiFy · prototipo 0.1 · Bacatá Creative",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TarjetaTuLlave(saldo: Int, alRecargar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        val tenue = LocalContentColor.current.copy(alpha = 0.7f)
        Column(Modifier.padding(20.dp)) {
            FranjaMarca(alto = 4.dp)
            Spacer(Modifier.height(16.dp))
            Row {
                Text("TuLlave", style = MaterialTheme.typography.labelLarge, color = tenue, modifier = Modifier.weight(1f))
                Text("•••• 4821", style = MaterialTheme.typography.labelLarge, color = tenue)
            }
            Spacer(Modifier.height(6.dp))
            Text(pesos(saldo), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp))
            Text(
                "Te alcanza para ${saldo / Transmilenio.TARIFA} viajes (tarifa ${pesos(Transmilenio.TARIFA)})",
                style = MaterialTheme.typography.bodySmall,
                color = tenue,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = alRecargar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    contentColor = MaterialTheme.colorScheme.inverseSurface,
                ),
            ) { Text("Simular recarga de $10.000") }
            Spacer(Modifier.height(10.dp))
            Text(
                "Próximamente: consulta el saldo acercando la tarjeta al teléfono (NFC).",
                style = MaterialTheme.typography.bodySmall,
                color = tenue,
            )
        }
    }
}

@Composable
private fun OpcionInterruptor(titulo: String, detalle: String, activo: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = activo, onValueChange = alCambiar, role = Role.Switch)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = activo, onCheckedChange = null)
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun PerfilPreview() {
    RoutiFyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Transmilenio.inicializar(LocalContext.current)
            PerfilScreen(saldo = 12_400, preferencias = Preferencias(), alCambiarPreferencias = {}, alRecargar = {})
        }
    }
}

package com.example.myapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface Ubicacion {
    data object SinPermiso : Ubicacion
    data object Buscando : Ubicacion
    data object NoDisponible : Ubicacion
    data class Lista(val latitud: Double, val longitud: Double) : Ubicacion
}

class EstadoUbicacion(val ubicacion: Ubicacion, val pedirPermiso: () -> Unit)

private val PERMISOS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

// Si el teléfono ya tiene una posición de hace menos de 5 min, se usa sin esperar al GPS
private const val VIGENCIA_ULTIMA_MS = 5 * 60_000L

/** La ubicación del teléfono, pedida una vez al abrir la app. */
@Composable
fun rememberUbicacion(): EstadoUbicacion {
    val contexto = LocalContext.current
    var permiso by remember { mutableStateOf(tienePermiso(contexto)) }
    var ubicacion by remember { mutableStateOf<Ubicacion>(if (permiso) Ubicacion.Buscando else Ubicacion.SinPermiso) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        permiso = respuesta.values.any { it }
    }
    LaunchedEffect(permiso) {
        if (permiso) {
            ubicacion = Ubicacion.Buscando
            ubicacion = obtener(contexto)
        }
    }
    return EstadoUbicacion(ubicacion) { lanzador.launch(PERMISOS) }
}

private fun tienePermiso(contexto: Context) =
    PERMISOS.any { ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED }

@SuppressLint("MissingPermission")
private suspend fun obtener(contexto: Context): Ubicacion {
    val gestor = contexto.getSystemService(LocationManager::class.java) ?: return Ubicacion.NoDisponible
    val proveedor = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            LocationManagerCompat.hasProvider(gestor, LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
        gestor.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        gestor.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> return Ubicacion.NoDisponible
    }
    gestor.getLastKnownLocation(proveedor)
        ?.takeIf { System.currentTimeMillis() - it.time < VIGENCIA_ULTIMA_MS }
        ?.let { return Ubicacion.Lista(it.latitude, it.longitude) }
    return suspendCancellableCoroutine { continuacion ->
        val cancelar = CancellationSignal()
        continuacion.invokeOnCancellation { cancelar.cancel() }
        LocationManagerCompat.getCurrentLocation(
            gestor, proveedor, cancelar, ContextCompat.getMainExecutor(contexto),
        ) { lugar ->
            continuacion.resume(if (lugar != null) Ubicacion.Lista(lugar.latitude, lugar.longitude) else Ubicacion.NoDisponible)
        }
    }
}

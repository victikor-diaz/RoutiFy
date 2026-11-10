package com.example.myapplication

import com.example.myapplication.data.Transmilenio
import com.example.myapplication.ui.Pantalla
import com.example.myapplication.ui.codificar
import com.example.myapplication.ui.decodificarPantalla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** Lo que se guarda al girar el teléfono debe reconstruir exactamente la misma pantalla. */
class NavegacionTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun cargar() = cargarRed()
    }

    @Test
    fun cadaPantallaSobreviveAGuardarseYRestaurarse() {
        val origen = Transmilenio.estacion("Calle 100 - Marketmedios")
        val destino = Transmilenio.estacion("Ciudad Universitaria")
        val salida = instante(20261026, 8, 17)
        val opciones = Transmilenio.planearViaje(origen, destino, salida, caminataMin = 5)
        assertTrue(opciones.isNotEmpty())
        val pantallas = listOf(
            Pantalla.Inicio,
            Pantalla.Alertas,
            Pantalla.Perfil,
            Pantalla.Buscar,
            Pantalla.Resultados(origen, destino, salida, 5),
        ) + opciones.map { Pantalla.Detalle(it) } + opciones.map { Pantalla.EnViaje(it) }

        pantallas.forEach { assertEquals(it, decodificarPantalla(it.codificar())) }
    }
}

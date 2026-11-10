package com.example.myapplication

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.data.Transmilenio
import com.example.myapplication.ui.RoutiFyApp
import com.example.myapplication.ui.theme.RoutiFyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Transmilenio.inicializar(applicationContext)
        // En depuración: adb shell am start -n com.example.myapplication/.MainActivity --es hora 08:00
        val hora = intent.getStringExtra("hora")
        if (hora != null && applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Transmilenio.fijarHora(hora)
        }
        enableEdgeToEdge()
        setContent {
            RoutiFyTheme {
                RoutiFyApp()
            }
        }
    }
}

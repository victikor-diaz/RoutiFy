package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Tinta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8E3),
    onPrimaryContainer = Tinta,
    secondary = Azul,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E9F7),
    onSecondaryContainer = Color(0xFF0B3A5C),
    tertiary = Color(0xFF0B7A6B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD2F2EC),
    onTertiaryContainer = Color(0xFF00382F),
    error = Color(0xFFC63D20),
    background = Papel,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Color(0xFFEDEDE8),
    onSurfaceVariant = TintaSuave,
    inverseSurface = Tinta,
    inverseOnSurface = Color.White,
    outline = Color(0xFFBDBDB6),
    outlineVariant = Color(0xFFE3E3DE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBF9),
    surfaceContainer = Color(0xFFF3F3EF),
    surfaceContainerHigh = Color(0xFFEDEDE8),
    surfaceContainerHighest = Color(0xFFE7E7E2),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F2),
    onPrimary = Tinta,
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = Color(0xFFF5F5F2),
    secondary = Color(0xFF6CB4E8),
    onSecondary = Color(0xFF00243D),
    secondaryContainer = Color(0xFF0E3E60),
    onSecondaryContainer = Color(0xFFD6E9F7),
    tertiary = Color(0xFF5ED6C3),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF0B4A40),
    onTertiaryContainer = Color(0xFFD2F2EC),
    error = Color(0xFFFF8A70),
    background = PapelOscuro,
    onBackground = Color(0xFFF2F2EF),
    surface = SuperficieOscura,
    onSurface = Color(0xFFF2F2EF),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB8B8B2),
    inverseSurface = Color(0xFFEDEDE8),
    inverseOnSurface = Tinta,
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF333333),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2F2F2F),
)

// Sin color dinámico: la app conserva siempre los colores de la marca
@Composable
fun RoutiFyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

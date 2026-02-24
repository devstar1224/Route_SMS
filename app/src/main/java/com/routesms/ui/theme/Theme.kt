package com.routesms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Navy40,
    onPrimary = Color.White,
    primaryContainer = Navy80,
    onPrimaryContainer = Navy30,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal80,
    error = Red40,
    errorContainer = Red80,
    background = Color.White,
    onBackground = Grey20,
    surface = Color.White,
    onSurface = Grey20,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey50,
)

private val DarkColorScheme = darkColorScheme(
    primary = Navy80,
    onPrimary = Navy30,
    primaryContainer = Navy40,
    secondary = Teal80,
    onSecondary = Teal40,
    secondaryContainer = Teal40,
    error = Red80,
    errorContainer = Red40,
)

@Composable
fun RouteSmsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

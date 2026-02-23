package com.routesms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue80,
    secondary = Green40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Green80,
    error = Red40,
    errorContainer = Red80,
    background = Grey90,
    surface = androidx.compose.ui.graphics.Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue30,
    primaryContainer = Blue40,
    secondary = Green80,
    onSecondary = Green40,
    secondaryContainer = Green40,
    error = Red80,
    errorContainer = Red40,
)

@Composable
fun RouteSmsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

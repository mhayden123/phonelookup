package dev.hackunderway.phonelookup.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FDBA8),
    onPrimary = Color(0xFF003823),
    primaryContainer = Color(0xFF00513A),
    onPrimaryContainer = Color(0xFF8CF8C3),
    secondary = Color(0xFFB3CCBE),
    onSecondary = Color(0xFF1E352B),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDFE4E0),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDFE4E0),
    surfaceVariant = Color(0xFF3A4A42),
    onSurfaceVariant = Color(0xFFBFCCC4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9DF6C7),
    onPrimaryContainer = Color(0xFF002115),
    secondary = Color(0xFF4C6358),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun PhoneLookupTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

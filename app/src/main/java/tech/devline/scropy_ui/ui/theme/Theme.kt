package tech.devline.scropy_ui.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OrbitaColorScheme = darkColorScheme(
    primary = OrbitaBlue,
    onPrimary = OrbitaNavy,
    primaryContainer = OrbitaViolet,
    onPrimaryContainer = OrbitaText,
    secondary = OrbitaViolet,
    onSecondary = OrbitaText,
    secondaryContainer = OrbitaSurface2,
    onSecondaryContainer = OrbitaText,
    tertiary = OrbitaMagenta,
    onTertiary = OrbitaText,
    background = OrbitaNavy,
    onBackground = OrbitaText,
    surface = OrbitaNavy2,
    onSurface = OrbitaText,
    surfaceVariant = OrbitaSurface,
    onSurfaceVariant = OrbitaTextMuted,
    outline = OrbitaViolet,
    error = OrbitaError,
)

@Composable
fun ScropyTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OrbitaColorScheme,
        typography = Typography,
        content = content,
    )
}

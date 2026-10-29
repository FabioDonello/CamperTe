package camperte.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    secondary = Color(0xFF2F5D50),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8E2),
    onSecondaryContainer = Color(0xFF1B3D33),
    tertiary = Color(0xFF1B6E34),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDEFE2),
    onTertiaryContainer = Color(0xFF155A2B),
    background = Color(0xFFFBFAF7),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFBFAF7),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFECEAE3),
    onSurfaceVariant = Color(0xFF4A4843),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFFB9CDBF),
    outlineVariant = Color(0xFFE4E1D8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF9CCBB8),
    onSecondary = Color(0xFF0F2A22),
    secondaryContainer = Color(0xFF2C4A40),
    onSecondaryContainer = Color(0xFFDCEDE4),
    tertiary = Color(0xFF1B6E34),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1E3A28),
    onTertiaryContainer = Color(0xFFA8DDB5),
    background = Color(0xFF141412),
    onBackground = Color(0xFFECEAE3),
    surface = Color(0xFF141412),
    onSurface = Color(0xFFECEAE3),
    surfaceVariant = Color(0xFF2B2A26),
    onSurfaceVariant = Color(0xFFC4C1B8),
    surfaceContainerLowest = Color(0xFF1E1E1B),
    outline = Color(0xFF4F6B5F),
    outlineVariant = Color(0xFF34332E),
)

val LocalWarningColor = staticCompositionLocalOf { Color(0xFF6B4A00) }

@Composable
fun CamperTeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val warningColor = if (darkTheme) Color(0xFFF0C36B) else Color(0xFF6B4A00)
    CompositionLocalProvider(LocalWarningColor provides warningColor) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}

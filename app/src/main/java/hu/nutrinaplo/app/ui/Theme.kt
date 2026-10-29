package hu.nutrinaplo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppColors {
    val Brand = Color(0xFF12A15F)
    val BrandDark = Color(0xFF34D399)
    val Protein = Color(0xFFEF5D60)
    val Carbs = Color(0xFFF5A524)
    val Fat = Color(0xFF7C6CF2)
    val Fiber = Color(0xFF22A06B)
    val Water = Color(0xFF2F9BF5)
    val Kcal = Color(0xFFFF7A3D)
    val Deficient = Color(0xFFE5484D)
    val Low = Color(0xFFF5A524)
    val Ok = Color(0xFF30A46C)
    val High = Color(0xFFE5484D)
}

private val Light = lightColorScheme(
    primary = AppColors.Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F5E3),
    onPrimaryContainer = Color(0xFF00391C),
    secondary = Color(0xFF2F9BF5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEEFF),
    onSecondaryContainer = Color(0xFF002A4D),
    tertiary = Color(0xFFFF7A3D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6D9),
    onTertiaryContainer = Color(0xFF4A1700),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF15181D),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF15181D),
    surfaceVariant = Color(0xFFE9ECF0),
    onSurfaceVariant = Color(0xFF5B636E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceContainerHigh = Color(0xFFEAEDF1),
    surfaceContainerHighest = Color(0xFFE3E7EC),
    outline = Color(0xFFC5CBD3),
    outlineVariant = Color(0xFFE1E5EA),
)

private val Dark = darkColorScheme(
    primary = AppColors.BrandDark,
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF0F3D2A),
    onPrimaryContainer = Color(0xFFBDF2D6),
    secondary = Color(0xFF7CC0FF),
    onSecondary = Color(0xFF00315A),
    secondaryContainer = Color(0xFF113553),
    onSecondaryContainer = Color(0xFFD4E9FF),
    tertiary = Color(0xFFFFA477),
    onTertiary = Color(0xFF5A1E00),
    tertiaryContainer = Color(0xFF4A2412),
    onTertiaryContainer = Color(0xFFFFDCCB),
    background = Color(0xFF0E1013),
    onBackground = Color(0xFFE6E8EB),
    surface = Color(0xFF0E1013),
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = Color(0xFF22262C),
    onSurfaceVariant = Color(0xFFA3AAB4),
    surfaceContainerLowest = Color(0xFF0A0B0D),
    surfaceContainerLow = Color(0xFF16191D),
    surfaceContainer = Color(0xFF1A1D22),
    surfaceContainerHigh = Color(0xFF22262C),
    surfaceContainerHighest = Color(0xFF2B3037),
    outline = Color(0xFF454C55),
    outlineVariant = Color(0xFF2C3138),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NutriTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = AppShapes,
        content = content,
    )
}

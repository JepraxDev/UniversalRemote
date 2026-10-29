package com.universalremote.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalremote.core.model.ThemePreference

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4388FF), onPrimary = Color(0xFF07142B),
    primaryContainer = Color(0xFF213B66), onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFFAAC7FF), onSecondary = Color(0xFF102644),
    secondaryContainer = Color(0xFF24334E), onSecondaryContainer = Color(0xFFE0E9FF),
    tertiary = Color(0xFF35C995), onTertiary = Color(0xFF002D1E),
    background = Color(0xFF0B1020), onBackground = Color.White,
    surface = Color(0xFF151D30), onSurface = Color.White,
    surfaceVariant = Color(0xFF243047), onSurfaceVariant = Color(0xFFAAB5CC),
    surfaceContainer = Color(0xFF151D30), surfaceContainerLow = Color(0xFF11192A),
    surfaceContainerHigh = Color(0xFF1E2940), surfaceContainerHighest = Color(0xFF28354D),
    outline = Color(0xFF7F8DA7), outlineVariant = Color(0xFF35415B),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF205EC7), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF), onPrimaryContainer = Color(0xFF102C5D),
    secondary = Color(0xFF405F8F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E9F9), onSecondaryContainer = Color(0xFF182D4D),
    tertiary = Color(0xFF006C4B), onTertiary = Color.White,
    background = Color(0xFFF4F7FD), onBackground = Color(0xFF111C30),
    surface = Color.White, onSurface = Color(0xFF111C30),
    surfaceVariant = Color(0xFFE5EBF5), onSurfaceVariant = Color(0xFF4C5B73),
    surfaceContainer = Color(0xFFEDF2FA), surfaceContainerLow = Color(0xFFF8FAFF),
    surfaceContainerHigh = Color(0xFFE5ECF7), surfaceContainerHighest = Color(0xFFDCE5F2),
    outline = Color(0xFF68778F), outlineVariant = Color(0xFFBFCBDE),
)

private val RemoteTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun UniversalRemoteTheme(preference: ThemePreference, content: @Composable () -> Unit) {
    val dark = preference == ThemePreference.DARK || (preference == ThemePreference.SYSTEM && isSystemInDarkTheme())
    val target = if (dark) DarkColors else LightColors
    // Compose animations obey the system animator duration scale, including scale zero.
    val background by animateColorAsState(target.background, label = "background")
    val surface by animateColorAsState(target.surface, label = "surface")
    MaterialTheme(
        colorScheme = target.copy(background = background, surface = surface),
        typography = RemoteTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}

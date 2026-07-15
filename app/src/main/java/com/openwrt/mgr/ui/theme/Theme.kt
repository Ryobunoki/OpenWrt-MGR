package com.openwrt.mgr.ui.theme

import com.openwrt.mgr.ui.i18n.L10n

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

enum class ThemeStyle(val key: String, val label: String) {
    SAKURA("sakura", "樱花粉"),
    DYNAMIC("dynamic", "系统动态色"),
    MINT("mint", "薄荷绿"),
    OCEAN("ocean", "雾海蓝"),
    CUSTOM("custom", "自定义配色");

    fun displayName(l: L10n): String = when (this) {
        SAKURA -> l.themeSakura
        DYNAMIC -> l.themeDynamic
        MINT -> l.themeMint
        OCEAN -> l.themeOcean
        CUSTOM -> l.themeCustom
    }

    companion object {
        fun fromKey(key: String?): ThemeStyle =
            entries.firstOrNull { it.key == key } ?: SAKURA
    }
}

enum class BackgroundStyle(val key: String, val label: String) {
    NONE("none", "纯色"),
    SOFT_GRADIENT("soft_gradient", "柔和渐变"),
    BLOOM("bloom", "花瓣光晕"),
    MESH("mesh", "网格光斑"),
    CUSTOM_IMAGE("custom_image", "自定义图片");

    fun displayName(l: L10n): String = when (this) {
        NONE -> l.bgNone
        SOFT_GRADIENT -> l.bgSoftGradient
        BLOOM -> l.bgBloom
        MESH -> l.bgMesh
        CUSTOM_IMAGE -> l.bgCustomImage
    }

    companion object {
        fun fromKey(key: String?): BackgroundStyle =
            entries.firstOrNull { it.key == key } ?: SOFT_GRADIENT
    }
}

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色"),
    AMOLED("amoled", "AMOLED 纯黑");

    fun displayName(l: L10n): String = when (this) {
        SYSTEM -> l.modeSystem
        LIGHT -> l.modeLight
        DARK -> l.modeDark
        AMOLED -> l.modeAmoled
    }

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Launcher icon styles switched via activity-alias. */
enum class AppIconStyle(val key: String, val label: String, val componentSuffix: String) {
    SAKURA("sakura", "樱花粉", "MainAliasSakura"),
    MINT("mint", "薄荷绿", "MainAliasMint"),
    OCEAN("ocean", "雾海蓝", "MainAliasOcean"),
    MINIMAL("minimal", "简约白", "MainAliasMinimal"),
    CUSTOM("custom", "自定义图片", "MainAliasCustom");

    fun displayName(l: L10n): String = when (this) {
        SAKURA -> l.iconSakura
        MINT -> l.iconMint
        OCEAN -> l.iconOcean
        MINIMAL -> l.iconMinimal
        CUSTOM -> l.iconCustom
    }

    companion object {
        fun fromKey(key: String?): AppIconStyle =
            entries.firstOrNull { it.key == key } ?: SAKURA
    }
}



private val SakuraLight = lightColorScheme(
    primary = Color(0xFFB12D5B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3F001A),
    secondary = Color(0xFF74565E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5734),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC0),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFF7F2F3),
    onBackground = Color(0xFF1C1416),
    surface = Color(0xFFFFFBFC),
    onSurface = Color(0xFF1C1416),
    surfaceVariant = Color(0xFFF0E2E6),
    onSurfaceVariant = Color(0xFF4F4347),
    outline = Color(0xFF817377),
    outlineVariant = Color(0xFFD3C2C6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val SakuraDark = darkColorScheme(
    primary = Color(0xFFFFB1C7),
    onPrimary = Color(0xFF650033),
    primaryContainer = Color(0xFF8E1144),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE2BDC6),
    onSecondary = Color(0xFF422930),
    secondaryContainer = Color(0xFF5B3F46),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEEBD93),
    onTertiary = Color(0xFF472A0B),
    tertiaryContainer = Color(0xFF61401F),
    onTertiaryContainer = Color(0xFFFFDCC0),
    background = Color(0xFF141012),
    onBackground = Color(0xFFEDE0E2),
    surface = Color(0xFF1C1719),
    onSurface = Color(0xFFEDE0E2),
    surfaceVariant = Color(0xFF4F4347),
    onSurfaceVariant = Color(0xFFD3C2C6),
    outline = Color(0xFF9C8C90),
    outlineVariant = Color(0xFF4F4347),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val MintLight = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF89F8C7),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9D9),
    onSecondaryContainer = Color(0xFF0A1F16),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF2F6F3),
    onBackground = Color(0xFF151B17),
    surface = Color(0xFFFBFCFB),
    onSurface = Color(0xFF151B17),
    surfaceVariant = Color(0xFFD9E5DD),
    onSurfaceVariant = Color(0xFF3F4943)
)

private val MintDark = darkColorScheme(
    primary = Color(0xFF6CDBAB),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFF89F8C7),
    secondary = Color(0xFFB3CCBD),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE9D9),
    tertiary = Color(0xFFA5CCDF),
    onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF244C5B),
    onTertiaryContainer = Color(0xFFC1E8FB),
    background = Color(0xFF0E1411),
    onBackground = Color(0xFFDCE3DD),
    surface = Color(0xFF151B18),
    onSurface = Color(0xFFDCE3DD),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBDC9C1)
)

private val OceanLight = lightColorScheme(
    primary = Color(0xFF00658C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC5E7FF),
    onPrimaryContainer = Color(0xFF001E2D),
    secondary = Color(0xFF4E616D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E5F4),
    onSecondaryContainer = Color(0xFF0A1E28),
    tertiary = Color(0xFF615A7C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7DEFF),
    onTertiaryContainer = Color(0xFF1D1735),
    background = Color(0xFFF2F6FA),
    onBackground = Color(0xFF151A1D),
    surface = Color(0xFFFCFDFE),
    onSurface = Color(0xFF151A1D),
    surfaceVariant = Color(0xFFDBE3EA),
    onSurfaceVariant = Color(0xFF40484D)
)

private val OceanDark = darkColorScheme(
    primary = Color(0xFF80CFFF),
    onPrimary = Color(0xFF00344B),
    primaryContainer = Color(0xFF004C6B),
    onPrimaryContainer = Color(0xFFC5E7FF),
    secondary = Color(0xFFB5C9D7),
    onSecondary = Color(0xFF20333E),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD1E5F4),
    tertiary = Color(0xFFCBC1E9),
    onTertiary = Color(0xFF322C4C),
    tertiaryContainer = Color(0xFF494263),
    onTertiaryContainer = Color(0xFFE7DEFF),
    background = Color(0xFF0E1316),
    onBackground = Color(0xFFDDE2E6),
    surface = Color(0xFF151A1D),
    onSurface = Color(0xFFDDE2E6),
    surfaceVariant = Color(0xFF40484D),
    onSurfaceVariant = Color(0xFFBFC7CE)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp)
)

/** KernelSU-style pure black surfaces for OLED. */
fun ColorScheme.amoledized(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A)
)

fun customSchemeFromPrimary(primaryArgb: Long, dark: Boolean): ColorScheme {
    val primary = Color(primaryArgb or 0xFF000000L)
    val container = if (dark) primary.copy(alpha = 0.35f).compositeOn(Color(0xFF1A1A1A)) else primary.copy(alpha = 0.22f).compositeOn(Color.White)
    val onPrimary = if (primary.luminance() > 0.55f) Color(0xFF1A1A1A) else Color.White
    val onContainer = if (dark) Color(0xFFF0F0F0) else Color(0xFF1A1A1A)
    return if (dark) {
        darkColorScheme(
            primary = primary.lighten(0.18f),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = primary.lighten(0.28f),
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = Color(0xFF2A2A2A),
            onSecondaryContainer = Color(0xFFE8E8E8),
            tertiary = primary.shiftHue(40f).lighten(0.2f),
            onTertiary = Color(0xFF1A1A1A),
            background = Color(0xFF121212),
            onBackground = Color(0xFFEAEAEA),
            surface = Color(0xFF1A1A1A),
            onSurface = Color(0xFFEAEAEA),
            surfaceVariant = Color(0xFF2C2C2C),
            onSurfaceVariant = Color(0xFFC8C8C8)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = primary.darken(0.12f),
            onSecondary = Color.White,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
            tertiary = primary.shiftHue(40f),
            onTertiary = Color.White,
            background = Color(0xFFF7F7F8),
            onBackground = Color(0xFF1A1A1A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFECECEC),
            onSurfaceVariant = Color(0xFF4A4A4A)
        )
    }
}

private fun Color.compositeOn(bg: Color): Color {
    val a = alpha.coerceIn(0f, 1f)
    return Color(
        red = red * a + bg.red * (1 - a),
        green = green * a + bg.green * (1 - a),
        blue = blue * a + bg.blue * (1 - a),
        alpha = 1f
    )
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

private fun Color.lighten(amount: Float): Color = Color(
    red = min(1f, red + amount),
    green = min(1f, green + amount),
    blue = min(1f, blue + amount),
    alpha = alpha
)

private fun Color.darken(amount: Float): Color = Color(
    red = max(0f, red - amount),
    green = max(0f, green - amount),
    blue = max(0f, blue - amount),
    alpha = alpha
)

private fun Color.shiftHue(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun OpenWrtTheme(
    themeStyle: ThemeStyle = ThemeStyle.SAKURA,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    customPrimaryArgb: Long = 0xFFB12D5B,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    var scheme = rememberColorScheme(themeStyle, dark, customPrimaryArgb)
    if (themeMode == ThemeMode.AMOLED) {
        scheme = scheme.amoledized()
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}

@Composable
private fun rememberColorScheme(style: ThemeStyle, dark: Boolean, customPrimaryArgb: Long): ColorScheme {
    val context = LocalContext.current
    return when (style) {
        ThemeStyle.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (dark) SakuraDark else SakuraLight
            }
        }
        ThemeStyle.SAKURA -> if (dark) SakuraDark else SakuraLight
        ThemeStyle.MINT -> if (dark) MintDark else MintLight
        ThemeStyle.OCEAN -> if (dark) OceanDark else OceanLight
        ThemeStyle.CUSTOM -> customSchemeFromPrimary(customPrimaryArgb, dark)
    }
}

/** Preset swatches for custom primary picker. */
val CustomPrimaryPresets = listOf(
    0xFFB12D5B, // sakura
    0xFFE91E63,
    0xFF9C27B0,
    0xFF673AB7,
    0xFF3F51B5,
    0xFF2196F3,
    0xFF00BCD4,
    0xFF009688,
    0xFF4CAF50,
    0xFF8BC34A,
    0xFFFF9800,
    0xFFFF5722,
    0xFF795548,
    0xFF607D8B,
    0xFF111111,
    0xFFF5A9C0
)

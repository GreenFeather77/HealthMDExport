package com.healthmd.exporter.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ============================================================
// 「春野 Vitality」设计体系 —— 浅色 · 生机 · 灵动
// ============================================================

// 品牌色：鲜绿（生机）+ 珊瑚（活力）+ 天蓝（清澈）
val FreshGreen = Color(0xFF2BA36B)
val DeepGreen = Color(0xFF1E7A4E)
val Mint = Color(0xFF8FE3B5)
val Cream = Color(0xFFF6FAF3)
val Coral = Color(0xFFFF7A59)
val CoralDark = Color(0xFFC2492B)
val Sky = Color(0xFF4A9FE8)
val SkyDark = Color(0xFF1B5E96)
val Lavender = Color(0xFF8B7CF6)
val LavenderDark = Color(0xFF5344C9)
val Honey = Color(0xFFF5A623)
val Ink = Color(0xFF1B2B22)
val InkSoft = Color(0xFF5F7267)

// 各指标卡的四色系
val StepsContainer = Color(0xFFD6F2E2)
val CaloriesContainer = Color(0xFFFFE2D8)
val SleepContainer = Color(0xFFE7E2FB)
val WaterContainer = Color(0xFFD8ECFB)

private val LightColors = lightColorScheme(
    primary = FreshGreen,
    onPrimary = Color.White,
    primaryContainer = StepsContainer,
    onPrimaryContainer = DeepGreen,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = CaloriesContainer,
    onSecondaryContainer = CoralDark,
    tertiary = Sky,
    onTertiary = Color.White,
    tertiaryContainer = WaterContainer,
    onTertiaryContainer = SkyDark,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAF3EC),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD6E4DA),
    outlineVariant = Color(0xFFE3EFE6),
)

val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun HealthMdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// ============================================================
// 灵动感：弹簧入场动画（交错渐入 + 上浮 + 微缩放）
// ============================================================

/** 元素入场动效：delay(index) 交错出现，弹簧曲线 */
fun Modifier.vitalEnter(index: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 140f))
    }
    val p = progress.value
    this
        .graphicsLayer {
            alpha = p
            scaleX = 0.92f + 0.08f * p
            scaleY = scaleX
        }
        .offset { IntOffset(0, ((1f - p) * 30.dp.toPx()).roundToInt()) }
}

/** 数值变化弹簧过渡 */
@Composable
fun springFloat(target: Float): Float {
    val value by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 120f),
        label = "spring"
    )
    return value
}

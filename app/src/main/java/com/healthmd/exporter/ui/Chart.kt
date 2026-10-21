package com.healthmd.exporter.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.max

data class ChartBar(
    val label: String,
    val sub: String,
    val value: Float,
    val highlight: Boolean = false
)

/** 最近 7 天柱状图：弹簧生长动画 + 今日高亮 */
@Composable
fun WeeklyBarChart(bars: List<ChartBar>, modifier: Modifier = Modifier) {
    if (bars.isEmpty() || bars.all { it.value <= 0f }) {
        Box(
            modifier = modifier.height(170.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxV = max(bars.maxOf { it.value }, 1f)
    // 入场时从 0 弹簧长出
    val grow = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) {
        grow.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 110f))
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().height(195.dp)
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val valueText = when {
                    bar.value <= 0f -> ""
                    bar.value >= 10000 -> String.format("%.1f万", bar.value / 10000f)
                    else -> String.format("%,d", bar.value.toLong())
                }
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bar.highlight) CoralDark else InkSoft,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // 轨道
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    // 数值柱（弹簧生长）
                    val barH = 120.dp * (bar.value / maxV) * grow.value
                    if (barH > 0.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barH)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (bar.highlight) Coral else FreshGreen)
                        )
                    }
                }
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bar.highlight) CoralDark else Ink,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = bar.sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                    maxLines = 1
                )
            }
        }
    }
}

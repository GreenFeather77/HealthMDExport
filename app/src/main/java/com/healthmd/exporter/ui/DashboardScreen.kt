package com.healthmd.exporter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.healthmd.exporter.HealthConnectManager
import com.healthmd.exporter.HealthMetrics
import com.healthmd.exporter.R
import com.healthmd.exporter.SdkAvailability
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DashUi(
    val loading: Boolean = true,
    val hcMissing: Boolean = false,
    val needPermission: Boolean = false,
    val steps: Long? = null,
    val calories: Long? = null,
    val sleepMinutes: Long? = null,
    val hydrationLiters: Double? = null,
    val bars: List<ChartBar> = emptyList()
)

@Composable
fun DashboardScreen(
    onQuickExport: () -> Unit,
    onGoConnect: () -> Unit,
    refreshTick: Int
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(DashUi()) }

    LaunchedEffect(refreshTick) {
        state = DashUi()
        val ctx = context.applicationContext
        state = withContext(Dispatchers.IO) {
            runCatching { loadDash(ctx) }.getOrDefault(DashUi(loading = false))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        HeroCard(onQuickExport, onGoConnect)

        if (state.hcMissing) {
            WarningBanner("Health Connect 不可用，无法读取数据")
        }
        if (state.needPermission) {
            WarningBanner("尚未授权任何数据权限，请到「连接」页授权后查看")
        }

        SectionTitle("今日概览")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f).vitalEnter(1),
                label = "步数",
                value = state.steps?.let { String.format("%,d", it) } ?: "--",
                unit = "步",
                iconRes = R.drawable.ic_steps,
                container = StepsContainer,
                tint = DeepGreen
            )
            StatCard(
                modifier = Modifier.weight(1f).vitalEnter(2),
                label = "活动消耗",
                value = state.calories?.toString() ?: "--",
                unit = "千卡",
                iconRes = R.drawable.ic_flame,
                container = CaloriesContainer,
                tint = CoralDark
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f).vitalEnter(3),
                label = "睡眠时长",
                value = state.sleepMinutes?.let { "${it / 60}时${it % 60}分" } ?: "--",
                unit = "",
                iconRes = R.drawable.ic_moon,
                container = SleepContainer,
                tint = LavenderDark
            )
            StatCard(
                modifier = Modifier.weight(1f).vitalEnter(4),
                label = "补水量",
                value = state.hydrationLiters?.let { String.format("%.2f", it) } ?: "--",
                unit = "升",
                iconRes = R.drawable.ic_drop,
                container = WaterContainer,
                tint = SkyDark
            )
        }

        SectionTitle("最近 7 天步数")
        Card(
            modifier = Modifier.fillMaxWidth().vitalEnter(5),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            WeeklyBarChart(bars = state.bars, modifier = Modifier.padding(18.dp))
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = Ink,
        modifier = Modifier.padding(top = 26.dp, bottom = 14.dp)
    )
}

@Composable
private fun HeroCard(onQuickExport: () -> Unit, onGoConnect: () -> Unit) {
    val hour = java.time.LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE"))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(FreshGreen, Color(0xFF52C68E), Mint)))
            .padding(24.dp)
    ) {
        // 装饰性光斑，增加生机层次
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(180.dp, (-50).dp)
                .background(Color.White.copy(alpha = 0.13f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(230.dp, 40.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
        )

        Column {
            Text(
                text = "$greeting 👋",
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = dateText,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onQuickExport,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DeepGreen
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp, vertical = 12.dp
                    )
                ) {
                    Text("导出最近 7 天", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onGoConnect,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp, Color.White.copy(alpha = 0.8f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp, vertical = 12.dp
                    )
                ) {
                    Text("去授权", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WarningBanner(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE2D8))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = CoralDark,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    iconRes: Int,
    container: Color,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(tint)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

/** 读取今日概览 + 最近 7 天步数（IO 线程） */
private suspend fun loadDash(ctx: android.content.Context): DashUi {
    val availability = HealthConnectManager.sdkAvailability(ctx)
    if (availability != SdkAvailability.AVAILABLE) {
        return DashUi(loading = false, hcMissing = true)
    }
    val granted = runCatching { HealthConnectManager.grantedPermissions(ctx) }
        .getOrDefault(emptySet())
    if (granted.isEmpty()) return DashUi(loading = false, needPermission = true)

    val client = HealthConnectManager.client(ctx)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val dayStart = today.atStartOfDay(zone).toInstant()
    val now = Instant.now()

    var steps: Long? = null
    var calories: Long? = null
    var sleepMinutes: Long? = null
    var hydration: Double? = null

    if (HealthMetrics.byKey("steps")!!.permission in granted) {
        steps = HealthConnectManager.readAll(client, StepsRecord::class, dayStart, now)
            .sumOf { it.count }
    }
    if (HealthMetrics.byKey("calories_active")!!.permission in granted) {
        calories = HealthConnectManager.readAll(client, ActiveCaloriesBurnedRecord::class, dayStart, now)
            .sumOf { it.energy.inKilocalories }.roundToLong()
    }
    if (HealthMetrics.byKey("sleep")!!.permission in granted) {
        val sessions = HealthConnectManager.readAll(
            client, SleepSessionRecord::class,
            dayStart.minus(Duration.ofHours(12)), now
        )
        var mins = 0L
        for (s in sessions) {
            val overlapStart = maxOf(s.startTime.toEpochMilli(), dayStart.toEpochMilli())
            val overlapEnd = minOf(s.endTime.toEpochMilli(), now.toEpochMilli())
            if (overlapEnd > overlapStart) mins += (overlapEnd - overlapStart) / 60000
        }
        if (mins > 0) sleepMinutes = mins
    }
    if (HealthMetrics.byKey("hydration")!!.permission in granted) {
        val liters = HealthConnectManager.readAll(client, HydrationRecord::class, dayStart, now)
            .sumOf { it.volume.inLiters }
        if (liters > 0) hydration = liters
    }

    var bars = emptyList<ChartBar>()
    if (HealthMetrics.byKey("steps")!!.permission in granted) {
        val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant()
        val records = HealthConnectManager.readAll(client, StepsRecord::class, weekStart, now)
        val dayFmt = DateTimeFormatter.ofPattern("M/d")
        val weekFmt = DateTimeFormatter.ofPattern("E")
        bars = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val start = day.atStartOfDay(zone).toInstant()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant()
            val total = records.filter { it.startTime >= start && it.startTime < end }
                .sumOf { it.count }
            ChartBar(
                label = if (offset == 0) "今天" else day.format(weekFmt),
                sub = day.format(dayFmt),
                value = total.toFloat(),
                highlight = offset == 0
            )
        }
    }

    return DashUi(
        loading = false,
        steps = steps,
        calories = calories,
        sleepMinutes = sleepMinutes,
        hydrationLiters = hydration,
        bars = bars
    )
}

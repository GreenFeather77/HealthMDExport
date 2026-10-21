package com.healthmd.exporter.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.healthmd.exporter.HealthConnectManager
import com.healthmd.exporter.HealthMetrics
import com.healthmd.exporter.MetricDef
import com.healthmd.exporter.R
import com.healthmd.exporter.SdkAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ConnectScreen(refreshTick: Int) {
    val context = LocalContext.current
    var availability by remember { mutableStateOf(SdkAvailability.NOT_INSTALLED) }
    var granted by remember { mutableStateOf<Set<String>>(emptySet()) }
    var requestFailed by remember { mutableStateOf(false) }
    var guidance by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { result ->
        val requested = HealthConnectManager.allPermissions()
        val grantedNow = requested.intersect(result)
        granted = grantedNow
        if (grantedNow.size < requested.size) guidance = true
    }

    LaunchedEffect(refreshTick) {
        val ctx = context.applicationContext
        availability = withContext(Dispatchers.IO) { HealthConnectManager.sdkAvailability(ctx) }
        granted = withContext(Dispatchers.IO) {
            if (availability == SdkAvailability.AVAILABLE)
                runCatching { HealthConnectManager.grantedPermissions(ctx) }.getOrDefault(emptySet())
            else emptySet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        HealthStatusCard(availability)

        SectionTitle("数据权限")

        val grantedCount = HealthMetrics.ALL.count { it.permission in granted }
        Card(
            modifier = Modifier.fillMaxWidth().vitalEnter(1),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "已授权 $grantedCount / ${HealthMetrics.ALL.size} 项",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (grantedCount == HealthMetrics.ALL.size) "已就绪" else "待授权",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (grantedCount == HealthMetrics.ALL.size) DeepGreen else Honey
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { grantedCount.toFloat() / HealthMetrics.ALL.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = FreshGreen,
                    trackColor = Color(0xFFE3F2E9),
                )

                Spacer(Modifier.height(16.dp))
                HealthMetrics.ALL.forEachIndexed { index, metric ->
                    PermissionRow(metric, metric.permission in granted)
                    if (index < HealthMetrics.ALL.size - 1) Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (availability != SdkAvailability.AVAILABLE) {
                    openHealthConnect(context)
                } else {
                    val perms = HealthConnectManager.allPermissions()
                    if (perms.isNotEmpty()) {
                        runCatching { permissionLauncher.launch(perms) }
                            .onFailure { requestFailed = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .vitalEnter(2),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshGreen)
        ) {
            Text("请求数据权限", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "心率/静息心率属于受限权限，需在 Google Play 开发者控制台申请批准后才能授权，其余数据直接授权即可使用。",
            style = MaterialTheme.typography.bodyMedium,
            color = Honey,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
        )
    }

    if (guidance) {
        AlertDialog(
            onDismissRequest = { guidance = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("需要授权才能导出数据") },
            text = { Text("可在 Health Connect 的「应用和服务」中找到「HealthMD 导出」，手动开启需要的数据权限。") },
            confirmButton = {
                TextButton(onClick = { guidance = false; openHealthConnect(context) }) { Text("打开 Health Connect") }
            },
            dismissButton = {
                TextButton(onClick = { guidance = false }) { Text("稍后") }
            }
        )
    }

    if (requestFailed) {
        AlertDialog(
            onDismissRequest = { requestFailed = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("无法自动打开授权界面") },
            text = { Text("请手动打开 Health Connect →「应用和服务」→ 找到「HealthMD 导出」→ 开启所需数据权限。") },
            confirmButton = {
                TextButton(onClick = { requestFailed = false; openHealthConnect(context) }) { Text("打开 Health Connect") }
            },
            dismissButton = {
                TextButton(onClick = { requestFailed = false }) { Text("稍后") }
            }
        )
    }
}

@Composable
private fun HealthStatusCard(availability: SdkAvailability) {
    val context = LocalContext.current
    val (iconRes, tint, container, title, sub) = when (availability) {
        SdkAvailability.AVAILABLE -> Five(R.drawable.ic_home, DeepGreen, StepsContainer, "Health Connect 已就绪", "可以读取并导出健康数据")
        SdkAvailability.UPDATE_REQUIRED -> Five(R.drawable.ic_home, Honey, Color(0xFFFFF1D6), "Health Connect 需要更新", "请前往应用商店更新后继续")
        SdkAvailability.NOT_INSTALLED -> Five(R.drawable.ic_home, CoralDark, CaloriesContainer, "未安装 Health Connect", "本机没有 Google Health Connect，请先安装")
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).vitalEnter(0),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(container, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    colorFilter = ColorFilter.tint(tint)
                )
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = InkSoft, modifier = Modifier.padding(top = 2.dp))
            }
            TextButton(onClick = { openHealthConnect(context) }) {
                Text(
                    when (availability) {
                        SdkAvailability.AVAILABLE -> "打开"
                        SdkAvailability.UPDATE_REQUIRED -> "去更新"
                        SdkAvailability.NOT_INSTALLED -> "去安装"
                    },
                    fontWeight = FontWeight.Bold,
                    color = FreshGreen
                )
            }
        }
    }
}

private data class Five<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

@Composable
private fun PermissionRow(metric: MetricDef, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val iconRes = when (metric.key) {
            "steps", "distance", "floors" -> R.drawable.ic_steps
            "calories", "calories_active" -> R.drawable.ic_flame
            "sleep" -> R.drawable.ic_moon
            "hydration" -> R.drawable.ic_drop
            "weight", "height", "body_fat" -> R.drawable.ic_home
            else -> R.drawable.ic_home
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (granted) StepsContainer else Color(0xFFF0F3F1),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                colorFilter = ColorFilter.tint(if (granted) DeepGreen else InkSoft)
            )
        }
        Text(
            text = metric.label,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(start = 12.dp).weight(1f)
        )
        if (metric.restricted) {
            Badge("受限", Color(0xFFFFF1D6), Honey)
            Spacer(Modifier.size(6.dp))
        }
        if (granted) {
            Badge("已授权", StepsContainer, DeepGreen)
        } else {
            Badge("未授权", Color(0xFFF3E4E0), CoralDark)
        }
    }
}

@Composable
private fun Badge(text: String, container: Color, tint: Color) {
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
    }
}

/** 打开 Health Connect（设置页优先，未安装跳商店） */
fun openHealthConnect(context: Context) {
    val availability = HealthConnectManager.sdkAvailability(context)
    if (availability == SdkAvailability.AVAILABLE) {
        val candidates = listOfNotNull(
            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS),
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata"),
            runCatching { HealthConnectClient.getHealthConnectManageDataIntent(context) }.getOrNull()
        )
        for (intent in candidates) {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        }
    } else {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
        val intent = if (market.resolveActivity(context.packageManager) != null) {
            market
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
        }
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

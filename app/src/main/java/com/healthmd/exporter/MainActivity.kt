package com.healthmd.exporter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.healthmd.exporter.ui.ConnectScreen
import com.healthmd.exporter.ui.DashboardScreen
import com.healthmd.exporter.ui.ExportScreen
import com.healthmd.exporter.ui.HealthMdTheme
import com.healthmd.exporter.ui.HistoryScreen
import com.healthmd.exporter.ui.Honey
import com.healthmd.exporter.ui.Ink
import com.healthmd.exporter.ui.InkSoft
import com.healthmd.exporter.ui.FreshGreen
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 强制浅色：「春野」为浅色设计体系
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            HealthMdTheme {
                HealthMdApp()
            }
        }
    }
}

private data class TabDef(val label: String, val iconRes: Int)

@Composable
private fun HealthMdApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refreshTick by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val tabs = listOf(
        TabDef("概览", R.drawable.ic_dashboard),
        TabDef("连接", R.drawable.ic_home),
        TabDef("导出", R.drawable.ic_export),
        TabDef("记录", R.drawable.ic_history)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopRow(
                onRefresh = { refreshTick++ },
                onSettings = { showSettings = true },
                onAbout = { showAbout = true }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, def ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            tab = index
                            refreshTick++
                        },
                        icon = {
                            Icon(painterResource(def.iconRes), contentDescription = def.label)
                        },
                        label = { Text(def.label, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = com.healthmd.exporter.ui.DeepGreen,
                            indicatorColor = FreshGreen,
                            unselectedIconColor = InkSoft,
                            unselectedTextColor = InkSoft
                        )
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (slideInVertically { it / 5 } + fadeIn()) togetherWith fadeOut()
            },
            label = "tab",
            modifier = Modifier.padding(padding)
        ) { current ->
            when (current) {
                0 -> DashboardScreen(
                    onQuickExport = { tab = 2; refreshTick++ },
                    onGoConnect = { tab = 1; refreshTick++ },
                    refreshTick = refreshTick
                )
                1 -> ConnectScreen(refreshTick)
                2 -> ExportScreen()
                3 -> HistoryScreen(refreshTick)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("关于 HealthMD 导出") },
            text = {
                Text(
                    "本地优先的健康数据导出工具。\n\n" +
                        "将手机 Health Connect 中的数据导出为 Markdown / CSV / JSON 文件，" +
                        "数据仅保存在本机，不上传任何云端。\n\n" +
                        "参考 health.md（AGPL-3.0，healthmd.app）的设计理念独立实现，供个人本地使用。"
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("好的", fontWeight = FontWeight.Bold, color = FreshGreen)
                }
            }
        )
    }
}

@Composable
private fun TopRow(onRefresh: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HealthMD 导出",
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = InkSoft)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = InkSoft)
        }
        IconButton(onClick = onAbout) {
            Icon(Icons.Rounded.Info, contentDescription = "关于", tint = InkSoft)
        }
    }
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var autoExport by remember { mutableStateOf(Prefs.autoExportEnabled(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("自动导出设置") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("每天自动导出最近 7 天数据", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(
                            "保存在应用目录 auto_exports 下，仅保留最近 30 份。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = autoExport,
                        onCheckedChange = { checked ->
                            autoExport = checked
                            Prefs.setAutoExportEnabled(context, checked)
                            if (checked) {
                                val request = PeriodicWorkRequestBuilder<AutoExportWorker>(1, TimeUnit.DAYS).build()
                                WorkManager.getInstance(context)
                                    .enqueueUniquePeriodicWork("auto_export", ExistingPeriodicWorkPolicy.UPDATE, request)
                                promptBatteryOptimization(context)
                            } else {
                                WorkManager.getInstance(context).cancelUniqueWork("auto_export")
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FreshGreen)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "提示：本应用完全本地运行，数据不会离开手机。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Honey
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", fontWeight = FontWeight.Bold, color = FreshGreen)
            }
        }
    )
}

/** 国内 ROM 常杀后台：开启自动导出时引导允许忽略电池优化 */
private fun promptBatteryOptimization(context: android.content.Context) {
    val pm = context.getSystemService(PowerManager::class.java)
    if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
    val intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

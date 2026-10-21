package com.healthmd.exporter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.health.connect.client.HealthConnectClient
import com.healthmd.exporter.ExportConfig
import com.healthmd.exporter.ExportEngine
import com.healthmd.exporter.ExportResult
import com.healthmd.exporter.HealthConnectManager
import com.healthmd.exporter.HealthMetrics
import com.healthmd.exporter.R
import com.healthmd.exporter.SdkAvailability
import com.healthmd.exporter.ShareUtil
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private data class Preset(val label: String, val days: Long)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedPreset by remember { mutableStateOf(1) }
    var formats by remember { mutableStateOf(setOf("markdown")) }
    var metrics by remember { mutableStateOf(HealthMetrics.ALL.map { it.key }.toSet()) }
    var exporting by remember { mutableStateOf(false) }
    var progressMsg by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ExportResult?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        Preset("今天", 0), Preset("近 7 天", 6), Preset("近 30 天", 29), Preset("近 90 天", 89)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SectionTitle("时间范围")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEachIndexed { index, preset ->
                FilterChip(
                    selected = selectedPreset == index,
                    onClick = {
                        selectedPreset = index
                        val today = LocalDate.now()
                        endDate = today
                        startDate = today.minusDays(preset.days)
                    },
                    label = { Text(preset.label) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FreshGreen,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFFD6E4DA),
                        selectedBorderColor = FreshGreen,
                        enabled = true,
                        selected = selectedPreset == index
                    )
                )
            }
            FilterChip(
                selected = selectedPreset == 4,
                onClick = {
                    selectedPreset = 4
                    android.app.DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val chosen = LocalDate.of(y, m + 1, d)
                            startDate = chosen
                            if (startDate.isAfter(endDate)) endDate = startDate
                        },
                        startDate.year, startDate.monthValue - 1, startDate.dayOfMonth
                    ).show()
                },
                label = { Text("自定义") },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FreshGreen,
                    selectedLabelColor = Color.White
                )
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).vitalEnter(1),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${startDate.format(dateFmt)}  →  ${endDate.format(dateFmt)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(endDate.toEpochDay() - startDate.toEpochDay() + 1)} 天",
                    style = MaterialTheme.typography.labelLarge,
                    color = FreshGreen
                )
            }
        }

        SectionTitle("导出格式")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormatCard("Markdown", "MD", "Obsidian 友好", "markdown", formats, Modifier.weight(1f)) {
                formats = if ("markdown" in formats) formats - "markdown" else formats + "markdown"
            }
            FormatCard("CSV", "CSV", "按指标分文件", "csv", formats, Modifier.weight(1f)) {
                formats = if ("csv" in formats) formats - "csv" else formats + "csv"
            }
            FormatCard("JSON", "JSON", "原始记录", "json", formats, Modifier.weight(1f)) {
                formats = if ("json" in formats) formats - "json" else formats + "json"
            }
        }

        SectionTitle("数据指标")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            HealthMetrics.ALL.forEachIndexed { index, metric ->
                val selected = metric.key in metrics
                val bg by animateColorAsState(
                    if (selected) StepsContainer else Color.White,
                    label = "chip"
                )
                val scale by animateFloatAsState(
                    if (selected) 1f else 0.96f,
                    spring(dampingRatio = 0.5f, stiffness = 600f),
                    label = "chipScale"
                )
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .clickable {
                            metrics = if (selected) metrics - metric.key else metrics + metric.key
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (selected) "✓ ${metric.label}" else metric.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) DeepGreen else InkSoft
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (exporting) return@Button
                if (formats.isEmpty() || metrics.isEmpty()) {
                    errorMsg = "请至少选择一种格式和一项指标"
                    return@Button
                }
                if (HealthConnectManager.sdkAvailability(context) != SdkAvailability.AVAILABLE) {
                    errorMsg = "Health Connect 不可用，请先安装或更新"
                    return@Button
                }
                val config = ExportConfig(startDate, endDate, metrics, formats)
                exporting = true
                scope.launch {
                    result = runCatching {
                        ExportEngine.export(context, config) { progressMsg = it }
                    }.onFailure {
                        errorMsg = "导出失败：${it.message ?: "未知错误"}"
                    }.getOrNull()
                    exporting = false
                    progressMsg = ""
                }
            },
            enabled = !exporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .vitalEnter(2),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FreshGreen)
        ) {
            if (exporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.size(10.dp))
                Text("正在导出…", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("开始导出", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (exporting && progressMsg.isNotEmpty()) {
            Text(
                text = progressMsg,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = { result = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("导出完成") },
            text = {
                Text(
                    "时间范围：${r.startDateStr} ~ ${r.endDateStr}\n" +
                        "共 ${r.totalRecords} 条记录，${r.files.size} 个文件\n" +
                        "格式：${r.formats.joinToString("/")}\n" +
                        "位置：${r.exportDir.absolutePath}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    result = null
                    ShareUtil.shareFiles(context, r.files)
                }) { Text("分享文件", fontWeight = FontWeight.Bold, color = FreshGreen) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { result = null; openFolder(context, r.exportDir) }) { Text("打开目录") }
                    TextButton(onClick = { result = null }) { Text("关闭") }
                }
            }
        )
    }

    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMsg = null }) { Text("知道了", fontWeight = FontWeight.Bold, color = FreshGreen) }
            }
        )
    }
}

@Composable
private fun FormatCard(
    title: String,
    tag: String,
    sub: String,
    key: String,
    selected: Set<String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isOn = key in selected
    val container by animateColorAsState(if (isOn) StepsContainer else Color.White, label = "fmt")
    val scale by animateFloatAsState(
        if (isOn) 1f else 0.95f,
        spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "fmtScale"
    )
    Card(
        modifier = modifier.scale(scale).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(if (isOn) 2.dp else 1.dp, if (isOn) FreshGreen else Color(0xFFE3EFE6))
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .background(
                        if (isOn) FreshGreen else Color(0xFFF0F4F1),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(tag, style = MaterialTheme.typography.labelSmall, color = if (isOn) Color.White else InkSoft, fontWeight = FontWeight.Bold)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isOn) DeepGreen else Ink,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(sub, style = MaterialTheme.typography.labelSmall, color = InkSoft, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** 通过 FileProvider 尝试让文件管理器打开导出目录 */
private fun openFolder(context: android.content.Context, dir: File) {
    val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dir)
    } catch (_: Exception) {
        return
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "resource/folder")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

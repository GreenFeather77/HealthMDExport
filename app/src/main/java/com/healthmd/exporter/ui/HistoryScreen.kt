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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthmd.exporter.ExportHistoryStore
import com.healthmd.exporter.HistoryEntry
import com.healthmd.exporter.ShareUtil
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(refreshTick: Int) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(ExportHistoryStore.load(context)) }
    var pendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }

    LaunchedEffect(refreshTick) {
        entries = ExportHistoryStore.load(context)
    }

    if (entries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(StepsContainer, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📄", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = "还没有导出记录",
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.padding(top = 18.dp)
            )
            Text(
                text = "去「导出」页生成第一份吧",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SectionTitle("导出历史")
        entries.forEachIndexed { index, entry ->
            HistoryCard(
                entry = entry,
                index = index,
                onShare = {
                    val dir = File(context.getExternalFilesDir(null), "exports/${entry.id}")
                    val files = dir.listFiles()?.toList() ?: emptyList()
                    if (files.isNotEmpty()) ShareUtil.shareFiles(context, files)
                },
                onDelete = { pendingDelete = entry }
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(24.dp))
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("删除导出记录") },
            text = { Text("确定删除这份导出及其文件吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    ExportHistoryStore.delete(context, entry)
                    entries = ExportHistoryStore.load(context)
                    pendingDelete = null
                }) { Text("删除", fontWeight = FontWeight.Bold, color = CoralDark) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, index: Int, onShare: () -> Unit, onDelete: () -> Unit) {
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    val createdAt = remember(entry.createdAt) {
        Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).format(fmt)
    }

    Card(
        modifier = Modifier.fillMaxWidth().vitalEnter(index.coerceAtMost(5)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.startDate} 至 ${entry.endDate}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                Spacer(Modifier.weight(1f))
                Text(createdAt, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                entry.formats.forEach { format ->
                    Box(
                        modifier = Modifier
                            .background(StepsContainer, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(format.uppercase(), style = MaterialTheme.typography.labelSmall, color = DeepGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                StatText("${entry.totalRecords}", "条记录")
                StatText("${entry.fileCount}", "个文件")
                StatText(formatSize(entry.sizeBytes), "大小")
            }

            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onShare) { Text("分享", fontWeight = FontWeight.Bold, color = FreshGreen) }
                TextButton(onClick = onDelete) { Text("删除", fontWeight = FontWeight.Bold, color = CoralDark) }
            }
        }
    }
}

@Composable
private fun StatText(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft, modifier = Modifier.padding(start = 3.dp, bottom = 2.dp))
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

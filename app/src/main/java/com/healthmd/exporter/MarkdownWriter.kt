package com.healthmd.exporter

import androidx.health.connect.client.records.Record
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 生成 Obsidian 友好的 Markdown 汇总报告（YAML 头 + 汇总表 + 逐日明细） */
object MarkdownWriter {

    private val DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd（E）", Locale.CHINESE)
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    fun write(exportDir: File, config: ExportConfig, data: Map<String, List<Record>>): File {
        val summaries = ExportEngine.buildDaySummaries(config, data)
        val sb = StringBuilder()

        // YAML 头（Obsidian 友好）
        sb.append("---\n")
        sb.append("title: 健康数据导出\n")
        sb.append("provider: health_connect\n")
        sb.append("generated_at: ").append(Instant.now().toString()).append('\n')
        sb.append("start_date: ").append(config.startDate).append('\n')
        sb.append("end_date: ").append(config.endDate).append('\n')
        sb.append("metrics: ")
            .append(HealthMetrics.ALL.filter { it.key in config.metricKeys }.joinToString("、") { it.label })
            .append('\n')
        sb.append("---\n\n")

        sb.append("# 健康数据导出\n\n")
        sb.append("> 导出范围：").append(config.startDate).append(" 至 ").append(config.endDate).append('\n')
        sb.append("> 数据来源：Google Health Connect（本机读取，未上传云端）\n\n")

        // 汇总表
        val totals = totalsOf(summaries)
        sb.append("## 汇总\n\n| 指标 | 合计 |\n| --- | --- |\n")
        totals.forEach { (k, v) -> sb.append("| ").append(k).append(" | ").append(v).append(" |\n") }
        sb.append('\n')

        for (s in summaries) {
            sb.append("## ").append(s.date.format(DAY_FMT)).append("\n\n")

            val activityLines = mutableListOf<String>()
            s.steps?.let { activityLines += "- 步数：${formatInt(it)} 步" }
            s.distanceMeters?.let { activityLines += "- 距离：${formatDistance(it)}" }
            s.caloriesKcal?.let { activityLines += "- 消耗卡路里：${formatNum(it)} kcal" }
            s.activeCaloriesKcal?.let { activityLines += "- 活动卡路里：${formatNum(it)} kcal" }
            s.floors?.let { activityLines += "- 爬楼：${formatNum(it)} 层" }
            if (activityLines.isNotEmpty()) {
                sb.append("### 活动\n\n")
                activityLines.forEach { sb.append(it).append('\n') }
                sb.append('\n')
            }

            if (s.sleepMinutes != null) {
                sb.append("### 睡眠\n\n")
                sb.append("- 总时长：").append(formatMinutes(s.sleepMinutes)).append('\n')
                s.sleepStages.forEach { (k, v) ->
                    sb.append("- ").append(k).append("：").append(formatMinutes(v)).append('\n')
                }
                sb.append('\n')
            }

            val bodyLines = mutableListOf<String>()
            s.weightKg?.let { bodyLines += "- 体重：${formatNum(it)} kg" }
            s.heightCm?.let { bodyLines += "- 身高：${formatNum(it)} cm" }
            s.bodyFatPct?.let { bodyLines += "- 体脂率：${formatNum(it)} %" }
            s.hydrationL?.let { bodyLines += "- 补水量：${formatNum(it)} L" }
            if (bodyLines.isNotEmpty()) {
                sb.append("### 身体数据\n\n")
                bodyLines.forEach { sb.append(it).append('\n') }
                sb.append('\n')
            }

            if (s.exercises.isNotEmpty()) {
                sb.append("### 运动\n\n")
                for (e in s.exercises) {
                    val dur = (e.end.toEpochMilli() - e.start.toEpochMilli()) / 60000.0
                    val title = e.title?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                    sb.append("- ").append(HealthLabels.exerciseName(e.type))
                        .append(" · ").append(TIME_FMT.format(e.start))
                        .append("–").append(TIME_FMT.format(e.end))
                        .append(" · ").append(formatMinutes(dur.toLong()))
                        .append(title).append('\n')
                }
                sb.append('\n')
            }

            if (s.heartRateSamples > 0 || s.restingHr != null) {
                sb.append("### 心率\n\n")
                if (s.heartRateSamples > 0) {
                    sb.append("- 采样 ").append(s.heartRateSamples).append(" 条 · 平均 ")
                        .append(formatNum(s.heartRateAvg ?: 0.0)).append(" bpm · 最大 ")
                        .append(s.heartRateMax).append(" bpm\n")
                }
                s.restingHr?.let { sb.append("- 静息心率：").append(it).append(" bpm\n") }
                sb.append('\n')
            }
        }

        sb.append("---\n\n*由 HealthMD 导出生成 · 数据仅保存在本机*\n")

        val file = File(exportDir, "health-summary.md")
        file.writeText(sb.toString())
        return file
    }

    private fun totalsOf(summaries: List<DaySummary>): List<Pair<String, String>> {
        val steps = summaries.mapNotNull { it.steps }.sum()
        val distance = summaries.mapNotNull { it.distanceMeters }.sum()
        val calories = summaries.mapNotNull { it.caloriesKcal }.sum()
        val floors = summaries.mapNotNull { it.floors }.sum()
        return listOfNotNull(
            "步数" to "${formatInt(steps)} 步",
            distance.takeIf { it > 0 }?.let { "距离" to formatDistance(it) },
            calories.takeIf { it > 0 }?.let { "消耗卡路里" to "${formatNum(it)} kcal" },
            floors.takeIf { it > 0 }?.let { "爬楼" to "${formatNum(it)} 层" }
        )
    }

    private fun formatInt(v: Long): String = String.format(Locale.US, "%,d", v)

    private fun formatNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(Locale.US, "%.1f", v)

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) String.format(Locale.US, "%.2f km", meters / 1000)
        else "${formatInt(meters.toLong())} m"

    private fun formatMinutes(minutes: Long): String {
        if (minutes < 60) return "$minutes 分钟"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "$h 小时" else "$h 小时 $m 分钟"
    }
}

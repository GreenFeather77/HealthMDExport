package com.healthmd.exporter

import android.content.Context
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportConfig(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val metricKeys: Set<String>,
    val formats: Set<String>
)

data class MetricResult(
    val key: String,
    val label: String,
    val count: Long,
    val granted: Boolean
)

data class ExerciseSummary(
    val type: Int,
    val start: Instant,
    val end: Instant,
    val title: String?
)

/** 单日汇总，供 Markdown / JSON 使用 */
data class DaySummary(
    val date: LocalDate,
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val caloriesKcal: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val floors: Double? = null,
    val sleepMinutes: Long? = null,
    val sleepStages: Map<String, Long> = emptyMap(),
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val bodyFatPct: Double? = null,
    val hydrationL: Double? = null,
    val exercises: List<ExerciseSummary> = emptyList(),
    val heartRateSamples: Int = 0,
    val heartRateAvg: Double? = null,
    val heartRateMax: Long? = null,
    val restingHr: Long? = null
)

data class ExportResult(
    val exportDir: File,
    val files: List<File>,
    val metricResults: List<MetricResult>,
    val totalRecords: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val startDateStr: String,
    val endDateStr: String,
    val formats: List<String>,
    val metricKeys: List<String>
)

/** 导出引擎：读取 Health Connect 数据 → 按格式写出到本地目录（本地优先，不上传） */
object ExportEngine {

    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val TS_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")

    suspend fun export(
        context: Context,
        config: ExportConfig,
        autoDir: Boolean = false,
        onProgress: (String) -> Unit = {}
    ): ExportResult = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val startInstant = config.startDate.atStartOfDay(zone).toInstant()
        val endInstant = config.endDate.plusDays(1).atStartOfDay(zone).toInstant()

        val client = HealthConnectManager.client(context)
        val granted = client.permissionController.getGrantedPermissions()

        val baseDir = File(context.getExternalFilesDir(null), if (autoDir) "auto_exports" else "exports")
        val dirName =
            "${LocalDateTime.now().format(TS_FMT)}_${config.startDate.format(DATE_FMT)}_${config.endDate.format(DATE_FMT)}"
        val exportDir = File(baseDir, dirName).apply { mkdirs() }

        val data = LinkedHashMap<String, List<Record>>()
        val metricResults = mutableListOf<MetricResult>()
        val selectedMetrics = HealthMetrics.ALL.filter { it.key in config.metricKeys }

        for (metric in selectedMetrics) {
            val allowed = metric.permission in granted
            if (allowed) {
                onProgress("正在读取${metric.label}…")
                data[metric.key] = try {
                    HealthConnectManager.readAll(client, metric.recordClass, startInstant, endInstant)
                } catch (_: Exception) {
                    // 单个指标读取失败（如权限中途被撤回）不影响其他指标
                    emptyList()
                }
            }
            metricResults.add(
                MetricResult(metric.key, metric.label, data[metric.key]?.size?.toLong() ?: 0L, allowed)
            )
        }

        val files = mutableListOf<File>()
        if ("markdown" in config.formats) {
            onProgress("正在生成 Markdown…")
            files += MarkdownWriter.write(exportDir, config, data)
        }
        if ("csv" in config.formats) {
            onProgress("正在生成 CSV…")
            files += CsvWriter.write(exportDir, config, data)
        }
        if ("json" in config.formats) {
            onProgress("正在生成 JSON…")
            files += JsonWriter.write(exportDir, config, data)
        }

        val totalRecords = metricResults.sumOf { it.count }
        val result = ExportResult(
            exportDir = exportDir,
            files = files,
            metricResults = metricResults,
            totalRecords = totalRecords,
            startedAt = System.currentTimeMillis(),
            finishedAt = System.currentTimeMillis(),
            startDateStr = config.startDate.toString(),
            endDateStr = config.endDate.toString(),
            formats = config.formats.toList(),
            metricKeys = selectedMetrics.map { it.key }
        )

        if (!autoDir) {
            ExportHistoryStore.add(context, result)
        }
        result
    }

    /** 按天构建汇总（记录归属其开始时间所在日期；点状记录归属记录时间所在日期） */
    fun buildDaySummaries(config: ExportConfig, data: Map<String, List<Record>>): List<DaySummary> {
        val zone = ZoneId.systemDefault()
        val grouped = mutableMapOf<String, Map<LocalDate, List<Record>>>()
        for ((key, records) in data) {
            grouped[key] = records.groupBy { recordLocalDate(key, it, zone) }
        }

        val days = mutableListOf<DaySummary>()
        var day = config.startDate
        while (!day.isAfter(config.endDate)) {
            days += summarizeDay(day, zone, grouped)
            day = day.plusDays(1)
        }
        return days
    }

    private fun recordLocalDate(key: String, record: Record, zone: ZoneId): LocalDate {
        val instant = when (record) {
            is StepsRecord -> record.startTime
            is DistanceRecord -> record.startTime
            is TotalCaloriesBurnedRecord -> record.startTime
            is ActiveCaloriesBurnedRecord -> record.startTime
            is FloorsClimbedRecord -> record.startTime
            is SleepSessionRecord -> record.startTime
            is WeightRecord -> record.time
            is HeightRecord -> record.time
            is ExerciseSessionRecord -> record.startTime
            is BodyFatRecord -> record.time
            is HydrationRecord -> record.startTime
            is HeartRateRecord -> record.startTime
            is RestingHeartRateRecord -> record.time
            else -> throw IllegalArgumentException("不支持的记录类型: $key")
        }
        return instant.atZone(zone).toLocalDate()
    }

    private fun summarizeDay(
        day: LocalDate,
        zone: ZoneId,
        grouped: Map<String, Map<LocalDate, List<Record>>>
    ): DaySummary {
        val dayStart = day.atStartOfDay(zone).toInstant()
        val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant()

        var steps: Long? = null
        var distanceM: Double? = null
        var calories: Double? = null
        var activeCal: Double? = null
        var floors: Double? = null
        var sleepMin: Long? = null
        val sleepStages = mutableMapOf<String, Long>()
        var weight: Double? = null
        var height: Double? = null
        var bodyFat: Double? = null
        var hydration: Double? = null
        val exercises = mutableListOf<ExerciseSummary>()
        var hrSamples = 0
        var hrSum = 0.0
        var hrMax: Long? = null
        var restingHr: Long? = null

        grouped["steps"]?.get(day)?.forEach { r ->
            steps = (steps ?: 0L) + (r as StepsRecord).count
        }
        grouped["distance"]?.get(day)?.forEach { r ->
            distanceM = (distanceM ?: 0.0) + (r as DistanceRecord).distance.inMeters
        }
        grouped["calories"]?.get(day)?.forEach { r ->
            calories = (calories ?: 0.0) + (r as TotalCaloriesBurnedRecord).energy.inKilocalories
        }
        grouped["calories_active"]?.get(day)?.forEach { r ->
            activeCal = (activeCal ?: 0.0) + (r as ActiveCaloriesBurnedRecord).energy.inKilocalories
        }
        grouped["floors"]?.get(day)?.forEach { r ->
            floors = (floors ?: 0.0) + (r as FloorsClimbedRecord).floors
        }
        grouped["sleep"]?.get(day)?.forEach { r ->
            val rec = r as SleepSessionRecord
            val mins = (rec.endTime.toEpochMilli() - rec.startTime.toEpochMilli()) / 60000.0
            sleepMin = (sleepMin ?: 0L) + mins.toLong()
            for (stage in rec.stages) {
                val key = HealthLabels.stageLabel(stage.stage)
                val stageMins = (stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()) / 60000.0
                sleepStages[key] = (sleepStages[key] ?: 0L) + stageMins.toLong()
            }
        }
        grouped["weight"]?.get(day)?.let { list ->
            weight = list.maxByOrNull { (it as WeightRecord).time }?.let { (it as WeightRecord).weight.inKilograms }
        }
        grouped["height"]?.get(day)?.let { list ->
            height = list.maxByOrNull { (it as HeightRecord).time }?.let { (it as HeightRecord).height.inMeters * 100.0 }
        }
        grouped["body_fat"]?.get(day)?.let { list ->
            bodyFat = list.maxByOrNull { (it as BodyFatRecord).time }?.let { (it as BodyFatRecord).percentage.value * 100.0 }
        }
        grouped["hydration"]?.get(day)?.forEach { r ->
            hydration = (hydration ?: 0.0) + (r as HydrationRecord).volume.inLiters
        }
        grouped["exercise"]?.get(day)?.forEach { r ->
            val rec = r as ExerciseSessionRecord
            exercises += ExerciseSummary(rec.exerciseType, rec.startTime, rec.endTime, rec.title)
        }
        grouped["heart_rate"]?.get(day)?.forEach { r ->
            val rec = r as HeartRateRecord
            for (sample in rec.samples) {
                if (!sample.time.isBefore(dayStart) && sample.time.isBefore(dayEnd)) {
                    hrSamples++
                    hrSum += sample.beatsPerMinute.toDouble()
                    if (hrMax == null || sample.beatsPerMinute > hrMax!!) hrMax = sample.beatsPerMinute
                }
            }
        }
        grouped["resting_hr"]?.get(day)?.let { list ->
            restingHr = list.maxByOrNull { (it as RestingHeartRateRecord).time }?.let { (it as RestingHeartRateRecord).beatsPerMinute }
        }

        return DaySummary(
            date = day,
            steps = steps,
            distanceMeters = distanceM,
            caloriesKcal = calories,
            activeCaloriesKcal = activeCal,
            floors = floors,
            sleepMinutes = sleepMin,
            sleepStages = sleepStages,
            weightKg = weight,
            heightCm = height,
            bodyFatPct = bodyFat,
            hydrationL = hydration,
            exercises = exercises,
            heartRateSamples = hrSamples,
            heartRateAvg = if (hrSamples > 0) hrSum / hrSamples else null,
            heartRateMax = hrMax,
            restingHr = restingHr
        )
    }
}

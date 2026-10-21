package com.healthmd.exporter

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

/** 生成 CSV：每个指标一个文件，时间为 ISO-8601，数值使用 SI 单位 */
object CsvWriter {

    fun write(exportDir: File, config: ExportConfig, data: Map<String, List<Record>>): List<File> {
        val files = mutableListOf<File>()
        for ((key, records) in data) {
            if (records.isEmpty()) continue
            val content = when (key) {
                "steps" -> stepsCsv(records.map { it as StepsRecord })
                "distance" -> distanceCsv(records.map { it as DistanceRecord })
                "calories" -> totalCaloriesCsv(records.map { it as TotalCaloriesBurnedRecord })
                "calories_active" -> activeCaloriesCsv(records.map { it as ActiveCaloriesBurnedRecord })
                "floors" -> floorsCsv(records.map { it as FloorsClimbedRecord })
                "sleep" -> sleepCsv(records.map { it as SleepSessionRecord })
                "weight" -> weightCsv(records.map { it as WeightRecord })
                "height" -> heightCsv(records.map { it as HeightRecord })
                "exercise" -> exerciseCsv(records.map { it as ExerciseSessionRecord })
                "body_fat" -> bodyFatCsv(records.map { it as BodyFatRecord })
                "hydration" -> hydrationCsv(records.map { it as HydrationRecord })
                "heart_rate" -> heartRateCsv(records.map { it as HeartRateRecord })
                "resting_hr" -> restingCsv(records.map { it as RestingHeartRateRecord })
                else -> continue
            }
            val f = File(exportDir, "$key.csv")
            f.writeText(content)
            files += f
        }
        return files
    }

    private fun stepsCsv(records: List<StepsRecord>): String = buildString {
        appendLine("start,end,count")
        records.forEach { appendLine("${it.startTime},${it.endTime},${it.count}") }
    }

    private fun distanceCsv(records: List<DistanceRecord>): String = buildString {
        appendLine("start,end,distance_m")
        records.forEach { appendLine("${it.startTime},${it.endTime},${it.distance.inMeters}") }
    }

    private fun totalCaloriesCsv(records: List<TotalCaloriesBurnedRecord>): String = buildString {
        appendLine("start,end,energy_kcal")
        records.forEach { appendLine("${it.startTime},${it.endTime},${it.energy.inKilocalories}") }
    }

    private fun activeCaloriesCsv(records: List<ActiveCaloriesBurnedRecord>): String = buildString {
        appendLine("start,end,energy_kcal")
        records.forEach { appendLine("${it.startTime},${it.endTime},${it.energy.inKilocalories}") }
    }

    private fun floorsCsv(records: List<FloorsClimbedRecord>): String = buildString {
        appendLine("start,end,floors")
        records.forEach { appendLine("${it.startTime},${it.endTime},${it.floors}") }
    }

    private fun sleepCsv(records: List<SleepSessionRecord>): String = buildString {
        appendLine("start,end,duration_min,title")
        records.forEach {
            val mins = (it.endTime.toEpochMilli() - it.startTime.toEpochMilli()) / 60000.0
            appendLine("${it.startTime},${it.endTime},${mins.toLong()},${csv(it.title ?: "")}")
        }
    }

    private fun weightCsv(records: List<WeightRecord>): String = buildString {
        appendLine("time,weight_kg")
        records.forEach { appendLine("${it.time},${it.weight.inKilograms}") }
    }

    private fun heightCsv(records: List<HeightRecord>): String = buildString {
        appendLine("time,height_cm")
        records.forEach { appendLine("${it.time},${it.height.inMeters * 100.0}") }
    }

    private fun exerciseCsv(records: List<ExerciseSessionRecord>): String = buildString {
        appendLine("start,end,type,title,duration_min")
        records.forEach {
            val mins = (it.endTime.toEpochMilli() - it.startTime.toEpochMilli()) / 60000.0
            appendLine(
                "${it.startTime},${it.endTime},${HealthLabels.exerciseName(it.exerciseType)}," +
                    "${csv(it.title ?: "")},${mins.toLong()}"
            )
        }
    }

    private fun bodyFatCsv(records: List<BodyFatRecord>): String = buildString {
        appendLine("time,body_fat_pct")
        records.forEach { appendLine("${it.time},${it.percentage.value * 100.0}") }
    }

    private fun hydrationCsv(records: List<HydrationRecord>): String = buildString {
        appendLine("start,end,volume_l")
        records.forEach { appendLine("${it.startTime},${it.endTime},${it.volume.inLiters}") }
    }

    private fun heartRateCsv(records: List<HeartRateRecord>): String = buildString {
        appendLine("time,bpm")
        records.forEach { rec -> rec.samples.forEach { appendLine("${it.time},${it.beatsPerMinute}") } }
    }

    private fun restingCsv(records: List<RestingHeartRateRecord>): String = buildString {
        appendLine("time,bpm")
        records.forEach { appendLine("${it.time},${it.beatsPerMinute}") }
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}

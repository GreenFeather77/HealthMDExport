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
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/** 生成 JSON：按指标分组的原始记录，数值携带单位 */
object JsonWriter {

    fun write(exportDir: File, config: ExportConfig, data: Map<String, List<Record>>): File {
        val root = JSONObject()
        root.put("provider", "health_connect")
        root.put("generated_at", Instant.now().toString())
        root.put("start_date", config.startDate.toString())
        root.put("end_date", config.endDate.toString())

        val recordsObj = JSONObject()
        for ((key, records) in data) {
            if (records.isEmpty()) continue
            recordsObj.put(key, recordsJson(key, records))
        }
        root.put("records", recordsObj)

        val file = File(exportDir, "records.json")
        file.writeText(root.toString(2))
        return file
    }

    private fun recordsJson(key: String, records: List<Record>): JSONArray = when (key) {
        "steps" -> JSONArray().apply { records.forEach { r ->
            val rec = r as StepsRecord
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString()).put("count", rec.count))
        } }
        "distance" -> JSONArray().apply { records.forEach { r ->
            val rec = r as DistanceRecord
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString()).put("distance_m", rec.distance.inMeters))
        } }
        "calories" -> JSONArray().apply { records.forEach { r ->
            val rec = r as TotalCaloriesBurnedRecord
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString()).put("energy_kcal", rec.energy.inKilocalories))
        } }
        "calories_active" -> JSONArray().apply { records.forEach { r ->
            val rec = r as ActiveCaloriesBurnedRecord
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString()).put("energy_kcal", rec.energy.inKilocalories))
        } }
        "floors" -> JSONArray().apply { records.forEach { r ->
            val rec = r as FloorsClimbedRecord
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString()).put("floors", rec.floors))
        } }
        "sleep" -> JSONArray().apply { records.forEach { r ->
            val rec = r as SleepSessionRecord
            val stages = JSONArray()
            rec.stages.forEach { s ->
                stages.put(JSONObject()
                    .put("stage", HealthLabels.stageLabel(s.stage))
                    .put("start", s.startTime.toString())
                    .put("end", s.endTime.toString()))
            }
            val dur = (rec.endTime.toEpochMilli() - rec.startTime.toEpochMilli()) / 60000.0
            put(JSONObject()
                .put("start", rec.startTime.toString())
                .put("end", rec.endTime.toString())
                .put("duration_min", dur.toLong())
                .put("title", rec.title ?: "")
                .put("stages", stages))
        } }
        "weight" -> JSONArray().apply { records.forEach { r ->
            val rec = r as WeightRecord
            put(JSONObject().put("time", rec.time.toString()).put("weight_kg", rec.weight.inKilograms))
        } }
        "height" -> JSONArray().apply { records.forEach { r ->
            val rec = r as HeightRecord
            put(JSONObject().put("time", rec.time.toString()).put("height_cm", rec.height.inMeters * 100.0))
        } }
        "exercise" -> JSONArray().apply { records.forEach { r ->
            val rec = r as ExerciseSessionRecord
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString())
                .put("type", HealthLabels.exerciseName(rec.exerciseType)).put("title", rec.title ?: ""))
        } }
        "body_fat" -> JSONArray().apply { records.forEach { r ->
            val rec = r as BodyFatRecord
            put(JSONObject().put("time", rec.time.toString()).put("body_fat_pct", rec.percentage.value * 100.0))
        } }
        "hydration" -> JSONArray().apply { records.forEach { r ->
            val rec = r as HydrationRecord
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString()).put("volume_l", rec.volume.inLiters))
        } }
        "heart_rate" -> JSONArray().apply { records.forEach { r ->
            val rec = r as HeartRateRecord
            val samples = JSONArray()
            rec.samples.forEach { s -> samples.put(JSONObject().put("time", s.time.toString()).put("bpm", s.beatsPerMinute)) }
            put(JSONObject().put("start", rec.startTime.toString()).put("end", rec.endTime.toString()).put("samples", samples))
        } }
        "resting_hr" -> JSONArray().apply { records.forEach { r ->
            val rec = r as RestingHeartRateRecord
            put(JSONObject().put("time", rec.time.toString()).put("bpm", rec.beatsPerMinute))
        } }
        else -> JSONArray()
    }
}

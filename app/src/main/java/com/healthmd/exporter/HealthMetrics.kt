package com.healthmd.exporter

import androidx.health.connect.client.permission.HealthPermission
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
import kotlin.reflect.KClass

/** 单一健康指标的元信息：标识、展示名、记录类型、权限、是否受限权限 */
data class MetricDef(
    val key: String,
    val label: String,
    val recordClass: KClass<out Record>,
    val permission: String,
    val restricted: Boolean
)

object HealthMetrics {

    /** 从记录类型推导读取权限（1.1.0 起权限常量不再公开，统一走 getReadPermission） */
    private fun readPermission(recordClass: KClass<out Record>): String =
        HealthPermission.getReadPermission(recordClass)

    val ALL: List<MetricDef> = listOf(
        MetricDef("steps", "步数", StepsRecord::class, readPermission(StepsRecord::class), false),
        MetricDef("distance", "距离", DistanceRecord::class, readPermission(DistanceRecord::class), false),
        MetricDef("calories", "总消耗卡路里", TotalCaloriesBurnedRecord::class, readPermission(TotalCaloriesBurnedRecord::class), false),
        MetricDef("calories_active", "活动卡路里", ActiveCaloriesBurnedRecord::class, readPermission(ActiveCaloriesBurnedRecord::class), false),
        MetricDef("floors", "爬楼层数", FloorsClimbedRecord::class, readPermission(FloorsClimbedRecord::class), false),
        MetricDef("sleep", "睡眠", SleepSessionRecord::class, readPermission(SleepSessionRecord::class), false),
        MetricDef("weight", "体重", WeightRecord::class, readPermission(WeightRecord::class), false),
        MetricDef("height", "身高", HeightRecord::class, readPermission(HeightRecord::class), false),
        MetricDef("exercise", "运动记录", ExerciseSessionRecord::class, readPermission(ExerciseSessionRecord::class), false),
        MetricDef("body_fat", "体脂率", BodyFatRecord::class, readPermission(BodyFatRecord::class), false),
        MetricDef("hydration", "补水量", HydrationRecord::class, readPermission(HydrationRecord::class), false),
        MetricDef("heart_rate", "心率", HeartRateRecord::class, readPermission(HeartRateRecord::class), true),
        MetricDef("resting_hr", "静息心率", RestingHeartRateRecord::class, readPermission(RestingHeartRateRecord::class), true)
    )

    fun byKey(key: String): MetricDef? = ALL.firstOrNull { it.key == key }
}

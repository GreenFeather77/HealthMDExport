package com.healthmd.exporter

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass

enum class SdkAvailability { AVAILABLE, UPDATE_REQUIRED, NOT_INSTALLED }

/** Health Connect 客户端封装：可用性、权限、分页读取 */
object HealthConnectManager {

    fun sdkAvailability(context: Context): SdkAvailability =
        // 单参数版本自动检测当前设备上的 Health Connect provider（Android 14+ 为系统模块，包名不固定）
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> SdkAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> SdkAvailability.UPDATE_REQUIRED
            else -> SdkAvailability.NOT_INSTALLED
        }

    fun client(context: Context): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    fun allPermissions(): Set<String> = HealthMetrics.ALL.map { it.permission }.toSet()

    suspend fun grantedPermissions(context: Context): Set<String> =
        client(context).permissionController.getGrantedPermissions()

    /** 分页读取某类型在时间范围内的全部记录 */
    suspend fun <T : Record> readAll(
        client: HealthConnectClient,
        recordType: KClass<T>,
        start: Instant,
        end: Instant
    ): List<T> {
        val all = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val request = ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageToken = pageToken
            )
            val response = client.readRecords(request)
            all += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return all
    }
}

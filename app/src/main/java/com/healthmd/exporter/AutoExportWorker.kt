package com.healthmd.exporter

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.time.LocalDate

/** 每日自动导出最近 7 天数据（Markdown），仅保留最近 30 份 */
class AutoExportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            if (!Prefs.autoExportEnabled(ctx)) return Result.success()

            // Health Connect 未就绪或未授权时静默跳过（下个周期再试），不无限重试
            if (HealthConnectManager.sdkAvailability(ctx) != SdkAvailability.AVAILABLE) return Result.success()
            val granted = HealthConnectManager.grantedPermissions(ctx)
            if (granted.isEmpty()) return Result.success()

            val end = LocalDate.now()
            val start = end.minusDays(6)
            val config = ExportConfig(
                startDate = start,
                endDate = end,
                metricKeys = HealthMetrics.ALL.map { it.key }.toSet(),
                formats = setOf("markdown")
            )
            ExportEngine.export(ctx, config, autoDir = true) {}
            pruneAutoExports(ctx)
            Result.success()
        } catch (_: Exception) {
            // 失败不重试：周期任务下个周期（每日）会再次执行，避免 WorkManager 无限重试
            Result.success()
        }
    }

    private fun pruneAutoExports(context: Context) {
        val dir = context.getExternalFilesDir(null)?.let { File(it, "auto_exports") } ?: return
        val dirs = dir.listFiles { f -> f.isDirectory }?.sortedByDescending { it.name } ?: return
        dirs.drop(30).forEach { it.deleteRecursively() }
    }
}

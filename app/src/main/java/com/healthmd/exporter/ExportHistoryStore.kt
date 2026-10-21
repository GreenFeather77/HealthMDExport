package com.healthmd.exporter

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val id: String,
    val createdAt: Long,
    val startDate: String,
    val endDate: String,
    val formats: List<String>,
    val metrics: List<String>,
    val totalRecords: Long,
    val fileCount: Int,
    val sizeBytes: Long
)

/** 导出历史：持久化到应用私有目录，记录文件与导出目录一一对应 */
object ExportHistoryStore {

    private const val FILE = "history.json"

    private fun file(context: Context): File = File(context.filesDir, FILE)

    fun load(context: Context): List<HistoryEntry> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                HistoryEntry(
                    id = o.getString("id"),
                    createdAt = o.getLong("createdAt"),
                    startDate = o.getString("startDate"),
                    endDate = o.getString("endDate"),
                    formats = o.getJSONArray("formats").let { a -> (0 until a.length()).map { a.getString(it) } },
                    metrics = o.getJSONArray("metrics").let { a -> (0 until a.length()).map { a.getString(it) } },
                    totalRecords = o.getLong("totalRecords"),
                    fileCount = o.getInt("fileCount"),
                    sizeBytes = o.getLong("sizeBytes")
                )
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, result: ExportResult) {
        val entry = HistoryEntry(
            id = result.exportDir.name,
            createdAt = result.startedAt,
            startDate = result.startDateStr,
            endDate = result.endDateStr,
            formats = result.formats,
            metrics = result.metricKeys,
            totalRecords = result.totalRecords,
            fileCount = result.files.size,
            sizeBytes = result.files.sumOf { it.length() }
        )
        val list = load(context).toMutableList()
        list.removeAll { it.id == entry.id }
        list.add(0, entry)
        save(context, list)
    }

    fun delete(context: Context, entry: HistoryEntry) {
        val list = load(context).toMutableList()
        list.removeAll { it.id == entry.id }
        save(context, list)
        runCatching {
            File(context.getExternalFilesDir(null), "exports/${entry.id}").deleteRecursively()
        }
    }

    private fun save(context: Context, list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("createdAt", e.createdAt)
                    .put("startDate", e.startDate)
                    .put("endDate", e.endDate)
                    .put("formats", JSONArray(e.formats))
                    .put("metrics", JSONArray(e.metrics))
                    .put("totalRecords", e.totalRecords)
                    .put("fileCount", e.fileCount)
                    .put("sizeBytes", e.sizeBytes)
            )
        }
        file(context).writeText(arr.toString(2))
    }
}

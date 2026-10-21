package com.healthmd.exporter

import android.content.Context
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File

/** 通过系统分享把导出文件发给其他应用（Obsidian、网盘、微信等） */
object ShareUtil {

    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.export_share))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

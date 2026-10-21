package com.healthmd.exporter

import android.content.Context

object Prefs {

    private const val NAME = "healthmd_settings"
    private const val KEY_AUTO_ENABLED = "auto_export_enabled"

    fun autoExportEnabled(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_ENABLED, false)

    fun setAutoExportEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_ENABLED, enabled)
            .apply()
    }
}

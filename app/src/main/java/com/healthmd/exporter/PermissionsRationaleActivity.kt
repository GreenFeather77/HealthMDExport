package com.healthmd.exporter

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.healthmd.exporter.databinding.ActivityRationaleBinding

/**
 * Health Connect 权限说明页。
 * Health Connect 要求应用声明 ACTION_SHOW_PERMISSIONS_RATIONALE，
 * 否则 Health Connect 的「应用权限」列表不会显示本应用，用户无法手动授权。
 */
class PermissionsRationaleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityRationaleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.app_name)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

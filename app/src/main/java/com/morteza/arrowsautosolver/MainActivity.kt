package com.morteza.arrowsautosolver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val captureRequestCode = 7301

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val templates = ArrowTemplateRepository.loadAll(this)
        val title = TextView(this).apply {
            text = "Arrows Auto Solver\n\nنسخه آزمایشی توسعه"
            textSize = 22f
            setPadding(32, 48, 32, 24)
        }
        status = TextView(this).apply {
            text = "الگوهای تصویری بارگذاری‌شده: ${templates.size} از ۴\n" +
                "دریافت تصویر صفحه و اسکن اولیه در حال آماده‌سازی است.\n" +
                "حل کامل و اجرای خودکار حرکت‌ها هنوز تکمیل نشده است."
            textSize = 16f
            setPadding(32, 12, 32, 24)
        }
        val capture = Button(this).apply {
            text = "شروع دریافت تصویر صفحه"
            setOnClickListener { requestScreenCapture() }
        }
        val settings = Button(this).apply {
            text = "تنظیمات دسترس‌پذیری اندروید"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(status)
            addView(capture)
            addView(settings)
        }
        setContentView(layout)
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), captureRequestCode)
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureRequestCode) return
        if (resultCode != RESULT_OK || data == null) {
            status.text = "مجوز دریافت تصویر صادر نشد."
            return
        }

        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        status.text = "دریافت تصویر فعال شد. اسکن اولیه در سرویس پس‌زمینه انجام می‌شود."
    }
}

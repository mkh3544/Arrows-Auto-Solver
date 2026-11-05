package com.morteza.arrowsautosolver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private var receiverRegistered = false

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(ScreenCaptureService.EXTRA_SUMMARY)
            if (!message.isNullOrBlank()) status.text = message
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Arrows Auto Solver\n\nنسخه آزمایشی اسکن صفحه"
            textSize = 22f
            setPadding(32, 48, 32, 24)
        }
        status = TextView(this).apply {
            text = "برای شروع، بازی را باز کن و دکمه دریافت تصویر را بزن.\n" +
                "پس از تأیید مجوز اندروید، شمارش فلش‌ها و مسیرهای باز اینجا نمایش داده می‌شود.\n\n" +
                "توجه: این نسخه هنوز ترتیب کامل حل یا لمس خودکار را اجرا نمی‌کند."
            textSize = 16f
            setPadding(32, 12, 32, 24)
        }
        val capture = Button(this).apply {
            text = "شروع دریافت تصویر و اسکن"
            setOnClickListener { requestScreenCapture() }
        }
        val stop = Button(this).apply {
            text = "توقف اسکن"
            setOnClickListener {
                stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                status.text = "اسکن متوقف شد."
            }
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
            addView(stop)
            addView(settings)
        }
        setContentView(layout)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ScreenCaptureService.ACTION_SCAN_UPDATE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(scanReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(scanReceiver)
            receiverRegistered = false
        }
        super.onStop()
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
        status.text = "دریافت تصویر فعال شد؛ منتظر اولین نتیجه اسکن هستیم..."
    }
}

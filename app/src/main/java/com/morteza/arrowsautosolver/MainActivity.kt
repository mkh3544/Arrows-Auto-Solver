package com.morteza.arrowsautosolver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            text = "Arrows Auto Solver\n\nنسخه اولیه پروژه اندروید"
            textSize = 22f
            setPadding(32, 48, 32, 24)
        }
        val status = TextView(this).apply {
            text = "این نسخه هنوز الگوریتم تشخیص و حل فلش‌ها را ندارد."
            textSize = 16f
            setPadding(32, 12, 32, 24)
        }
        val settings = Button(this).apply {
            text = "تنظیمات دسترس‌پذیری اندروید"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(status)
            addView(settings)
        }
        setContentView(layout)
    }
}

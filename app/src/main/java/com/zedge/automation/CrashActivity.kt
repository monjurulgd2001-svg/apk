package com.zedge.automation

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

/**
 * Shown instead of a silent crash. Plain Android views (no Compose) so it can
 * never crash itself. The user can screenshot this to report the exact error.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra("trace") ?: "Unknown error"
        val tv = TextView(this).apply {
            text = "\u26a0\ufe0f App crashed \u2014 send a screenshot of this screen:\n\n$trace"
            setTextIsSelectable(true)
            setPadding(40, 80, 40, 80)
            textSize = 12f
            setTextColor(Color.BLACK)
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(tv)
        }
        setContentView(scroll)
    }
}

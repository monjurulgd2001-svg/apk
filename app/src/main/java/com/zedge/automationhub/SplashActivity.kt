package com.zedge.automationhub

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val icon = findViewById<ImageView>(R.id.splashIcon)
        val title = findViewById<TextView>(R.id.splashTitle)
        val subtitle = findViewById<TextView>(R.id.splashSubtitle)

        icon.alpha = 0f
        icon.scaleX = 0.5f
        icon.scaleY = 0.5f
        title.alpha = 0f
        title.translationY = 30f
        subtitle.alpha = 0f
        subtitle.translationY = 20f

        val iconAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(icon, "scaleX", 0.5f, 1f),
                ObjectAnimator.ofFloat(icon, "scaleY", 0.5f, 1f)
            )
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        val titleAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(title, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(title, "translationY", 30f, 0f)
            )
            duration = 500
            startDelay = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        val subtitleAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(subtitle, "translationY", 20f, 0f)
            )
            duration = 500
            startDelay = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        val pulseAnim = ObjectAnimator.ofFloat(icon, "alpha", 1f, 0.7f, 1f).apply {
            duration = 1200
            startDelay = 800
            repeatCount = ObjectAnimator.INFINITE
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }
}

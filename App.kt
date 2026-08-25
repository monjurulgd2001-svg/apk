package com.zedge.automation

import android.app.Application
import android.content.Intent
import android.util.Log
import com.zedge.automation.data.FirebaseInit
import kotlin.system.exitProcess

/**
 * Initializes BOTH Firebase projects programmatically with the exact same
 * config values as the web dashboard, so no google-services.json is needed
 * and both clients read/write the same Realtime Databases.
 *
 * Also installs a crash handler that shows the real error on screen
 * (CrashActivity) instead of silently closing the app.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val intent = Intent(this, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("trace", Log.getStackTraceString(throwable))
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            } catch (_: Exception) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }

        try {
            FirebaseInit.ensure(this)
        } catch (_: Exception) {
            // Never block app startup; FirebaseRepo will retry lazily.
        }
    }
}

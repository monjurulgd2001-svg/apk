package com.zedge.automation

import android.app.Application
import com.zedge.automation.data.FirebaseInit

/**
 * Initializes BOTH Firebase projects programmatically with the exact same
 * config values as the web dashboard, so no google-services.json is needed
 * and both clients read/write the same Realtime Databases.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseInit.ensure(this)
        } catch (_: Exception) {
            // Never block app startup; FirebaseRepo will retry lazily.
        }
    }
}

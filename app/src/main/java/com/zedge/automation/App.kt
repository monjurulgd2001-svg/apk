package com.zedge.automation

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.zedge.automation.config.AppConfig

/**
 * Initializes BOTH Firebase projects programmatically with the exact same
 * config values as the web dashboard, so no google-services.json is needed
 * and both clients read/write the same Realtime Databases.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppConfig.FIREBASE_PROJECTS.forEach { (key, cfg) ->
            val options = FirebaseOptions.Builder()
                .setApiKey(cfg.apiKey)
                .setApplicationId(cfg.appId)
                .setDatabaseUrl(cfg.databaseUrl)
                .setProjectId(cfg.projectId)
                .setStorageBucket(cfg.storageBucket)
                .setGcmSenderId(cfg.messagingSenderId)
                .build()
            if (FirebaseApp.getApps(this).none { it.name == key }) {
                FirebaseApp.initializeApp(this, options, key)
            }
        }
    }
}

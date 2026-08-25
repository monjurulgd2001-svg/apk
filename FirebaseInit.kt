package com.zedge.automation.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.zedge.automation.config.AppConfig

/**
 * Crash-safe Firebase initialization. Initializes BOTH named projects and a
 * default app so nothing in the SDK ever throws "FirebaseApp not initialized".
 * Safe to call multiple times from anywhere.
 */
object FirebaseInit {

    @Volatile
    var appContext: Context? = null
        private set

    @Synchronized
    fun ensure(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        AppConfig.FIREBASE_PROJECTS.forEach { (key, cfg) ->
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey(cfg.apiKey)
                    .setApplicationId(cfg.appId)
                    .setDatabaseUrl(cfg.databaseUrl)
                    .setProjectId(cfg.projectId)
                    .setStorageBucket(cfg.storageBucket)
                    .setGcmSenderId(cfg.messagingSenderId)
                    .build()
                if (FirebaseApp.getApps(ctx).none { it.name == key }) {
                    val app = FirebaseApp.initializeApp(ctx, options, key)
                    // v3.5: offline cache — screens render instantly from disk,
                    // then live-sync in the background. Must run before any
                    // getReference() is used on this database instance.
                    try {
                        val fdb = FirebaseDatabase.getInstance(app)
                        fdb.setPersistenceEnabled(true)
                        fdb.getReference(AppConfig.QUEUE_PATH).keepSynced(true)
                    } catch (_: Exception) { /* never crash on init */ }
                }
                // Also register a default app (some SDK internals expect one).
                if (FirebaseApp.getApps(ctx).none { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
                    FirebaseApp.initializeApp(ctx, options)
                }
            } catch (_: Exception) {
                // Never crash the app because of init issues.
            }
        }
    }

    /** Get a named FirebaseApp, re-initializing first if needed. */
    fun app(projectKey: String): FirebaseApp {
        return try {
            FirebaseApp.getInstance(projectKey)
        } catch (e: Exception) {
            appContext?.let { ensure(it) }
            FirebaseApp.getInstance(projectKey)
        }
    }
}

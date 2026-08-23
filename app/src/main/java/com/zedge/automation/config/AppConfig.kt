package com.zedge.automation.config

/**
 * IDENTICAL configuration to the web dashboard (main.js / index.html).
 * Do NOT change these values independently — keep web + mobile in sync.
 */
object AppConfig {

    data class FirebaseProject(
        val key: String,
        val apiKey: String,
        val authDomain: String,
        val databaseUrl: String,
        val projectId: String,
        val storageBucket: String,
        val messagingSenderId: String,
        val appId: String,
        val measurementId: String
    )

    // Same as `configs` in main.js
    val FIREBASE_PROJECTS: Map<String, FirebaseProject> = linkedMapOf(
        "zedge1" to FirebaseProject(
            key = "zedge1",
            apiKey = "AIzaSyBsoxNIpAECkPaFOU0wUHY6q0NcvWbK4AI",
            authDomain = "zedge-r2-edward-hermes.firebaseapp.com",
            databaseUrl = "https://zedge-r2-edward-hermes-default-rtdb.firebaseio.com",
            projectId = "zedge-r2-edward-hermes",
            storageBucket = "zedge-r2-edward-hermes.firebasestorage.app",
            messagingSenderId = "539346447692",
            appId = "1:539346447692:web:dc9d1f1b8f90ca5f7133e9",
            measurementId = "G-838FST5ZKT"
        ),
        "zedge2" to FirebaseProject(
            key = "zedge2",
            apiKey = "AIzaSyBW7Pm5Eegg90htCAcnMZXT5kLCQ2bSxEc",
            authDomain = "zedge-r2-ryan-hermes.firebaseapp.com",
            databaseUrl = "https://zedge-r2-ryan-hermes-default-rtdb.firebaseio.com",
            projectId = "zedge-r2-ryan-hermes",
            storageBucket = "zedge-r2-ryan-hermes.firebasestorage.app",
            messagingSenderId = "347245367374",
            appId = "1:347245367374:web:1a2f6f9e956f263059331f",
            measurementId = "G-L8MCR58M68"
        )
    )

    // Same as R2_WORKER_URL in main.js
    const val R2_WORKER_URL = "https://proud-paper-6fd7.monjurulgd2001.workers.dev/"

    // Same RTDB paths as the web dashboard
    const val QUEUE_PATH = "wallpaperQueue"
    const val UPLOAD_STATE_PATH = "uploadState"

    // Same default Gemini model
    const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"

    // Same Stable Audio endpoint
    const val STABLE_AUDIO_URL =
        "https://api.stableaudio.com/v1alpha/generations/stable-audio-v2-5/text-to-music"

    // Same category lists as main.js
    val AI_IMG_CATS = listOf(
        "ANIMALS","ANIME","BOLLYWOOD","BRANDS","CARS_N_VEHICLES","COMICS","DESIGNS",
        "DRAWINGS","ENTERTAINMENT","FUNNY","GAMES","HOLIDAYS","LOGOS","LOVE","MUSIC",
        "NATURE","NEWS_POLITICS","OTHER","PATTERN","PEOPLE","SAYINGS","SPACE",
        "SPIRITUAL","SPORTS","TECHNOLOGY"
    )

    val AI_RING_CATS = listOf(
        "LATIN","MESSAGE_TONES","OTHER","POP","RNB_SOUL","REGGAE","RELIGIOUS","ROCK",
        "SAYINGS","ALTERNATIVE","ANIMALS","BLUES","BOLLYWOOD","CHILDREN","CLASSICAL",
        "SOUND_EFFECTS","WORLD","COMEDY","CONTACT_RINGTONES","COUNTRY","DANCE",
        "ELECTRONICA","GAMES","HIP_HOP","HOLIDAYS","JAZZ"
    )
}

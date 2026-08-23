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

    // Hidden fallback Mistral keys — auto-rotate, never shown in UI
    val FALLBACK_MISTRAL_KEYS = listOf(
        "wXFCsmmPgZb0KBYDQb1vVEHrevO9DxZl","NAsd2c2R58SWPd8x9XnZYdJxOqDE0m28",
        "WJL70yBR5jWJnMUJrL0Wst6EBsvITdmb","73YfdXAz0gDzwgSzOWsMXgcVuwaaJMy0",
        "cTq5smUFjrRq6uKwHtE00DO4BWf2jHNg","5YuzhSy4KN2N9Ga9SoGDOjIlWrd5Dwpi",
        "frNeyxEbzP3zd8vHrio1sg5PZqVMrLHm","QgVrotkIZPNcieM2XtEc4EUGBAoOS1o4",
        "c5eg5t6zzVtKaEFO2xH7s2nQ7xqQJpym","ccVDyCIgj32JkuOW70GHbalpnHybxkfT",
        "8Yuu3BFrYc5U5aOl0qswnDGbFT3GLQRT","uIGEGcYTt7mkhaR14DoBmi5zXCVMEYFk",
        "a9vEEBTSu86rKHFlWgHOmRRxziXB7ml2","lVtdGW94gqPTIabUJKjG2sCm7x0pIi2K",
        "kqadc3YtU2u5BfCPGVK7WbJK4jntUE89","StZZoOd8OlXfj4L2IIli7WZLJjCIuQfE",
        "beFXydDILA1SmNO7qMOLqdYcf5DMV4Su","GwF53Mvo3MK7Fw4gDLsEddY5YWkDYX8V",
        "cQ8770aYrt1sGWBwcsOkrTEUyGXycbB8","PJaiBbXWbpztKitWWLsuKcCsrGk6jZZL",
        "UBNwLLVSUSebPQRwop2O8p0qj3Hyk0Of","vcLsOvxOQF5iY8gyKGm16rhKZa2Z0IDg",
        "soD0lxXH0wkmw72aDf96zuFhzrFnupES","Z5khZxIcEgch6A1gJwkz69HbCdLX2Obz",
        "MEYbfM3nf8t2ccKK8DEYP59tVrjRZRLr","QRPH8Y1HcN5DRAUSXcvTVSdnjdwOcuvX",
        "Ql6a4Y2X8fq6cEUilDFI4KCDGNu7auV4","bPjiNZf7vcErdfLXkUHaxGRAkiHTiEfl",
        "RMPemuZL5rpQ2CiqpNiDmlbHuxNTHt9i","weMHi8jFMpOmr7IRQMcVjF08YM4pW7tC",
        "OgZMa8krBC02URXpigY8hRdF3dkXbOLB","zDPK0uUoaZNj6cMw9GsgYBfVeuHZDiSg",
        "YWBI3T9FbFv1PoCDFQZZQDbH0qRu4hcj","WmAa7OPq2DeGFfkKNcEbsciXgyoLcNAu",
        "d0H2MBW5hkXd5XtorPkvAKfFpXcmUz7Q","AzKEr2QnFONlBfTtNXLrzsFNKSOew8aX",
        "81U8PSFkmrxX5iS8pAITmctk1h7F6aYF","bkGjWmw8pGm6Vrg6IDunDIAw88dfs5Xz",
        "GTLh5HDjhAq8FipTj6U2E5EJ1ae0aJ69","goy0is5odptBClgfieD8q0O8WYDlbSKV",
        "sLZ3bm4HcuVukSObiURECGaiCkKosqF6","q3KToLuDblcB9fCiMlJl4p1rgCpDSG3q",
        "fcW87zaBuVVBEObIB0xpJRCxp74zkAFQ","uDGevMWjPp865y5smyyEN9QfLGIijDYO",
        "hVdfjV4T40DeFQxxkaOtb9xPx1ffskNL","JE1UfH7nbsydBlDFA0OP4rFNVOOhLZO9",
        "B0mvkCr75DRLDDEoSQqho8Et1l7Kxa1h","qFfPjepCjVHQHGFtlJnfqZWYQfvoMLzW",
        "XU0CkSYLM8r3tNLDhkKC8aCdQOkgYSNa","G6miSZ96G1nZDFhigdW9aM0qKBVZAgdm",
        "reF2sEuJBqCNHjW1ZzKLaDYknTL7aGaW","Zb5zUtItbefjHuCDiSDkAEiOR5RUBBKW",
        "UopPXaQF65bEsyiUG8SqvpIlyj5DrZMI","cqr8PyscnQYqWEhbexguT6C4TBAJ0Evf",
        "KzdCFwqfhS41x3tkMfMGU4kwCIxnK8eL","vtfB3nOLPYUj9MMJ8cx5rSiBcqs9meMx",
        "4ONmV3UiZs959AL5SZPzPEfgMQo1Q3YY","9QboudzwOedlpV0hhlL0fm8a8uID69Gj",
        "wPXyVm5zLqAIKvXjX3iLYDGjcwNaY3NP","fBaQCu18iXRY4nFIUrhJ4vJgSWZ52nGk",
        "P5itVGhOTrIQwdIF0Q1d8QAChDl0KdVw","ecOWx7cOPQzVdl6fNkInCHy7CDuZ3IsE",
        "BjsKGPmRj8ezheCnsDSJVcgsEXLUHapL","qFbVfPo52JUW6Qt0Iq7BDQTWPQEfaI90",
        "pRx8XcMJKpwl2bR8PXuyFThHAFTlxNW3","fbGHaOhb9tBphlr5DCGewG2kIDUTvyJ9",
        "O6OKHFE478MWO3BDNcTkWo55n0KGZMJZ","bsmtQtq32xD1b0vVYZmkkbiPFikVFmcW",
        "GISw9P4MM9CBxo5PMIyOqDofEDQ2qcY6","S4EJMWAvyMIgtjoMQn57fNJbkmi9zSxB",
        "96SJt0jwjCdLJeQTkZ5euLQHFMsvZvMs","j1dBeA76xMG30yNMBNEhqhHYo0QrXviC",
        "yvXFvqPjoA4xgrfNIVCuLUDAANFKCCH1","YR13v7YOsO3HJvLNtmqunZsdTxlSYn7j",
        "c8yDrSPrauL2ccO0um0RFpdm8wsllFmF","iACkkP5xPqWNAeNmHipLyayp04yPJTQT",
        "FT61SmNshboGkcyYKF2M03rLoRdBxq15","nBLh1Q6mf9gRAIAtEbzAs1ArnVt9CJxE",
        "St1lMdS9sgMNKMQCJhLxxj5mpdCFH1NH","DpweLFVdiMf8HUggX1YdGBzlyjBVrRK5",
        "hk8dh5OacO1ZrGNK0iwVYdkHZCslMRe7","IIh1oUXDMx6GuelXhIHFuSoM5EIgaJa1",
        "aeagyoX8IILsUgNjiUn7iPwwEKyBFcgB","CDOoZwkiEsNmJsJJLS04YQelaxXvljyW",
        "IK7fwXzKuWOPXXY1sg8y5oGoVYxisKd3","GkBVApBgmLgImACWeUMSlybYa4zwKOGT",
        "DpVo1jkQ9AKcMEfFvt7oZGyqixzjpdWY","S5TaLvbsGmcrzhcPZYACV7n55CsOqBpd",
        "lhoMSoBeR9gnayWpMQ3uVTREBBGEluic","Nq4qvQfTugQxFO6o6ZaAYFvwAn6xRNlP",
        "spPLvENACwqYGh9f5Vc8YRuODTorqahK","LH0cQqRqCKiyI3oL0IR114E7EWeEB09m",
        "Az5Zu4m9IsqDOCFUXiROq8g4x9L1IFKZ","BDd2UQkAz7cQGwwKyuz0dwmunNsfVqNp",
        "a0MHbQ5CNF2itvrVKnKuaW5J4sdEmUFK","48x7Td6gbW1ixZnYALJILx5vEgMzkzhl",
        "3lS0MZcF9mpbj3zGPL4B1dDI0GMWd7fc","vVAOA4p6WbVcNImu506qA6m2RNZdaDVQ",
        "iFamdEExa9mExiQYybimiN0ZDBANOa9E","THwHqM8RjwTAWSTNXzYRQtW8BsYWdCG8",
        "5CQG7BvCvvLvoWbliSsbNfJSsHcCFvcz","NcooD35BgUtTcIWu3KOoGR3EIJ9eZu2Z",
        "fLDyCln0SVfrMOxslHD3C4wc85KibNeg","y0SKO9RF57ZRt5i1Rzknl3XOAFym7f5c",
        "sxXrjSjobt3j18ItAZuStJAYZEYIaYIY","lqqoUIKjNVVxQ7Cq4YPm5UlzljvkHciR",
        "F43ABRwDM2jNRtVJ7lnODNC3EoRo5q9Y","Pld9pIBu2ZlSEnL8pjT9f8z0VEdgUDJU",
        "x7dj4tIU7ZNgnF5JgOcOJSZbjLBXrPoI","ItJzKmIx5HpNxw49XRBgOeD81nHnYFE6",
        "GGt3mZARLHE13HULDCQsywUE4QTx9Pyq","zVD87BikaIP3FzA4RJccD7VyZhcTwIg4",
        "X5lKdLUvQ0vlAI1tU5lpkJFprZIbIpVW","qSugnKrUc9FpDKxL2EdqPic2pDmEeXeS",
        "093V4lRPcMWMETBhdsqqAO2h1T5l4bmx","fNJx634lHyrhxjdfZgCRyrNRzWm6hvrS",
        "Dl6wkp4KDFvLnSPDH26FWrzM4JE5dV3o","J2V7ik6zcBWwnkjV1kwaBy4S8p52ADcU",
        "ZR5pm2ovE20jvugHLKFhieafbGoveRkd","AYGF7YEHkhjsUIIqLTqK9bjONB9u7myJ"
    )
}

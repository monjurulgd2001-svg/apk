plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zedge.automation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zedge.automation"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "3.5.1"
    }

    // v3.5: fixed signing key committed with the project so every CI build
    // installs as an UPDATE over the previous one (no uninstall needed).
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "zedge2026"
            keyAlias = "zedge"
            keyPassword = "zedge2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = false }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    // Self-heal: the launcher icons ship as .webp. If stray .png copies with
    // the same resource names sneak into the repo, the resource merger fails
    // with "Duplicate resources". Ignore them here (default AAPT pattern kept).
    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~:!ic_launcher.png:!ic_launcher_round.png:!ic_launcher_foreground.png"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Firebase Realtime Database - SAME projects as the web dashboard
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")

    // Networking (R2 gateway worker, Gemini, Stable Audio)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Image loading for wallpaper thumbnails
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// ── Self-heal: auto-remove duplicate launcher PNGs before every build ──
// The project's launcher icons are .webp. Stray .png copies of the same
// resource names (added to the repo by mistake) break mergeResources with
// "Duplicate resources". This deletes them automatically at build time,
// so no manual cleanup is ever needed.
val removeDuplicateLauncherPngs by tasks.registering(Delete::class) {
    delete(
        fileTree("src/main/res") {
            include("mipmap-*/ic_launcher.png")
            include("mipmap-*/ic_launcher_round.png")
            include("mipmap-*/ic_launcher_foreground.png")
        }
    )
}
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(removeDuplicateLauncherPngs)
}

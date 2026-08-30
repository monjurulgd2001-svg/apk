# Automation Hub - Android App

## Project Structure

This is a complete Android Studio project that wraps the existing web-based Automation Hub SPA into a native Android application.

## How to Build & Run

1. Open `zedge-android` folder in Android Studio
2. Let Gradle sync complete
3. Connect an Android device or start an emulator
4. Click Run (▶) or use `./gradlew assembleDebug`

## Features

- **Native Bottom Navigation** - 5 tabs: Home, Upload, 24H, Schedule, Distribute
- **Splash Screen** - Animated app intro
- **WebView with JavaScript Bridge** - Full SPA functionality
- **File Upload Support** - ChromeClient handles file picker
- **Pull to Refresh** - Swipe down to reload
- **Back Button Navigation** - Smart back handling (closes modals first, then navigates tabs, then exits)
- **Offline Support** - Uses DOM storage for local data
- **Portrait Locked** - Consistent Android app behavior

## Minimum SDK

- Android 7.0 (API 24) and above

## Required Setup

Make sure your web files (`index.html`, `style.css`, `main.js`) in `app/src/main/assets/` have your Firebase and R2 credentials configured before building.

## Permissions

- INTERNET - Network access for Firebase & R2
- READ_MEDIA_IMAGES - File upload picker
- CAMERA - (optional, for future use)

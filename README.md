# Zedge Automation Publish — Kotlin Android App (v3.0)

ওয়েব ড্যাশবোর্ডের (index.html / main.js) হুবহু Kotlin + Jetpack Compose মোবাইল ভার্সন।

## ✅ ওয়েব ড্যাশবোর্ডের সাথে ১০০% সিঙ্ক

নিচের কনফিগারেশনগুলো ওয়েব ড্যাশবোর্ডের সাথে **একদম সেম** রাখা হয়েছে (`app/src/main/java/com/zedge/automation/config/AppConfig.kt`):

| কনফিগ | মান |
|---|---|
| Firebase প্রজেক্ট | `zedge1` (zedge-r2-edward-hermes), `zedge2` (zedge-r2-ryan-hermes) — apiKey, databaseURL, appId সহ সব ভ্যালু main.js-এর identical কপি |
| RTDB নোড | `wallpaperQueue`, `uploadState` |
| R2 Gateway Worker | `https://proud-paper-6fd7.monjurulgd2001.workers.dev/` (POST + `X-File-Name`/`X-File-Type`, DELETE + `X-File-Name`) |
| Gemini API | একই endpoint, key rotation, retry, একই ৪টি parallel prompt, একই ক্যাটাগরি লিস্ট |
| Stable Audio | একই `v1alpha/.../text-to-music` endpoint + ৩ সেকেন্ড পরপর poll |
| ডেটা স্কিমা | `name, type, size, isMp3, fileUrl, title, tags, category, description, duration, status, createdAt, distributedTo` — ফিল্ডনাম অপরিবর্তিত |

তাই মোবাইল থেকে আপলোড করা যেকোনো আইটেম **সাথে সাথে পিসির ওয়েব ভিউতে** দেখা যাবে (এবং উল্টোটাও), কারণ দুটোই একই Realtime Database ও একই R2 বাকেটে লেখে।

> Firebase SDK এখানে `FirebaseOptions.Builder` দিয়ে প্রোগ্রাম্যাটিক্যালি initialize হয় (App.kt), তাই **google-services.json লাগবে না**।

## 📱 ট্যাব (Bottom Navigation)

| ট্যাব | কাজ |
|---|---|
| 🏠 Home | ড্যাশবোর্ড স্ট্যাটস, সাম্প্রতিক আপলোড, uploadState |
| ☁️ Upload Queue | Wallpaper/Ringtone আপলোড + Gemini অটো-মেটাডাটা + এডিট/ডিলিট |
| 🪄 AI Studio | Stable Audio রিংটোন জেনারেট + Bulk Generation + Add to Queue |
| 📅 Schedule | ২৮-দিনের ক্যালেন্ডার (Asia/Dhaka টাইমজোন, ওয়েবের মতোই) |
| 🖼️ Distribute | ইমেজ round-robin ডিস্ট্রিবিউশন (zedge1 → zedge2 → zedge1 ...) |
| ⚙️ Settings | Gemini API Keys (অটো-রোটেট) + Model + Stable Audio Token |

Settings ওয়েবের localStorage কী-গুলোর মিরর: `activeProject`, `geminiApiKeys`, `geminiModel`, `stableAudioToken` (SharedPreferences-এ, শুধু এই ডিভাইসে)।

## 🛠️ বিল্ড করার নিয়ম

1. **Android Studio** (Koala বা নতুন) খুলে `ZedgeAutomationApp` ফোল্ডারটি **Open** করো
2. Gradle sync হতে দাও (ইন্টারনেট লাগবে — dependencies নামবে)
3. `Run ▶` চাপো (minSdk 24, অর্থাৎ Android 7.0+)
4. রিলিজ APK: `Build > Generate Signed Bundle / APK`

## ⚙️ Settings ট্যাব সেটআপ (ওয়েবের মতোই)

- **Gemini**: aistudio.google.com → Get API key → Settings ট্যাবে এক লাইনে একটা করে paste → Save
- **Stable Audio**: stableaudio.com → Bearer token → Settings ট্যাবে paste → Save
- Key না দিলে ফাইলের নাম থেকে fallback মেটাডাটা হবে (ওয়েবের মতোই)

## 📂 প্রোজেক্ট স্ট্রাকচার

```
app/src/main/java/com/zedge/automation/
├── App.kt                    # দুই Firebase প্রজেক্ট init (zedge1, zedge2)
├── MainActivity.kt           # Bottom navigation + অ্যাকাউন্ট সুইচার
├── config/AppConfig.kt       # ⭐ সব শেয়ার্ড কনফিগ (ওয়েবের সাথে সেম)
├── data/
│   ├── QueueItem.kt          # wallpaperQueue স্কিমা
│   ├── FirebaseRepo.kt       # RTDB লাইভ লিসেনার + রাইট
│   ├── R2Client.kt           # R2 gateway worker আপলোড/ডিলিট
│   ├── GeminiClient.kt       # অটো-মেটাডাটা (vision + text)
│   ├── StableAudioClient.kt  # মিউজিক জেনারেশন
│   └── SettingsStore.kt      # localStorage-সমতুল্য প্রেফারেন্স
├── viewmodel/MainViewModel.kt
└── ui/
    ├── theme/Theme.kt        # style.css-এর কালার প্যালেট
    └── screens/              # ৬টি ট্যাবের স্ক্রিন
```

## ⚠️ নোট

- ওয়েবের **Audio Editor** (waveform trim/volume) ও **Auto-Create Account browser extension** ফিচার দুটি ব্রাউজার-নির্ভর, তাই মোবাইলে বেসিক প্লেব্যাক রাখা হয়েছে; বাকি সব ফ্লো এক।
- ভবিষ্যতে ওয়েবে Firebase config বদলালে শুধু `AppConfig.kt`-এ একই ভ্যালু বসালেই হবে।

---

## 🔒 v3.1 — Metadata Fix

Bulk/AI ringtone metadata generation was rewritten for Zedge account safety:

- **One strict-JSON AI call** (instead of 4 separate free-text calls) — title, tags, category & description come together; ~4x fewer rate-limit (429) failures. Temperature lowered to 0.4 in JSON mode for reliable instruction-following.
- **Hard validation, no silent junk**: if a valid title/tags/category cannot be produced, the item is **NOT uploaded** — the bulk list shows `Failed: Metadata AI failed — NOT uploaded`. (Previously the raw prompt text silently became the title, category fell back to OTHER, and the item still showed "Done".)
- **Case/format-insensitive category matching** — answers like `Electronica`, `HIP HOP`, `hip_hop.` now map to the correct category instead of OTHER.
- **Title cleanup**: strips `Title:` prefixes, markdown (`**`), bpm mentions and the word "ringtone"; duplicate titles get a natural suffix (Vibes/Tone/Mix/...) instead of 3 random letters.
- **New “Generating metadata...” step** shown in the bulk status list (also counted as a running state so the Generate button stays locked).
- **Accurate duration**: after Auto Trim + Boost, the real post-trim duration is saved instead of the requested length.
- Mistral fallback now also uses strict JSON mode (`response_format: json_object`).

## 📅 v3.2 — Schedule Planner + Duration

- **Duration range is now 1–180 seconds** in AI Studio (was 5–180); label shows the valid range.
- **Schedule Calendar completely rebuilt** as a 1:1 port of the web dashboard's "Publishing Layout Planner":
  - Stats legend: Active DB · Remaining Today · Uploaded Today (x/3) · Audio Queue · Wallpapers
  - Days alternate AUDIO / WALLPAPER types with 3 upload slots per day
  - Today's day type & remaining slots read live from Firebase `uploadState` (lastUploadDate / uploadDayType / totalUploadsToday) — same `M/d/yyyy` Dhaka date key as the dashboard
  - Queued items (status = queued, oldest first) are allocated into future slots: audio → AUDIO days, wallpapers → WALLPAPER days
  - Planner extends past 28 days until every queued item has a slot; 28 days per page with Prev/Next pagination
  - "All Uploads Done! 🎉" state when today's 3 uploads are complete
  - Previous screen only showed a history of when items were created — it did not reflect the real upload schedule.

## v3.3 — Back Button Handling (2026-08-24)

- **Bulk-run exit guard**: pressing back on Home during an active bulk run now shows a confirm dialog ("Bulk generation running!") instead of instantly closing the app. "Exit anyway" stops the run cleanly via stopBulkGeneration() before exiting; "Keep running" dismisses.
- **Double-press to exit**: on Home, first back press shows "Press back again to exit" toast (2s window) — no more accidental exits.
- **Stable Audio login smart back**: system back and the toolbar arrow show a "Login in progress" toast while token extraction is busy; normal back otherwise.
- **Predictive back gesture**: android:enableOnBackInvokedCallback="true" added to the manifest (Android 13+ animations).
- Dialogs (queue edit, logout confirm) already close on back by default (Compose AlertDialog) — unchanged.

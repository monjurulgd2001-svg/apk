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

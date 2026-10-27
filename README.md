# CEBible (汉英圣经 / 漢英聖經)

An easy-to-use, feature-rich, **100% offline and ad-free** bilingual Chinese-English Bible application for Android.

[![Android Version](https://img.shields.io/badge/Android-5.0%2B%20%28API%2021%2B%29-brightgreen.svg)](https://developer.android.com)
[![Gradle Version](https://img.shields.io/badge/Gradle-9.6.0-blue.svg)](https://gradle.org)
[![AGP Version](https://img.shields.io/badge/AGP-9.4.1-blue.svg)](https://developer.android.com/studio/releases/gradle-plugin)

---

## 🌟 Key Features

* **Bilingual Display Modes**: Easily switch between Simplified Chinese (简体和合本), Traditional Chinese (繁體和合本), and English translations (WEB / KJV), or read them side-by-side in parallel columns.
* **Continuous Audio Reading**: Listen to Bible chapters continuously with real-time verse highlighting, automatic book and chapter speech announcements, and seamless auto-page advancement.
* **Floating Contextual Action Bar (CAB)**: Long-pressing any verse launches a floating toolbar directly beside the verse with **Favorite**, **Unfavorite**, **Share**, and **Copy** actions with soft verse text highlighting.
* **Multi-Occurrence Search Engine**: Advanced SQLite search algorithm supporting multi-keyword queries (e.g. `"love & love"` or `"神 & 神 & 神"`), returning verses containing 2, 3, or $N$ exact occurrences.
* **Automated In-App Update Checker**: Asynchronously checks Google Play Store for new Major/Minor releases (`UpdateCheckManager`) and prompts the user with a custom rounded modal dialog.
* **100% Offline & Ad-Free**: Read scriptures, listen to audio, search keywords, and manage bookmarks without an internet connection or advertisements.

---

## 🔄 Version Update Rules (`UpdateCheckManager`)

The app includes an in-app update checker (`UpdateCheckManager`) that compares the current installed version (`BuildConfig.VERSION_NAME`) against the latest published version on Google Play Store:

1. **Semantic Version Format**: App versions follow `Major.Minor.Patch` (e.g. `4.3.3`).
2. **Major / Minor Increase Required**: The update prompt dialog is triggered **only when the Major or Minor number increases** (e.g. `4.3.3` ➔ `4.4.0` or `5.0.0`).
3. **Patch Updates Ignored**: Patch-only increases (e.g. `4.3.3` ➔ `4.3.4`) are silently ignored to prevent unnecessary prompts for minor bugfix patches.
4. **Non-Blocking User Options**:
   * **Update**: Directs the user to the Google Play Store app listing page (`market://details?id=com.hz7225.cebible`).
   * **Remind Me Later**: Dismisses the dialog for the current session.

---

## 📖 Bible Translations Included

| Language | Translation | Asset Database File |
| :--- | :--- | :--- |
| **Simplified Chinese** | 简体和合本 (CUV-S) | `CB_cuvslite.bbl.db` |
| **Traditional Chinese** | 繁體和合本 (CUV-T) | `CB_cuvtlite.bbl.db` |
| **English** | World English Bible (WEB) | `EB_web_bbl.db` |
| **English** | King James Version (KJV) | `EB_kjv_bbl.db` |

---

## 🛠️ Tech Stack & Requirements

* **Language**: Java 17 / Kotlin
* **Build System**: Gradle 9.6.0 with Android Gradle Plugin (AGP) 9.4.1
* **Compile SDK**: API 37 (Android 17)
* **Target SDK**: API 37 (Android 17)
* **Min SDK**: API 21 (Android 5.0 Lollipop)
* **Database**: SQLite pre-populated databases (`app/src/main/assets/`)

---

## 🚀 Building & Running

1. Open the project in **Android Studio**.
2. Sync the project with Gradle files.
3. Build the debug APK:
   ```bash
   ./gradlew app:assembleDebug
   ```
4. Install on a connected Android device or emulator via `adb`:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 License & Attribution

* Built for scripture reading, study, and listening.
* Pre-populated Bible text databases sourced from public domain and open-access scripture repositories.

# Lambda — native Android app (Kotlin + Jetpack Compose)

A fully native rebuild of the Lambda Personal OS website: Dashboard, Planner, Work session,
Pomodoro, Academics (HSC syllabus), Habits, Daily log, Analytics, Notes, Goals, Settings and Zen focus mode.

* **Offline:** the manifest has no INTERNET permission at all.
* **Data stays on the phone:** one JSON file in the app's private storage (`filesDir/lambda_data.json`);
  note attachments are copied to `filesDir/attachments/`. Auto-backup to the cloud is switched off.
* **Min Android 8.0 (API 26)**, target/compile SDK 35.

## Build the APK

1. Install **Android Studio** (Koala 2024.1 or newer) — it brings JDK 17 and the Android SDK.
2. `File ▸ Open…` and choose this `Lambda` folder. Let Gradle sync finish (needs internet once, to download libraries).
3. Plug in a phone (USB debugging on) or start an emulator, then press **Run ▶**.
4. To get an installable file: `Build ▸ Build Bundle(s) / APK(s) ▸ Build APK(s)`.
   The file is `app/build/outputs/apk/debug/app-debug.apk` — copy it to your phone and install.
5. For a release build to share: `Build ▸ Generate Signed Bundle / APK…` and create a keystore (keep it safe).

Command line alternative: `./gradlew assembleDebug`.

## Moving your existing website data into the app

On the website: **Settings ▸ Export JSON**. Send the file to your phone, then in the app:
**Settings ▸ Import JSON**. (Attached files from the website are not part of that backup.)

## Project map

| Path | What it is |
| --- | --- |
| `data/Models.kt`, `Repository.kt`, `DefaultSyllabus.kt` | data classes, on-device JSON storage, starter HSC syllabus |
| `LambdaViewModel.kt` | all app logic (tasks, syllabus, habits, timers, notes, goals, backup) |
| `util/` | Dhaka (UTC+6) time helpers, progress / streak / deadline maths, chimes + ambient sound |
| `ui/` | theme, shared components, navigation drawer shell |
| `ui/screens/` | one file per screen |
| `res/mipmap-*`, `res/drawable/lambda_logo.png` | your logo as launcher icon and in-app logo |

## Build the APK online with GitHub (no Android Studio needed)

1. Create a new **private** repository on github.com.
2. Upload everything in this folder (including the hidden `.github` folder) to it, e.g. with GitHub Desktop or `git push`.
3. Open the repo's **Actions** tab ▸ **Build Lambda APK** ▸ wait for the green tick (about 5–10 minutes).
4. Open that run and download the **Lambda-debug-apk** artifact (a zip containing `app-debug.apk`), then install it on your phone.
   If the run fails, open it and copy the error text — it shows exactly what to fix.

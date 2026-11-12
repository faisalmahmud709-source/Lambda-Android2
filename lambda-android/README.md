# Lambda — Native Android application

**Brand:** Lambda — *Track the trace.*  
**Application ID:** `com.lambda.app`  
**Stack:** Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, Coroutines/Flow and Navigation Compose.

This is a native Android implementation. It does not embed the website and does not use WebView, Firebase, Supabase, remote databases, authentication services, analytics, advertising or tracking SDKs. The manifest has no `INTERNET` permission. All academic records are stored in the local Room database; preferences use DataStore.

## Build environment

The source was prepared in an environment with Java 21 but **without Android SDK, Gradle, an emulator, or a connected Android device**. Outbound DNS/network access was unavailable during this session, so Gradle and Android SDK components could not be installed. Consequently, no APK build, device installation, instrumentation test run, or offline-device test is claimed. The project is not certified production-ready until those checks pass.

To build locally:

1. Open the `lambda-android` folder in a current stable Android Studio.
2. Use JDK 17 for Gradle (Android Studio's bundled JBR is suitable).
3. Install Android SDK Platform 36 and matching Build Tools when prompted.
4. Allow Gradle to resolve the pinned plugins and dependencies.
5. Run `test` and `connectedAndroidTest` from the Gradle tool window.
6. Run `assembleDebug` to create `app/build/outputs/apk/debug/app-debug.apk`.
7. Install on a physical Android device and repeat the offline/restart/data-integrity acceptance checks below.

A Gradle wrapper is not included because the environment could not obtain the wrapper distribution. Android Studio can generate it, or a local Gradle installation can run the project.

## Feature migration

| Website area | Native implementation |
|---|---|
| Dashboard | Local task, study, syllabus, goal and upcoming-deadline summaries |
| Planner | Date-scoped tasks, inbox, priority, scheduled date, independent due date/time, edit, complete and delete |
| Work session | Stopwatch, subject and planner-task association, scratchpad, laps and saved session history |
| Pomodoro / Zen Focus | Configurable focus/short/long cycles, 25/50/custom presets, task linking, stopwatch, fullscreen, Space/Escape keyboard shortcuts and locally synthesized ambient noise |
| Academics | Subject/paper/chapter hierarchy, configurable study-stamp columns, lecture counts, revision level, check maps, Drive/slide links and progress calculations |
| Habits | Daily check-ins, color, history, 30-day consistency, current and longest streaks |
| Daily log | Sleep, screen time, extra study, mood and reflection |
| Analytics | Date ranges, study hours by day, goal days, task completions by day, sleep/screen trends, subject totals and habit completion trend |
| Notes | Search, tags, pinning, Markdown copy/export, SAF attachments, open/remove and attachment-inclusive backup |
| Goals | Categories, deadlines, milestones and automatic completion when all milestones are checked |
| Settings | Name, theme, study goal, focus durations, long-break interval, sound preference and JSON backup/restore |

## Data and backup

- Room is the source of truth for academic records.
- Room schema version is `2`. Migration `1 -> 2` adds `notes.attachmentsJson`; no destructive migration fallback is configured.
- Foreign-key cascades remove dependent groups/chapters, habit logs and goal milestones when their parent is deleted.
- DataStore stores lightweight preferences (theme, display name, timer durations, long-break interval, daily goal and sound preference).
- JSON backups use `lambda-backup-v1`. Restore validates the format/record ceiling and merges by record ID in a Room transaction; records absent from the backup are not deleted.
- Notes attachments are included as data URLs when exporting a backup, so a restore does not depend on the original document URI.
- The Settings restore action also accepts the original website's `ureka_personal_os_data_v4` localStorage JSON export. It imports nested subjects/groups/chapters, tasks, habits and logs, sessions, daily logs, notes/attachments, goals/milestones and preferences. Website account/PWA controls are intentionally excluded to preserve the offline-only requirement.
- The app has no automatic cloud backup and requests no internet permission.

## Source layout

- `app/src/main/java/com/lambda/app/MainActivity.kt` — splash, Compose shell, navigation and screen UI.
- `LambdaViewModel.kt` — screen state and operations.
- `data/Entities.kt`, `LambdaDao.kt`, `LambdaDatabase.kt` — Room entities, queries and migrations.
- `data/LambdaRepository.kt` — Flow aggregation, seed data, JSON import/export and legacy website import.
- `data/Preferences.kt` — DataStore preferences.
- `data/AmbientAudio.kt` — offline ambient audio generation.
- `data/DefaultSyllabus.kt` — starter syllabus.
- `domain/AcademicProgress.kt` — website-matched academic and habit calculations.
- `ui/theme/Theme.kt` — Lambda Material 3 theme.
- `app/src/test` — domain unit tests.
- `app/src/androidTest` — Room CRUD/cascade and Compose smoke tests (authored, not executed in this environment).

## Verification status and remaining acceptance work

Source-level checks completed in this environment: Kotlin parser reported no syntax errors; Android XML resources parsed successfully; manifest inspection confirms no `INTERNET` permission; project source archive integrity checked.

Not yet verified: Gradle dependency resolution, KSP/Room code generation, compilation, JVM/Android test execution, Android 1 -> 2 migration against an actual v1 database, APK install/launch, audio behavior on hardware, attachment provider behavior across apps, background/process-death timer semantics, force-stop/reboot persistence, and offline-device operation. These are release blockers, not assumed successes.

Before production release, build with Android SDK 36/JDK 17, fix any compiler or test failures, execute the full unit/instrumentation suite, test v1 database migration, install on supported physical devices, and verify the offline/restart/backup acceptance path. Do not distribute an APK as production-ready until those checks pass.

## Build without installing Android Studio (GitHub Actions)

A GitHub Actions workflow is included at `.github/workflows/android.yml`. It provisions JDK 17, Gradle 8.13 and Android SDK 36 on a hosted runner, builds the debug APK, runs JVM unit tests, and then runs instrumentation tests on an Android emulator. It uploads the debug APK and test reports as workflow artifacts when available.

To use it without Android Studio (beginner steps):

1. Create a GitHub account at https://github.com/signup.
2. While signed in, open https://github.com/new. Repository name: `Lambda-Android`. Choose **Private**, leave the other options as they are, and press **Create repository**.
3. Download `lambda-android-source.zip` and extract/unzip it on your computer. Inside it, open the folder named `lambda-android`.
4. In your new empty GitHub repository, click **Add file → Upload files**.
5. Open the extracted `lambda-android` folder on your computer. Select and upload everything inside it, including the `app` folder, the Gradle `.kts` files, README, and the hidden `.github` folder. The `.github/workflows/android.yml` file is essential: it tells GitHub how to build the APK. If your computer hides `.github`, enable viewing hidden files or create the workflow file on GitHub at `.github/workflows/android.yml` and paste in the copy from this ZIP.
6. Scroll down to **Commit changes**. Keep the default message and click **Commit changes**.
7. In the repository, click the **Actions** tab. Choose **Lambda Android CI** on the left. If it has not started automatically, click **Run workflow → Run workflow**.
8. Wait for the run to finish. Open it and check both jobs. Green means the configured build and tests passed; red means there is an error to fix.
9. If successful, return to that run's summary page. Scroll to **Artifacts**, click **Lambda-debug-apk**, and download it. Unzip the downloaded artifact; the file inside is `app-debug.apk`.
10. Send `app-debug.apk` to your Android phone, open it, and follow Android's install prompts. If Android blocks it, allow your browser or file manager to install unknown apps for this one installation.

Important: upload the *contents* of the `lambda-android` folder, not the outer folder itself. The workflow expects `app/` and `build.gradle.kts` directly in the repository root. This workflow builds a debug APK, not a signed Play Store release. A failed workflow does not produce a verified APK. If it turns red, share the failed step's error log; do not share passwords or signing keys.

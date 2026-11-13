# Lambda — Native Android application

**Brand:** Lambda — *Track the trace.*  
**Application ID:** `com.lambda.app`  
**Stack:** Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, Coroutines/Flow and Navigation Compose.

This is a native Android implementation. It does not embed the website and does not use WebView, Firebase, Supabase, remote databases, authentication services, analytics, advertising or tracking SDKs. The manifest has no `INTERNET` permission. All academic records are stored in the local Room database; preferences use DataStore.

## Build (easiest: GitHub Actions)

1. Create a **private** repository on GitHub.
2. Upload the *contents* of this `lambda-android` folder (including the hidden `.github` folder and `gradlew`) to the repository root. Using Git is most reliable: `git init && git add -A && git commit -m init && git branch -M main && git remote add origin <url> && git push -u origin main`.
3. Open the **Actions** tab -> **Build Lambda APK**. It runs automatically on push (or press **Run workflow**).
4. When it turns green, open the run, scroll to **Artifacts**, download **Lambda-debug-apk**, unzip it and install `app-debug.apk` on your phone (allow "install unknown apps" once).

Local build: Android Studio (JDK 17) -> open this folder -> Run, or `./gradlew assembleDebug`.

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

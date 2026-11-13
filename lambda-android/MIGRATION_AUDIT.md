# Lambda website source audit and native migration map

## Source inspected

`app-creator-hub.zip` (78 archive entries). Main sources: `src/App.tsx`, `src/types.ts`, `src/store/useStore.ts`, all route components under `src/pages`, `src/data/defaultSyllabus.ts`, `src/data/initialData.ts`, reusable UI components, focus modal, hooks and utilities.

## Website routes

`/`, `/planner`, `/work`, `/pomodoro`, `/academics`, `/habits`, `/tracking`, `/analytics`, `/notes`, `/goals`, `/settings`.

## Original data model and behaviors

- Tasks: title, P1-P4 priority, scheduled date/inbox, independent due date/time, completion timestamp and creation timestamp.
- Academics: subjects, ordered groups, configurable study-stamp columns, chapters, lecture counts, revision level, checks and optional slide/Drive links.
- Habits: name, icon, frequency, target days, color and date-keyed completion map.
- Sessions: work/Pomodoro kind, title/notes, optional subject, start/end, minutes, date and laps.
- Daily log: sleep, screen time, extra study, mood and notes.
- Notes: title/content/tags/pinned state, attachments and timestamps.
- Goals: category, deadline, completion timestamp and milestones.
- Preferences: display name, focus/break lengths, long-break interval, daily goal and sound preference.
- Runtime focus: Pomodoro phase and stopwatch elapsed/laps.

Source-specific calculation rules retained: chapter progress is the mean of stamp completion, lecture completion (only when lecture total is positive, capped at 100%), and revision level divided by three; group progress averages chapters; subject progress averages chapters; syllabus progress averages subjects and rounds to a percentage. Habit current streak starts today if checked, otherwise yesterday; longest streak scans consecutive dates. Analytics includes timed sessions plus daily-log extra study, goal days, completion-date task counts, sleep/screen averages, subject grouping and habit completion trend. Deadline calculations use task due date or scheduled date and goal deadline, with end-of-day handling and academic-keyword classification. Local date operations use `Asia/Dhaka`.

## Native implementation notes

Native destinations: Home, Planner, Academics, Focus, Habits, Daily log, Analytics, Notes, Goals and Settings. A More screen links secondary destinations. Native UI replaces desktop/web layouts. Room is authoritative; DataStore holds preferences. Starter syllabus data is seeded locally. JSON restore is transactional and merge/upsert-based. Original website `ureka_personal_os_data_v4` JSON is accepted for legacy import. Attachments are persisted as SAF document references and embedded in exported backups.

## Explicit exclusions

Website account/Google login, PWA install controls, browser service worker, auto-cloud-backup and remote services are intentionally excluded by the offline-only Android requirements.

## Release blockers / verification still required

The Android SDK/Gradle toolchain was unavailable in the implementation environment. Compilation, KSP code generation, Gradle tests, instrumentation tests, actual v1->v2 migration, APK installation, hardware audio, SAF/FileProvider interoperability, process-death timer behavior, reboot persistence and real-device offline testing remain unverified. Treat these as release blockers; do not describe the project as production-ready until they pass.

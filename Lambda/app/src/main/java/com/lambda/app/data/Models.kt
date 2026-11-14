package com.lambda.app.data

import kotlinx.serialization.Serializable

// NOTE: every field has a default so that backups exported from the web version of Lambda
// (and older/partial files) can be imported without errors.

@Serializable
data class Task(
    val id: String = "",
    val title: String = "",
    val priority: String = "p3", // p1 | p2 | p3 | p4
    val date: String? = null,    // yyyy-MM-dd, null = Inbox
    val dueDate: String? = null,
    val dueTime: String? = null, // HH:mm (24h)
    val done: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String = ""
)

@Serializable
data class Chapter(
    val id: String = "",
    val name: String = "",
    val lecturesDone: Int = 0,
    val lecturesTotal: Int = 0,
    val revision: Int = 0,
    val checks: Map<String, Boolean> = emptyMap(),
    val slideLinks: List<String> = emptyList()
)

@Serializable
data class SubjectGroup(
    val id: String = "",
    val name: String = "",
    val columns: List<String> = emptyList(),
    val chapters: List<Chapter> = emptyList()
)

@Serializable
data class Subject(
    val id: String = "",
    val name: String = "",
    val icon: String = "📘",
    val groups: List<SubjectGroup> = emptyList()
)

@Serializable
data class Habit(
    val id: String = "",
    val name: String = "",
    val icon: String = "⚡",
    val frequency: String = "daily",
    val color: String = "#f0bc68",
    val log: Map<String, Boolean> = emptyMap()
)

@Serializable
data class Lap(val time: Long = 0, val label: String = "")

@Serializable
data class Session(
    val id: String = "",
    val kind: String = "work", // work | pomodoro
    val title: String = "",
    val notes: String? = null,
    val subjectId: String? = null,
    val startedAt: String = "",
    val endedAt: String = "",
    val minutes: Int = 0,
    val date: String = "",
    val laps: List<Lap> = emptyList()
)

@Serializable
data class DailyLogEntry(
    val date: String = "",
    val sleepHours: Double? = null,
    val screenHours: Double? = null,
    val extraStudyHours: Double? = null,
    val mood: Int? = null,
    val notes: String? = null
)

@Serializable
data class NoteAttachment(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val size: Long = 0,
    val path: String? = null, // file name inside filesDir/attachments
    val addedAt: String = ""
)

@Serializable
data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val attachments: List<NoteAttachment> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class Milestone(val id: String = "", val title: String = "", val done: Boolean = false)

@Serializable
data class Goal(
    val id: String = "",
    val title: String = "",
    val category: String = "Academic",
    val deadline: String? = null,
    val completedAt: String? = null,
    val createdAt: String = "",
    val milestones: List<Milestone> = emptyList()
)

@Serializable
data class Settings(
    val name: String = "",
    val appName: String = "Lambda",
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val dailyStudyGoalHours: Int = 6,
    val soundEnabled: Boolean = true
)

@Serializable
data class ActiveWork(
    val title: String = "",
    val subjectId: String? = null,
    val notes: String = "",
    val runningSince: Long? = null,
    val accumulatedMs: Long = 0,
    val firstStartedAt: Long = 0,
    val laps: List<Lap> = emptyList()
)

/** Everything that is persisted on the phone. */
@Serializable
data class AppData(
    val onboarded: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val subjects: List<Subject> = createDefaultSubjects(),
    val habits: List<Habit> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val dailyLogs: Map<String, DailyLogEntry> = emptyMap(),
    val notes: List<Note> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val settings: Settings = Settings(),
    val activeWork: ActiveWork? = null
)

/** Same shape as the web app's "Export JSON" backup, all fields optional. */
@Serializable
data class Backup(
    val version: Int = 2,
    val exportDate: String? = null,
    val settings: Settings? = null,
    val tasks: List<Task>? = null,
    val subjects: List<Subject>? = null,
    val sessions: List<Session>? = null,
    val habits: List<Habit>? = null,
    val dailyLogs: Map<String, DailyLogEntry>? = null,
    val notes: List<Note>? = null,
    val goals: List<Goal>? = null
)

/** In-memory only (like the web app). */
data class PomodoroState(
    val phase: String = "focus", // focus | short | long
    val running: Boolean = false,
    val endsAt: Long? = null,
    val remainingMs: Long = 25 * 60_000L,
    val completedFocus: Int = 0,
    val label: String = "",
    val subjectId: String? = null
)

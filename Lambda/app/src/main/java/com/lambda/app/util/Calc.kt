package com.lambda.app.util

import com.lambda.app.data.Chapter
import com.lambda.app.data.Goal
import com.lambda.app.data.Habit
import com.lambda.app.data.Subject
import com.lambda.app.data.SubjectGroup
import com.lambda.app.data.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ---- Syllabus progress -------------------------------------------------------------------

fun chapterProgress(c: Chapter, columns: List<String>): Float {
    val parts = mutableListOf<Float>()
    if (columns.isNotEmpty()) {
        parts += columns.count { c.checks[it] == true }.toFloat() / columns.size
    }
    if (c.lecturesTotal > 0) parts += min(1f, c.lecturesDone.toFloat() / c.lecturesTotal)
    parts += min(1f, c.revision / 3f)
    return parts.sum() / parts.size
}

fun groupProgress(g: SubjectGroup): Float =
    if (g.chapters.isEmpty()) 0f else g.chapters.map { chapterProgress(it, g.columns) }.average().toFloat()

fun subjectProgress(s: Subject): Float {
    var total = 0
    var sum = 0f
    s.groups.forEach { g -> g.chapters.forEach { c -> total++; sum += chapterProgress(c, g.columns) } }
    return if (total > 0) sum / total else 0f
}

fun syllabusProgressPct(subjects: List<Subject>): Int {
    if (subjects.isEmpty()) return 0
    return (subjects.map { subjectProgress(it) }.average() * 100).roundToInt()
}

// ---- Habits ------------------------------------------------------------------------------

data class Streak(val current: Int, val longest: Int)

fun habitStreak(h: Habit): Streak {
    var current = 0
    var d = LocalDate.now()
    if (h.log[d.toString()] != true) d = d.minusDays(1)
    while (h.log[d.toString()] == true) {
        current++
        d = d.minusDays(1)
    }
    val dates = h.log.filter { it.value }.keys.sorted().mapNotNull { parseDate(it) }
    var longest = 0
    var streak = 0
    var prev: LocalDate? = null
    for (cur in dates) {
        streak = if (prev == null) 1 else {
            val diff = ChronoUnit.DAYS.between(prev, cur)
            if (diff == 1L) streak + 1 else if (diff > 1L) 1 else streak
        }
        prev = cur
        if (streak > longest) longest = streak
    }
    return Streak(current, max(longest, current))
}

// ---- Deadlines (tasks + goals) -----------------------------------------------------------

enum class DeadlineLevel { Critical, Warning, Normal }

data class Deadline(
    val id: String,
    val title: String,
    val date: String,
    val kind: String, // Task | Goal
    val isAcademic: Boolean,
    val diffMs: Long,
    val hoursLeft: Long,
    val within48h: Boolean,
    val overdue: Boolean,
    val level: DeadlineLevel,
    val label: String
)

private val ACADEMIC_KEYWORDS = listOf(
    "physics", "chem", "math", "bio", "ict", "bangla", "english", "exam", "quiz", "test",
    "assignment", "homework", "lecture", "revision", "buet", "medical", "engineering", "hsc",
    "syllabus", "slide", "chapter", "paper", "solve", "cq", "mcq", "study", "class"
)

private fun isAcademic(title: String, category: String? = null): Boolean {
    if (category != null) {
        val c = category.lowercase()
        if (c.contains("acad") || c.contains("exam") || c.contains("study")) return true
    }
    val l = title.lowercase()
    return ACADEMIC_KEYWORDS.any { l.contains(it) }
}

private fun endOfDayMs(dateStr: String): Long? = runCatching {
    LocalDate.parse(dateStr).atTime(LocalTime.of(23, 59, 59)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrNull()

private fun buildDeadline(id: String, title: String, date: String, kind: String, category: String?, now: Long, threshold: Int): Deadline? {
    val end = endOfDayMs(date) ?: return null
    val diff = end - now
    val hours = (diff / 3_600_000.0).roundToInt().toLong()
    val overdue = diff < 0
    val within = overdue || hours <= threshold
    val level: DeadlineLevel
    val label: String
    when {
        overdue -> {
            level = DeadlineLevel.Critical
            label = "${max(1, abs((diff / 86_400_000.0).roundToInt()))}d overdue"
        }
        hours <= 24 -> {
            level = DeadlineLevel.Critical
            label = if (hours == 0L) "Due now" else "Due in ${hours}h"
        }
        hours <= 48 -> {
            level = DeadlineLevel.Warning
            label = "Due tomorrow (${hours}h)"
        }
        else -> {
            level = DeadlineLevel.Normal
            label = "In ${ceil(diff / 86_400_000.0).toInt()} days"
        }
    }
    return Deadline(id, title, date, kind, isAcademic(title, category), diff, hours, within, overdue, level, label)
}

fun computeDeadlines(tasks: List<Task>, goals: List<Goal>, now: Long, thresholdHours: Int = 48): List<Deadline> {
    val items = mutableListOf<Deadline>()
    tasks.filter { !it.done && (it.dueDate != null || it.date != null) }.forEach { t ->
        buildDeadline(t.id, t.title, (t.dueDate ?: t.date)!!, "Task", null, now, thresholdHours)?.let { items += it }
    }
    goals.filter { it.completedAt == null && it.deadline != null }.forEach { g ->
        buildDeadline(g.id, g.title, g.deadline!!, "Goal", g.category, now, thresholdHours)?.let { items += it }
    }
    return items.sortedBy { it.diffMs }
}

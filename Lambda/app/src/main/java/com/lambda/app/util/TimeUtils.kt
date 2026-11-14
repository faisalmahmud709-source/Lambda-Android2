package com.lambda.app.util

import com.lambda.app.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val DhakaZone: ZoneId = ZoneId.of("Asia/Dhaka")

fun todayStr(): String = LocalDate.now().toString()

fun dateStr(d: LocalDate): String = d.toString()

fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()

fun shiftDateStr(s: String, days: Long): String =
    (parseDate(s) ?: LocalDate.now()).plusDays(days).toString()

fun offsetDateStr(days: Long): String = LocalDate.now().plusDays(days).toString()

fun prettyDate(s: String, pattern: String = "EEE, d MMM yyyy"): String {
    val d = parseDate(s) ?: return s
    return d.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
}

fun longTodayHeader(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))

fun greeting(hour: Int = java.time.LocalTime.now().hour): String = when {
    hour < 5 -> "Burning the midnight oil"
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    else -> "Good evening"
}

fun formatMinutes(mins: Double): String {
    if (mins < 60) return "${mins.roundToInt()}m"
    val h = (mins / 60).toInt()
    val r = (mins % 60).roundToInt()
    return if (r > 0) "${h}h ${r}m" else "${h}h"
}

fun formatMinutes(mins: Int): String = formatMinutes(mins.toDouble())

fun hhmmss(totalMs: Long): String {
    val secs = totalMs / 1000
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun countdown(ms: Long): String {
    val total = kotlin.math.ceil(ms / 1000.0).toLong().coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}

// ---- Dhaka clock -------------------------------------------------------------------------

fun dhakaNow(nowMs: Long = System.currentTimeMillis()): ZonedDateTime =
    Instant.ofEpochMilli(nowMs).atZone(DhakaZone)

fun dhakaTimeStr(nowMs: Long): String =
    dhakaNow(nowMs).format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH))

fun dhakaDateStr(nowMs: Long): String =
    dhakaNow(nowMs).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH))

const val DHAKA_TZ_LABEL = "UTC+6 · Dhaka"

// ---- 12h / 24h helpers -------------------------------------------------------------------

data class TimeParts(val hour: Int, val minute: Int, val pm: Boolean)

fun formatTime24to12(time24: String?): String {
    if (time24.isNullOrBlank()) return ""
    val parts = time24.split(":")
    if (parts.size < 2) return time24
    var h = parts[0].toIntOrNull() ?: return time24
    val m = parts[1].padStart(2, '0')
    val ampm = if (h >= 12) "PM" else "AM"
    h %= 12
    if (h == 0) h = 12
    return "$h:$m $ampm"
}

fun formatTime12to24(hour: Int, minute: Int, pm: Boolean): String {
    var h = hour % 12
    if (pm) h += 12
    return "%02d:%02d".format(h, minute)
}

fun parseTime24(time24: String?): TimeParts {
    if (time24.isNullOrBlank()) return TimeParts(10, 0, true)
    val parts = time24.split(":")
    var h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val pm = h >= 12
    h %= 12
    if (h == 0) h = 12
    return TimeParts(h, m, pm)
}

// ---- Task countdown ----------------------------------------------------------------------

enum class Urgency { Normal, Urgent, Overdue, Done }

data class TaskRemaining(
    val label: String,
    val timeFormatted: String,
    val urgency: Urgency,
    val minutesLeft: Long
)

fun calculateTaskRemaining(task: Task, nowMs: Long = System.currentTimeMillis()): TaskRemaining? {
    if (task.done) {
        return TaskRemaining("Done", formatTime24to12(task.dueTime), Urgency.Done, 0)
    }
    val dueTime = task.dueTime ?: return null
    val dateStr = task.date ?: task.dueDate ?: return null
    val target = runCatching {
        LocalDate.parse(dateStr).atTime(LocalTime.parse(dueTime)).atZone(DhakaZone).toInstant().toEpochMilli()
    }.getOrNull() ?: return null

    val minutesLeft = ((target - nowMs) / 60000.0).roundToInt().toLong()
    val formatted = formatTime24to12(dueTime)

    if (minutesLeft < 0) {
        val over = abs(minutesLeft)
        val label = when {
            over < 60 -> "${over}m overdue"
            over < 1440 -> {
                val h = over / 60; val m = over % 60
                if (m > 0) "${h}h ${m}m overdue" else "${h}h overdue"
            }
            else -> "${over / 1440}d overdue"
        }
        return TaskRemaining(label, formatted, Urgency.Overdue, minutesLeft)
    }
    if (minutesLeft <= 60) {
        val label = if (minutesLeft == 0L) "Due now" else "Due in ${minutesLeft}m"
        return TaskRemaining(label, formatted, Urgency.Urgent, minutesLeft)
    }
    if (minutesLeft <= 1440) {
        val h = minutesLeft / 60; val m = minutesLeft % 60
        val label = if (m > 0) "${h}h ${m}m left" else "${h}h left"
        return TaskRemaining(label, formatted, if (minutesLeft <= 120) Urgency.Urgent else Urgency.Normal, minutesLeft)
    }
    return TaskRemaining("In ${minutesLeft / 1440}d", formatted, Urgency.Normal, minutesLeft)
}

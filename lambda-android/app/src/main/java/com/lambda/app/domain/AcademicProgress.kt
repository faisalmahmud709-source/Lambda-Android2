package com.lambda.app.domain

import com.lambda.app.data.ChapterEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Mirrors the original website's chapter progress weighting: stamps, lectures and revisions are averaged equally. */
fun chapterProgress(chapter: ChapterEntity, columns: List<String>): Double {
    val parts = mutableListOf<Double>()
    if (columns.isNotEmpty()) {
        parts += columns.count { col -> Regex("\"${Regex.escape(col)}\"\\s*:\\s*true").containsMatchIn(chapter.checksJson) }.toDouble() / columns.size
    }
    if (chapter.lecturesTotal > 0) parts += (chapter.lecturesDone.toDouble() / chapter.lecturesTotal).coerceAtMost(1.0)
    parts += (chapter.revision.toDouble() / 3.0).coerceAtMost(1.0)
    return parts.average()
}
fun groupProgress(chapters: List<ChapterEntity>, columns: List<String>): Double = if (chapters.isEmpty()) 0.0 else chapters.map { chapterProgress(it, columns) }.average()
fun syllabusProgress(subjects: List<List<Pair<ChapterEntity, List<String>>>>): Int {
    if (subjects.isEmpty()) return 0
    val perSubject = subjects.map { subject ->
        val chapters = subject
        if (chapters.isEmpty()) 0.0 else chapters.map { chapterProgress(it.first, it.second) }.average()
    }
    return (perSubject.average() * 100).toInt()
}
fun habitStreak(completedDates: Set<LocalDate>, today: LocalDate): Pair<Int, Int> {
    val currentStart = if (today in completedDates) today else today.minusDays(1)
    var current = 0; var d = currentStart
    while (d in completedDates) { current++; d=d.minusDays(1) }
    var longest=0; var run=0; var previous:LocalDate?=null
    completedDates.sorted().forEach { date -> run=if(previous==null || ChronoUnit.DAYS.between(previous,date)==1L) run+1 else 1; longest=maxOf(longest,run); previous=date }
    return current to maxOf(longest,current)
}

package com.lambda.app.domain

import com.lambda.app.data.ChapterEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AcademicProgressTest {
    @Test fun chapterProgressAveragesStampsLecturesAndRevision() {
        val c=ChapterEntity("c","g","Vectors",5,10,3,"{\"Class\":true}")
        assertEquals((0.5+0.5+1.0)/3.0,chapterProgress(c,listOf("Class","Short Notes")),0.0001)
    }
    @Test fun emptyChapterStillHasRevisionComponent() {
        assertEquals(0.0,chapterProgress(ChapterEntity("c","g","x"),emptyList()),0.0)
    }
    @Test fun habitStreakAllowsYesterdayAsActiveStart() {
        val today=LocalDate.parse("2026-11-12")
        val streak=habitStreak(setOf(today.minusDays(1),today.minusDays(2),today.minusDays(4)),today)
        assertEquals(2,streak.first); assertEquals(2,streak.second)
    }
}

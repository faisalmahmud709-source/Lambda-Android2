package com.lambda.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LambdaDaoTest {
    private lateinit var db: LambdaDatabase
    private lateinit var dao: LambdaDao

    @Before fun createDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), LambdaDatabase::class.java).allowMainThreadQueries().build()
        dao = db.dao()
    }
    @After fun closeDb() = db.close()

    @Test fun taskInsertToggleAndDeletePersistInRoom() = runBlocking {
        val task = TaskEntity("task-1", "Revise calculus")
        dao.putTask(task)
        assertEquals("Revise calculus", dao.tasks().firstValue().single().title)
        dao.toggleTask(task.id, 1234L)
        assertTrue(dao.tasks().firstValue().single().done)
        dao.deleteTask(task.id)
        assertTrue(dao.tasks().firstValue().isEmpty())
    }

    @Test fun deletingSubjectCascadesToGroupsAndChapters() = runBlocking {
        dao.putSubject(SubjectEntity("s", "Physics"))
        dao.putGroup(GroupEntity("g", "s", "Paper 1"))
        dao.putChapter(ChapterEntity("c", "g", "Vectors"))
        dao.deleteSubject("s")
        assertTrue(dao.groups().firstValue().isEmpty())
        assertTrue(dao.chapters().firstValue().isEmpty())
    }

    @Test fun milestoneToggleUpdatesStoredValueAndCompletesGoal() = runBlocking {
        dao.putGoal(GoalEntity("goal", "Finish syllabus"))
        dao.putMilestone(MilestoneEntity("m", "goal", "Complete vectors"))
        dao.toggleMilestoneAndRefreshGoal("m")
        assertTrue(dao.milestones().firstValue().single().done)
        assertTrue(dao.goals().firstValue().single().completedAt != null)
        dao.toggleMilestoneAndRefreshGoal("m")
        assertFalse(dao.milestones().firstValue().single().done)
        assertEquals(null,dao.goals().firstValue().single().completedAt)
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T = first()
}

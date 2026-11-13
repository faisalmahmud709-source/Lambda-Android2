package com.lambda.app.data

import androidx.room.*

@Entity(tableName = "tasks", indices = [Index("date"), Index("done")])
data class TaskEntity(@PrimaryKey val id: String, val title: String, val priority: Int = 2, val date: String? = null, val dueDate: String? = null, val dueTime: String? = null, val done: Boolean = false, val completedAt: Long? = null, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "subjects")
data class SubjectEntity(@PrimaryKey val id: String, val name: String, val icon: String = "📘", val sortOrder: Int = 0)
@Entity(tableName = "subject_groups", foreignKeys = [ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.CASCADE)], indices = [Index("subjectId")])
data class GroupEntity(@PrimaryKey val id: String, val subjectId: String, val name: String, val columnsCsv: String = "Class,Short Notes,Engineering,Test Paper,Question Bank", val sortOrder: Int = 0)
@Entity(tableName = "chapters", foreignKeys = [ForeignKey(entity = GroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE)], indices = [Index("groupId")])
data class ChapterEntity(@PrimaryKey val id: String, val groupId: String, val name: String, val lecturesDone: Int = 0, val lecturesTotal: Int = 0, val revision: Int = 0, val checksJson: String = "{}", val slideLinksJson: String = "[]")
@Entity(tableName = "habits")
data class HabitEntity(@PrimaryKey val id: String, val name: String, val icon: String = "⚡", val frequency: String = "daily", val targetDays: Int? = null, val color: String = "#F0BC68")
@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "date"], foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)], indices = [Index("date")])
data class HabitLogEntity(val habitId: String, val date: String, val completed: Boolean)
@Entity(tableName = "sessions", indices = [Index("date"), Index("subjectId")])
data class SessionEntity(@PrimaryKey val id: String, val kind: String, val title: String, val notes: String = "", val subjectId: String? = null, val startedAt: Long, val endedAt: Long, val minutes: Int, val date: String, val lapsJson: String = "[]")
@Entity(tableName = "daily_logs")
data class DailyLogEntity(@PrimaryKey val date: String, val sleepHours: Double? = null, val screenHours: Double? = null, val extraStudyHours: Double = 0.0, val mood: Int? = null, val notes: String = "")
@Entity(tableName = "notes", indices = [Index("updatedAt")])
data class NoteEntity(@PrimaryKey val id: String, val title: String, val content: String = "", val tagsCsv: String = "", val pinned: Boolean = false, @ColumnInfo(defaultValue = "'[]'") val attachmentsJson: String = "[]", val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis())
@Entity(tableName = "goals")
data class GoalEntity(@PrimaryKey val id: String, val title: String, val category: String = "Academic", val deadline: String? = null, val createdAt: Long = System.currentTimeMillis(), val completedAt: Long? = null)
@Entity(tableName = "milestones", foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)], indices = [Index("goalId")])
data class MilestoneEntity(@PrimaryKey val id: String, val goalId: String, val title: String, val done: Boolean = false)

@Entity(tableName = "app_meta")
data class AppMetaEntity(@PrimaryKey val key: String, val value: String)

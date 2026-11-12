package com.lambda.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LambdaDao {
    @Query("SELECT * FROM tasks ORDER BY done, date IS NULL, date, priority, createdAt DESC") fun tasks(): Flow<List<TaskEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTask(item: TaskEntity)
    @Query("UPDATE tasks SET title=:title, priority=:priority, date=:date, dueDate=:dueDate, dueTime=:dueTime WHERE id=:id") suspend fun updateTaskFields(id:String,title:String,priority:Int,date:String?,dueDate:String?,dueTime:String?)
    @Query("UPDATE tasks SET done = NOT done, completedAt = CASE WHEN done = 0 THEN :now ELSE NULL END WHERE id = :id") suspend fun toggleTask(id: String, now: Long)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: String)

    @Query("SELECT value FROM app_meta WHERE `key` = 'syllabus_seeded' LIMIT 1") suspend fun syllabusSeeded(): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeta(item: AppMetaEntity)
    @Query("SELECT EXISTS(SELECT 1 FROM habit_logs WHERE habitId=:habitId AND date=:date)") suspend fun habitLogExists(habitId: String, date: String): Boolean
    @Query("SELECT * FROM subjects ORDER BY sortOrder, name") fun subjects(): Flow<List<SubjectEntity>>
    @Query("SELECT * FROM subject_groups ORDER BY sortOrder, name") fun groups(): Flow<List<GroupEntity>>
    @Query("SELECT * FROM chapters ORDER BY rowid") fun chapters(): Flow<List<ChapterEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSubject(item: SubjectEntity)
    @Query("UPDATE subjects SET name=:name WHERE id=:id") suspend fun renameSubject(id:String,name:String)
    @Query("UPDATE chapters SET name=:name WHERE id=:id") suspend fun renameChapter(id:String,name:String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGroup(item: GroupEntity)
    @Query("UPDATE subject_groups SET columnsCsv=:columns WHERE id=:id") suspend fun updateGroupColumns(id:String,columns:String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putChapter(item: ChapterEntity)
    @Query("UPDATE chapters SET lecturesDone=:done, lecturesTotal=:total, revision=:revision WHERE id=:id") suspend fun updateChapterProgress(id: String, done: Int, total: Int, revision: Int)
    @Query("UPDATE chapters SET checksJson=:checks WHERE id=:id") suspend fun updateChapterChecks(id: String, checks: String)
    @Query("UPDATE chapters SET slideLinksJson=:links WHERE id=:id") suspend fun updateChapterLinks(id:String,links:String)
    @Query("DELETE FROM subjects WHERE id=:id") suspend fun deleteSubject(id: String)
    @Query("DELETE FROM subject_groups WHERE id=:id") suspend fun deleteGroup(id: String)
    @Query("DELETE FROM chapters WHERE id=:id") suspend fun deleteChapter(id: String)

    @Query("SELECT * FROM habits ORDER BY name") fun habits(): Flow<List<HabitEntity>>
    @Query("SELECT * FROM habit_logs") fun habitLogs(): Flow<List<HabitLogEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabit(item: HabitEntity)
    @Query("UPDATE habits SET color=:color WHERE id=:id") suspend fun updateHabitColor(id:String,color:String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabitLog(item: HabitLogEntity)
    @Query("DELETE FROM habit_logs WHERE habitId=:habitId AND date=:date") suspend fun deleteHabitLog(habitId: String, date: String)
    @Query("DELETE FROM habits WHERE id=:id") suspend fun deleteHabit(id: String)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC") fun sessions(): Flow<List<SessionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSession(item: SessionEntity)
    @Query("DELETE FROM sessions WHERE id=:id") suspend fun deleteSession(id: String)
    @Query("SELECT * FROM daily_logs ORDER BY date DESC") fun dailyLogs(): Flow<List<DailyLogEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDailyLog(item: DailyLogEntity)
    @Query("DELETE FROM daily_logs WHERE date=:date") suspend fun deleteDailyLog(date: String)

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC") fun notes(): Flow<List<NoteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNote(item: NoteEntity)
    @Query("DELETE FROM notes WHERE id=:id") suspend fun deleteNote(id: String)
    @Query("SELECT * FROM goals ORDER BY createdAt DESC") fun goals(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM milestones ORDER BY rowid") fun milestones(): Flow<List<MilestoneEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoal(item: GoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMilestone(item: MilestoneEntity)
    @Query("UPDATE milestones SET done=NOT done WHERE id=:id") suspend fun toggleMilestone(id: String)
    @Query("SELECT goalId FROM milestones WHERE id=:id") suspend fun milestoneGoalId(id:String):String?
    @Query("UPDATE goals SET completedAt = CASE WHEN EXISTS(SELECT 1 FROM milestones WHERE goalId=:goalId) AND NOT EXISTS(SELECT 1 FROM milestones WHERE goalId=:goalId AND done=0) THEN :now ELSE NULL END WHERE id=:goalId") suspend fun refreshGoalCompletion(goalId:String,now:Long)
    @Transaction suspend fun toggleMilestoneAndRefreshGoal(id:String) { val goalId=milestoneGoalId(id) ?: return; toggleMilestone(id); refreshGoalCompletion(goalId,System.currentTimeMillis()) }
    @Query("DELETE FROM goals WHERE id=:id") suspend fun deleteGoal(id: String)

    @Transaction suspend fun importBackup(tasks: List<TaskEntity>, subjects: List<SubjectEntity>, groups: List<GroupEntity>, chapters: List<ChapterEntity>, habits: List<HabitEntity>, habitLogs: List<HabitLogEntity>, sessions: List<SessionEntity>, dailyLogs: List<DailyLogEntity>, notes: List<NoteEntity>, goals: List<GoalEntity>, milestones: List<MilestoneEntity>) {
        tasks.forEach { putTask(it) }; subjects.forEach { putSubject(it) }; groups.forEach { putGroup(it) }; chapters.forEach { putChapter(it) }; habits.forEach { putHabit(it) }; habitLogs.forEach { putHabitLog(it) }; sessions.forEach { putSession(it) }; dailyLogs.forEach { putDailyLog(it) }; notes.forEach { putNote(it) }; goals.forEach { putGoal(it) }; milestones.forEach { putMilestone(it) }
    }

    @Transaction suspend fun seedSyllabus(subjects: List<SubjectEntity>, groups: List<GroupEntity>, chapters: List<ChapterEntity>) {
        subjects.forEach { putSubject(it) }; groups.forEach { putGroup(it) }; chapters.forEach { putChapter(it) }; putMeta(AppMetaEntity("syllabus_seeded", "true"))
    }
}

package com.lambda.app.data

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

 data class WorkspaceState(
    val tasks: List<TaskEntity> = emptyList(), val subjects: List<SubjectEntity> = emptyList(), val groups: List<GroupEntity> = emptyList(), val chapters: List<ChapterEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(), val habitLogs: List<HabitLogEntity> = emptyList(), val sessions: List<SessionEntity> = emptyList(), val dailyLogs: List<DailyLogEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(), val goals: List<GoalEntity> = emptyList(), val milestones: List<MilestoneEntity> = emptyList()
)
private data class WorkspaceCoreA(val tasks: List<TaskEntity>, val subjects: List<SubjectEntity>, val groups: List<GroupEntity>, val chapters: List<ChapterEntity>, val habits: List<HabitEntity>)
private data class WorkspaceCoreB(val habitLogs: List<HabitLogEntity>, val sessions: List<SessionEntity>, val dailyLogs: List<DailyLogEntity>, val notes: List<NoteEntity>, val goals: List<GoalEntity>)
class LambdaRepository(private val dao: LambdaDao) {
    private val coreA = combine(dao.tasks(), dao.subjects(), dao.groups(), dao.chapters(), dao.habits()) { tasks, subjects, groups, chapters, habits -> WorkspaceCoreA(tasks, subjects, groups, chapters, habits) }
    private val coreB = combine(dao.habitLogs(), dao.sessions(), dao.dailyLogs(), dao.notes(), dao.goals()) { logs, sessions, dailyLogs, notes, goals -> WorkspaceCoreB(logs, sessions, dailyLogs, notes, goals) }
    val workspace: Flow<WorkspaceState> = combine(coreA, coreB, dao.milestones()) { a, b, milestones ->
        WorkspaceState(a.tasks, a.subjects, a.groups, a.chapters, a.habits, b.habitLogs, b.sessions, b.dailyLogs, b.notes, b.goals, milestones)
    }
    suspend fun seedIfEmpty() {
        if (dao.syllabusSeeded() != "true") {
            val seed = DefaultSyllabus.build()
            dao.seedSyllabus(seed.first, seed.second, seed.third)
        }
    }
    suspend fun addTask(title: String, date: String?, priority: Int = 2, dueDate: String? = null, dueTime: String? = null) = dao.putTask(TaskEntity(UUID.randomUUID().toString(), title.trim(), priority, date, dueDate, dueTime))
    suspend fun toggleTask(id: String) = dao.toggleTask(id, System.currentTimeMillis())
    suspend fun updateTask(id:String,title:String,priority:Int,date:String?,dueDate:String?,dueTime:String?) = dao.updateTaskFields(id,title.trim(),priority,date,dueDate,dueTime)
    suspend fun deleteTask(id: String) = dao.deleteTask(id)
    suspend fun addSubject(name: String, icon: String) = dao.putSubject(SubjectEntity(UUID.randomUUID().toString(), name.trim(), icon))
    suspend fun renameSubject(id:String,name:String) = dao.renameSubject(id,name.trim())
    suspend fun renameChapter(id:String,name:String) = dao.renameChapter(id,name.trim())
    suspend fun addGroup(subjectId: String, name: String) = dao.putGroup(GroupEntity(UUID.randomUUID().toString(), subjectId, name.trim()))
    suspend fun addChapter(groupId: String, name: String) = dao.putChapter(ChapterEntity(UUID.randomUUID().toString(), groupId, name.trim()))
    suspend fun updateChapter(ch: ChapterEntity, done: Int, total: Int, revision: Int) = dao.updateChapterProgress(ch.id, done.coerceAtLeast(0), total.coerceAtLeast(0), revision.coerceIn(0, 3))
    suspend fun updateChapterChecks(id: String, checks: String) = dao.updateChapterChecks(id, checks)
    suspend fun updateChapterLinks(id:String,links:String) = dao.updateChapterLinks(id,links)
    suspend fun updateGroupColumns(id:String,columns:String) = dao.updateGroupColumns(id,columns)
    suspend fun deleteSubject(id: String) = dao.deleteSubject(id)
    suspend fun deleteGroup(id: String) = dao.deleteGroup(id)
    suspend fun deleteChapter(id: String) = dao.deleteChapter(id)
    suspend fun addHabit(name: String, icon: String, color:String="#F0BC68") = dao.putHabit(HabitEntity(UUID.randomUUID().toString(), name.trim(), icon, "daily", null, color))
    suspend fun toggleHabit(habitId: String, date: String) { val exists = dao.habitLogExists(habitId, date); if (exists) dao.deleteHabitLog(habitId, date) else dao.putHabitLog(HabitLogEntity(habitId, date, true)) }
    suspend fun deleteHabit(id: String) = dao.deleteHabit(id)
    suspend fun saveDailyLog(date: String, sleep: Double?, screen: Double?, extra: Double, mood: Int?, notes: String) = dao.putDailyLog(DailyLogEntity(date, sleep, screen, extra, mood, notes))
    suspend fun deleteDailyLog(date: String) = dao.deleteDailyLog(date)
    suspend fun addSession(kind: String, title: String, minutes: Int, subjectId: String? = null, notes: String = "", lapsJson:String="[]") { val now=System.currentTimeMillis(); dao.putSession(SessionEntity(UUID.randomUUID().toString(), kind, title, notes, subjectId, now-minutes*60000L, now, minutes, java.time.LocalDate.now(java.time.ZoneId.of("Asia/Dhaka")).toString(),lapsJson)) }
    suspend fun deleteSession(id: String) = dao.deleteSession(id)
    suspend fun saveNote(id: String?, title: String, content: String, tags: String, pinned: Boolean = false, attachmentsJson: String = "[]") {
        val now=System.currentTimeMillis();
        dao.putNote(NoteEntity(id ?: UUID.randomUUID().toString(), title, content, tags, pinned, attachmentsJson, now, now))
    }
    suspend fun deleteNote(id: String) = dao.deleteNote(id)
    suspend fun addGoal(title: String, category: String, deadline: String?, milestones: List<String>) { val id=UUID.randomUUID().toString(); dao.putGoal(GoalEntity(id,title,category,deadline)); milestones.filter { it.isNotBlank() }.forEach { dao.putMilestone(MilestoneEntity(UUID.randomUUID().toString(),id,it.trim())) } }
    suspend fun toggleMilestone(id: String) = dao.toggleMilestoneAndRefreshGoal(id)
    suspend fun deleteGoal(id: String) = dao.deleteGoal(id)
    suspend fun importJson(json: String) {
        val root=JSONObject(json)
        if (root.optString("format") != "lambda-backup-v1") {
            importLegacyWebsite(root)
            return
        }
        fun <T> parse(name:String, mapper:(JSONObject)->T):List<T> { val a=root.optJSONArray(name) ?: return emptyList(); return (0 until a.length()).map { mapper(a.getJSONObject(it)) } }
        val tasks=parse("tasks"){ TaskEntity(it.getString("id"),it.getString("title"),it.optInt("priority",2),it.optString("date").takeUnless{v->v=="null"||v.isBlank()},it.optString("dueDate").takeUnless{v->v=="null"||v.isBlank()},it.optString("dueTime").takeUnless{v->v=="null"||v.isBlank()},it.optBoolean("done"),if(it.isNull("completedAt"))null else it.optLong("completedAt"),it.optLong("createdAt")) }
        val subjects=parse("subjects"){ SubjectEntity(it.getString("id"),it.getString("name"),it.optString("icon","📘"),it.optInt("sortOrder")) }
        val groups=parse("groups"){ GroupEntity(it.getString("id"),it.getString("subjectId"),it.getString("name"),it.optString("columnsCsv"),it.optInt("sortOrder")) }
        val chapters=parse("chapters"){ ChapterEntity(it.getString("id"),it.getString("groupId"),it.getString("name"),it.optInt("lecturesDone"),it.optInt("lecturesTotal"),it.optInt("revision"),it.optString("checksJson","{}"),it.optString("slideLinksJson","[]")) }
        val habits=parse("habits"){ HabitEntity(it.getString("id"),it.getString("name"),it.optString("icon","⚡"),it.optString("frequency","daily")) }
        val habitLogs=parse("habitLogs"){ HabitLogEntity(it.getString("habitId"),it.getString("date"),it.optBoolean("completed",true)) }
        val sessions=parse("sessions"){ SessionEntity(it.getString("id"),it.getString("kind"),it.getString("title"),it.optString("notes"),it.optString("subjectId").takeUnless{v->v=="null"||v.isBlank()},it.optLong("startedAt"),it.optLong("endedAt"),it.optInt("minutes"),it.getString("date"),it.optString("lapsJson","[]")) }
        val dailyLogs=parse("dailyLogs"){ DailyLogEntity(it.getString("date"),if(it.isNull("sleepHours"))null else it.optDouble("sleepHours"),if(it.isNull("screenHours"))null else it.optDouble("screenHours"),it.optDouble("extraStudyHours"),if(it.isNull("mood"))null else it.optInt("mood"),it.optString("notes")) }
        val notes=parse("notes"){ NoteEntity(it.getString("id"),it.getString("title"),it.optString("content"),it.optString("tagsCsv"),it.optBoolean("pinned"),it.optString("attachmentsJson","[]"),it.optLong("createdAt"),it.optLong("updatedAt")) }
        val goals=parse("goals"){ GoalEntity(it.getString("id"),it.getString("title"),it.optString("category","Academic"),it.optString("deadline").takeUnless{v->v=="null"||v.isBlank()},it.optLong("createdAt"),if(it.isNull("completedAt"))null else it.optLong("completedAt")) }
        val milestones=parse("milestones"){ MilestoneEntity(it.getString("id"),it.getString("goalId"),it.getString("title"),it.optBoolean("done")) }
        require(tasks.size+subjects.size+groups.size+chapters.size+habits.size+habitLogs.size+sessions.size+dailyLogs.size+notes.size+goals.size+milestones.size <= 100000) { "Backup contains too many records" }
        dao.importBackup(tasks,subjects,groups,chapters,habits,habitLogs,sessions,dailyLogs,notes,goals,milestones)
    }

    /** Imports the website's localStorage object (ureka_personal_os_data_v4) without requiring its browser. */
    private suspend fun importLegacyWebsite(root: JSONObject) {
        fun millis(value: Any?): Long = when(value) {
            is Number -> value.toLong()
            is String -> runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrElse { runCatching { java.time.LocalDate.parse(value).atStartOfDay(java.time.ZoneId.of("Asia/Dhaka")).toInstant().toEpochMilli() }.getOrDefault(System.currentTimeMillis()) }
            else -> System.currentTimeMillis()
        }
        fun optionalString(o:JSONObject,key:String):String? = if(o.isNull(key)) null else o.optString(key).takeIf{it.isNotBlank() && it!="null"}
        fun arr(o:JSONObject,key:String)=o.optJSONArray(key) ?: JSONArray()
        val tasks=(0 until arr(root,"tasks").length()).mapNotNull { i -> runCatching {
            val o=arr(root,"tasks").getJSONObject(i); val priority=o.optString("priority","p2").removePrefix("p").toIntOrNull()?.coerceIn(1,4)?:2
            val done=o.optBoolean("done"); TaskEntity(o.optString("id",UUID.randomUUID().toString()),o.getString("title"),priority,optionalString(o,"date"),optionalString(o,"dueDate"),optionalString(o,"dueTime"),done,if(o.isNull("completedAt"))null else millis(o.opt("completedAt")),millis(o.opt("createdAt")))
        }.getOrNull() }
        val subjects=mutableListOf<SubjectEntity>(); val groups=mutableListOf<GroupEntity>(); val chapters=mutableListOf<ChapterEntity>()
        val sourceSubjects=arr(root,"subjects")
        for(i in 0 until sourceSubjects.length()) runCatching {
            val sub=sourceSubjects.getJSONObject(i); val sid=sub.optString("id",UUID.randomUUID().toString())
            subjects += SubjectEntity(sid,sub.optString("name","Subject"),sub.optString("icon","📘"),i)
            val sourceGroups=arr(sub,"groups")
            for(g in 0 until sourceGroups.length()) {
                val group=sourceGroups.getJSONObject(g); val gid=group.optString("id",UUID.randomUUID().toString())
                val cols=arr(group,"columns"); val columns=(0 until cols.length()).map{cols.optString(it)}.filter{it.isNotBlank()}.joinToString(",").ifBlank{"Class,Short Notes,Engineering,Test Paper,Question Bank"}
                groups += GroupEntity(gid,sid,group.optString("name","Paper ${g+1}"),columns,g)
                val sourceChapters=arr(group,"chapters")
                for(c in 0 until sourceChapters.length()) {
                    val ch=sourceChapters.getJSONObject(c); val checks=ch.optJSONObject("checks") ?: JSONObject(); val links=ch.optJSONArray("slideLinks") ?: JSONArray()
                    chapters += ChapterEntity(ch.optString("id",UUID.randomUUID().toString()),gid,ch.optString("name","Chapter ${c+1}"),ch.optInt("lecturesDone"),ch.optInt("lecturesTotal"),ch.optInt("revision").coerceIn(0,3),checks.toString(),links.toString())
                }
            }
        }
        val habits=mutableListOf<HabitEntity>(); val logs=mutableListOf<HabitLogEntity>(); val sourceHabits=arr(root,"habits")
        for(i in 0 until sourceHabits.length()) runCatching {
            val h=sourceHabits.getJSONObject(i); val id=h.optString("id",UUID.randomUUID().toString())
            habits += HabitEntity(id,h.optString("name","Habit"),h.optString("icon","⚡"),h.optString("frequency","daily"),if(h.isNull("targetDays"))null else h.optInt("targetDays"),h.optString("color","#F0BC68"))
            val log=h.optJSONObject("log") ?: JSONObject(); log.keys().forEach { date -> if(log.optBoolean(date)) logs += HabitLogEntity(id,date,true) }
        }
        val sessions=(0 until arr(root,"sessions").length()).mapNotNull { i -> runCatching {
            val o=arr(root,"sessions").getJSONObject(i); val start=millis(o.opt("startedAt")); val end=millis(o.opt("endedAt")); val minutes=o.optInt("minutes",((end-start)/60000L).toInt()).coerceAtLeast(0)
            SessionEntity(o.optString("id",UUID.randomUUID().toString()),o.optString("kind","work"),o.optString("title","Study session"),o.optString("notes"),optionalString(o,"subjectId"),start,end,minutes,o.optString("date",java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.of("Asia/Dhaka")).toLocalDate().toString()),o.optJSONArray("laps")?.toString() ?: "[]")
        }.getOrNull() }
        val daily=mutableListOf<DailyLogEntity>(); val sourceDaily=root.optJSONObject("dailyLogs") ?: JSONObject()
        sourceDaily.keys().forEach { date -> runCatching { val o=sourceDaily.getJSONObject(date); daily += DailyLogEntity(date,if(o.isNull("sleepHours"))null else o.optDouble("sleepHours"),if(o.isNull("screenHours"))null else o.optDouble("screenHours"),o.optDouble("extraStudyHours"),if(o.isNull("mood"))null else o.optInt("mood"),o.optString("notes")) } }
        val notes=(0 until arr(root,"notes").length()).mapNotNull { i -> runCatching {
            val o=arr(root,"notes").getJSONObject(i); val attachments=JSONArray(); val source=o.optJSONArray("attachments") ?: JSONArray()
            for(a in 0 until source.length()) { val item=source.getJSONObject(a); attachments.put(JSONObject().put("name",item.optString("name","Attachment")).put("type",item.optString("type","application/octet-stream")).put("size",item.optLong("size")).put("dataUrl",item.optString("dataUrl"))) }
            val tags=o.optJSONArray("tags")?.let{a->(0 until a.length()).joinToString(","){a.optString(it)}} ?: o.optString("tagsCsv")
            NoteEntity(o.optString("id",UUID.randomUUID().toString()),o.optString("title","Untitled"),o.optString("content"),tags,o.optBoolean("pinned"),attachments.toString(),millis(o.opt("createdAt")),millis(o.opt("updatedAt")))
        }.getOrNull() }
        val goals=mutableListOf<GoalEntity>(); val milestones=mutableListOf<MilestoneEntity>(); val sourceGoals=arr(root,"goals")
        for(i in 0 until sourceGoals.length()) runCatching {
            val o=sourceGoals.getJSONObject(i); val id=o.optString("id",UUID.randomUUID().toString())
            goals += GoalEntity(id,o.optString("title","Goal"),o.optString("category","Academic"),optionalString(o,"deadline"),millis(o.opt("createdAt")),if(o.isNull("completedAt"))null else millis(o.opt("completedAt")))
            val ms=arr(o,"milestones"); for(j in 0 until ms.length()) { val m=ms.getJSONObject(j); milestones += MilestoneEntity(m.optString("id",UUID.randomUUID().toString()),id,m.optString("title","Milestone"),m.optBoolean("done")) }
        }
        val count=tasks.size+subjects.size+groups.size+chapters.size+habits.size+logs.size+sessions.size+daily.size+notes.size+goals.size+milestones.size
        require(count <= 100000) { "Website backup contains too many records" }
        require(count > 0) { "This file does not contain Lambda website records" }
        dao.importBackup(tasks,subjects,groups,chapters,habits,logs,sessions,daily,notes,goals,milestones)
    }

    suspend fun exportJson(state: WorkspaceState): String {
        val root=JSONObject(); fun arr(name:String, values:List<JSONObject>) { root.put(name, JSONArray(values)) }
        arr("tasks",state.tasks.map { JSONObject().put("id",it.id).put("title",it.title).put("priority",it.priority).put("date",it.date).put("dueDate",it.dueDate).put("dueTime",it.dueTime).put("done",it.done).put("completedAt",it.completedAt).put("createdAt",it.createdAt) })
        arr("subjects",state.subjects.map { JSONObject().put("id",it.id).put("name",it.name).put("icon",it.icon).put("sortOrder",it.sortOrder) })
        arr("groups",state.groups.map { JSONObject().put("id",it.id).put("subjectId",it.subjectId).put("name",it.name).put("columnsCsv",it.columnsCsv).put("sortOrder",it.sortOrder) })
        arr("chapters",state.chapters.map { JSONObject().put("id",it.id).put("groupId",it.groupId).put("name",it.name).put("lecturesDone",it.lecturesDone).put("lecturesTotal",it.lecturesTotal).put("revision",it.revision).put("checksJson",it.checksJson).put("slideLinksJson",it.slideLinksJson) })
        arr("habits",state.habits.map { JSONObject().put("id",it.id).put("name",it.name).put("icon",it.icon).put("frequency",it.frequency) })
        arr("habitLogs",state.habitLogs.map { JSONObject().put("habitId",it.habitId).put("date",it.date).put("completed",it.completed) })
        arr("sessions",state.sessions.map { JSONObject().put("id",it.id).put("kind",it.kind).put("title",it.title).put("notes",it.notes).put("subjectId",it.subjectId).put("startedAt",it.startedAt).put("endedAt",it.endedAt).put("minutes",it.minutes).put("date",it.date).put("lapsJson",it.lapsJson) })
        arr("dailyLogs",state.dailyLogs.map { JSONObject().put("date",it.date).put("sleepHours",it.sleepHours).put("screenHours",it.screenHours).put("extraStudyHours",it.extraStudyHours).put("mood",it.mood).put("notes",it.notes) })
        arr("notes",state.notes.map { JSONObject().put("id",it.id).put("title",it.title).put("content",it.content).put("tagsCsv",it.tagsCsv).put("pinned",it.pinned).put("attachmentsJson",it.attachmentsJson).put("createdAt",it.createdAt).put("updatedAt",it.updatedAt) })
        arr("goals",state.goals.map { JSONObject().put("id",it.id).put("title",it.title).put("category",it.category).put("deadline",it.deadline).put("createdAt",it.createdAt).put("completedAt",it.completedAt) })
        arr("milestones",state.milestones.map { JSONObject().put("id",it.id).put("goalId",it.goalId).put("title",it.title).put("done",it.done) })
        root.put("format", "lambda-backup-v1"); root.put("exportedAt", System.currentTimeMillis()); return root.toString(2)
    }
}

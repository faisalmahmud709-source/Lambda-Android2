package com.lambda.app

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lambda.app.data.*
import com.lambda.app.util.Chime
import com.lambda.app.util.todayStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class LambdaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    val data: StateFlow<AppData> = repo.state

    private val _pomodoro = MutableStateFlow(PomodoroState(remainingMs = repo.state.value.settings.focusMinutes * 60_000L))
    val pomodoro: StateFlow<PomodoroState> = _pomodoro.asStateFlow()

    init {
        // Pomodoro ticker (timestamps based, so it stays accurate even if the UI is paused)
        viewModelScope.launch {
            while (true) {
                delay(500)
                tickPomodoro()
            }
        }
    }

    fun flush() = repo.flush()

    private fun id(prefix: String) = "$prefix-${System.currentTimeMillis()}-${Random.nextInt(1000, 9999)}"
    private fun nowIso() = Instant.now().toString()
    private fun sound() = repo.state.value.settings.soundEnabled
    private fun chime(k: Chime.Kind) { if (sound()) Chime.play(k) }

    // ---------------------------------------------------------------- onboarding / settings
    fun finishOnboarding(name: String) = repo.update {
        it.copy(onboarded = true, settings = it.settings.copy(name = name.trim()))
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        repo.update { it.copy(settings = transform(it.settings)) }
        // keep an idle pomodoro in sync with new durations
        val p = _pomodoro.value
        if (!p.running) {
            _pomodoro.update { it.copy(remainingMs = phaseMinutes(it.phase) * 60_000L) }
        }
    }

    private fun phaseMinutes(phase: String): Int {
        val s = repo.state.value.settings
        return when (phase) {
            "focus" -> s.focusMinutes
            "short" -> s.shortBreakMinutes
            else -> s.longBreakMinutes
        }
    }

    // ---------------------------------------------------------------- tasks
    fun addTask(title: String, priority: String, date: String?, dueTime: String?) {
        val t = Task(
            id = id("task"), title = title.trim(), priority = priority,
            date = date, dueDate = date, dueTime = dueTime,
            done = false, completedAt = null, createdAt = nowIso()
        )
        repo.update { it.copy(tasks = listOf(t) + it.tasks) }
    }

    fun updateTask(id: String, f: (Task) -> Task) =
        repo.update { d -> d.copy(tasks = d.tasks.map { if (it.id == id) f(it) else it }) }

    fun moveTaskToDate(id: String, date: String) = updateTask(id) { it.copy(date = date, dueDate = date) }

    fun toggleTask(id: String) {
        var becameDone = false
        repo.update { d ->
            d.copy(tasks = d.tasks.map {
                if (it.id == id) {
                    val next = !it.done
                    becameDone = next
                    it.copy(done = next, completedAt = if (next) nowIso() else null)
                } else it
            })
        }
        if (becameDone) chime(Chime.Kind.Click)
    }

    fun deleteTask(id: String) = repo.update { d -> d.copy(tasks = d.tasks.filter { it.id != id }) }

    // ---------------------------------------------------------------- syllabus
    private fun mapSubject(subjectId: String, f: (Subject) -> Subject) =
        repo.update { d -> d.copy(subjects = d.subjects.map { if (it.id == subjectId) f(it) else it }) }

    fun addSubject(name: String, icon: String) {
        val s = Subject(
            id = id("subj"), name = name.trim().ifBlank { "New Subject" }, icon = icon.ifBlank { "📘" },
            groups = listOf(SubjectGroup(id("group"), "1st Paper", DEFAULT_COLUMNS, emptyList()))
        )
        repo.update { it.copy(subjects = it.subjects + s) }
    }

    fun renameSubject(subjectId: String, name: String) = mapSubject(subjectId) { it.copy(name = name.trim().ifBlank { it.name }) }
    fun deleteSubject(subjectId: String) = repo.update { d -> d.copy(subjects = d.subjects.filter { it.id != subjectId }) }

    fun addGroup(subjectId: String, name: String) = mapSubject(subjectId) {
        it.copy(groups = it.groups + SubjectGroup(id("group"), name.trim().ifBlank { "New Paper" }, DEFAULT_COLUMNS, emptyList()))
    }

    fun deleteGroup(subjectId: String, groupId: String) =
        mapSubject(subjectId) { it.copy(groups = it.groups.filter { g -> g.id != groupId }) }

    private fun mapGroup(subjectId: String, groupId: String, f: (SubjectGroup) -> SubjectGroup) =
        mapSubject(subjectId) { s -> s.copy(groups = s.groups.map { if (it.id == groupId) f(it) else it }) }

    fun addChapter(subjectId: String, groupId: String, name: String) = mapGroup(subjectId, groupId) {
        it.copy(chapters = it.chapters + Chapter(id = id("chap"), name = name.trim().ifBlank { "New Chapter" }, lecturesTotal = 8))
    }

    fun updateChapter(subjectId: String, groupId: String, chapterId: String, f: (Chapter) -> Chapter) =
        mapGroup(subjectId, groupId) { g -> g.copy(chapters = g.chapters.map { if (it.id == chapterId) f(it) else it }) }

    fun deleteChapter(subjectId: String, groupId: String, chapterId: String) =
        mapGroup(subjectId, groupId) { g -> g.copy(chapters = g.chapters.filter { it.id != chapterId }) }

    // ---------------------------------------------------------------- habits
    fun addHabit(name: String, icon: String, color: String) {
        val h = Habit(id("habit"), name.trim().ifBlank { "New Habit" }, icon.ifBlank { "⚡" }, "daily", color, emptyMap())
        repo.update { it.copy(habits = it.habits + h) }
    }

    fun toggleHabitDay(habitId: String, date: String) {
        var checked = false
        repo.update { d ->
            d.copy(habits = d.habits.map { h ->
                if (h.id != habitId) h else {
                    val log = h.log.toMutableMap()
                    if (log[date] == true) log.remove(date) else { log[date] = true; checked = true }
                    h.copy(log = log)
                }
            })
        }
        if (checked) chime(Chime.Kind.Click)
    }

    fun deleteHabit(habitId: String) = repo.update { d -> d.copy(habits = d.habits.filter { it.id != habitId }) }

    // ---------------------------------------------------------------- daily log
    fun saveDailyLog(entry: DailyLogEntry) = repo.update { it.copy(dailyLogs = it.dailyLogs + (entry.date to entry)) }
    fun deleteDailyLog(date: String) = repo.update { it.copy(dailyLogs = it.dailyLogs - date) }

    // ---------------------------------------------------------------- sessions
    fun deleteSession(sessionId: String) = repo.update { d -> d.copy(sessions = d.sessions.filter { it.id != sessionId }) }

    fun addSession(title: String, minutes: Int, kind: String, subjectId: String? = null, notes: String? = null) {
        val now = System.currentTimeMillis()
        val s = Session(
            id = id(if (kind == "pomodoro") "pomo" else "work"), kind = kind, title = title,
            notes = notes, subjectId = subjectId,
            startedAt = Instant.ofEpochMilli(now - minutes * 60_000L).toString(),
            endedAt = Instant.ofEpochMilli(now).toString(),
            minutes = minutes, date = todayStr()
        )
        repo.update { it.copy(sessions = listOf(s) + it.sessions) }
    }

    // ---------------------------------------------------------------- notes
    fun addNote(title: String, tags: List<String>): String {
        val n = Note(
            id = id("note"), title = title.trim().ifBlank { "Untitled Note" }, content = "", tags = tags,
            pinned = false, attachments = emptyList(), createdAt = nowIso(), updatedAt = nowIso()
        )
        repo.update { it.copy(notes = listOf(n) + it.notes) }
        return n.id
    }

    fun updateNote(id: String, f: (Note) -> Note) =
        repo.update { d -> d.copy(notes = d.notes.map { if (it.id == id) f(it).copy(updatedAt = nowIso()) else it }) }

    fun deleteNote(id: String) {
        val n = repo.state.value.notes.firstOrNull { it.id == id }
        n?.attachments?.forEach { deleteAttachmentFile(it) }
        repo.update { d -> d.copy(notes = d.notes.filter { it.id != id }) }
    }

    private fun deleteAttachmentFile(a: NoteAttachment) {
        a.path?.let { runCatching { File(repo.attachmentsDir, it).delete() } }
    }

    fun attachmentFile(a: NoteAttachment): File? =
        a.path?.let { File(repo.attachmentsDir, it) }?.takeIf { it.exists() }

    /** Copies a picked file into the app's private storage. */
    fun addAttachment(noteId: String, uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val result = withContext(Dispatchers.IO) {
                try {
                    var name = "file"
                    var size = 0L
                    resolver.query(uri, null, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val si = c.getColumnIndex(OpenableColumns.SIZE)
                            if (ni >= 0) name = c.getString(ni) ?: name
                            if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                        }
                    }
                    if (size > 50L * 1024 * 1024) return@withContext "\"$name\" is larger than 50 MB."
                    val attId = id("att")
                    val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val stored = "${attId}_$safe"
                    val out = File(repo.attachmentsDir, stored)
                    resolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                        ?: return@withContext "Could not read that file."
                    val att = NoteAttachment(
                        id = attId, name = name, type = resolver.getType(uri) ?: "",
                        size = out.length(), path = stored, addedAt = nowIso()
                    )
                    repo.update { d ->
                        d.copy(notes = d.notes.map {
                            if (it.id == noteId) it.copy(attachments = it.attachments + att, updatedAt = nowIso()) else it
                        })
                    }
                    "Attached \"$name\""
                } catch (e: Exception) {
                    "Could not attach file."
                }
            }
            onResult(result)
        }
    }

    fun removeAttachment(noteId: String, att: NoteAttachment) {
        deleteAttachmentFile(att)
        repo.update { d ->
            d.copy(notes = d.notes.map {
                if (it.id == noteId) it.copy(attachments = it.attachments.filter { a -> a.id != att.id }, updatedAt = nowIso()) else it
            })
        }
    }

    // ---------------------------------------------------------------- goals
    fun addGoal(title: String, category: String, deadline: String?, milestones: List<String>) {
        val g = Goal(
            id = id("goal"), title = title.trim().ifBlank { "New Goal" }, category = category,
            deadline = deadline, completedAt = null, createdAt = nowIso(),
            milestones = milestones.mapIndexed { i, m -> Milestone("ms-${System.currentTimeMillis()}-$i", m.trim(), false) }
        )
        repo.update { it.copy(goals = listOf(g) + it.goals) }
    }

    fun toggleMilestone(goalId: String, milestoneId: String) {
        var completed = false
        repo.update { d ->
            d.copy(goals = d.goals.map { g ->
                if (g.id != goalId) g else {
                    val ms = g.milestones.map { if (it.id == milestoneId) it.copy(done = !it.done) else it }
                    val all = ms.isNotEmpty() && ms.all { it.done }
                    if (all && g.completedAt == null) completed = true
                    g.copy(milestones = ms, completedAt = if (all) nowIso() else null)
                }
            })
        }
        if (completed) chime(Chime.Kind.Complete)
    }

    fun deleteGoal(goalId: String) = repo.update { d -> d.copy(goals = d.goals.filter { it.id != goalId }) }

    // ---------------------------------------------------------------- work session (stopwatch)
    fun startWork(title: String, subjectId: String?, notes: String) {
        val now = System.currentTimeMillis()
        repo.update {
            it.copy(activeWork = ActiveWork(
                title = title.trim().ifBlank { "Work session" }, subjectId = subjectId, notes = notes,
                runningSince = now, accumulatedMs = 0, firstStartedAt = now, laps = emptyList()
            ))
        }
    }

    fun pauseWork() = repo.update { d ->
        val w = d.activeWork ?: return@update d
        val since = w.runningSince ?: return@update d
        d.copy(activeWork = w.copy(runningSince = null, accumulatedMs = w.accumulatedMs + (System.currentTimeMillis() - since)))
    }

    fun resumeWork() = repo.update { d ->
        val w = d.activeWork ?: return@update d
        if (w.runningSince != null) d else d.copy(activeWork = w.copy(runningSince = System.currentTimeMillis()))
    }

    fun addLap() = repo.update { d ->
        val w = d.activeWork ?: return@update d
        val cur = w.accumulatedMs + (w.runningSince?.let { System.currentTimeMillis() - it } ?: 0L)
        d.copy(activeWork = w.copy(laps = w.laps + Lap(cur, "Lap ${w.laps.size + 1}")))
    }

    fun updateWorkNotes(notes: String) = repo.update { d ->
        val w = d.activeWork ?: return@update d
        d.copy(activeWork = w.copy(notes = notes))
    }

    fun stopAndSaveWork() {
        val d = repo.state.value
        val w = d.activeWork ?: return
        val now = System.currentTimeMillis()
        val totalMs = w.accumulatedMs + (w.runningSince?.let { now - it } ?: 0L)
        val minutes = maxOf(1, Math.round(totalMs / 60000.0).toInt())
        val startDate = Instant.ofEpochMilli(w.firstStartedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val s = Session(
            id = id("work"), kind = "work", title = w.title, notes = w.notes, subjectId = w.subjectId,
            startedAt = Instant.ofEpochMilli(w.firstStartedAt).toString(), endedAt = Instant.ofEpochMilli(now).toString(),
            minutes = minutes, date = startDate, laps = w.laps
        )
        repo.update { it.copy(sessions = listOf(s) + it.sessions, activeWork = null) }
        chime(Chime.Kind.Complete)
    }

    fun discardWork() = repo.update { it.copy(activeWork = null) }

    // ---------------------------------------------------------------- pomodoro
    private fun tickPomodoro() {
        val p = _pomodoro.value
        val ends = p.endsAt
        if (!p.running || ends == null) return
        val now = System.currentTimeMillis()
        if (ends - now > 0) return

        chime(Chime.Kind.Complete)
        val s = repo.state.value.settings
        val isFocus = p.phase == "focus"
        val nextCompleted = if (isFocus) p.completedFocus + 1 else p.completedFocus
        if (isFocus) {
            val sess = Session(
                id = "pomo-$now", kind = "pomodoro", title = p.label.trim().ifBlank { "Focus sprint" },
                notes = "Completed pomodoro sprint", subjectId = p.subjectId,
                startedAt = Instant.ofEpochMilli(now - s.focusMinutes * 60_000L).toString(),
                endedAt = Instant.ofEpochMilli(now).toString(), minutes = s.focusMinutes, date = todayStr()
            )
            repo.update { it.copy(sessions = listOf(sess) + it.sessions) }
        }
        val nextPhase = if (isFocus) {
            if (nextCompleted > 0 && nextCompleted % s.longBreakEvery == 0) "long" else "short"
        } else "focus"
        _pomodoro.value = p.copy(
            phase = nextPhase, running = false, endsAt = null,
            remainingMs = phaseMinutes(nextPhase) * 60_000L, completedFocus = nextCompleted
        )
    }

    fun startPomodoro() = _pomodoro.update { it.copy(running = true, endsAt = System.currentTimeMillis() + it.remainingMs) }

    fun pausePomodoro() = _pomodoro.update {
        val ends = it.endsAt ?: return@update it
        it.copy(running = false, endsAt = null, remainingMs = maxOf(0L, ends - System.currentTimeMillis()))
    }

    fun resetPomodoro() = _pomodoro.update { it.copy(running = false, endsAt = null, remainingMs = phaseMinutes(it.phase) * 60_000L) }

    fun skipPomodoro() = _pomodoro.update {
        val next = if (it.phase == "focus") "short" else "focus"
        it.copy(phase = next, running = false, endsAt = null, remainingMs = phaseMinutes(next) * 60_000L)
    }

    fun setPomodoroPhase(phase: String) = _pomodoro.update {
        it.copy(phase = phase, running = false, endsAt = null, remainingMs = phaseMinutes(phase) * 60_000L)
    }

    fun setPomodoroLabel(label: String, subjectId: String?) = _pomodoro.update { it.copy(label = label, subjectId = subjectId) }

    // ---------------------------------------------------------------- backup / reset
    fun exportJson(): String {
        val d = repo.state.value
        val b = Backup(
            version = 2, exportDate = nowIso(), settings = d.settings, tasks = d.tasks, subjects = d.subjects,
            sessions = d.sessions, habits = d.habits, dailyLogs = d.dailyLogs,
            // attached files live only on the phone; backup keeps the note text + file names
            notes = d.notes.map { n -> n.copy(attachments = n.attachments.map { it.copy(path = null) }) },
            goals = d.goals
        )
        return LambdaJson.encodeToString(Backup.serializer(), b)
    }

    /** Accepts backups from this app and from the web version. */
    fun importJson(text: String): Boolean {
        return try {
            val b = LambdaJson.decodeFromString(Backup.serializer(), text)
            val d = repo.state.value
            repo.replace(
                d.copy(
                    settings = b.settings ?: d.settings,
                    tasks = b.tasks ?: d.tasks,
                    subjects = b.subjects ?: d.subjects,
                    sessions = b.sessions ?: d.sessions,
                    habits = b.habits ?: d.habits,
                    dailyLogs = b.dailyLogs ?: d.dailyLogs,
                    notes = b.notes?.map { n -> n.copy(attachments = n.attachments.filter { a -> a.path != null && attachmentFile(a) != null }) } ?: d.notes,
                    goals = b.goals ?: d.goals,
                    onboarded = true
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resetAll() {
        runCatching { repo.attachmentsDir.listFiles()?.forEach { it.delete() } }
        _pomodoro.value = PomodoroState()
        repo.replace(AppData(onboarded = true, settings = Settings(name = repo.state.value.settings.name)))
        _pomodoro.value = PomodoroState(remainingMs = 25 * 60_000L)
    }

    companion object {
        val DEFAULT_COLUMNS = listOf("Class", "Short Notes", "Engineering", "Test Paper", "Question Bank")
    }
}

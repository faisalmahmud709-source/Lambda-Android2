package com.lambda.app

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lambda.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LambdaViewModel(app: Application) : AndroidViewModel(app) {
    private val db = LambdaDatabase.get(app)
    private val repo = LambdaRepository(db.dao())
    private val prefs = Preferences(app)
    val state = repo.workspace.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceState())
    val preferences = prefs.values.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrefState("dark", "", 25, 5, 15, 6, true, 4))
    init { viewModelScope.launch { repo.seedIfEmpty() } }
    private fun playChime(duration:Int=110) {
        if(!preferences.value.sound) return
        runCatching { val tone=ToneGenerator(AudioManager.STREAM_MUSIC,45); tone.startTone(ToneGenerator.TONE_PROP_BEEP,duration); Handler(Looper.getMainLooper()).postDelayed({tone.release()},duration+100L) }
    }
    fun chimeFocusComplete() = playChime(280)
    fun addTask(title:String,date:String?,priority:Int=2,dueDate:String?=null,dueTime:String?=null)=viewModelScope.launch { if(title.isNotBlank()) repo.addTask(title,date,priority,dueDate,dueTime) }
    fun toggleTask(id:String)=viewModelScope.launch { if(state.value.tasks.firstOrNull{it.id==id}?.done==false) playChime(); repo.toggleTask(id) }
    fun updateTask(id:String,title:String,priority:Int,date:String?,dueDate:String?,dueTime:String?)=viewModelScope.launch { if(title.isNotBlank()) repo.updateTask(id,title,priority,date,dueDate,dueTime) }
    fun deleteTask(id:String)=viewModelScope.launch { repo.deleteTask(id) }
    fun addSubject(name:String,icon:String="📘")=viewModelScope.launch { if(name.isNotBlank()) repo.addSubject(name,icon) }
    fun renameSubject(id:String,name:String)=viewModelScope.launch { if(name.isNotBlank()) repo.renameSubject(id,name) }
    fun renameChapter(id:String,name:String)=viewModelScope.launch { if(name.isNotBlank()) repo.renameChapter(id,name) }
    fun addGroup(subjectId:String,name:String)=viewModelScope.launch { if(name.isNotBlank()) repo.addGroup(subjectId,name) }
    fun addChapter(groupId:String,name:String)=viewModelScope.launch { if(name.isNotBlank()) repo.addChapter(groupId,name) }
    fun updateChapter(c:ChapterEntity,done:Int,total:Int,revision:Int)=viewModelScope.launch { repo.updateChapter(c,done,total,revision) }
    fun updateChapterChecks(id:String,checks:String)=viewModelScope.launch { repo.updateChapterChecks(id,checks) }
    fun updateChapterLinks(id:String,links:String)=viewModelScope.launch { repo.updateChapterLinks(id,links) }
    fun updateGroupColumns(id:String,columns:String)=viewModelScope.launch { repo.updateGroupColumns(id,columns) }
    fun deleteSubject(id:String)=viewModelScope.launch { repo.deleteSubject(id) }
    fun deleteGroup(id:String)=viewModelScope.launch { repo.deleteGroup(id) }
    fun deleteChapter(id:String)=viewModelScope.launch { repo.deleteChapter(id) }
    fun addHabit(name:String,icon:String="⚡",color:String="#F0BC68")=viewModelScope.launch { if(name.isNotBlank()) repo.addHabit(name,icon,color) }
    fun toggleHabit(id:String,date:String)=viewModelScope.launch { if(state.value.habitLogs.none{it.habitId==id&&it.date==date}) playChime(); repo.toggleHabit(id,date) }
    fun deleteHabit(id:String)=viewModelScope.launch { repo.deleteHabit(id) }
    fun saveDailyLog(date:String,sleep:Double?,screen:Double?,extra:Double,mood:Int?,notes:String)=viewModelScope.launch { repo.saveDailyLog(date,sleep,screen,extra,mood,notes) }
    fun deleteDailyLog(date:String)=viewModelScope.launch { repo.deleteDailyLog(date) }
    fun addSession(kind:String,title:String,minutes:Int,subjectId:String?=null,notes:String="",lapsJson:String="[]")=viewModelScope.launch { if(minutes>0) repo.addSession(kind,title,minutes,subjectId,notes,lapsJson) }
    fun deleteSession(id:String)=viewModelScope.launch { repo.deleteSession(id) }
    fun saveNote(id:String?,title:String,content:String,tags:String,pinned:Boolean=false,attachmentsJson:String="[]")=viewModelScope.launch { repo.saveNote(id,title,content,tags,pinned,attachmentsJson) }
    fun deleteNote(id:String)=viewModelScope.launch { repo.deleteNote(id) }
    fun addGoal(title:String,category:String,deadline:String?,milestones:List<String>)=viewModelScope.launch { if(title.isNotBlank()) repo.addGoal(title,category,deadline,milestones) }
    fun toggleMilestone(id:String)=viewModelScope.launch { val m=state.value.milestones.firstOrNull{it.id==id}; if(m!=null && !m.done && state.value.milestones.filter{it.goalId==m.goalId}.all{it.id==m.id || it.done}) playChime(280); repo.toggleMilestone(id) }
    fun deleteGoal(id:String)=viewModelScope.launch { repo.deleteGoal(id) }
    fun setTheme(v:String)=viewModelScope.launch { prefs.setTheme(v) }
    fun setName(v:String)=viewModelScope.launch { prefs.setName(v) }
    fun setTimer(f:Int,s:Int,l:Int,every:Int=4)=viewModelScope.launch { prefs.setTimer(f.coerceIn(1,180),s.coerceIn(1,60),l.coerceIn(1,90),every) }
    fun setGoal(v:Int)=viewModelScope.launch { prefs.setGoal(v.coerceIn(1,24)) }
    fun setSound(v:Boolean)=viewModelScope.launch { prefs.setSound(v) }
    suspend fun exportJson():String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val snapshot=state.value
        val portableNotes=snapshot.notes.map { note ->
            val source=runCatching { org.json.JSONArray(note.attachmentsJson) }.getOrElse { org.json.JSONArray() }
            val portable=org.json.JSONArray()
            for(index in 0 until source.length()) {
                val item=source.optJSONObject(index) ?: continue
                val uri=item.optString("uri")
                if(uri.isNotBlank()) {
                    val parsed=android.net.Uri.parse(uri)
                    val bytes=runCatching{getApplication<Application>().contentResolver.openInputStream(parsed)?.use{it.readBytes()}}.getOrNull() ?: error("Could not read attachment ${item.optString("name")}. Remove it from the note or re-attach it, then export again.")
                    val mime=getApplication<Application>().contentResolver.getType(parsed) ?: item.optString("type","application/octet-stream")
                    portable.put(org.json.JSONObject().put("name",item.optString("name","Attachment")).put("type",mime).put("size",bytes.size).put("dataUrl","data:$mime;base64,${android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)}"))
                } else portable.put(item)
            }
            note.copy(attachmentsJson=portable.toString())
        }
        repo.exportJson(snapshot.copy(notes=portableNotes))
    }
    suspend fun importJson(json:String) {
        val root=org.json.JSONObject(json)
        repo.importJson(json)
        if(root.optString("format")!="lambda-backup-v1") {
            val settings=root.optJSONObject("settings")
            val user=root.optJSONObject("user")
            val displayName=settings?.optString("name")?.takeIf{it.isNotBlank()} ?: user?.optString("name")?.takeIf{it.isNotBlank()}
            if(displayName!=null) prefs.setName(displayName)
            if(settings!=null) {
                prefs.setTimer(settings.optInt("focusMinutes",25).coerceIn(1,180),settings.optInt("shortBreakMinutes",5).coerceIn(1,60),settings.optInt("longBreakMinutes",15).coerceIn(1,90),settings.optInt("longBreakEvery",4).coerceIn(2,8))
                prefs.setGoal(settings.optInt("dailyStudyGoalHours",6).coerceIn(1,24))
                prefs.setSound(settings.optBoolean("soundEnabled",true))
            }
        }
    }
}

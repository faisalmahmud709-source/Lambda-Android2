package com.lambda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lambda.app.data.*
import com.lambda.app.ui.theme.LambdaTheme
import com.lambda.app.domain.chapterProgress
import com.lambda.app.domain.habitStreak
import java.time.ZoneId
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: LambdaViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val pref by vm.preferences.collectAsStateWithLifecycle()
            val dark = pref.theme != "light"
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            LambdaTheme(dark) { LambdaApp(vm) }
        }
    }
}

private enum class Page(val route:String, val label:String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("home","Home", Icons.Default.Home), PLANNER("planner","Planner", Icons.Default.CalendarMonth), ACADEMICS("academics","Academics", Icons.Default.MenuBook), FOCUS("focus","Focus", Icons.Default.Timer), MORE("more","More", Icons.Default.GridView),
    HABITS("habits","Habits", Icons.Default.LocalFireDepartment), LOG("log","Daily log", Icons.Default.EditCalendar), ANALYTICS("analytics","Analytics", Icons.Default.Insights), NOTES("notes","Notes", Icons.Default.Notes), GOALS("goals","Goals", Icons.Default.Flag), SETTINGS("settings","Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LambdaApp(vm: LambdaViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pref by vm.preferences.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1050); showSplash=false }
    if (showSplash) { Splash(); return }
    val navController= rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute=backStack?.destination?.route ?: Page.HOME.route
    val page=Page.entries.firstOrNull{it.route==currentRoute} ?: Page.HOME
    val navigate:(Page)->Unit = { target -> navController.navigate(target.route) { launchSingleTop=true; restoreState=true; popUpTo(Page.HOME.route) { saveState=true } } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Lambda", fontWeight=FontWeight.SemiBold, letterSpacing=0.5.sp) }, navigationIcon = { Image(painterResource(com.lambda.app.R.drawable.lambda_logo), null, Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))) }, actions = { Text("Track the trace.", style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.primary, modifier=Modifier.padding(end=16.dp)) }, colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background)) },
        bottomBar = { NavigationBar(containerColor=MaterialTheme.colorScheme.surface) { listOf(Page.HOME,Page.PLANNER,Page.ACADEMICS,Page.FOCUS,Page.MORE).forEach { p -> NavigationBarItem(selected=page==p, onClick={navigate(p)}, icon={Icon(p.icon,null)}, label={Text(p.label)}) } } }
    ) { pad ->
        NavHost(navController=navController,startDestination=Page.HOME.route,modifier=Modifier.padding(pad).fillMaxSize()) {
            composable(Page.HOME.route) { DashboardScreen(state,pref,vm,navigate) }
            composable(Page.PLANNER.route) { PlannerScreen(state,vm) }
            composable(Page.ACADEMICS.route) { AcademicsScreen(state,vm) }
            composable(Page.FOCUS.route) { FocusScreen(state,pref,vm) }
            composable(Page.MORE.route) { MoreScreen(navigate) }
            composable(Page.HABITS.route) { HabitsScreen(state,vm) }
            composable(Page.LOG.route) { DailyLogScreen(state,vm) }
            composable(Page.ANALYTICS.route) { AnalyticsScreen(state,pref) }
            composable(Page.NOTES.route) { NotesScreen(state,vm) }
            composable(Page.GOALS.route) { GoalsScreen(state,vm) }
            composable(Page.SETTINGS.route) { SettingsScreen(state,pref,vm) }
        }
    }
}

@Composable private fun Splash() {
    var shifted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(250); shifted=true }
    Box(Modifier.fillMaxSize().background(Color(0xFF004126)), contentAlignment=Alignment.Center) {
        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center) {
            Image(painterResource(R.drawable.lambda_logo),null,Modifier.size(if(shifted) 82.dp else 148.dp).clip(RoundedCornerShape(14.dp)))
            AnimatedVisibility(shifted, enter=fadeIn(tween(500))+slideInHorizontally(tween(500)) { it/3 }) { Text("Track the trace.", color=Color.White, fontSize=21.sp, fontWeight=FontWeight.Medium, modifier=Modifier.padding(start=14.dp)) }
        }
    }
}

@Composable private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp, vertical=14.dp), verticalArrangement=Arrangement.spacedBy(14.dp), content=content) }
@Composable private fun Header(title:String, subtitle:String="") { Column { Text(title, fontSize=27.sp, fontWeight=FontWeight.SemiBold); if(subtitle.isNotBlank()) Text(subtitle, color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize=13.sp) } }
@Composable private fun Panel(title:String, trailing:@Composable RowScope.()->Unit={}, content:@Composable ColumnScope.()->Unit) { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface), shape=RoundedCornerShape(18.dp), modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Text(title, fontWeight=FontWeight.SemiBold, modifier=Modifier.weight(1f)); trailing() }; content() } } }
@Composable private fun Metric(label:String,value:String,modifier:Modifier=Modifier) { Card(modifier, colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface), shape=RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(value,fontSize=23.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary); Text(label,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun ActionButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,icon: androidx.compose.ui.graphics.vector.ImageVector?=null) { Button(onClick,modifier=modifier,shape=RoundedCornerShape(12.dp)) { if(icon!=null) Icon(icon,null,Modifier.size(17.dp)); Spacer(Modifier.width(if(icon!=null) 6.dp else 0.dp)); Text(text) } }
@Composable private fun AddDialog(title:String,fields:List<Pair<String,String>>,onDismiss:()->Unit,onSave:(List<String>)->Unit) {
    val values= remember(title) { fields.map { it.second }.toMutableStateList() }
    AlertDialog(onDismissRequest=onDismiss, title={Text(title)}, text={ Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) { fields.forEachIndexed { i,f -> val multi=f.first.contains("one per line",true)||f.first=="Notes"||f.first=="Content"; OutlinedTextField(value=values[i],onValueChange={values[i]=it},label={Text(f.first)},modifier=Modifier.fillMaxWidth(),singleLine=!multi, minLines=if(multi) 3 else 1) } } }, confirmButton={TextButton(onClick={onSave(values.toList())}) {Text("Save")}}, dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable private fun Empty(text:String) { Box(Modifier.fillMaxWidth().padding(20.dp),contentAlignment=Alignment.Center) { Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
private fun today()=LocalDate.now(ZoneId.of("Asia/Dhaka")).toString()
private fun formatMins(m:Int) = if(m<60) "${m}m" else "${m/60}h ${m%60}m" 

@Composable private fun DashboardScreen(s:WorkspaceState,p:PrefState,vm:LambdaViewModel,navigate:(Page)->Unit) {
    val today=today(); val todayTasks=s.tasks.filter{it.date==today}; val completed=todayTasks.count{it.done}; val studied=s.sessions.filter{it.date==today}.sumOf{it.minutes}+((s.dailyLogs.find{it.date==today}?.extraStudyHours?:0.0)*60).toInt()
    val syllabus=if(s.chapters.isEmpty()) 0f else s.chapters.count{it.lecturesTotal>0 && it.lecturesDone>=it.lecturesTotal}.toFloat()/s.chapters.size
    ScreenColumn {
        val hour=java.time.LocalTime.now(ZoneId.of("Asia/Dhaka")).hour; val greet=when{hour<12->"Good morning";hour<17->"Good afternoon";else->"Good evening"}
        Header(if(p.name.isBlank()) "$greet." else "$greet, ${p.name}.",today)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) { Metric("Tasks done","$completed/${todayTasks.size}",Modifier.weight(1f)); Metric("Study time",formatMins(studied),Modifier.weight(1f)); Metric("Syllabus", "${(syllabus*100f).toInt()}%",Modifier.weight(1f)) }
        Panel("Today's priorities", trailing={TextButton(onClick={navigate(Page.PLANNER)}){Text("Planner")}}) {
            var taskText by remember { mutableStateOf("") }
            Row(verticalAlignment=Alignment.CenterVertically) { OutlinedTextField(taskText,{taskText=it},Modifier.weight(1f),placeholder={Text("Add a task for today")},singleLine=true); IconButton(onClick={vm.addTask(taskText,today);taskText=""}){Icon(Icons.Default.Add,"Add task")} }
            if(todayTasks.isEmpty()) Empty("A clean slate. Add your first task.")
            todayTasks.take(6).forEach { t -> Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(t.done,{vm.toggleTask(t.id)}); Text(t.title,Modifier.weight(1f),color=if(t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface); IconButton(onClick={vm.deleteTask(t.id)}){Icon(Icons.Default.DeleteOutline,"Delete")} } }
        }
        Panel("Study shortcuts") { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { ActionButton("Start focus",{navigate(Page.FOCUS)},Modifier.weight(1f),Icons.Default.Timer); ActionButton("Academics",{navigate(Page.ACADEMICS)},Modifier.weight(1f),Icons.Default.MenuBook) } }
        Panel("Habits check-in",trailing={TextButton(onClick={navigate(Page.HABITS)} ){Text("All habits")}}) {
            if(s.habits.isEmpty()) Empty("No habits yet") else s.habits.take(5).forEach { h -> val done=s.habitLogs.any{it.habitId==h.id&&it.date==today}; Row(verticalAlignment=Alignment.CenterVertically) { Text(h.icon); Text(h.name,Modifier.weight(1f)); Checkbox(done,{vm.toggleHabit(h.id,today)}) } }
        }
        val deadlineItems=(s.tasks.filter{!it.done && (it.dueDate ?: it.date)!=null}.map{Triple(it.title,it.dueDate?:it.date!!,"Task")} + s.goals.filter{it.completedAt==null && it.deadline!=null}.map{Triple(it.title,it.deadline!!,"Goal")}).sortedBy{it.second}.take(5)
        Panel("Upcoming deadlines",trailing={TextButton(onClick={navigate(Page.PLANNER)}){Text("Planner")}}) { if(deadlineItems.isEmpty()) Empty("No upcoming deadlines") else deadlineItems.forEach { item ->
            val due=item.second; val hours=runCatching{java.time.Duration.between(java.time.Instant.now(),LocalDate.parse(due).atTime(23,59,59).atZone(ZoneId.of("Asia/Dhaka")).toInstant()).toHours()}.getOrDefault(0L)
            val urgency=when { hours<0 -> "${maxOf(1,(-hours/24))}d overdue"; hours<=24 -> if(hours==0L)"Due now" else "Due in ${hours}h"; hours<=48 -> "Due tomorrow (${hours}h)"; else -> "In ${(hours+23)/24} days" }
            val academic=listOf("physics","chem","math","bio","ict","bangla","english","exam","quiz","test","assignment","homework","lecture","revision","buet","medical","engineering","hsc","syllabus","slide","chapter","paper","solve","cq","mcq","study","class").any{item.first.contains(it,true)} || (item.third=="Goal" && s.goals.firstOrNull{it.title==item.first}?.category?.let{it.contains("acad",true)||it.contains("exam",true)||it.contains("study",true)}==true)
            Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.first,fontWeight=FontWeight.Medium);Text("${item.third} · $due",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}; Text(urgency,fontSize=11.sp,color=when{hours<0->MaterialTheme.colorScheme.error;hours<=24->MaterialTheme.colorScheme.tertiary;else->MaterialTheme.colorScheme.onSurfaceVariant}); if(academic) Icon(Icons.Default.School,"Academic deadline",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(16.dp))}
        } }
        Panel("Active goals",trailing={TextButton(onClick={navigate(Page.GOALS)}){Text("View all")}}) { if(s.goals.none{it.completedAt==null}) Empty("No active goals") else s.goals.filter{it.completedAt==null}.take(3).forEach { g -> val ms=s.milestones.filter{it.goalId==g.id}; val done=ms.count{it.done}; Text("${g.title}  ·  $done/${ms.size}"); LinearProgressIndicator(progress={if(ms.isEmpty()) 0f else done.toFloat()/ms.size},modifier=Modifier.fillMaxWidth()) } }
    }
}

@Composable private fun PlannerScreen(s:WorkspaceState,vm:LambdaViewModel) {
    var date by remember { mutableStateOf(today()) }; var add by remember { mutableStateOf(false) }; var addInbox by remember{mutableStateOf(false)}
    ScreenColumn { Header("Daily planner","Plan the day, keep the important work visible.")
        Row(verticalAlignment=Alignment.CenterVertically) { OutlinedTextField(date,{date=it},Modifier.weight(1f),label={Text("Date (YYYY-MM-DD)")},singleLine=true); IconButton(onClick={date=runCatching{LocalDate.parse(date).minusDays(1).toString()}.getOrDefault(today())}){Icon(Icons.Default.ChevronLeft,"Previous day")}; IconButton(onClick={date=runCatching{LocalDate.parse(date).plusDays(1).toString()}.getOrDefault(today())}){Icon(Icons.Default.ChevronRight,"Next day")} }
        ActionButton("Add task",{add=true},icon=Icons.Default.Add)
        val list=s.tasks.filter{it.date==date}; val inbox=s.tasks.filter{it.date==null && !it.done}; val overdue=s.tasks.filter{!it.done && (it.dueDate?:it.date)?.let{it<date}==true}
        Panel("${date} · ${list.count{it.done}}/${list.size} complete") { if(list.isEmpty()) Empty("Nothing planned for this day") else list.forEach { t -> TaskRow(t,vm) } }
        Panel("Overdue") { if(overdue.isEmpty()) Empty("Nothing overdue") else overdue.forEach{TaskRow(it,vm)} }
        Panel("Inbox",trailing={TextButton(onClick={addInbox=true}){Text("+ Add")}}) { if(inbox.isEmpty()) Empty("Inbox zero") else inbox.forEach{TaskRow(it,vm)} }
    }
    if(addInbox) AddDialog("Add to inbox",listOf("Task title" to "","Priority (1-4)" to "2"),{addInbox=false}){v->vm.addTask(v[0],null,v[1].toIntOrNull()?.coerceIn(1,4)?:2,null,null);addInbox=false}
    if(add) AddDialog("New task",listOf("Task title" to "","Priority (1-4)" to "2","Scheduled date (YYYY-MM-DD, blank = Inbox)" to date,"Due date (YYYY-MM-DD, optional)" to date,"Due time (HH:mm, optional)" to ""),{add=false}) { v-> vm.addTask(v[0],v[2].ifBlank{null},v[1].toIntOrNull()?.coerceIn(1,4)?:2,v[3].ifBlank{null},v[4].ifBlank{null});add=false }
}
@Composable private fun TaskRow(t:TaskEntity,vm:LambdaViewModel) {
    var editing by remember{mutableStateOf(false)}
    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(t.done,{vm.toggleTask(t.id)}); Column(Modifier.weight(1f).clickable{editing=true}) { Text(t.title,fontWeight=FontWeight.Medium); Text("P${t.priority}${t.dueDate?.let{" · due $it"}?:""}${t.dueTime?.let{" · $it"}?:""}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick={editing=true}){Icon(Icons.Default.Edit,"Edit task")}; IconButton(onClick={vm.deleteTask(t.id)}){Icon(Icons.Default.DeleteOutline,"Delete task")} }
    if(editing) AddDialog("Edit task",listOf("Task title" to t.title,"Priority (1-4)" to t.priority.toString(),"Scheduled date (YYYY-MM-DD, blank = Inbox)" to (t.date?:""),"Due date (YYYY-MM-DD, optional)" to (t.dueDate?:""),"Due time (HH:mm, optional)" to (t.dueTime?:"")),{editing=false}){v->vm.updateTask(t.id,v[0],v[1].toIntOrNull()?.coerceIn(1,4)?:2,v[2].ifBlank{null},v[3].ifBlank{null},v[4].ifBlank{null});editing=false}
}

@Composable private fun AcademicsScreen(s:WorkspaceState,vm:LambdaViewModel) {
    val context=LocalContext.current
    var addSubject by remember { mutableStateOf(false) }; var editColumnsFor by remember { mutableStateOf<GroupEntity?>(null) }; var editLinksFor by remember { mutableStateOf<ChapterEntity?>(null) }; var addGroupFor by remember { mutableStateOf<String?>(null) }; var addChapterFor by remember { mutableStateOf<String?>(null) }; var renameSubject by remember { mutableStateOf<SubjectEntity?>(null) }; var renameChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    val subjectProgress=s.subjects.map { sub -> val gs=s.groups.filter{it.subjectId==sub.id}; val items=gs.flatMap{g->s.chapters.filter{it.groupId==g.id}.map{it to g.columnsCsv.split(",")}}; if(items.isEmpty()) 0.0 else items.map{chapterProgress(it.first,it.second)}.average() }; val overall=if(subjectProgress.isEmpty())0 else (subjectProgress.average()*100).toInt(); val total=s.chapters.size; val completed=s.chapters.count{it.lecturesTotal>0&&it.lecturesDone>=it.lecturesTotal}
    ScreenColumn { Header("Academics","Subjects, papers, chapters and syllabus progress.")
        Panel("Overall syllabus · $overall%") { LinearProgressIndicator(progress={overall/100f},modifier=Modifier.fillMaxWidth()); Text("$completed of $total chapters marked complete",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        ActionButton("Add subject",{addSubject=true},icon=Icons.Default.Add)
        s.subjects.forEach { sub ->
            val groups=s.groups.filter{it.subjectId==sub.id}; val subCh=groups.flatMap{g->s.chapters.filter{it.groupId==g.id}}; val subPct=if(subCh.isEmpty())0 else (groups.flatMap{g->s.chapters.filter{it.groupId==g.id}.map{chapterProgress(it,g.columnsCsv.split(","))}}.average()*100).toInt();
            Panel("${sub.icon}  ${sub.name}  ·  ${subPct}%",trailing={IconButton(onClick={renameSubject=sub}){Icon(Icons.Default.Edit,"Rename subject")}; IconButton(onClick={vm.deleteSubject(sub.id)}){Icon(Icons.Default.DeleteOutline,"Delete subject")}}) {
                groups.forEach { g ->
                    Row(verticalAlignment=Alignment.CenterVertically) { Text(g.name,Modifier.weight(1f),fontWeight=FontWeight.SemiBold); TextButton(onClick={editColumnsFor=g}){Text("Columns")}; TextButton(onClick={addChapterFor=g.id}){Text("+ Chapter")}; IconButton(onClick={vm.deleteGroup(g.id)}){Icon(Icons.Default.DeleteOutline,"Delete paper")} }
                    s.chapters.filter{it.groupId==g.id}.forEach { c ->
                        var expanded by remember(c.id) { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f)).padding(10.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) { TextButton(onClick={renameChapter=c},modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp)){Text(c.name,fontSize=13.sp)}; Text("${c.lecturesDone}/${c.lecturesTotal} lectures",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant); IconButton(onClick={expanded=!expanded}){Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,"Progress")} }
                            if(expanded) {
                                var done by remember(c.id,c.lecturesDone) { mutableIntStateOf(c.lecturesDone) }; var all by remember(c.id,c.lecturesTotal) { mutableIntStateOf(c.lecturesTotal) }; var rev by remember(c.id,c.revision) { mutableIntStateOf(c.revision) }
                                var checks by remember(c.id,c.checksJson) { mutableStateOf(runCatching { JSONObject(c.checksJson) }.getOrDefault(JSONObject())) }
                                Text("Study stamps",fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                                val links=runCatching{org.json.JSONArray(c.slideLinksJson)}.getOrElse{org.json.JSONArray()}
                                if(links.length()>0) for(linkIndex in 0 until links.length()) { val link=links.optString(linkIndex); TextButton(onClick={runCatching{val uri=android.net.Uri.parse(link);val intent=android.content.Intent(android.content.Intent.ACTION_VIEW,uri).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(intent)}}){Text("Open resource ${linkIndex+1}")} }
                                TextButton(onClick={editLinksFor=c}){Text("Manage Drive / slide links") }
                                g.columnsCsv.split(",").forEach { col -> Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(checks.optBoolean(col),{ checks=JSONObject(checks.toString()).put(col,it); vm.updateChapterChecks(c.id,checks.toString()) }); Text(col,fontSize=12.sp) } }
                                Row(verticalAlignment=Alignment.CenterVertically) { Text("Lectures",Modifier.weight(1f),fontSize=12.sp); OutlinedTextField(done.toString(),{done=it.toIntOrNull()?.coerceAtLeast(0)?:0},Modifier.width(72.dp),singleLine=true); Text("/",Modifier.padding(horizontal=5.dp)); OutlinedTextField(all.toString(),{all=it.toIntOrNull()?.coerceAtLeast(0)?:0},Modifier.width(72.dp),singleLine=true) }
                                Row(verticalAlignment=Alignment.CenterVertically) { Text("Revision",Modifier.weight(1f),fontSize=12.sp); (0..3).forEach { n -> FilterChip(selected=rev==n,onClick={rev=n},label={Text("$n")}) } }
                                Row { TextButton(onClick={vm.updateChapter(c,done,all,rev)}){Text("Save progress")}; TextButton(onClick={vm.deleteChapter(c.id)}){Text("Delete")} }
                                LinearProgressIndicator(progress={if(all==0)0f else (done.toFloat()/all).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                TextButton(onClick={addGroupFor=sub.id}) { Text("+ Add paper / group") }
            }
        }
    }
    editColumnsFor?.let { item -> AddDialog("Edit study-stamp columns",listOf("Columns (comma separated)" to item.columnsCsv),{editColumnsFor=null}) { values -> val cols=values[0].split(",").map{it.trim()}.filter{it.isNotBlank()}.distinct(); if(cols.isNotEmpty())vm.updateGroupColumns(item.id,cols.joinToString(","));editColumnsFor=null } }
    editLinksFor?.let { item -> val existing=runCatching{org.json.JSONArray(item.slideLinksJson)}.getOrElse{org.json.JSONArray()}; val lines=(0 until existing.length()).joinToString("\n"){existing.optString(it)}; AddDialog("Drive / slide links",listOf("URLs (one per line)" to lines),{editLinksFor=null}) { values -> val urls=values[0].lines().map{it.trim()}.filter{it.startsWith("https://")||it.startsWith("http://")};vm.updateChapterLinks(item.id,org.json.JSONArray(urls).toString());editLinksFor=null } }
    if(addSubject) AddDialog("Add subject",listOf("Subject name" to "","Icon emoji" to "📘"),{addSubject=false}){vm.addSubject(it[0],it[1].ifBlank{"📘"});addSubject=false}
    addGroupFor?.let { id->AddDialog("Add paper",listOf("Paper / group name" to ""),{addGroupFor=null}){vm.addGroup(id,it[0]);addGroupFor=null} }
    addChapterFor?.let { id->AddDialog("Add chapter",listOf("Chapter name" to ""),{addChapterFor=null}){vm.addChapter(id,it[0]);addChapterFor=null} }
    renameSubject?.let { item->AddDialog("Rename subject",listOf("Subject name" to item.name),{renameSubject=null}){vm.renameSubject(item.id,it[0]);renameSubject=null} }
    renameChapter?.let { item->AddDialog("Rename chapter",listOf("Chapter name" to item.name),{renameChapter=null}){vm.renameChapter(item.id,it[0]);renameChapter=null} }
}

@Composable private fun MoreScreen(navigate:(Page)->Unit) { ScreenColumn { Header("Your workspace","More tools for the daily loop."); listOf(Page.HABITS,Page.LOG,Page.ANALYTICS,Page.NOTES,Page.GOALS,Page.SETTINGS).forEach { p -> Card(onClick={navigate(p)},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) { Icon(p.icon,null,tint=MaterialTheme.colorScheme.primary); Text(p.label,Modifier.weight(1f).padding(start=14.dp),fontWeight=FontWeight.Medium); Icon(Icons.Default.ChevronRight,null) } } } } }

@Composable private fun HabitsScreen(s:WorkspaceState,vm:LambdaViewModel) {
    var add by remember{mutableStateOf(false)}; var name by remember{mutableStateOf("")}; var icon by remember{mutableStateOf("⚡")}; var color by remember{mutableStateOf("#F0BC68")}; val date=today()
    ScreenColumn { Header("Habits","Daily non-negotiables. Consistency beats intensity."); ActionButton("Create habit",{add=true;name="";icon="⚡";color="#F0BC68"},icon=Icons.Default.Add)
        if(s.habits.isEmpty()) Empty("No habits created yet") else s.habits.forEach { h ->
            val checked=s.habitLogs.any{it.habitId==h.id&&it.date==date}; val history=s.habitLogs.filter{it.habitId==h.id&&it.completed}.sortedByDescending{it.date}
            val streak=habitStreak(history.mapNotNull{runCatching{LocalDate.parse(it.date)}.getOrNull()}.toSet(),LocalDate.now(ZoneId.of("Asia/Dhaka")))
            val thirty=history.count{runCatching{LocalDate.parse(it.date)>=LocalDate.now(ZoneId.of("Asia/Dhaka")).minusDays(29)}.getOrDefault(false)}
            Panel("${h.icon}  ${h.name}",trailing={IconButton(onClick={vm.deleteHabit(h.id)}){Icon(Icons.Default.DeleteOutline,"Delete habit")}}) {
                Row(verticalAlignment=Alignment.CenterVertically) { Text("Today",Modifier.weight(1f)); Checkbox(checked,{vm.toggleHabit(h.id,date)}) }
                Text("${streak.first} day streak · longest ${streak.second} · ${history.size} logged days",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress={thirty/30f},Modifier.fillMaxWidth(),color=runCatching{Color(android.graphics.Color.parseColor(h.color))}.getOrDefault(MaterialTheme.colorScheme.primary))
                Text("30-day consistency · $thirty/30 days",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if(add) AlertDialog(onDismissRequest={add=false},title={Text("Create new habit")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedTextField(name,{name=it},label={Text("Habit name")},singleLine=true); OutlinedTextField(icon,{icon=it},label={Text("Icon emoji")},singleLine=true); OutlinedTextField(color,{color=it},label={Text("Color (hex)")},singleLine=true) }},confirmButton={TextButton(onClick={if(name.isNotBlank()){vm.addHabit(name,icon.ifBlank{"⚡"},if(color.matches(Regex("#[0-9A-Fa-f]{6}")))color else "#F0BC68");add=false}}){Text("Save")}},dismissButton={TextButton(onClick={add=false}){Text("Cancel")}})
}

@Composable private fun DailyLogScreen(s:WorkspaceState,vm:LambdaViewModel) {
    var date by remember{mutableStateOf(today())}; val existing=s.dailyLogs.find{it.date==date}; var sleep by remember(date,existing){mutableStateOf(existing?.sleepHours?.toString()?:"")}; var screen by remember(date,existing){mutableStateOf(existing?.screenHours?.toString()?:"")}; var extra by remember(date,existing){mutableStateOf(existing?.extraStudyHours?.toString()?:"0")}; var mood by remember(date,existing){mutableIntStateOf(existing?.mood?:3)}; var notes by remember(date,existing){mutableStateOf(existing?.notes?:"")}
    ScreenColumn { Header("Daily log","Sleep, screen time, extra study and reflection."); OutlinedTextField(date,{date=it},Modifier.fillMaxWidth(),label={Text("Date (YYYY-MM-DD)")},singleLine=true)
        Panel("Log a day") { OutlinedTextField(sleep,{sleep=it},Modifier.fillMaxWidth(),label={Text("Sleep hours")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal)); OutlinedTextField(screen,{screen=it},Modifier.fillMaxWidth(),label={Text("Screen time hours")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal)); OutlinedTextField(extra,{extra=it},Modifier.fillMaxWidth(),label={Text("Extra study hours")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal)); Text("Daily energy & focus level · $mood/5"); Slider(mood.toFloat(),{mood=it.toInt().coerceIn(1,5)},valueRange=1f..5f,steps=3); OutlinedTextField(notes,{notes=it},Modifier.fillMaxWidth(),label={Text("Daily reflection / notes")},minLines=3); ActionButton("Save daily log",{vm.saveDailyLog(date,sleep.toDoubleOrNull(),screen.toDoubleOrNull(),extra.toDoubleOrNull()?:0.0,mood,notes)},Modifier.fillMaxWidth()) }
        Panel("Recent days") { if(s.dailyLogs.isEmpty()) Empty("Nothing logged yet") else s.dailyLogs.take(14).forEach { l -> Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(l.date,fontWeight=FontWeight.Medium);Text("Sleep ${l.sleepHours?:"—"}h · Screen ${l.screenHours?:"—"}h · Extra ${l.extraStudyHours}h",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}; TextButton(onClick={date=l.date}){Text("Edit")}; IconButton(onClick={vm.deleteDailyLog(l.date)}){Icon(Icons.Default.DeleteOutline,"Delete log")} } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FocusScreen(s:WorkspaceState,p:PrefState,vm:LambdaViewModel) {
    val focusContext=LocalContext.current
    var mode by remember{mutableStateOf("Pomodoro")}; var title by remember{mutableStateOf("")}; var running by remember{mutableStateOf(false)}
    var phase by remember{mutableStateOf("Focus")}; var remaining by remember{mutableLongStateOf(p.focus*60_000L)}; var elapsed by remember{mutableLongStateOf(0L)}
    var subject by remember{mutableStateOf("")}; var workNotes by remember{mutableStateOf("")}; var laps by remember{mutableStateOf(listOf<Long>())}; var completedFocus by remember{mutableIntStateOf(0)}
    var preset by remember{mutableStateOf("Custom")}; var selectedTaskId by remember{mutableStateOf("")}; var taskMenu by remember{mutableStateOf(false)}
    var ambient by remember{mutableStateOf("off")}; var ambientVolume by remember{mutableFloatStateOf(.35f)}; var fullScreen by remember{mutableStateOf(false)}
    DisposableEffect(Unit) { onDispose { com.lambda.app.data.AmbientAudio.stop() } }
    LaunchedEffect(ambient) { com.lambda.app.data.AmbientAudio.start(ambient,ambientVolume) }
    LaunchedEffect(ambientVolume) { com.lambda.app.data.AmbientAudio.setVolume(ambientVolume) }
    val linkedTask=s.tasks.firstOrNull{it.id==selectedTaskId}
    val focusDuration=when(preset){"25"->25;"50"->50;else->p.focus}
    val phaseMinutes=when(phase){"Short break"->p.short;"Long break"->p.long;else->focusDuration}
    LaunchedEffect(running,mode,phase) {
        while(running) {
            delay(1000)
            if(mode=="Pomodoro") {
                remaining=(remaining-1000).coerceAtLeast(0)
                if(remaining==0L) {
                    if(phase=="Focus") {
                        vm.addSession("pomodoro",linkedTask?.title ?: title.ifBlank{"Focus sprint"},focusDuration,subject.ifBlank{null})
                        completedFocus++
                        vm.chimeFocusComplete()
                        android.widget.Toast.makeText(focusContext,"Focus sprint complete. Take a breather!",android.widget.Toast.LENGTH_SHORT).show()
                        phase=if(completedFocus%p.longBreakEvery==0) "Long break" else "Short break"
                        remaining=(if(completedFocus%p.longBreakEvery==0)p.long else p.short)*60_000L
                    } else { phase="Focus"; remaining=focusDuration*60_000L }
                    running=false
                }
            } else elapsed+=1000
        }
    }
    val shown=if(mode=="Pomodoro") remaining else elapsed
    val time="%02d:%02d".format(Locale.US,shown/60000,(shown/1000)%60)
    ScreenColumn { Header("Focus","A quiet place for deep work.")
        Panel("Session type") { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { listOf("Pomodoro","Stopwatch").forEachIndexed { i,v -> SegmentedButton(selected=mode==v,onClick={running=false;mode=v;phase="Focus";remaining=focusDuration*60000L;elapsed=0; laps=emptyList()},shape=SegmentedButtonDefaults.itemShape(i,2)){Text(v)} } } }
        if(mode=="Pomodoro") Panel("Quick presets") { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("25" to 25,"50" to 50,"Custom" to p.focus).forEach { (label,mins) -> FilterChip(selected=preset==label,onClick={running=false;preset=label;phase="Focus";remaining=mins*60_000L},label={Text(if(label=="Custom")"${mins} min" else "$label min")}) } } }
        Panel("What are you working on?") {
            OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),placeholder={Text("e.g. Higher Math calculus practice")},singleLine=true)
            var subjectExpanded by remember{mutableStateOf(false)}
            ExposedDropdownMenuBox(subjectExpanded,{subjectExpanded=it}) {
                OutlinedTextField(s.subjects.find{it.id==subject}?.name?:"",{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,label={Text("Subject (optional)")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded)})
                ExposedDropdownMenu(subjectExpanded,{subjectExpanded=false}) { DropdownMenuItem(text={Text("No subject")},onClick={subject="";subjectExpanded=false}); s.subjects.forEach { DropdownMenuItem(text={Text(it.name)},onClick={subject=it.id;subjectExpanded=false}) } }
            }
            ExposedDropdownMenuBox(taskMenu,{taskMenu=it}) {
                OutlinedTextField(linkedTask?.title ?: "",{},Modifier.menuAnchor().fillMaxWidth(),readOnly=true,label={Text("Link planner task (optional)")},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(taskMenu)})
                ExposedDropdownMenu(taskMenu,{taskMenu=false}) { DropdownMenuItem(text={Text("No linked task")},onClick={selectedTaskId="";taskMenu=false}); s.tasks.filter{!it.done && (it.date==today() || it.dueDate==today())}.take(40).forEach { task -> DropdownMenuItem(text={Text(task.title)},onClick={selectedTaskId=task.id;title=task.title;taskMenu=false}) } }
            }
            if(mode=="Stopwatch") OutlinedTextField(workNotes,{workNotes=it},Modifier.fillMaxWidth(),label={Text("Session scratchpad / notes")},minLines=3)
        }
        Panel(if(mode=="Pomodoro") "$phase timer" else "Study stopwatch") {
            Box(Modifier.fillMaxWidth().padding(vertical=22.dp),contentAlignment=Alignment.Center) { Text(time,fontSize=58.sp,fontWeight=FontWeight.Light,color=MaterialTheme.colorScheme.primary) }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                ActionButton(if(running)"Pause" else "Start",{running=!running},Modifier.weight(1f),if(running)Icons.Default.Pause else Icons.Default.PlayArrow)
                OutlinedButton(onClick={if(mode=="Stopwatch"){running=false;if(elapsed>0){vm.addSession("work",title.ifBlank{"Work session"},(elapsed/60000).toInt().coerceAtLeast(1),subject.ifBlank{null},workNotes,org.json.JSONArray(laps.mapIndexed{i,v->org.json.JSONObject().put("time",v).put("label","Lap ${i+1}")}).toString());elapsed=0;workNotes="";laps=emptyList()}}else{running=false;remaining=phaseMinutes*60000L}},modifier=Modifier.weight(1f)){Text(if(mode=="Stopwatch")"Stop & save" else "Reset")}
            }
            if(mode=="Pomodoro") {
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("Focus","Short break","Long break").forEach { item -> FilterChip(selected=phase==item,onClick={running=false;phase=item;remaining=(when(item){"Short break"->p.short;"Long break"->p.long;else->focusDuration})*60000L},label={Text(item)}) } }
                TextButton(onClick={running=false;val next=if(phase=="Focus")"Short break" else "Focus";phase=next;remaining=(if(next=="Short break")p.short else focusDuration)*60000L}){Text("Skip phase")}
            } else {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={laps=laps+elapsed}){Text("Record lap")}; Text("${laps.size} laps",Modifier.align(Alignment.CenterVertically),color=MaterialTheme.colorScheme.onSurfaceVariant) }
                laps.forEachIndexed { i,v -> Text("Lap ${i+1} · ${formatMins((v/60000).toInt())}",fontSize=12.sp) }
            }
        }
        Panel("Ambient focus sound") {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("off" to "Off","brown" to "Brown","pink" to "Pink","binaural" to "Binaural").forEach { (key,label) -> FilterChip(selected=ambient==key,onClick={ambient=key},label={Text(label)}) } }
            if(ambient!="off") { Text("Volume · ${(ambientVolume*100).toInt()}%"); Slider(ambientVolume,{ambientVolume=it},valueRange=0f..0.8f) }
        }
        OutlinedButton(onClick={fullScreen=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Fullscreen,null);Spacer(Modifier.width(6.dp));Text("Enter full-screen focus")}
        if(completedFocus>0) Panel("Momentum") { Text("✨ $completedFocus focus sprint${if(completedFocus==1)"" else "s"} completed this session. Keep the streak going.",color=MaterialTheme.colorScheme.primary) }
        Panel("Today's focus sprints") { val sessions=s.sessions.filter{it.date==today()}; Text("${sessions.count{it.kind=="pomodoro"}} sprints · ${formatMins(sessions.sumOf{it.minutes})} total focus"); sessions.take(5).forEach{Text("${it.title} · ${formatMins(it.minutes)}${runCatching{org.json.JSONArray(it.lapsJson).length()}.getOrDefault(0).let{count->if(count>0)" · $count laps" else ""}}",fontSize=12.sp)} }
        Panel("Durations (minutes)") { Text("Focus ${p.focus} · Short break ${p.short} · Long break ${p.long}",color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if(fullScreen) {
        androidx.activity.compose.BackHandler { fullScreen=false }
        androidx.compose.ui.window.Dialog(onDismissRequest={fullScreen=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().padding(24.dp).onPreviewKeyEvent { event ->
                    if(event.type==androidx.compose.ui.input.key.KeyEventType.KeyDown) when(event.key) {
                        androidx.compose.ui.input.key.Key.Spacebar -> { running=!running; true }
                        androidx.compose.ui.input.key.Key.Escape -> { fullScreen=false; true }
                        else -> false
                    } else false
                },verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("LAMBDA · FOCUS",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Text(linkedTask?.title ?: title.ifBlank{"Deep work"},style=MaterialTheme.typography.titleLarge)
                    Text(time,fontSize=76.sp,fontWeight=FontWeight.Light,color=MaterialTheme.colorScheme.primary)
                    Text(if(mode=="Pomodoro")phase else "Stopwatch",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { ActionButton(if(running)"Pause" else "Start",{running=!running}); OutlinedButton(onClick={running=false;fullScreen=false}){Text("Exit focus")} }
                    Spacer(Modifier.height(20.dp)); Text("Track the trace.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable private fun AnalyticsScreen(s:WorkspaceState,p:PrefState) {
    var range by remember{mutableIntStateOf(14)}; val dhakaToday=LocalDate.now(ZoneId.of("Asia/Dhaka")); val start=dhakaToday.minusDays((range-1).toLong()); val sessions=s.sessions.filter{runCatching{val d=LocalDate.parse(it.date);d>=start&&d<=dhakaToday}.getOrDefault(false)}; val logsInRange=s.dailyLogs.filter{runCatching{val d=LocalDate.parse(it.date);d>=start&&d<=dhakaToday}.getOrDefault(false)}; val extraMinutes=logsInRange.sumOf{(it.extraStudyHours*60).toInt()}; val total=sessions.sumOf{it.minutes}+extraMinutes; val goalDays=(0 until range).count { n -> val d=dhakaToday.minusDays(n.toLong()).toString(); sessions.filter{it.date==d}.sumOf{it.minutes}+(s.dailyLogs.find{it.date==d}?.extraStudyHours?.times(60)?.toInt()?:0)>=p.goal*60 }
    ScreenColumn { Header("Analytics","Every metric is calculated from your local records."); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ listOf(7,14,30,90).forEach{ FilterChip(range==it,{range=it},label={Text("${it}d")}) } }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Metric("Studied",formatMins(total),Modifier.weight(1f)); Metric("Goal days","$goalDays/$range",Modifier.weight(1f)) }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Metric("Sessions",sessions.size.toString(),Modifier.weight(1f)); Metric("Avg / day",String.format(Locale.US,"%.1fh",total.toDouble()/range/60),Modifier.weight(1f)) }
        val completedInRange=s.tasks.count{it.done && it.completedAt?.let{stamp->runCatching{val d=java.time.Instant.ofEpochMilli(stamp).atZone(ZoneId.of("Asia/Dhaka")).toLocalDate();d>=start && d<=dhakaToday}.getOrDefault(false)}==true}
        val avgSleep=logsInRange.mapNotNull{it.sleepHours}.averageOrNull(); val avgScreen=logsInRange.mapNotNull{it.screenHours}.averageOrNull()
        val avgHabitPct=if(s.habits.isEmpty())0 else (0 until range).map{n->val day=dhakaToday.minusDays(n.toLong()).toString();s.habitLogs.count{it.completed&&it.date==day}*100/s.habits.size}.average().toInt()
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("Tasks done",completedInRange.toString(),Modifier.weight(1f));Metric("Habits", "$avgHabitPct%",Modifier.weight(1f))}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("Avg sleep",avgSleep?.let{String.format(Locale.US,"%.1fh",it)}?:"—",Modifier.weight(1f));Metric("Avg screen",avgScreen?.let{String.format(Locale.US,"%.1fh",it)}?:"—",Modifier.weight(1f))}
        Panel("Study hours by day") { if(sessions.isEmpty() && logsInRange.none{it.extraStudyHours>0}) Empty("No study records in this range") else (0 until range).reversed().forEach { n -> val d=dhakaToday.minusDays(n.toLong()); val mins=sessions.filter{it.date==d.toString()}.sumOf{it.minutes}+(s.dailyLogs.find{it.date==d.toString()}?.extraStudyHours?.times(60)?.toInt()?:0); Row(verticalAlignment=Alignment.CenterVertically){Text(d.format(DateTimeFormatter.ofPattern("EEE d")),Modifier.width(62.dp),fontSize=11.sp); LinearProgressIndicator(progress={(mins.toFloat()/(p.goal*60)).coerceIn(0f,1f)},Modifier.weight(1f).height(8.dp)); Text(formatMins(mins),Modifier.width(54.dp),fontSize=11.sp)} } }
        Panel("Sleep & screen time by day") { (0 until range).reversed().forEach { n -> val d=dhakaToday.minusDays(n.toLong()); val log=s.dailyLogs.find{it.date==d.toString()}; if(log?.sleepHours!=null || log?.screenHours!=null) Text("${d.format(DateTimeFormatter.ofPattern("EEE d"))} · Sleep ${log?.sleepHours?.let{"${it}h"}?:"—"} · Screen ${log?.screenHours?.let{"${it}h"}?:"—"}",fontSize=12.sp) } }
        Panel("Completed tasks by day") { (0 until range).reversed().forEach { n -> val d=dhakaToday.minusDays(n.toLong()); val count=s.tasks.count{it.done&&it.completedAt?.let{stamp->runCatching{java.time.Instant.ofEpochMilli(stamp).atZone(ZoneId.of("Asia/Dhaka")).toLocalDate()==d}.getOrDefault(false)}==true}; Row(verticalAlignment=Alignment.CenterVertically){Text(d.format(DateTimeFormatter.ofPattern("EEE d")),Modifier.width(62.dp),fontSize=11.sp);LinearProgressIndicator(progress={(count/5f).coerceIn(0f,1f)},Modifier.weight(1f).height(7.dp));Text(count.toString(),Modifier.width(24.dp),fontSize=11.sp)} } }
        Panel("Timed study by subject") { sessions.groupBy{it.subjectId?.let{id->s.subjects.find{it.id==id}?.name}?:"General"}.forEach{(name,items)->Text("$name · ${formatMins(items.sumOf{it.minutes})}")} }
        Panel("Habit completion trend") { if(s.habits.isEmpty()) Empty("Create habits to see consistency") else (0 until range).reversed().forEach { n -> val d=dhakaToday.minusDays(n.toLong()); val count=s.habitLogs.count{it.completed&&it.date==d.toString()}; val pct=count*100/s.habits.size; Row(verticalAlignment=Alignment.CenterVertically){Text(d.format(DateTimeFormatter.ofPattern("EEE d")),Modifier.width(62.dp),fontSize=11.sp);LinearProgressIndicator(progress={pct/100f},Modifier.weight(1f).height(7.dp));Text("$pct%",Modifier.width(42.dp),fontSize=11.sp)} } }
    }
}
private fun List<Double>.averageOrNull():Double?=if(isEmpty())null else average()

@Composable private fun NotesScreen(s:WorkspaceState,vm:LambdaViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var search by remember{mutableStateOf("")}; var editing by remember{mutableStateOf<NoteEntity?>(null)}; var add by remember{mutableStateOf(false)}
    var title by remember(editing?.id,add){mutableStateOf(editing?.title.orEmpty())}
    var content by remember(editing?.id,add){mutableStateOf(editing?.content.orEmpty())}
    var tags by remember(editing?.id,add){mutableStateOf(editing?.tagsCsv.orEmpty())}
    var pinned by remember(editing?.id,add){mutableStateOf(editing?.pinned?:false)}
    var attachmentJson by remember(editing?.id,add){mutableStateOf(editing?.attachmentsJson?:"[]")}
    var exportTarget by remember{mutableStateOf("")}
    val pickFiles = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val array = runCatching { org.json.JSONArray(attachmentJson) }.getOrElse { org.json.JSONArray() }
            uris.forEach { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val name = runCatching { context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { c -> if(c.moveToFirst()) c.getString(0) else null } }.getOrNull() ?: (uri.lastPathSegment ?: "Attachment")
                val size = runCatching { context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c -> if(c.moveToFirst()) c.getLong(0) else 0L } ?: 0L }.getOrDefault(0L)
                array.put(org.json.JSONObject().put("uri",uri.toString()).put("name",name).put("size",size))
            }
            attachmentJson = array.toString()
        }
    }
    val markdownExport = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if(uri!=null) scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(exportTarget.toByteArray(Charsets.UTF_8)) } ?: error("Cannot write file") } }
    }
    ScreenColumn {
        Header("Notes","Search, organise and attach study material locally.")
        OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),placeholder={Text("Search notes and tags…")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        ActionButton("New note",{editing=null;add=true;title="";content="";tags="";pinned=false;attachmentJson="[]"},icon=Icons.Default.Add)
        val filtered=s.notes.filter{it.title.contains(search,true)||it.content.contains(search,true)||it.tagsCsv.contains(search,true)}
        if(filtered.isEmpty()) Empty("No matching notes") else filtered.forEach { n ->
            val attachments=runCatching{org.json.JSONArray(n.attachmentsJson).length()}.getOrDefault(0)
            Panel(if(n.pinned)"📌 ${n.title}" else n.title,trailing={IconButton(onClick={vm.deleteNote(n.id)}){Icon(Icons.Default.DeleteOutline,"Delete note")}}) {
                Text(n.content.take(180),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)
                if(n.tagsCsv.isNotBlank())Text(n.tagsCsv,fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
                if(attachments>0) Text("$attachments attachment(s)",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick={editing=n;add=false;title=n.title;content=n.content;tags=n.tagsCsv;pinned=n.pinned;attachmentJson=n.attachmentsJson}){Text("Open / edit")}
                    TextButton(onClick={clipboard.setText(androidx.compose.ui.text.AnnotatedString("# ${n.title}\n\n${n.content}\n\nTags: ${n.tagsCsv}"))}){Text("Copy Markdown")}
                    TextButton(onClick={exportTarget="# ${n.title}\n\n${n.content}\n\nTags: ${n.tagsCsv}";markdownExport.launch((n.title.ifBlank{"lambda-note"}.replace(Regex("[^A-Za-z0-9_-]"),"_").take(48))+".md")}){Text("Export")}
                }
            }
        }
    }
    if(add||editing!=null) {
        AlertDialog(onDismissRequest={add=false;editing=null},title={Text(if(editing==null)"New note" else "Edit note")},
            text={Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Title")},singleLine=true)
                OutlinedTextField(content,{content=it},Modifier.fillMaxWidth(),label={Text("Markdown content")},minLines=5)
                OutlinedTextField(tags,{tags=it},Modifier.fillMaxWidth(),label={Text("Tags (comma separated)")},singleLine=true)
                Row(verticalAlignment=Alignment.CenterVertically){Text("Pinned",Modifier.weight(1f));Switch(pinned,{pinned=it})}
                Text("Attachments",fontWeight=FontWeight.SemiBold)
                val arr=runCatching{org.json.JSONArray(attachmentJson)}.getOrElse{org.json.JSONArray()}
                for(i in 0 until arr.length()) {
                    val item=arr.optJSONObject(i) ?: continue
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(item.optString("name","File"),Modifier.weight(1f),fontSize=12.sp,maxLines=2)
                        TextButton(onClick={runCatching {
                            val storedUri=item.optString("uri")
                            val uri=if(storedUri.isNotBlank()) android.net.Uri.parse(storedUri) else {
                                val dataUrl=item.optString("dataUrl"); require(dataUrl.startsWith("data:")) { "Attachment data is unavailable" }
                                val meta=dataUrl.substringBefore(","); val raw=dataUrl.substringAfter(","); val bytes=if(meta.contains(";base64")) android.util.Base64.decode(raw,android.util.Base64.DEFAULT) else java.net.URLDecoder.decode(raw,"UTF-8").toByteArray()
                                val dir=java.io.File(context.cacheDir,"note-attachments").apply{mkdirs()}; val safe=item.optString("name","attachment").replace(Regex("[^A-Za-z0-9._-]"),"_"); val file=java.io.File(dir,"${System.currentTimeMillis()}_$safe");file.writeBytes(bytes)
                                androidx.core.content.FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
                            }
                            val intent=android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri,item.optString("type").ifBlank{context.contentResolver.getType(uri)?:"*/*"}).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);context.startActivity(intent)
                        }.onFailure { android.widget.Toast.makeText(context,it.message ?: "Unable to open attachment",android.widget.Toast.LENGTH_SHORT).show() } }) { Text("Open") }
                        TextButton(onClick={val updated=org.json.JSONArray();for(j in 0 until arr.length())if(j!=i)updated.put(arr.get(j));attachmentJson=updated.toString()}){Text("Remove")}
                    }
                }
                OutlinedButton(onClick={pickFiles.launch(arrayOf("*/*"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.AttachFile,null);Spacer(Modifier.width(6.dp));Text("Attach files")}
                Text("Files remain in the location you selected; Lambda stores their local document references.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }},
            confirmButton={TextButton(onClick={if(title.isNotBlank()){vm.saveNote(editing?.id,title,content,tags,pinned,attachmentJson);add=false;editing=null}}){Text("Save")}},
            dismissButton={TextButton(onClick={add=false;editing=null}){Text("Cancel")}})
    }
}

@Composable private fun GoalsScreen(s:WorkspaceState,vm:LambdaViewModel) {
    var add by remember{mutableStateOf(false)}
    ScreenColumn { Header("Goals","Turn intentions into measurable milestones."); ActionButton("Create new goal",{add=true},icon=Icons.Default.Add)
        if(s.goals.isEmpty()) Empty("No goals yet") else s.goals.forEach { g -> val ms=s.milestones.filter{it.goalId==g.id}; val done=ms.count{it.done}; Panel(g.title,trailing={IconButton(onClick={vm.deleteGoal(g.id)}){Icon(Icons.Default.DeleteOutline,"Delete goal")}}) { Text("${g.category}${g.deadline?.let{" · Due $it"}?:""}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant); Text("${if(ms.isEmpty())0 else done*100/ms.size}% complete"); LinearProgressIndicator(progress={if(ms.isEmpty())0f else done.toFloat()/ms.size},Modifier.fillMaxWidth()); ms.forEach{m->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(m.done,{vm.toggleMilestone(m.id)});Text(m.title,Modifier.weight(1f),fontSize=13.sp)}} } }
    }
    if(add) AddDialog("Create new goal",listOf("Goal title" to "","Category (Academic / Exam / Habit / Life)" to "Academic","Target deadline (YYYY-MM-DD)" to "","Milestones (one per line)" to ""),{add=false}){vm.addGoal(it[0],it[1],it[2].ifBlank{null},it[3].lines());add=false}
}

@Composable private fun SettingsScreen(s:WorkspaceState,p:PrefState,vm:LambdaViewModel) {
    val context= LocalContext.current
    val scope= rememberCoroutineScope()
    var backupMessage by remember { mutableStateOf("") }
    val export= androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) { scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.use{it.write(vm.exportJson().toByteArray())} ?: error("Could not open destination") }.onSuccess { backupMessage="Backup exported successfully." }.onFailure { backupMessage="Export failed: ${it.message}" } } } }
    val importLauncher= androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) { scope.launch { runCatching { val json=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: error("Could not read backup"); vm.importJson(json) }.onSuccess { backupMessage="Import complete. Existing matching records were updated; new records were added." }.onFailure { backupMessage="Import failed: ${it.message}" } } } }
    ScreenColumn { Header("Settings & data","Local-first storage. No account or cloud connection.")
        Panel("Personalise") { var name by remember(p.name){mutableStateOf(p.name)}; OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Your name")},singleLine=true); ActionButton("Save name",{vm.setName(name)},Modifier.fillMaxWidth()); Text("App name · Lambda",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        Panel("Appearance") { Row(verticalAlignment=Alignment.CenterVertically){Text("Theme",Modifier.weight(1f)); FilterChip(p.theme=="dark",{vm.setTheme("dark")},label={Text("Dark")}); Spacer(Modifier.width(6.dp)); FilterChip(p.theme=="light",{vm.setTheme("light")},label={Text("Light")})} }
        Panel("Focus & goals") { Text("Daily study goal · ${p.goal} hours"); Slider(p.goal.toFloat(),{vm.setGoal(it.toInt())},valueRange=1f..12f,steps=10); Text("Focus ${p.focus} min · Short break ${p.short} min · Long break ${p.long} min"); var f by remember(p.focus){mutableIntStateOf(p.focus)}; var sh by remember(p.short){mutableIntStateOf(p.short)}; var l by remember(p.long){mutableIntStateOf(p.long)}; var every by remember(p.longBreakEvery){mutableIntStateOf(p.longBreakEvery)}; Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ OutlinedTextField(f.toString(),{f=it.toIntOrNull()?:f},Modifier.weight(1f),label={Text("Focus")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)); OutlinedTextField(sh.toString(),{sh=it.toIntOrNull()?:sh},Modifier.weight(1f),label={Text("Short")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)); OutlinedTextField(l.toString(),{l=it.toIntOrNull()?:l},Modifier.weight(1f),label={Text("Long")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)); OutlinedTextField(every.toString(),{every=it.toIntOrNull()?:every},Modifier.weight(1f),label={Text("Every")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)) }; ActionButton("Save durations",{vm.setTimer(f,sh,l,every)},Modifier.fillMaxWidth()); Row(verticalAlignment=Alignment.CenterVertically){Text("Audio chime",Modifier.weight(1f));Switch(p.sound,{vm.setSound(it)})} }
        Panel("On this device") { Text("${s.tasks.size} tasks · ${s.subjects.size} subjects · ${s.sessions.size} sessions · ${s.habits.size} habits",color=MaterialTheme.colorScheme.onSurfaceVariant); ActionButton("Export workspace backup (JSON)",{export.launch("lambda-backup-${today()}.json")},Modifier.fillMaxWidth(),Icons.Default.FileDownload); OutlinedButton(onClick={importLauncher.launch(arrayOf("application/json","text/json","text/plain","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.FileUpload,null); Spacer(Modifier.width(8.dp)); Text("Restore from backup")}; if(backupMessage.isNotBlank()) Text(backupMessage,fontSize=12.sp,color=MaterialTheme.colorScheme.primary); Text("Restore accepts Lambda JSON backups and exports from the original Lambda website (ureka_personal_os_data_v4). Existing records are merged by ID; attachments from website exports are imported with the notes. Backups stay local; no automatic cloud backup.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        Panel("Privacy") { Text("Lambda stores academic records in a local Room database. This build does not request internet access, analytics, advertising or account permissions.",fontSize=13.sp) }
    }
}

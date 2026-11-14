package com.lambda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.ui.*
import com.lambda.app.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(vm: LambdaViewModel, onNavigate: (Route) -> Unit, onZen: (String?) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val now by rememberNow(1000)
    val today = todayStr()

    var newTitle by remember { mutableStateOf("") }
    var quickTime by remember { mutableStateOf("") }

    val todayTasks = data.tasks.filter { it.date == today || it.dueDate == today }
    val tasksDone = todayTasks.count { it.done }
    val todayStudyMin = data.sessions.filter { it.date == today }.sumOf { it.minutes } +
        ((data.dailyLogs[today]?.extraStudyHours ?: 0.0) * 60)
    val goalMin = data.settings.dailyStudyGoalHours * 60
    val sessionCount = data.sessions.count { it.date == today }
    val habitsDone = data.habits.count { it.log[today] == true }
    val syllabus = syllabusProgressPct(data.subjects)

    val scores = buildList {
        if (todayTasks.isNotEmpty()) add(tasksDone.toFloat() / todayTasks.size)
        if (goalMin > 0) add(minOf(1.0, todayStudyMin / goalMin).toFloat())
        if (data.habits.isNotEmpty()) add(habitsDone.toFloat() / data.habits.size)
    }
    val dailyScore = if (scores.isEmpty()) 0f else scores.average().toFloat()

    val deadlines = remember(data.tasks, data.goals, now / 60000) { computeDeadlines(data.tasks, data.goals, now) }
    val urgent = deadlines.filter { it.within48h }
    val activeGoals = data.goals.filter { it.completedAt == null }.take(6)
    val name = data.settings.name.trim()

    ScreenColumn {
        PageHeader(
            eyebrow = "${longTodayHeader()} · $DHAKA_TZ_LABEL",
            title = "${greeting()}${if (name.isNotEmpty()) ", $name" else ""}.",
            actions = {
                GhostButton("Zen Mode", { onZen(todayTasks.firstOrNull { !it.done }?.id) }, icon = Icons.Filled.AutoAwesome, color = Color(0xFFD8B4FE), borderColor = LC.Purple.copy(alpha = 0.4f))
                GhostButton("Pomodoro", { onNavigate(Route.Pomodoro) }, icon = Icons.Filled.HourglassTop)
                GhostButton("Start work", { onNavigate(Route.Work) }, icon = Icons.Filled.PlayArrow)
            }
        )

        // Live Dhaka clock
        LCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(LC.Emerald))
                Spacer(Modifier.width(8.dp))
                Eyebrow("Dhaka Time", Modifier.weight(1f))
                Badge("UTC+6", LC.Purple)
            }
            Spacer(Modifier.height(8.dp))
            Text(dhakaTimeStr(now), fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, color = LC.Text)
            Spacer(Modifier.height(2.dp))
            Text(dhakaDateStr(now), color = LC.Muted, fontSize = 12.sp)
        }

        // 2x2 stats
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                "Tasks today", "$tasksDone/${todayTasks.size}",
                if (todayTasks.isNotEmpty()) "${(tasksDone * 100f / todayTasks.size).toInt()}% complete" else "Nothing planned yet",
                Icons.Filled.Checklist, Modifier.weight(1f).fillMaxHeight()
            ) { onNavigate(Route.Planner) }
            StatCard(
                "Studied today", formatMinutes(todayStudyMin),
                "Goal ${data.settings.dailyStudyGoalHours}h · $sessionCount session${if (sessionCount == 1) "" else "s"}",
                Icons.Filled.HourglassTop, Modifier.weight(1f).fillMaxHeight()
            ) { onNavigate(Route.Analytics) }
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                "Habits today", "$habitsDone/${data.habits.size}",
                if (data.habits.isNotEmpty()) "Tap below to check in" else "Create your first habit",
                Icons.Filled.LocalFireDepartment, Modifier.weight(1f).fillMaxHeight()
            ) { onNavigate(Route.Habits) }
            StatCard(
                "Syllabus", "$syllabus%", "${data.subjects.size} subjects tracked",
                Icons.Filled.MenuBook, Modifier.weight(1f).fillMaxHeight()
            ) { onNavigate(Route.Academics) }
        }

        // 48h academic deadline banner
        if (urgent.isNotEmpty()) {
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(LC.Amber.copy(alpha = 0.07f))
                    .border(1.dp, LC.Amber.copy(alpha = 0.3f), shape).padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = LC.Amber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Deadlines · Next 48 Hours", color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Badge("${urgent.size} due", LC.Amber)
                }
                Spacer(Modifier.height(10.dp))
                urgent.take(6).forEach { d ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Badge(d.kind, LC.Muted)
                                if (d.isAcademic) Badge("Academic", LC.Indigo)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(d.title, color = LC.Text, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        DeadlineLabel(d)
                    }
                }
            }
        }

        // Today's priorities
        LCard(title = "Today's priorities", action = {
            Text("Open planner →", color = LC.Primary, fontSize = 11.sp, modifier = Modifier.clickable { onNavigate(Route.Planner) })
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LTextField(newTitle, { newTitle = it }, Modifier.weight(1f), placeholder = "Add a task for today…")
                PrimaryButton("", {
                    if (newTitle.isNotBlank()) {
                        vm.addTask(newTitle, "p2", today, quickTime.ifBlank { null })
                        newTitle = ""; quickTime = ""
                    }
                }, icon = Icons.Filled.Add, enabled = newTitle.isNotBlank(), modifier = Modifier.width(52.dp))
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = LC.Muted, modifier = Modifier.size(14.dp))
                Eyebrow("By when")
                listOf("None" to "", "10:00 PM" to "22:00", "11:30 PM" to "23:30", "1:00 AM" to "01:00").forEach { (l, v) ->
                    Chip(l, quickTime == v, { quickTime = v })
                }
            }
            Spacer(Modifier.height(12.dp))
            if (todayTasks.isEmpty()) {
                EmptyState("A clean slate", "Add your top three high-impact items for today to build momentum.", Icons.Filled.Checklist)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todayTasks.forEach { t ->
                        TaskRow(
                            t, calculateTaskRemaining(t, now),
                            onToggle = { vm.toggleTask(t.id) }, onDelete = { vm.deleteTask(t.id) },
                            onFocus = { onZen(t.id) }
                        )
                    }
                }
            }
        }

        // Daily progress
        LCard(title = "Daily progress") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RingProgress(dailyScore, 120.dp, 10.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(dailyScore * 100).toInt()}%", fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = LC.Text)
                        Text("of today", color = LC.Muted, fontSize = 10.sp)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MeterRow("Tasks", "$tasksDone/${todayTasks.size}", if (todayTasks.isNotEmpty()) tasksDone.toFloat() / todayTasks.size else 0f)
                    MeterRow("Study", "${formatMinutes(todayStudyMin)} / ${data.settings.dailyStudyGoalHours}h", if (goalMin > 0) (todayStudyMin / goalMin).toFloat() else 0f)
                    MeterRow("Habits", "$habitsDone/${data.habits.size}", if (data.habits.isNotEmpty()) habitsDone.toFloat() / data.habits.size else 0f)
                }
            }
        }

        // Upcoming deadlines
        LCard(title = "Upcoming deadlines", action = {
            if (urgent.isNotEmpty()) Badge("${urgent.size} within 48h", LC.Amber)
        }) {
            if (deadlines.isEmpty()) {
                EmptyState("No upcoming deadlines", "Add a deadline to a task or goal and it automatically organizes here.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    deadlines.take(7).forEach { d ->
                        val shape = RoundedCornerShape(8.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(if (d.within48h) LC.Amber.copy(alpha = 0.06f) else Color.Transparent)
                                .then(if (d.within48h) Modifier.border(1.dp, LC.Amber.copy(alpha = 0.2f), shape) else Modifier)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(d.kind, LC.Muted)
                            Spacer(Modifier.width(8.dp))
                            Text(d.title, color = LC.Text, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(8.dp))
                            DeadlineLabel(d)
                        }
                    }
                }
            }
        }

        // Habits check-in
        LCard(title = "Habits check-in", action = {
            Text("All habits →", color = LC.Primary, fontSize = 11.sp, modifier = Modifier.clickable { onNavigate(Route.Habits) })
        }) {
            if (data.habits.isEmpty()) {
                EmptyState("No habits yet", "Create a habit to start building consistent daily streaks.", Icons.Filled.LocalFireDepartment)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.habits.forEach { h ->
                        val checked = h.log[today] == true
                        val streak = habitStreak(h)
                        val shape = RoundedCornerShape(10.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(if (checked) LC.Primary.copy(alpha = 0.1f) else LC.White05)
                                .border(1.dp, if (checked) LC.Primary.copy(alpha = 0.4f) else LC.White10, shape)
                                .clickable { vm.toggleHabitDay(h.id, today) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(h.icon, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(h.name, color = if (checked) LC.Text else LC.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = if (checked) LC.Primary else LC.Muted, modifier = Modifier.size(14.dp))
                            Text("${streak.current}d", color = if (checked) LC.Primary else LC.Muted, fontFamily = MonoFont, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active goals
        if (activeGoals.isNotEmpty()) {
            LCard(title = "Active goals", action = {
                Text("Manage →", color = LC.Primary, fontSize = 11.sp, modifier = Modifier.clickable { onNavigate(Route.Goals) })
            }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeGoals.forEach { g ->
                        val total = g.milestones.size
                        val done = g.milestones.count { it.done }
                        val p = if (total > 0) done.toFloat() / total else 0f
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.TrackChanges, null, tint = LC.Primary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(g.title, color = LC.Text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearBar(p)
                            Spacer(Modifier.height(4.dp))
                            Row {
                                Text("$done/$total milestones", color = LC.Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("${(p * 100).toInt()}%", color = LC.Muted, fontSize = 10.sp, fontFamily = MonoFont)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeterRow(label: String, value: String, progress: Float) {
    Column {
        Row {
            Text(label, color = LC.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(value, color = LC.Text, fontSize = 11.sp, fontFamily = MonoFont)
        }
        Spacer(Modifier.height(4.dp))
        LinearBar(progress)
    }
}

@Composable
fun DeadlineLabel(d: Deadline) {
    val c = when (d.level) {
        DeadlineLevel.Critical -> if (d.overdue) LC.Red else LC.Amber
        DeadlineLevel.Warning -> LC.Primary
        DeadlineLevel.Normal -> LC.Muted
    }
    Badge(d.label, c)
}

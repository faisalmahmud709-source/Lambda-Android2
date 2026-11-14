package com.lambda.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.data.Task
import com.lambda.app.ui.*
import com.lambda.app.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val now by rememberNow(1000)
    val today = todayStr()

    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("p2") }
    var dueTime by remember { mutableStateOf<String?>("22:00") } // null = no time
    var inboxTitle by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomTime by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }

    val dayTasks = data.tasks.filter { it.date == selectedDate }
    val doneCount = dayTasks.count { it.done }
    val overdue = data.tasks.filter { !it.done && it.date != null && it.date < today }
    val inbox = data.tasks.filter { !it.done && it.date == null }

    ScreenColumn {
        PageHeader("Plan", "Daily planner", "Give every day a short list. Specify target completion times like 10 PM or 1 AM.")

        LCard {
            // date navigator
            Row(verticalAlignment = Alignment.CenterVertically) {
                LIconButton(Icons.Filled.ChevronLeft, "Previous day", { selectedDate = shiftDateStr(selectedDate, -1) })
                LIconButton(Icons.Filled.ChevronRight, "Next day", { selectedDate = shiftDateStr(selectedDate, 1) })
                Spacer(Modifier.width(6.dp))
                Row(Modifier.weight(1f).clickable { showDatePicker = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(prettyDate(selectedDate), color = LC.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (selectedDate == today) { Spacer(Modifier.width(8.dp)); Badge("TODAY", LC.Primary) }
                }
                if (selectedDate != today) {
                    Text("Today", color = LC.Primary, fontSize = 12.sp, modifier = Modifier.clickable { selectedDate = today }.padding(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            // add bar
            LTextField(title, { title = it }, placeholder = "What needs to get done on this day?")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("p1" to "P1 Urgent", "p2" to "P2 High", "p3" to "P3 Normal", "p4" to "P4 Low").forEach { (k, l) ->
                    Chip(l, priority == k, { priority = k }, Modifier.weight(1f), mono = false)
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = LC.Muted, modifier = Modifier.size(14.dp))
                Eyebrow("By when")
                Chip("10:00 PM", dueTime == "22:00", { dueTime = "22:00" })
                Chip("11:30 PM", dueTime == "23:30", { dueTime = "23:30" })
                Chip("1:00 AM", dueTime == "01:00", { dueTime = "01:00" })
                Chip("Custom…", dueTime != null && dueTime !in listOf("22:00", "23:30", "01:00"), { showCustomTime = true })
                Chip("No time", dueTime == null, { dueTime = null }, accent = LC.Red)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (dueTime != null) "Due by ${formatTime24to12(dueTime)}" else "Anytime during the day",
                color = LC.Muted, fontSize = 11.sp, fontFamily = MonoFont
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Add task", {
                if (title.isNotBlank()) {
                    vm.addTask(title, priority, selectedDate, dueTime)
                    title = ""
                }
            }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add, enabled = title.isNotBlank())

            Spacer(Modifier.height(14.dp))
            if (dayTasks.isEmpty()) {
                EmptyState("Nothing planned for this day", "Write down your plan or move items from your Inbox below.", Icons.Filled.CalendarMonth)
            } else {
                Row {
                    Text("$doneCount of ${dayTasks.size} completed", color = LC.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("${doneCount * 100 / dayTasks.size}%", color = LC.Primary, fontFamily = MonoFont, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                LinearBar(doneCount.toFloat() / dayTasks.size)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayTasks.forEach { t ->
                        TaskRow(
                            t, calculateTaskRemaining(t, now),
                            onToggle = { vm.toggleTask(t.id) }, onDelete = { vm.deleteTask(t.id) },
                            onTimeClick = { editing = t }, showCountdown = selectedDate == today
                        )
                    }
                }
            }
        }

        if (overdue.isNotEmpty()) {
            LCard(title = "Overdue · ${overdue.size}", action = { Icon(Icons.Filled.Warning, null, tint = LC.Red, modifier = Modifier.size(16.dp)) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    overdue.forEach { t ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(t.title, color = LC.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                LIconButton(Icons.Filled.Delete, "Delete", { vm.deleteTask(t.id) }, size = 30.dp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Due ${t.date}" + (t.dueTime?.let { " · by ${formatTime24to12(it)}" } ?: ""),
                                    color = LC.Muted, fontSize = 11.sp, fontFamily = MonoFont, modifier = Modifier.weight(1f)
                                )
                                Text("Move to today →", color = LC.Primary, fontSize = 12.sp, modifier = Modifier.clickable { vm.moveTaskToDate(t.id, today) }.padding(4.dp))
                            }
                        }
                    }
                }
            }
        }

        LCard(title = "Inbox", action = { Icon(Icons.Filled.Inbox, null, tint = LC.Muted, modifier = Modifier.size(16.dp)) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LTextField(inboxTitle, { inboxTitle = it }, Modifier.weight(1f), placeholder = "Unscheduled task…")
                PrimaryButton("", {
                    if (inboxTitle.isNotBlank()) {
                        vm.addTask(inboxTitle, "p3", null, null)
                        inboxTitle = ""
                    }
                }, icon = Icons.Filled.Add, enabled = inboxTitle.isNotBlank(), modifier = Modifier.width(52.dp))
            }
            Spacer(Modifier.height(10.dp))
            if (inbox.isEmpty()) {
                EmptyState("Inbox zero", "Capture tasks you haven't scheduled yet.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    inbox.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.title, color = LC.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                "Plan for ${if (selectedDate == today) "today" else "this date"}", color = LC.Primary, fontSize = 12.sp,
                                modifier = Modifier.clickable { vm.moveTaskToDate(t.id, selectedDate) }.padding(6.dp)
                            )
                            LIconButton(Icons.Filled.Delete, "Delete", { vm.deleteTask(t.id) }, size = 30.dp)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) LDatePickerDialog(selectedDate, { selectedDate = it }, { showDatePicker = false })
    if (showCustomTime) {
        TimeTargetDialog("Set target time", dueTime, onSave = { dueTime = it; showCustomTime = false }, onClear = null, onDismiss = { showCustomTime = false })
    }
    editing?.let { t ->
        TimeTargetDialog(
            "Set target time", t.dueTime,
            onSave = { vm.updateTask(t.id) { x -> x.copy(dueTime = it) }; editing = null },
            onClear = { vm.updateTask(t.id) { x -> x.copy(dueTime = null) }; editing = null },
            onDismiss = { editing = null }
        )
    }
}

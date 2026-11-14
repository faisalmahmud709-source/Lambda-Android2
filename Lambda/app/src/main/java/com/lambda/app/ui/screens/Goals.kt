package com.lambda.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.data.Goal
import com.lambda.app.ui.*
import com.lambda.app.util.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val CATEGORIES = listOf("Academic", "Exam", "Life", "Habit")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Goal?>(null) }

    val active = data.goals.filter { it.completedAt == null }.sortedBy { it.deadline ?: "9999" }
    val completed = data.goals.filter { it.completedAt != null }.sortedByDescending { it.completedAt }

    ScreenColumn {
        PageHeader("Long game", "Goals", "A goal completes automatically when every milestone is ticked.") {
            PrimaryButton("New goal", { showAdd = true }, icon = Icons.Filled.Add)
        }

        if (data.goals.isEmpty()) {
            LCard { EmptyState("No goals yet", "Break something big into milestones and tick them off.", Icons.Filled.TrackChanges) }
        }
        active.forEach { GoalCard(vm, it) { toDelete = it } }
        if (completed.isNotEmpty()) {
            Eyebrow("Completed · ${completed.size}")
            completed.forEach { GoalCard(vm, it) { toDelete = it } }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Academic") }
        var deadline by remember { mutableStateOf<String?>(null) }
        var milestones by remember { mutableStateOf("") }
        var picker by remember { mutableStateOf(false) }
        FormDialog("New goal", "Create", {
            val ms = milestones.lines().map { it.trim() }.filter { it.isNotEmpty() }
            vm.addGoal(title, category, deadline, ms); showAdd = false
        }, { showAdd = false }, confirmEnabled = title.isNotBlank()) {
            LTextField(title, { title = it }, placeholder = "e.g. Master Engineering Question Bank…", label = "Goal")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CATEGORIES.forEach { c -> Chip(c, category == c, { category = c }, mono = false) }
            }
            GhostButton(deadline?.let { "Deadline: ${prettyDate(it)}" } ?: "Set deadline (optional)", { picker = true }, Modifier.fillMaxWidth(), icon = Icons.Filled.CalendarMonth)
            if (deadline != null) Text("Clear deadline", color = LC.Muted, fontSize = 11.sp, modifier = Modifier.clickable { deadline = null })
            LTextField(milestones, { milestones = it }, placeholder = "One milestone per line", label = "Milestones", singleLine = false, minLines = 3)
            if (picker) LDatePickerDialog(deadline, { deadline = it }, { picker = false })
        }
    }
    toDelete?.let { g ->
        ConfirmDialog("Delete this goal?", "All milestone tracking and achievements for this goal will be removed.", "Delete", true,
            onConfirm = { vm.deleteGoal(g.id); toDelete = null }, onDismiss = { toDelete = null })
    }
}

@Composable
private fun GoalCard(vm: LambdaViewModel, g: Goal, onDelete: () -> Unit) {
    val total = g.milestones.size
    val done = g.milestones.count { it.done }
    val p = if (total > 0) done.toFloat() / total else 0f
    val dl = deadlineText(g.deadline)
    LCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(g.title, color = if (g.completedAt != null) LC.Muted else LC.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (g.completedAt != null) TextDecoration.LineThrough else TextDecoration.None)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Badge(g.category, LC.Indigo)
                    if (g.completedAt != null) Badge("Completed", LC.Emerald)
                    else if (dl != null) Text(dl.first, color = dl.second, fontSize = 11.sp)
                }
            }
            LIconButton(Icons.Filled.Delete, "Delete goal", onDelete, size = 32.dp)
        }
        Spacer(Modifier.height(10.dp))
        LinearBar(p, color = if (g.completedAt != null) LC.Emerald else LC.Primary)
        Spacer(Modifier.height(4.dp))
        Row {
            Text("$done/$total milestones", color = LC.Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text("${(p * 100).toInt()}%", color = LC.Muted, fontSize = 10.sp, fontFamily = MonoFont)
        }
        if (g.milestones.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                g.milestones.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CheckBox(m.done, { vm.toggleMilestone(g.id, m.id) })
                        Spacer(Modifier.width(10.dp))
                        Text(m.title, color = if (m.done) LC.Muted else LC.Text, fontSize = 13.sp,
                            textDecoration = if (m.done) TextDecoration.LineThrough else TextDecoration.None)
                    }
                }
            }
        }
    }
}

private fun deadlineText(deadline: String?): Pair<String, androidx.compose.ui.graphics.Color>? {
    val d = deadline?.let { parseDate(it) } ?: return null
    val diff = ChronoUnit.DAYS.between(LocalDate.now(), d)
    return when {
        diff < 0 -> "${-diff}d overdue" to LC.Red
        diff == 0L -> "Due today" to LC.Primary
        diff == 1L -> "Tomorrow" to LC.Amber
        else -> "$diff days left" to LC.Muted
    }
}

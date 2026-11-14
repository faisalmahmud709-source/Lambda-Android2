package com.lambda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.data.Habit
import com.lambda.app.ui.*
import com.lambda.app.util.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val HABIT_COLORS = listOf("#f0bc68", "#34d399", "#60a5fa", "#a78bfa", "#f87171", "#fbbf24")

@Composable
fun HabitsScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val today = todayStr()
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Habit?>(null) }

    ScreenColumn {
        PageHeader("Consistency", "Habits", "Daily non-negotiables. Consistency beats intensity.") {
            PrimaryButton("New habit", { showAdd = true }, icon = Icons.Filled.Add)
        }

        if (data.habits.isEmpty()) {
            LCard { EmptyState("No habits created yet", "Add a habit and tick it off every day to build streaks.", Icons.Filled.LocalFireDepartment) }
        }

        data.habits.forEach { h -> HabitCard(vm, h, today, onDelete = { toDelete = h }) }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var icon by remember { mutableStateOf("⚡") }
        var color by remember { mutableStateOf(HABIT_COLORS[0]) }
        FormDialog("New habit", "Create", {
            vm.addHabit(name, icon, color); showAdd = false
        }, { showAdd = false }, confirmEnabled = name.isNotBlank()) {
            LTextField(name, { name = it }, placeholder = "e.g. Read 20 pages, Morning Math Drill…", label = "Habit name")
            LTextField(icon, { icon = it.take(4) }, label = "Icon (emoji)")
            Eyebrow("Colour")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HABIT_COLORS.forEach { c ->
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(parseHexColor(c))
                            .border(if (color == c) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { color = c }
                    )
                }
            }
        }
    }
    toDelete?.let { h ->
        ConfirmDialog("Delete this habit?", "All streak records and historical heatmaps for this habit will be deleted.", "Delete", true,
            onConfirm = { vm.deleteHabit(h.id); toDelete = null }, onDismiss = { toDelete = null })
    }
}

@Composable
private fun HabitCard(vm: LambdaViewModel, h: Habit, today: String, onDelete: () -> Unit) {
    val color = parseHexColor(h.color)
    val streak = habitStreak(h)
    val checked = h.log[today] == true
    val rate30 = (0 until 30).count { h.log[LocalDate.now().minusDays(it.toLong()).toString()] == true } * 100 / 30

    // 26 weeks, Monday-first, ending with the current week
    val weeks = remember(today) {
        val endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val start = endOfWeek.minusDays(26L * 7 - 1)
        (0 until 26).map { w -> (0 until 7).map { d -> start.plusDays(w * 7L + d) } }
    }

    LCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(h.icon, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name, color = LC.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Best ${streak.longest}d · 30-day rate $rate30%", color = LC.Muted, fontSize = 11.sp)
            }
            LIconButton(Icons.Filled.Delete, "Delete habit", onDelete, size = 32.dp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("${streak.current}", color = LC.Text, fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                Text(" day streak", color = LC.Muted, fontSize = 12.sp)
            }
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier.clip(shape).background(if (checked) color.copy(alpha = 0.18f) else LC.White05)
                    .border(1.dp, if (checked) color.copy(alpha = 0.5f) else LC.White10, shape)
                    .clickable { vm.toggleHabitDay(h.id, today) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (checked) Icons.Filled.Check else Icons.Filled.Add, null, tint = if (checked) color else LC.Muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (checked) "Done today" else "Check in", color = if (checked) color else LC.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(12.dp))

        // heatmap (scrolled to the latest week)
        Row(Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    week.forEach { d ->
                        val ds = d.toString()
                        val done = h.log[ds] == true
                        val future = d.isAfter(LocalDate.now())
                        Box(
                            Modifier.size(16.dp).clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        done -> color
                                        future -> Color.Transparent
                                        else -> LC.White05
                                    }
                                )
                                .then(if (!future) Modifier.clickable { vm.toggleHabitDay(h.id, ds) } else Modifier)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Tap any past square to correct a day.", color = LC.Muted, fontSize = 10.sp)
    }
}

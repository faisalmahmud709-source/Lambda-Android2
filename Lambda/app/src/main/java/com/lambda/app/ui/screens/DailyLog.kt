package com.lambda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.data.DailyLogEntry
import com.lambda.app.ui.*
import com.lambda.app.util.*
import java.time.LocalDate

private val MOODS = listOf("😫", "😕", "😐", "🙂", "🔥")

private fun fmt(v: Double?): String = v?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""

@Composable
fun DailyLogScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val logs = data.dailyLogs
    var date by rememberSaveable { mutableStateOf(todayStr()) }
    var showPicker by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<String?>(null) }

    // (re)load the form whenever the date changes
    var sleep by remember(date) { mutableStateOf(fmt(logs[date]?.sleepHours)) }
    var screen by remember(date) { mutableStateOf(fmt(logs[date]?.screenHours)) }
    var extra by remember(date) { mutableStateOf(logs[date]?.extraStudyHours?.let { fmt(it) } ?: "0") }
    var mood by remember(date) { mutableIntStateOf(logs[date]?.mood ?: 5) }
    var notes by remember(date) { mutableStateOf(logs[date]?.notes ?: "") }

    val last7 = (0 until 7).mapNotNull { logs[LocalDate.now().minusDays(it.toLong()).toString()] }
    val sleepVals = last7.mapNotNull { it.sleepHours }
    val screenVals = last7.mapNotNull { it.screenHours }
    val avgSleep = if (sleepVals.isNotEmpty()) "%.1fh".format(sleepVals.average()) else "—"
    val avgScreen = if (screenVals.isNotEmpty()) "%.1fh".format(screenVals.average()) else "—"
    val extraTotal = "%.1fh".format(last7.sumOf { it.extraStudyHours ?: 0.0 })
    val sorted = logs.values.sortedByDescending { it.date }

    LaunchedEffect(saved) { if (saved) { kotlinx.coroutines.delay(1800); saved = false } }

    ScreenColumn {
        PageHeader("Body & attention", "Daily log", "Sleep, screen time and any study you did away from the timers. Extra study counts toward your daily study goal.")

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Sleep (7d)", avgSleep, "average", Icons.Filled.Bedtime, Modifier.weight(1f).fillMaxHeight())
            StatCard("Screen (7d)", avgScreen, "average", Icons.Filled.PhoneAndroid, Modifier.weight(1f).fillMaxHeight())
            StatCard("Extra (7d)", extraTotal, "study logged", Icons.Filled.MenuBook, Modifier.weight(1f).fillMaxHeight())
        }

        LCard(title = "Log a day") {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LC.Field)
                    .border(1.dp, LC.White10, RoundedCornerShape(10.dp)).clickable { showPicker = true }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CalendarMonth, null, tint = LC.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(prettyDate(date), color = LC.Text, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(sleep, { sleep = it }, Modifier.weight(1f), placeholder = "e.g. 7.5", label = "Sleep (h)")
                NumberField(screen, { screen = it }, Modifier.weight(1f), placeholder = "e.g. 3.0", label = "Screen (h)")
            }
            Spacer(Modifier.height(8.dp))
            NumberField(extra, { extra = it }, placeholder = "e.g. 2.0", label = "Extra study (h)")
            Spacer(Modifier.height(12.dp))
            Eyebrow("Mood")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MOODS.forEachIndexed { i, e ->
                    val sel = mood == i + 1
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (sel) LC.Primary.copy(alpha = 0.2f) else LC.White05)
                            .border(1.dp, if (sel) LC.Primary.copy(alpha = 0.5f) else LC.White05, RoundedCornerShape(10.dp))
                            .clickable { mood = i + 1 }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(e, fontSize = 20.sp) }
                }
            }
            Spacer(Modifier.height(10.dp))
            LTextField(notes, { notes = it }, placeholder = "What went well today? What hindered your flow?", singleLine = false, minLines = 3)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton("Save log", {
                    vm.saveDailyLog(
                        DailyLogEntry(
                            date = date, sleepHours = sleep.toDoubleOrNull(), screenHours = screen.toDoubleOrNull(),
                            extraStudyHours = extra.toDoubleOrNull(), mood = mood, notes = notes.trim().ifBlank { null }
                        )
                    )
                    saved = true
                }, icon = Icons.Filled.Check)
                if (saved) {
                    Spacer(Modifier.width(10.dp))
                    Text("Saved", color = LC.Emerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        LCard(title = "Recent days") {
            if (sorted.isEmpty()) {
                EmptyState("Nothing logged yet", "Your saved days will be listed here.", Icons.Filled.Assignment)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sorted.take(30).forEach { e ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LC.White05)
                                .clickable { date = e.date }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(MOODS.getOrElse((e.mood ?: 3) - 1) { "😐" }, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(prettyDate(e.date), color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "Sleep ${e.sleepHours?.let { fmt(it) + "h" } ?: "—"} · Screen ${e.screenHours?.let { fmt(it) + "h" } ?: "—"} · Extra ${fmt(e.extraStudyHours ?: 0.0)}h",
                                    color = LC.Muted, fontSize = 11.sp
                                )
                            }
                            LIconButton(Icons.Filled.Delete, "Delete log entry", { toDelete = e.date }, size = 30.dp)
                        }
                    }
                }
            }
        }
    }

    if (showPicker) LDatePickerDialog(date, { date = it }, { showPicker = false })
    toDelete?.let { d ->
        ConfirmDialog("Delete this day's log?", "This will remove sleep, screen time and extra study metrics for this date.", "Delete", true,
            onConfirm = { vm.deleteDailyLog(d); toDelete = null }, onDismiss = { toDelete = null })
    }
}

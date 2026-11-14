package com.lambda.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.ui.*
import com.lambda.app.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PomodoroScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val pomo by vm.pomodoro.collectAsStateWithLifecycle()
    val now by rememberNow(250)
    val s = data.settings
    val today = todayStr()

    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    val remaining = pomo.endsAt?.let { maxOf(0L, it - now) } ?: pomo.remainingMs
    val totalMs = when (pomo.phase) {
        "focus" -> s.focusMinutes
        "short" -> s.shortBreakMinutes
        else -> s.longBreakMinutes
    } * 60_000L
    val progress = if (totalMs > 0) 1f - remaining.toFloat() / totalMs else 0f
    val focusSessions = data.sessions.filter { it.kind == "pomodoro" }
    val todayCount = focusSessions.count { it.date == today }
    val phaseColor = if (pomo.phase == "focus") LC.Primary else LC.Emerald

    ScreenColumn {
        PageHeader("Focus", "Pomodoro", "Work in focused sprints. Completed focus sessions count toward your study hours.")

        LCard {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Chip("Focus", pomo.phase == "focus", { vm.setPomodoroPhase("focus") }, Modifier.weight(1f), mono = false)
                Chip("Short break", pomo.phase == "short", { vm.setPomodoroPhase("short") }, Modifier.weight(1f), accent = LC.Emerald, mono = false)
                Chip("Long break", pomo.phase == "long", { vm.setPomodoroPhase("long") }, Modifier.weight(1f), accent = LC.Emerald, mono = false)
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RingProgress(progress, 230.dp, 12.dp, color = phaseColor) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(countdown(remaining), fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, color = LC.Text)
                        Eyebrow(if (pomo.phase == "focus") "Focus sprint" else "Break", color = phaseColor)
                        Spacer(Modifier.height(2.dp))
                        Text("$todayCount sprint${if (todayCount == 1) "" else "s"} today", color = LC.Muted, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (pomo.running) PrimaryButton("Pause", { vm.pausePomodoro() }, Modifier.weight(1f), icon = Icons.Filled.Pause)
                else PrimaryButton("Start", { vm.startPomodoro() }, Modifier.weight(1f), icon = Icons.Filled.PlayArrow)
                GhostButton("Reset", { vm.resetPomodoro() }, Modifier.weight(1f), icon = Icons.Filled.Refresh)
                GhostButton("Skip", { vm.skipPomodoro() }, Modifier.weight(1f), icon = Icons.Filled.SkipNext)
            }
        }

        LCard(title = "What are you focusing on?") {
            LTextField(pomo.label, { vm.setPomodoroLabel(it, pomo.subjectId) }, placeholder = "What are you focusing on?")
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("General", pomo.subjectId == null, { vm.setPomodoroLabel(pomo.label, null) }, mono = false)
                data.subjects.forEach { sub ->
                    Chip("${sub.icon} ${sub.name}", pomo.subjectId == sub.id, { vm.setPomodoroLabel(pomo.label, sub.id) }, mono = false)
                }
            }
        }

        LCard(title = "Durations (minutes)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DurationField("Focus", s.focusMinutes, 5, 90, Modifier.weight(1f)) { v -> vm.updateSettings { it.copy(focusMinutes = v) } }
                DurationField("Short", s.shortBreakMinutes, 1, 30, Modifier.weight(1f)) { v -> vm.updateSettings { it.copy(shortBreakMinutes = v) } }
                DurationField("Long", s.longBreakMinutes, 5, 60, Modifier.weight(1f)) { v -> vm.updateSettings { it.copy(longBreakMinutes = v) } }
            }
        }

        LCard(title = "Recent focus sessions") {
            if (focusSessions.isEmpty()) {
                EmptyState("No focus sessions yet", "Complete a sprint and it will be logged here.", Icons.Filled.HourglassTop)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    focusSessions.take(30).forEach { f ->
                        val sub = data.subjects.firstOrNull { it.id == f.subjectId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f.title, color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "${prettyDate(f.date, "d MMM")} · ${formatMinutes(f.minutes)}" + (sub?.let { " · ${it.icon} ${it.name}" } ?: ""),
                                    color = LC.Muted, fontSize = 11.sp
                                )
                            }
                            LIconButton(Icons.Filled.Delete, "Delete", { sessionToDelete = f.id }, size = 30.dp)
                        }
                    }
                }
            }
        }
    }

    sessionToDelete?.let { id ->
        ConfirmDialog(
            "Delete this focus session?", "This will delete this focus sprint from your study records.", "Delete", true,
            onConfirm = { vm.deleteSession(id); sessionToDelete = null }, onDismiss = { sessionToDelete = null }
        )
    }
}

@Composable
private fun DurationField(label: String, value: Int, min: Int, max: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    NumberField(
        value = text,
        onValueChange = { t ->
            text = t
            t.toIntOrNull()?.takeIf { it in min..max }?.let(onChange)
        },
        modifier = modifier, label = label
    )
}

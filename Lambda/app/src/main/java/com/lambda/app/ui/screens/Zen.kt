package com.lambda.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.ui.*
import com.lambda.app.util.*

/** Full-screen distraction-free focus mode: Dhaka clock, task spotlight, sprint timer, ambient sound. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZenScreen(vm: LambdaViewModel, initialTaskId: String?, onClose: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val now by rememberNow(250)
    val today = todayStr()
    val view = LocalView.current

    val todayTasks = data.tasks.filter { it.date == today || it.dueDate == today }
    var selectedId by remember { mutableStateOf(initialTaskId) }
    val task = todayTasks.firstOrNull { it.id == selectedId } ?: todayTasks.firstOrNull { !it.done } ?: todayTasks.firstOrNull()
    var switcher by remember { mutableStateOf(false) }

    var preset by remember { mutableStateOf("25") } // "25" | "50" | "stopwatch"
    var running by remember { mutableStateOf(false) }
    var endsAt by remember { mutableLongStateOf(0L) }
    var pausedRemaining by remember { mutableLongStateOf(25 * 60_000L) }
    var swStart by remember { mutableLongStateOf(0L) }
    var swAcc by remember { mutableLongStateOf(0L) }
    var celebrate by remember { mutableStateOf(false) }
    var sprints by remember { mutableIntStateOf(0) }

    var ambient by remember { mutableStateOf(Ambient.OFF) }
    var volume by remember { mutableFloatStateOf(0.4f) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; Ambient.stop() }
    }
    LaunchedEffect(ambient) { Ambient.play(ambient, volume) }
    LaunchedEffect(celebrate) { if (celebrate) { kotlinx.coroutines.delay(4000); celebrate = false } }

    val isStopwatch = preset == "stopwatch"
    val remaining = if (running) maxOf(0L, endsAt - now) else pausedRemaining
    val elapsedSw = swAcc + if (running) now - swStart else 0L
    val totalMs = (if (preset == "50") 50 else 25) * 60_000L

    fun selectPreset(p: String) {
        preset = p; running = false
        pausedRemaining = (if (p == "50") 50 else 25) * 60_000L
        swAcc = 0; swStart = 0
    }

    val finished = running && !isStopwatch && now >= endsAt
    LaunchedEffect(finished) {
        if (finished) {
            running = false
            val mins = if (preset == "50") 50 else 25
            vm.addSession(task?.title ?: "Zen Focus Sprint", mins, "pomodoro")
            if (data.settings.soundEnabled) Chime.play(Chime.Kind.Complete)
            sprints++
            celebrate = true
            pausedRemaining = totalMs
        }
    }

    fun toggleRun() {
        val t = System.currentTimeMillis()
        if (running) {
            if (isStopwatch) swAcc += t - swStart else pausedRemaining = maxOf(0L, endsAt - t)
            running = false
        } else {
            if (isStopwatch) swStart = t else endsAt = t + pausedRemaining
            running = true
        }
    }

    fun reset() {
        running = false; swAcc = 0; swStart = 0
        pausedRemaining = totalMs
    }

    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        0.10f, 0.22f, infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )

    Box(
        Modifier.fillMaxSize()
            // swallow touches so nothing underneath the overlay can be tapped
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .background(Brush.radialGradient(listOf(LC.Purple.copy(alpha = breathe), Color(0xFF07040F)), radius = 1400f))
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Zen mode", Modifier.weight(1f), color = Color(0xFFD8B4FE))
                LIconButton(Icons.Filled.Close, "Exit Zen Mode", onClose, tint = LC.Text)
            }
            Spacer(Modifier.height(12.dp))

            Text(dhakaTimeStr(now), fontFamily = MonoFont, fontWeight = FontWeight.Light, fontSize = 40.sp, color = LC.Text)
            Text("${dhakaDateStr(now)} · $DHAKA_TZ_LABEL", color = LC.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(22.dp))

            // active task spotlight
            Eyebrow("Active task", color = LC.Primary)
            Spacer(Modifier.height(6.dp))
            if (task == null) {
                Text("No task for today", color = LC.Muted, fontSize = 18.sp, textAlign = TextAlign.Center)
                Text("Add one from the Planner to spotlight it here.", color = LC.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            } else {
                Text(
                    task.title, color = if (task.done) LC.Muted else LC.Text, fontSize = 24.sp, textAlign = TextAlign.Center,
                    fontFamily = DisplayFont, fontStyle = FontStyle.Italic, lineHeight = 30.sp
                )
                Spacer(Modifier.height(8.dp))
                val rem = calculateTaskRemaining(task, now)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemainingPill(task, rem)
                    PriorityBadge(task.priority)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(if (task.done) "Completed" else "Mark done", { vm.toggleTask(task.id) }, icon = Icons.Filled.Check, color = LC.Emerald, borderColor = LC.Emerald.copy(alpha = 0.4f))
                    if (todayTasks.size > 1) {
                        Box {
                            GhostButton("Switch task", { switcher = true }, icon = Icons.Filled.SwapHoriz)
                            DropdownMenu(expanded = switcher, onDismissRequest = { switcher = false }, containerColor = LC.Elevated) {
                                todayTasks.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.title, color = if (t.done) LC.Muted else LC.Text, fontSize = 13.sp) },
                                        onClick = { selectedId = t.id; switcher = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            // sprint timer
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("25 min", preset == "25", { selectPreset("25") }, mono = false)
                Chip("50 min", preset == "50", { selectPreset("50") }, mono = false)
                Chip("Stopwatch", preset == "stopwatch", { selectPreset("stopwatch") }, mono = false)
            }
            Spacer(Modifier.height(16.dp))
            val progress = if (isStopwatch) ((elapsedSw / 60_000f) % 60f) / 60f else 1f - remaining.toFloat() / totalMs
            RingProgress(progress, 220.dp, 8.dp, color = LC.Primary) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isStopwatch) hhmmss(elapsedSw) else countdown(remaining),
                        fontFamily = MonoFont, fontWeight = FontWeight.Light, fontSize = 44.sp, color = LC.Text
                    )
                    Text(if (running) "focusing…" else "ready", color = LC.Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (running) PrimaryButton("Pause", { toggleRun() }, icon = Icons.Filled.Pause)
                else PrimaryButton(if (isStopwatch && elapsedSw > 0) "Resume" else "Start", { toggleRun() }, icon = Icons.Filled.PlayArrow)
                GhostButton("Reset", { reset() }, icon = Icons.Filled.Refresh)
                if (isStopwatch && elapsedSw >= 60_000L) {
                    GhostButton("Save", {
                        val mins = maxOf(1, Math.round(elapsedSw / 60000.0).toInt())
                        vm.addSession(task?.title ?: "Zen Focus Session", mins, "work")
                        sprints++; celebrate = true; reset()
                    }, icon = Icons.Filled.Check, color = LC.Emerald, borderColor = LC.Emerald.copy(alpha = 0.4f))
                }
            }
            if (sprints > 0) {
                Spacer(Modifier.height(8.dp))
                Text("$sprints session${if (sprints == 1) "" else "s"} logged this visit", color = LC.Muted, fontSize = 11.sp)
            }
            if (celebrate) {
                Spacer(Modifier.height(10.dp))
                Text("Session complete — logged to your study hours ✨", color = LC.Emerald, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(26.dp))

            // ambient sound
            Eyebrow("Ambient sound")
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("Silent", ambient == Ambient.OFF, { ambient = Ambient.OFF }, mono = false)
                Chip("Deep hum", ambient == Ambient.BROWN, { ambient = Ambient.BROWN }, mono = false)
                Chip("Rain", ambient == Ambient.PINK, { ambient = Ambient.PINK }, mono = false)
                Chip("Warm", ambient == Ambient.WARM, { ambient = Ambient.WARM }, mono = false)
            }
            if (ambient != Ambient.OFF) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolumeUp, null, tint = LC.Muted, modifier = Modifier.size(18.dp))
                    Slider(
                        value = volume, onValueChange = { volume = it; Ambient.setVolume(it) }, modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = LC.Primary, activeTrackColor = LC.Primary, inactiveTrackColor = LC.White10)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

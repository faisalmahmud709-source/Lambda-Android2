package com.lambda.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.data.Subject
import com.lambda.app.ui.*
import com.lambda.app.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val now by rememberNow(500)
    val work = data.activeWork

    var title by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf<String?>(null) }
    var initialNotes by remember { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    val sessions = data.sessions.filter { it.kind == "work" }

    ScreenColumn {
        PageHeader("Deep work", "Work session", "Write down what you're about to do, start the stopwatch, and keep your notes right next to it.")

        if (work == null) {
            LCard(title = "New session") {
                LTextField(title, { title = it }, placeholder = "e.g. Higher Math Calculus Practice…")
                Spacer(Modifier.height(10.dp))
                Eyebrow("Subject (optional)")
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("None", subjectId == null, { subjectId = null }, mono = false)
                    data.subjects.forEach { s -> Chip("${s.icon} ${s.name}", subjectId == s.id, { subjectId = s.id }, mono = false) }
                }
                Spacer(Modifier.height(10.dp))
                LTextField(initialNotes, { initialNotes = it }, placeholder = "Goals for this session, target problems, page numbers…", singleLine = false, minLines = 3)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Start stopwatch", {
                    vm.startWork(title.ifBlank { "Deep work session" }, subjectId, initialNotes)
                    title = ""; initialNotes = ""; subjectId = null
                }, Modifier.fillMaxWidth(), icon = Icons.Filled.PlayArrow)
            }
        } else {
            val elapsed = work.accumulatedMs + (work.runningSince?.let { now - it } ?: 0L)
            val running = work.runningSince != null
            val subj = data.subjects.firstOrNull { it.id == work.subjectId }
            LCard {
                Eyebrow(if (running) "Running" else "Paused", color = if (running) LC.Emerald else LC.Amber)
                Spacer(Modifier.height(4.dp))
                Text(work.title, color = LC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (subj != null) Text("${subj.icon} ${subj.name}", color = LC.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    hhmmss(elapsed), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, color = LC.Primary
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (running) PrimaryButton("Pause", { vm.pauseWork() }, Modifier.weight(1f), icon = Icons.Filled.Pause)
                    else PrimaryButton("Resume", { vm.resumeWork() }, Modifier.weight(1f), icon = Icons.Filled.PlayArrow)
                    GhostButton("Lap", { vm.addLap() }, Modifier.weight(1f), icon = Icons.Filled.Flag)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Finish & save", { vm.stopAndSaveWork() }, Modifier.weight(1f), icon = Icons.Filled.Check, color = LC.Emerald, borderColor = LC.Emerald.copy(alpha = 0.4f))
                    GhostButton("Discard", { confirmDiscard = true }, Modifier.weight(1f), icon = Icons.Filled.Delete, color = LC.Red, borderColor = LC.Red.copy(alpha = 0.4f))
                }
                if (work.laps.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Eyebrow("Laps")
                    Spacer(Modifier.height(4.dp))
                    work.laps.asReversed().forEach { lap ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(lap.label, color = LC.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(hhmmss(lap.time), color = LC.Text, fontSize = 12.sp, fontFamily = MonoFont)
                        }
                    }
                }
            }
            LCard(title = "Session notes") {
                LTextField(work.notes, { vm.updateWorkNotes(it) }, placeholder = "Record insights, formulas, question numbers, or milestones here…", singleLine = false, minLines = 5)
            }
        }

        LCard(title = "Recent work sessions") {
            if (sessions.isEmpty()) {
                EmptyState("No work sessions yet", "Finished sessions show up here and count toward your study hours.", Icons.Filled.PlayArrow)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sessions.take(30).forEach { s ->
                        val sub = data.subjects.firstOrNull { it.id == s.subjectId }
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(s.title, color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "${prettyDate(s.date, "d MMM")} · ${formatMinutes(s.minutes)}" + (sub?.let { " · ${it.icon} ${it.name}" } ?: ""),
                                    color = LC.Muted, fontSize = 11.sp
                                )
                                if (!s.notes.isNullOrBlank()) Text(s.notes, color = LC.Muted, fontSize = 11.sp, maxLines = 2)
                            }
                            LIconButton(Icons.Filled.Delete, "Delete session", { sessionToDelete = s.id }, size = 30.dp)
                        }
                    }
                }
            }
        }
    }

    if (confirmDiscard) ConfirmDialog(
        "Discard this session?", "The stopwatch will be stopped and this session's time will not be saved.", "Discard", true,
        onConfirm = { vm.discardWork(); confirmDiscard = false }, onDismiss = { confirmDiscard = false }
    )
    sessionToDelete?.let { id ->
        ConfirmDialog(
            "Delete this session?", "This will permanently delete this record from your study history.", "Delete", true,
            onConfirm = { vm.deleteSession(id); sessionToDelete = null }, onDismiss = { sessionToDelete = null }
        )
    }
}

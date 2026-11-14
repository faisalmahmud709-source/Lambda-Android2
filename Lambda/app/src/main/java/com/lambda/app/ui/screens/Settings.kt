package com.lambda.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.ui.*
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val s = data.settings
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf(s.name) }
    var appName by remember { mutableStateOf(s.appName) }
    var goal by remember { mutableStateOf(s.dailyStudyGoalHours.toString()) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            status = try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
                "Backup saved."
            } catch (e: Exception) { "Could not save the backup." }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            status = if (text != null && vm.importJson(text)) "Backup restored successfully!" else "That file isn't a valid Lambda backup."
        }
    }

    ScreenColumn {
        PageHeader("Preferences", "Settings & data", "Personalise your workspace, adjust timers and manage your data. Everything stays on this phone.")

        LCard(title = "Personalise") {
            LTextField(name, { name = it; vm.updateSettings { st -> st.copy(name = it) } }, placeholder = "e.g. Mahmud", label = "Your name")
            Spacer(Modifier.height(8.dp))
            LTextField(appName, { appName = it.take(24); vm.updateSettings { st -> st.copy(appName = appName.ifBlank { "Lambda" }) } }, placeholder = "e.g. Lambda", label = "App name (shown inside the app)")
            Spacer(Modifier.height(8.dp))
            NumberField(goal, { t ->
                goal = t
                t.toIntOrNull()?.let { v -> vm.updateSettings { st -> st.copy(dailyStudyGoalHours = v.coerceIn(1, 18)) } }
            }, label = "Daily study goal (hours)")
        }

        LCard(title = "Focus & goals") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MinField("Focus", s.focusMinutes, 5, 90, Modifier.weight(1f)) { v -> vm.updateSettings { it.copy(focusMinutes = v) } }
                MinField("Short break", s.shortBreakMinutes, 1, 30, Modifier.weight(1f)) { v -> vm.updateSettings { it.copy(shortBreakMinutes = v) } }
                MinField("Long break", s.longBreakMinutes, 5, 60, Modifier.weight(1f)) { v -> vm.updateSettings { it.copy(longBreakMinutes = v) } }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Audio chime", color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Play a bell sound when focus sprints end and tasks complete", color = LC.Muted, fontSize = 11.sp)
                }
                Switch(
                    checked = s.soundEnabled, onCheckedChange = { v -> vm.updateSettings { it.copy(soundEnabled = v) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = LC.OnPrimary, checkedTrackColor = LC.Primary)
                )
            }
        }

        LCard(title = "On this phone") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Tasks", data.tasks.size); Stat("Subjects", data.subjects.size)
                Stat("Sessions", data.sessions.size); Stat("Habits", data.habits.size); Stat("Notes", data.notes.size)
            }
            Spacer(Modifier.height(14.dp))
            Text("Export workspace backup", color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Save your syllabus progress, tasks, habits and notes as a JSON file (attached files are not included).", color = LC.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            GhostButton("Export JSON", { exportLauncher.launch("lambda-os-backup-${LocalDate.now()}.json") }, Modifier.fillMaxWidth(), icon = Icons.Filled.FileDownload)
            Spacer(Modifier.height(14.dp))
            Text("Restore from backup", color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Import a JSON backup from this app or from the Lambda website version.", color = LC.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            GhostButton("Import JSON", { importLauncher.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), icon = Icons.Filled.FileUpload)
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                Text(status!!, color = LC.Primary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text("Reset to starter template", color = LC.Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Reset syllabus, tasks, notes and settings to the default HSC template.", color = LC.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            GhostButton("Reset database", { confirmReset = true }, Modifier.fillMaxWidth(), icon = Icons.Filled.RestartAlt, color = LC.Red, borderColor = LC.Red.copy(alpha = 0.4f))
        }

        LCard(title = "About") {
            Text("Lambda · Personal OS", color = LC.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Version 1.0.0 · Native Android (Kotlin + Jetpack Compose)", color = LC.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text("Fully offline. This app has no internet permission, and all your data lives only in this app's private storage on your phone.", color = LC.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text("Original web design: Faisal Mahmud — HSC & Engineering Prep", color = LC.Muted, fontSize = 11.sp)
        }
    }

    if (confirmReset) ConfirmDialog(
        "Erase and reset everything?", "This replaces all your current data with the default starter syllabus and template. This cannot be undone.",
        "Erase & Reset", true,
        onConfirm = { vm.resetAll(); confirmReset = false; Toast.makeText(context, "Reset complete", Toast.LENGTH_SHORT).show() },
        onDismiss = { confirmReset = false }
    )
}

@Composable
private fun Stat(label: String, v: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(label)
        Text("$v", color = LC.Text, fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

@Composable
private fun MinField(label: String, value: Int, min: Int, max: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    // only apply values that are already valid, so typing "25" isn't interrupted at "2"
    NumberField(text, { t -> text = t; t.toIntOrNull()?.takeIf { it in min..max }?.let(onChange) }, modifier, label = "$label (min)")
}

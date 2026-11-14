package com.lambda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.data.Chapter
import com.lambda.app.data.Subject
import com.lambda.app.data.SubjectGroup
import com.lambda.app.ui.*
import com.lambda.app.util.*

private val REVISION_LABELS = listOf("Not revised", "Revised once", "Revised twice", "Mastered")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicsScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf("all") }

    var showAddSubject by remember { mutableStateOf(false) }
    var showAddGroup by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var groupToDelete by remember { mutableStateOf<SubjectGroup?>(null) }

    val subjects = data.subjects
    val current = subjects.firstOrNull { it.id == activeId } ?: subjects.firstOrNull()
    val overall = syllabusProgressPct(subjects)
    val totalChapters = subjects.sumOf { s -> s.groups.sumOf { it.chapters.size } }

    ScreenColumn {
        PageHeader("Syllabus", "Academics", "Stamp each part as you finish it, count lectures and track how many times you've revised.")

        LCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Overall syllabus")
                    Text("$overall%", fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = LC.Text)
                }
                Text("$totalChapters chapters · ${subjects.size} subjects", color = LC.Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearBar(overall / 100f)
        }

        // subject selector
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            subjects.forEach { s ->
                Chip("${s.icon} ${s.name}  ${(subjectProgress(s) * 100).toInt()}%", current?.id == s.id, { activeId = s.id }, mono = false)
            }
            Chip("+ Subject", false, { showAddSubject = true }, mono = false, accent = LC.Purple)
        }

        if (current == null) {
            LCard { EmptyState("No subjects", "Add your first subject to start tracking chapters.", Icons.Filled.School) }
        } else {
            LCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(current.icon, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f).clickable { showRename = true }) {
                        Text(current.name, color = LC.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(subjectProgress(current) * 100).toInt()}% complete · tap name to rename", color = LC.Muted, fontSize = 11.sp)
                    }
                    LIconButton(Icons.Filled.Delete, "Delete subject", { subjectToDelete = current })
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("All", filter == "all", { filter = "all" }, mono = false)
                    Chip("Pending", filter == "pending", { filter = "pending" }, mono = false)
                    Chip("Done", filter == "done", { filter = "done" }, mono = false)
                }
            }

            if (current.groups.isEmpty()) {
                LCard { EmptyState("No papers yet", "Add a paper (e.g. 1st Paper) to start adding chapters.") }
            }

            current.groups.forEach { group ->
                GroupCard(vm, current, group, filter, onDeleteGroup = { groupToDelete = group })
            }

            GhostButton("Add paper / section", { showAddGroup = true }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
        }
    }

    if (showAddSubject) {
        var name by remember { mutableStateOf("") }
        var icon by remember { mutableStateOf("📘") }
        FormDialog("New subject", "Create", {
            vm.addSubject(name, icon); showAddSubject = false; activeId = null
        }, { showAddSubject = false }, confirmEnabled = name.isNotBlank()) {
            LTextField(name, { name = it }, placeholder = "e.g. Higher Math, Chemistry, Biology…", label = "Subject name")
            LTextField(icon, { icon = it.take(4) }, label = "Icon (emoji)")
        }
    }
    if (showAddGroup && current != null) {
        var name by remember { mutableStateOf("") }
        FormDialog("New paper / section", "Create", {
            vm.addGroup(current.id, name); showAddGroup = false
        }, { showAddGroup = false }, confirmEnabled = name.isNotBlank()) {
            LTextField(name, { name = it }, placeholder = "e.g. 1st Paper, 2nd Paper, Grammar…", label = "Name")
        }
    }
    if (showRename && current != null) {
        var name by remember { mutableStateOf(current.name) }
        FormDialog("Rename subject", "Save", {
            vm.renameSubject(current.id, name); showRename = false
        }, { showRename = false }, confirmEnabled = name.isNotBlank()) {
            LTextField(name, { name = it }, label = "Subject name")
        }
    }
    subjectToDelete?.let { s ->
        ConfirmDialog("Delete ${s.name}?", "All its chapters and progress will be removed.", "Delete", true,
            onConfirm = { vm.deleteSubject(s.id); subjectToDelete = null; activeId = null }, onDismiss = { subjectToDelete = null })
    }
    groupToDelete?.let { g ->
        ConfirmDialog("Delete \"${g.name}\"?", "All chapters in this paper will be removed.", "Delete", true,
            onConfirm = { current?.let { vm.deleteGroup(it.id, g.id) }; groupToDelete = null }, onDismiss = { groupToDelete = null })
    }
}

@Composable
private fun GroupCard(vm: LambdaViewModel, subject: Subject, group: SubjectGroup, filter: String, onDeleteGroup: () -> Unit) {
    var newChapter by remember(group.id) { mutableStateOf("") }
    val gp = groupProgress(group)
    val visible = group.chapters.filter {
        val p = chapterProgress(it, group.columns)
        when (filter) {
            "pending" -> p < 0.999f
            "done" -> p >= 0.999f
            else -> true
        }
    }
    LCard(title = group.name, action = {
        Text("${(gp * 100).toInt()}%", color = LC.Primary, fontFamily = MonoFont, fontSize = 12.sp)
        LIconButton(Icons.Filled.Delete, "Delete paper", onDeleteGroup, size = 30.dp)
    }) {
        LinearBar(gp)
        Spacer(Modifier.height(12.dp))
        if (visible.isEmpty()) {
            Text(
                if (group.chapters.isEmpty()) "No chapters yet — add one below." else "No chapters match this filter.",
                color = LC.Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visible.forEach { ch -> ChapterCard(vm, subject, group, ch) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LTextField(newChapter, { newChapter = it }, Modifier.weight(1f), placeholder = "Add a chapter…")
            PrimaryButton("", {
                if (newChapter.isNotBlank()) { vm.addChapter(subject.id, group.id, newChapter); newChapter = "" }
            }, icon = Icons.Filled.Add, enabled = newChapter.isNotBlank(), modifier = Modifier.width(52.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterCard(vm: LambdaViewModel, subject: Subject, group: SubjectGroup, ch: Chapter) {
    val progress = chapterProgress(ch, group.columns)
    val shape = RoundedCornerShape(10.dp)
    val isEngg = subject.name.lowercase().let { it.contains("physic") || it.contains("chem") || it.contains("math") }
    val slideLabel = if (isEngg) "ENGG Slide" else "Class Slide"

    var renaming by remember { mutableStateOf(false) }
    var showSlides by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var revMenu by remember { mutableStateOf(false) }

    fun update(f: (Chapter) -> Chapter) = vm.updateChapter(subject.id, group.id, ch.id, f)

    Column(Modifier.fillMaxWidth().clip(shape).background(LC.White05).border(1.dp, LC.White05, shape).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ch.name, color = LC.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp,
                modifier = Modifier.weight(1f).clickable { renaming = true }
            )
            Text("${(progress * 100).toInt()}%", color = if (progress >= 0.999f) LC.Emerald else LC.Muted, fontFamily = MonoFont, fontSize = 11.sp)
            LIconButton(Icons.Filled.Delete, "Delete chapter", { confirmDelete = true }, size = 30.dp)
        }
        Spacer(Modifier.height(6.dp))
        LinearBar(progress, color = if (progress >= 0.999f) LC.Emerald else LC.Primary, height = 4.dp)
        Spacer(Modifier.height(10.dp))

        // stamps
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            group.columns.forEach { col ->
                val on = ch.checks[col] == true
                Chip(col, on, { update { c -> c.copy(checks = c.checks + (col to !on)) } })
            }
        }
        Spacer(Modifier.height(10.dp))

        // lectures + revision + slides
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LIconButton(Icons.Filled.Remove, "One lecture less", { update { c -> c.copy(lecturesDone = maxOf(0, c.lecturesDone - 1)) } }, size = 30.dp)
                Text("${ch.lecturesDone}", color = LC.Text, fontFamily = MonoFont, fontSize = 13.sp)
                Text(" / ", color = LC.Muted, fontSize = 13.sp)
                var totalText by remember(ch.lecturesTotal) { mutableStateOf(if (ch.lecturesTotal > 0) ch.lecturesTotal.toString() else "") }
                androidx.compose.foundation.text.BasicTextField(
                    value = totalText,
                    onValueChange = { t ->
                        if (t.all { it.isDigit() } && t.length <= 3) {
                            totalText = t
                            val v = (t.toIntOrNull() ?: 0).coerceIn(0, 200)
                            update { c -> c.copy(lecturesTotal = v, lecturesDone = if (v > 0) minOf(c.lecturesDone, v) else c.lecturesDone) }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(color = LC.Text, fontFamily = MonoFont, fontSize = 13.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LC.Primary),
                    modifier = Modifier.width(34.dp),
                    decorationBox = { inner -> if (totalText.isEmpty()) Text("–", color = LC.Muted, fontFamily = MonoFont, fontSize = 13.sp); inner() }
                )
                LIconButton(Icons.Filled.Add, "One lecture more", { update { c -> c.copy(lecturesDone = c.lecturesDone + 1) } }, size = 30.dp)
            }
            Box {
                Chip(REVISION_LABELS.getOrElse(ch.revision) { REVISION_LABELS[0] }, ch.revision > 0, { revMenu = true }, accent = LC.Emerald)
                DropdownMenu(expanded = revMenu, onDismissRequest = { revMenu = false }, containerColor = LC.Elevated) {
                    REVISION_LABELS.forEachIndexed { i, l ->
                        DropdownMenuItem(text = { Text(l, color = LC.Text, fontSize = 13.sp) }, onClick = { update { c -> c.copy(revision = i) }; revMenu = false })
                    }
                }
            }
            Chip(
                slideLabel + if (ch.slideLinks.isNotEmpty()) " · ${ch.slideLinks.size}" else "",
                ch.slideLinks.isNotEmpty(), { showSlides = true }
            )
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(ch.name) }
        FormDialog("Rename chapter", "Save", { update { c -> c.copy(name = name.trim().ifBlank { c.name }) }; renaming = false }, { renaming = false }, confirmEnabled = name.isNotBlank()) {
            LTextField(name, { name = it }, label = "Chapter name", singleLine = false)
        }
    }
    if (confirmDelete) ConfirmDialog("Delete chapter?", "\"${ch.name}\" and its progress will be removed.", "Delete", true,
        onConfirm = { vm.deleteChapter(subject.id, group.id, ch.id); confirmDelete = false }, onDismiss = { confirmDelete = false })

    if (showSlides) SlideLinksDialog(slideLabel, ch.slideLinks,
        onAdd = { url -> update { c -> c.copy(slideLinks = c.slideLinks + url) } },
        onRemove = { i -> update { c -> c.copy(slideLinks = c.slideLinks.filterIndexed { idx, _ -> idx != i }) } },
        onDismiss = { showSlides = false })
}

@Composable
private fun SlideLinksDialog(label: String, links: List<String>, onAdd: (String) -> Unit, onRemove: (Int) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    FormDialog("$label & drive links", "Done", onDismiss, onDismiss) {
        Text("Saved on your device. Paste a Google Drive or slide link.", color = LC.Muted, fontSize = 12.sp)
        if (links.isEmpty()) Text("No links yet.", color = LC.Muted, fontSize = 12.sp)
        links.forEachIndexed { i, l ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l, color = LC.Text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                LIconButton(Icons.Filled.OpenInNew, "Open", {
                    val target = if (l.startsWith("http://") || l.startsWith("https://")) l else "https://$l"
                    runCatching { uriHandler.openUri(target) }
                }, size = 30.dp)
                LIconButton(Icons.Filled.ContentCopy, "Copy link", { clipboard.setText(AnnotatedString(l)) }, size = 30.dp)
                LIconButton(Icons.Filled.Close, "Remove link", { onRemove(i) }, size = 30.dp)
            }
        }
        LTextField(url, { url = it }, placeholder = "Paste Drive link (e.g. https://drive.google.com/…)")
        PrimaryButton("Add link", { if (url.isNotBlank()) { onAdd(url.trim()); url = "" } }, Modifier.fillMaxWidth(), icon = Icons.Filled.Link, enabled = url.isNotBlank())
    }
}

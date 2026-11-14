package com.lambda.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.data.Note
import com.lambda.app.data.NoteAttachment
import com.lambda.app.ui.*
import com.lambda.app.util.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var tagFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val active = data.notes.firstOrNull { it.id == activeId }
    if (active != null) {
        NoteEditor(vm, active, onBack = { activeId = null })
        return
    }

    val allTags = data.notes.flatMap { it.tags }.distinct()
    val filtered = data.notes.filter { n ->
        (query.isBlank() || n.title.contains(query, true) || n.content.contains(query, true) || n.tags.any { it.contains(query, true) }) &&
            (tagFilter == null || tagFilter in n.tags)
    }.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })

    ScreenColumn {
        PageHeader("Scratchpad", "Notes", "Jot down high-yield formulas, short notes, attach diagrams & formula sheets, and organize by tags.") {
            PrimaryButton("New note", { activeId = vm.addNote("Untitled note", listOfNotNull(tagFilter ?: "Formula")) }, icon = Icons.Filled.Add)
        }
        LTextField(query, { query = it }, placeholder = "Search notes and tags…", trailing = { Icon(Icons.Filled.Search, null, tint = LC.Muted) })
        if (allTags.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("All", tagFilter == null, { tagFilter = null }, mono = false)
                allTags.forEach { t -> Chip("#$t", tagFilter == t, { tagFilter = if (tagFilter == t) null else t }, mono = false) }
            }
        }
        if (filtered.isEmpty()) {
            LCard { EmptyState(if (data.notes.isEmpty()) "No notes yet" else "No matching notes", "Tap “New note” to start writing.", Icons.Filled.Description) }
        }
        filtered.forEach { n ->
            LCard(Modifier.clickable { activeId = n.id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (n.pinned) { Icon(Icons.Filled.PushPin, null, tint = LC.Primary, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)) }
                    Text(n.title, color = LC.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (n.attachments.isNotEmpty()) {
                        Icon(Icons.Filled.AttachFile, null, tint = LC.Muted, modifier = Modifier.size(14.dp))
                        Text("${n.attachments.size}", color = LC.Muted, fontSize = 11.sp)
                    }
                }
                if (n.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(n.content, color = LC.Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    n.tags.forEach { Badge("#$it", LC.Indigo) }
                    Text(formatUpdated(n.updatedAt), color = LC.Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

private fun formatUpdated(iso: String): String = runCatching {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH))
}.getOrDefault("")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteEditor(vm: LambdaViewModel, note: Note, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var newTag by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var attToRemove by remember { mutableStateOf<NoteAttachment?>(null) }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            vm.addAttachment(note.id, uri) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        }
    }
    val exportMd = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write("# $title\n\n$content".toByteArray()) }
                Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LIconButton(Icons.Filled.ArrowBack, "Back", onBack, tint = LC.Text)
            Spacer(Modifier.weight(1f))
            LIconButton(Icons.Filled.PushPin, "Pin", { vm.updateNote(note.id) { it.copy(pinned = !it.pinned) } }, tint = if (note.pinned) LC.Primary else LC.Muted)
            LIconButton(Icons.Filled.ContentCopy, "Copy note text", {
                clipboard.setText(AnnotatedString("$title\n\n$content"))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            })
            LIconButton(Icons.Filled.FileDownload, "Save a copy (Markdown)", {
                exportMd.launch((title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "note" }) + ".md")
            })
            LIconButton(Icons.Filled.Delete, "Delete note", { confirmDelete = true }, tint = LC.Red)
        }

        LTextField(title, { title = it; vm.updateNote(note.id) { n -> n.copy(title = it) } }, placeholder = "Note Title…")

        LCard(title = "Tags") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                note.tags.forEach { t ->
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(LC.Indigo.copy(alpha = 0.15f))
                            .clickable { vm.updateNote(note.id) { n -> n.copy(tags = n.tags - t) } }.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#$t", color = LC.Indigo, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Close, "Remove tag", tint = LC.Indigo, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LTextField(newTag, { newTag = it }, Modifier.weight(1f), placeholder = "+ tag")
                PrimaryButton("", {
                    val clean = newTag.trim()
                    if (clean.isNotEmpty() && clean !in note.tags) vm.updateNote(note.id) { n -> n.copy(tags = n.tags + clean) }
                    newTag = ""
                }, icon = Icons.Filled.Add, enabled = newTag.isNotBlank(), modifier = Modifier.width(52.dp))
            }
        }

        LCard {
            LTextField(
                content, { content = it; vm.updateNote(note.id) { n -> n.copy(content = it) } },
                placeholder = "Write formulas, study takeaways, practice questions, or markdown notes…",
                singleLine = false, minLines = 12
            )
            Spacer(Modifier.height(6.dp))
            val words = content.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
            Text("$words words · ${content.length} characters", color = LC.Muted, fontSize = 10.sp)
        }

        LCard(title = "Attached files", action = {
            GhostButton("Attach", { pickFiles.launch(arrayOf("*/*")) }, icon = Icons.Filled.AttachFile)
        }) {
            if (note.attachments.isEmpty()) {
                Text("No files attached. Diagrams, PDFs and formula sheets are stored privately on this phone.", color = LC.Muted, fontSize = 12.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                note.attachments.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.InsertDriveFile, null, tint = LC.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, color = LC.Text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(readableSize(a.size), color = LC.Muted, fontSize = 10.sp)
                        }
                        LIconButton(Icons.Filled.OpenInNew, "Open", { openAttachment(context, vm, a) }, size = 32.dp)
                        LIconButton(Icons.Filled.Close, "Remove attachment", { attToRemove = a }, size = 32.dp)
                    }
                }
            }
        }
    }

    if (confirmDelete) ConfirmDialog("Delete this note?", "This will permanently delete this note and its attachments.", "Delete", true,
        onConfirm = { vm.deleteNote(note.id); confirmDelete = false; onBack() }, onDismiss = { confirmDelete = false })
    attToRemove?.let { a ->
        ConfirmDialog("Remove attachment?", "\"${a.name}\" will be deleted from this phone.", "Remove", true,
            onConfirm = { vm.removeAttachment(note.id, a); attToRemove = null }, onDismiss = { attToRemove = null })
    }
}

private fun readableSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}

private fun openAttachment(context: android.content.Context, vm: LambdaViewModel, a: NoteAttachment) {
    val file = vm.attachmentFile(a)
    if (file == null) {
        Toast.makeText(context, "File not found on this phone.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, a.type.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, a.name))
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
    }
}

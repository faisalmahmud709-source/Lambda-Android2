package com.lambda.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lambda.app.data.Task
import com.lambda.app.ui.*
import com.lambda.app.util.TaskRemaining
import com.lambda.app.util.Urgency
import com.lambda.app.util.formatTime24to12

fun urgencyColor(u: Urgency): Color = when (u) {
    Urgency.Done -> LC.Emerald
    Urgency.Overdue -> Color(0xFFFCA5A5)
    Urgency.Urgent -> LC.Amber
    Urgency.Normal -> LC.Muted
}

/** Pill showing "by 10:00 PM · 2h left". */
@Composable
fun RemainingPill(task: Task, remaining: TaskRemaining?, showCountdown: Boolean = true, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(6.dp)
    if (task.dueTime == null) {
        if (onClick != null) {
            Row(
                Modifier.clip(shape).border(1.dp, LC.White10, shape).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Schedule, null, tint = LC.Muted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Time", color = LC.Muted, fontSize = 11.sp, fontFamily = MonoFont)
            }
        }
        return
    }
    val u = remaining?.urgency ?: Urgency.Normal
    val c = if (remaining == null || u == Urgency.Normal) LC.Primary else urgencyColor(u)
    Row(
        Modifier.clip(shape).background(c.copy(alpha = 0.12f)).border(1.dp, c.copy(alpha = 0.3f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Schedule, null, tint = c, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        val text = buildString {
            append("by ").append(formatTime24to12(task.dueTime))
            if (showCountdown && remaining != null && !task.done) append(" · ").append(remaining.label)
        }
        Text(text, color = c, fontSize = 11.sp, fontFamily = MonoFont)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRow(
    task: Task, remaining: TaskRemaining?,
    onToggle: () -> Unit, onDelete: () -> Unit,
    onFocus: (() -> Unit)? = null, onTimeClick: (() -> Unit)? = null, showCountdown: Boolean = true
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(LC.White05).border(1.dp, LC.White05, shape).padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CheckBox(task.done, onToggle)
            Spacer(Modifier.width(10.dp))
            Text(
                task.title, modifier = Modifier.weight(1f).padding(top = 2.dp),
                color = if (task.done) LC.Muted else LC.Text, fontSize = 14.sp, lineHeight = 19.sp,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
            LIconButton(Icons.Filled.Delete, "Delete task", onDelete, size = 30.dp)
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(
            Modifier.padding(start = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RemainingPill(task, remaining, showCountdown, onTimeClick)
            PriorityBadge(task.priority)
            if (onFocus != null && !task.done) {
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(LC.Purple.copy(alpha = 0.15f))
                        .clickable(onClick = onFocus).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFFD8B4FE), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Focus", color = Color(0xFFD8B4FE), fontSize = 11.sp, fontFamily = MonoFont)
                }
            }
        }
    }
}

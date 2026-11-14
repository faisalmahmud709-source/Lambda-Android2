package com.lambda.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneOffset

// ---------------------------------------------------------------- basics

@Composable
fun rememberNow(intervalMs: Long = 1000L): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(intervalMs)
    }
}

fun parseHexColor(hex: String, fallback: Color = LC.Primary): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = LC.Muted) {
    Text(text.uppercase(), modifier = modifier, color = color, fontSize = 10.sp, fontFamily = MonoFont, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun LCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LC.CardBrush)
            .border(1.dp, LC.Border, RoundedCornerShape(14.dp))
            .padding(contentPadding)
    ) {
        if (title != null || action != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (title != null) Text(title, color = LC.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                else Spacer(Modifier.weight(1f))
                action?.invoke()
            }
        }
        content()
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageHeader(eyebrow: String, title: String, description: String? = null, actions: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Eyebrow(eyebrow)
        Spacer(Modifier.height(4.dp))
        Text(title, fontFamily = DisplayFont, fontStyle = FontStyle.Italic, fontSize = 30.sp, color = LC.Text, lineHeight = 34.sp)
        if (description != null) {
            Spacer(Modifier.height(4.dp))
            Text(description, color = LC.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (actions != null) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, hint: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LC.CardBrush)
            .border(1.dp, LC.Border, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(label, Modifier.weight(1f))
            Icon(icon, null, tint = LC.Primary, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = LC.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(hint, color = LC.Muted, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
fun EmptyState(title: String, text: String, icon: ImageVector? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(LC.White05), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = LC.Muted, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(title, color = LC.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        Text(text, color = LC.Muted, fontSize = 11.sp, lineHeight = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ---------------------------------------------------------------- progress

@Composable
fun LinearBar(value: Float, modifier: Modifier = Modifier, color: Color = LC.Primary, height: Dp = 6.dp) {
    val v = value.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(LC.White10)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(v).clip(CircleShape).background(color))
    }
}

@Composable
fun RingProgress(
    value: Float, size: Dp, stroke: Dp,
    color: Color = LC.Primary, track: Color = LC.White10,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2
            val arcSize = Size(this.size.width - s, this.size.height - s)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(s))
            drawArc(color, -90f, 360f * value.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

// ---------------------------------------------------------------- buttons & chips

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LC.Primary, contentColor = LC.OnPrimary,
            disabledContainerColor = LC.White10, disabledContentColor = LC.Muted
        ),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GhostButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null,
    color: Color = LC.Text, borderColor: Color = LC.White10, enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color, disabledContentColor = LC.Muted),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(16.dp), tint = if (enabled) LC.Primary else LC.Muted); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, tint: Color = LC.Muted, size: Dp = 36.dp) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = LC.Primary, mono: Boolean = true) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.2f) else LC.White05)
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else LC.White05, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 11.sp, fontFamily = if (mono) MonoFont else null,
            color = if (selected) accent else Color(0xB3FFFFFF),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = color, fontSize = 9.sp, fontFamily = MonoFont, fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun PriorityBadge(priority: String) {
    val c = when (priority) {
        "p1" -> LC.Red
        "p2" -> LC.Primary
        else -> Color(0x66FFFFFF)
    }
    Badge(priority.uppercase(), c)
}

@Composable
fun CheckBox(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier.size(22.dp).clip(shape)
            .background(if (checked) LC.Primary else LC.White05)
            .border(1.dp, if (checked) LC.Primary else Color(0x33FFFFFF), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
    }
}

// ---------------------------------------------------------------- inputs

@Composable
fun LTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    placeholder: String = "", label: String? = null, singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text, minLines: Int = 1, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color(0x66FFFFFF), fontSize = 13.sp) },
        label = label?.let { { Text(it, fontSize = 12.sp) } },
        singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = LC.Text),
        shape = RoundedCornerShape(10.dp),
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LC.Primary, unfocusedBorderColor = LC.White10,
            focusedContainerColor = LC.Field, unfocusedContainerColor = LC.Field,
            focusedLabelColor = LC.Primary, unfocusedLabelColor = LC.Muted,
            cursorColor = LC.Primary, focusedTextColor = LC.Text, unfocusedTextColor = LC.Text
        )
    )
}

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", label: String? = null) {
    LTextField(
        value = value,
        onValueChange = { s -> if (s.all { it.isDigit() || it == '.' } && s.count { it == '.' } <= 1) onValueChange(s) },
        modifier = modifier, placeholder = placeholder, label = label, keyboardType = KeyboardType.Decimal
    )
}

// ---------------------------------------------------------------- dialogs

@Composable
fun ConfirmDialog(
    title: String, description: String, confirmText: String = "Confirm", danger: Boolean = false,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LC.Elevated,
        title = { Text(title, color = LC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = { Text(description, color = LC.Muted, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = if (danger) LC.Red else LC.Primary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = LC.Muted) } }
    )
}

@Composable
fun FormDialog(
    title: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    confirmEnabled: Boolean = true, content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LC.Elevated,
        title = { Text(title, color = LC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmText, color = if (confirmEnabled) LC.Primary else LC.Muted, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = LC.Muted) } }
    )
}

/** Material date picker returning a yyyy-MM-dd string. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LDatePickerDialog(initial: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val initMs = initial?.let { runCatching { java.time.LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initMs)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                }
                onDismiss()
            }) { Text("OK", color = LC.Primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = LC.Muted) } },
        colors = DatePickerDefaults.colors(containerColor = LC.Elevated)
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = LC.Elevated,
                selectedDayContainerColor = LC.Primary, selectedDayContentColor = LC.OnPrimary,
                todayContentColor = LC.Primary, todayDateBorderColor = LC.Primary
            )
        )
    }
}

/** "By when" picker: hour / minute / AM-PM, same behaviour as the web version. */
@Composable
fun TimeTargetDialog(
    title: String, initialTime24: String?, onSave: (String) -> Unit, onClear: (() -> Unit)?, onDismiss: () -> Unit
) {
    val init = remember { com.lambda.app.util.parseTime24(initialTime24 ?: "22:00") }
    var hour by remember { mutableIntStateOf(init.hour) }
    var minute by remember { mutableIntStateOf(init.minute) }
    var pm by remember { mutableStateOf(init.pm) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LC.Elevated,
        title = { Text(title, color = LC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Presets")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("10:00 PM", hour == 10 && minute == 0 && pm, { hour = 10; minute = 0; pm = true })
                    Chip("11:30 PM", hour == 11 && minute == 30 && pm, { hour = 11; minute = 30; pm = true })
                    Chip("1:00 AM", hour == 1 && minute == 0 && !pm, { hour = 1; minute = 0; pm = false })
                }
                Eyebrow("Hour")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in listOf(1..6, 7..12)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (h in row) Chip(h.toString(), hour == h, { hour = h }, Modifier.weight(1f))
                        }
                    }
                }
                Eyebrow("Minute")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in listOf(0, 15, 30, 45)) Chip("%02d".format(m), minute == m, { minute = m }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("AM", !pm, { pm = false }, Modifier.weight(1f))
                    Chip("PM", pm, { pm = true }, Modifier.weight(1f))
                }
                Text("Selected: $hour:${"%02d".format(minute)} ${if (pm) "PM" else "AM"}", color = LC.Primary, fontFamily = MonoFont, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(com.lambda.app.util.formatTime12to24(hour, minute, pm)) }) {
                Text("Save", color = LC.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = onClear) { Text("Clear", color = LC.Red) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = LC.Muted) }
            }
        }
    )
}

/** Scrollable page container used by every screen. */
@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        content()
        Spacer(Modifier.navigationBarsPadding().height(32.dp))
    }
}

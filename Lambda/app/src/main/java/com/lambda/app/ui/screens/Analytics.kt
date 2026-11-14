package com.lambda.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.ui.*
import com.lambda.app.util.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private data class DayPoint(
    val date: String, val label: String, val studyHours: Double,
    val sleep: Double?, val screen: Double?, val tasksDone: Int, val habitPct: Int
)

@Composable
fun AnalyticsScreen(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableIntStateOf(14) }

    val points = remember(data, range) {
        val habits = data.habits
        (range - 1 downTo 0).map { back ->
            val d = LocalDate.now().minusDays(back.toLong())
            val ds = d.toString()
            val sessionMins = data.sessions.filter { it.date == ds }.sumOf { it.minutes }
            val extra = data.dailyLogs[ds]?.extraStudyHours ?: 0.0
            val tasksDone = data.tasks.count { t ->
                t.done && t.completedAt != null &&
                    runCatching { Instant.parse(t.completedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString() }.getOrNull() == ds
            }
            val hd = habits.count { it.log[ds] == true }
            DayPoint(
                ds, "${d.dayOfMonth}/${d.monthValue}", ((sessionMins / 60.0 + extra) * 10).roundToInt() / 10.0,
                data.dailyLogs[ds]?.sleepHours, data.dailyLogs[ds]?.screenHours, tasksDone,
                if (habits.isNotEmpty()) (hd * 100.0 / habits.size).roundToInt() else 0
            )
        }
    }
    val dates = points.map { it.date }.toSet()
    val breakdown = remember(data, range) {
        val map = linkedMapOf<String, Int>()
        data.sessions.filter { it.date in dates }.forEach { s ->
            val n = data.subjects.firstOrNull { it.id == s.subjectId }?.name ?: "General"
            map[n] = (map[n] ?: 0) + s.minutes
        }
        map.entries.sortedByDescending { it.value }
    }

    val totalHours = points.sumOf { it.studyHours }
    val avgDaily = totalHours / range
    val goalDays = points.count { it.studyHours >= data.settings.dailyStudyGoalHours }
    val tasksDone = points.sumOf { it.tasksDone }
    val sleepVals = points.mapNotNull { it.sleep }
    val screenVals = points.mapNotNull { it.screen }
    val avgSleep = if (sleepVals.isNotEmpty()) "%.1fh".format(sleepVals.average()) else "—"
    val avgScreen = if (screenVals.isNotEmpty()) "%.1fh".format(screenVals.average()) else "—"
    val avgHabit = (points.sumOf { it.habitPct } / max(1, points.size))
    val labels = points.map { it.label }

    ScreenColumn {
        PageHeader("Patterns", "Analytics", "Every number here comes from your timers, tasks, habits and daily logs.")

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(7, 14, 30).forEach { Chip("$it days", range == it, { range = it }, mono = false) }
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Studied", "%.1fh".format(totalHours), "avg %.1fh / day".format(avgDaily), Icons.Filled.HourglassTop, Modifier.weight(1f).fillMaxHeight())
            StatCard("Goal days", "$goalDays/$range", "hit ${data.settings.dailyStudyGoalHours}h goal", Icons.Filled.Flag, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Tasks done", "$tasksDone", "in this range", Icons.Filled.Checklist, Modifier.weight(1f).fillMaxHeight())
            StatCard("Habits", "$avgHabit%", "avg completion", Icons.Filled.LocalFireDepartment, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Avg sleep", avgSleep, "per logged day", Icons.Filled.Bedtime, Modifier.weight(1f).fillMaxHeight())
            StatCard("Avg screen", avgScreen, "per logged day", Icons.Filled.PhoneAndroid, Modifier.weight(1f).fillMaxHeight())
        }

        LCard(title = "Study hours") {
            BarChart(points.map { it.studyHours.toFloat() }, labels, LC.Primary, goal = data.settings.dailyStudyGoalHours.toFloat())
            Spacer(Modifier.height(6.dp))
            Text("Dashed line = your daily goal (${data.settings.dailyStudyGoalHours}h)", color = LC.Muted, fontSize = 10.sp)
        }

        LCard(title = "Timed study by subject") {
            if (breakdown.isEmpty()) {
                EmptyState("No timed sessions in this range", "Use the Pomodoro or Work session timers and they'll show up here.")
            } else {
                val max = breakdown.first().value.toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    breakdown.forEachIndexed { i, e ->
                        val c = LC.ChartColors[i % LC.ChartColors.size]
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                                Spacer(Modifier.width(8.dp))
                                Text(e.key, color = LC.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text(formatMinutes(e.value), color = LC.Muted, fontFamily = MonoFont, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearBar(e.value / max, color = c)
                        }
                    }
                }
            }
        }

        LCard(title = "Sleep & screen time") {
            LineChart(
                listOf(points.map { it.sleep?.toFloat() }, points.map { it.screen?.toFloat() }),
                listOf(LC.Indigo, LC.Red), labels
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend("Sleep (h)", LC.Indigo)
                Legend("Screen (h)", LC.Red)
            }
        }

        LCard(title = "Habit completion trend") {
            LineChart(listOf(points.map { it.habitPct.toFloat() }), listOf(LC.Emerald), labels, fixedMax = 100f)
            Spacer(Modifier.height(6.dp))
            Text("% of your habits completed each day", color = LC.Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun Legend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = LC.Muted, fontSize = 11.sp)
    }
}

private fun labelStep(n: Int) = max(1, ceil(n / 6.0).toInt())

@Composable
private fun BarChart(values: List<Float>, labels: List<String>, color: Color, goal: Float? = null, height: Dp = 150.dp) {
    val tm = rememberTextMeasurer()
    val axisStyle = TextStyle(color = LC.Muted, fontSize = 9.sp)
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val n = values.size
        if (n == 0) return@Canvas
        val topPad = 8.dp.toPx()
        val bottomPad = 16.dp.toPx()
        val chartH = size.height - topPad - bottomPad
        val maxV = max(max(values.maxOrNull() ?: 0f, goal ?: 0f), 1f) * 1.1f
        val slot = size.width / n
        val barW = slot * 0.62f
        values.forEachIndexed { i, v ->
            val h = chartH * (v / maxV)
            drawRoundRect(
                color, Offset(i * slot + (slot - barW) / 2, topPad + chartH - h),
                Size(barW, max(h, 0f)), CornerRadius(3.dp.toPx())
            )
        }
        if (goal != null) {
            val y = topPad + chartH - chartH * (goal / maxV)
            drawLine(LC.Emerald.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
        val step = labelStep(n)
        labels.forEachIndexed { i, l ->
            if (i % step == 0 || i == n - 1) {
                val layout = tm.measure(l, axisStyle)
                val x = (i * slot + slot / 2 - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(x, size.height - bottomPad + 3.dp.toPx()))
            }
        }
    }
}

@Composable
private fun LineChart(series: List<List<Float?>>, colors: List<Color>, labels: List<String>, fixedMax: Float? = null, height: Dp = 150.dp) {
    val tm = rememberTextMeasurer()
    val axisStyle = TextStyle(color = LC.Muted, fontSize = 9.sp)
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val n = labels.size
        if (n == 0) return@Canvas
        val topPad = 8.dp.toPx()
        val bottomPad = 16.dp.toPx()
        val chartH = size.height - topPad - bottomPad
        val maxV = fixedMax ?: max(series.flatten().filterNotNull().maxOrNull() ?: 1f, 1f) * 1.15f
        val slot = size.width / n
        // grid
        for (g in 0..3) {
            val y = topPad + chartH * g / 3f
            drawLine(Color(0x14FFFFFF), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        series.forEachIndexed { si, s ->
            val c = colors[si % colors.size]
            var prev: Offset? = null
            s.forEachIndexed { i, v ->
                if (v == null) { prev = null; return@forEachIndexed }
                val p = Offset(i * slot + slot / 2, topPad + chartH - chartH * (v / maxV))
                prev?.let { drawLine(c, it, p, 2.dp.toPx()) }
                drawCircle(c, 3.dp.toPx(), p)
                prev = p
            }
        }
        val step = labelStep(n)
        labels.forEachIndexed { i, l ->
            if (i % step == 0 || i == n - 1) {
                val layout = tm.measure(l, axisStyle)
                val x = (i * slot + slot / 2 - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(x, size.height - bottomPad + 3.dp.toPx()))
            }
        }
    }
}

package com.lambda.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.app.LambdaViewModel
import com.lambda.app.R
import com.lambda.app.ui.screens.*
import com.lambda.app.util.countdown
import com.lambda.app.util.hhmmss
import kotlinx.coroutines.launch

enum class Route(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Filled.Dashboard),
    Planner("Planner", Icons.Filled.CalendarMonth),
    Work("Work session", Icons.Filled.PlayArrow),
    Pomodoro("Pomodoro", Icons.Filled.HourglassTop),
    Academics("Academics", Icons.Filled.School),
    Habits("Habits", Icons.Filled.LocalFireDepartment),
    DailyLog("Daily log", Icons.Filled.Assignment),
    Analytics("Analytics", Icons.Filled.BarChart),
    Notes("Notes", Icons.Filled.Description),
    Goals("Goals", Icons.Filled.TrackChanges),
    Settings("Settings & data", Icons.Filled.Settings)
}

@Composable
fun LambdaApp(vm: LambdaViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val pomo by vm.pomodoro.collectAsStateWithLifecycle()

    Box(
        Modifier.fillMaxSize().background(LC.Bg).background(
            Brush.radialGradient(listOf(Color(0x148B5CF6), Color.Transparent), radius = 1200f)
        )
    ) {
        if (!data.onboarded) {
            OnboardingScreen(vm)
            return@Box
        }

        var route by rememberSaveable { mutableStateOf(Route.Dashboard) }
        var zenOpen by rememberSaveable { mutableStateOf(false) }
        var zenTask by rememberSaveable { mutableStateOf<String?>(null) }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val now by rememberNow(500)

        fun go(r: Route) { route = r; scope.launch { drawerState.close() } }
        fun openZen(taskId: String?) { zenTask = taskId; zenOpen = true; scope.launch { drawerState.close() } }

        BackHandler(enabled = zenOpen) { zenOpen = false }
        BackHandler(enabled = !zenOpen && route != Route.Dashboard && !drawerState.isOpen) { route = Route.Dashboard }

        val appName = data.settings.appName.ifBlank { "Lambda" }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !zenOpen,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = LC.Sidebar, modifier = Modifier.width(300.dp)) {
                    Column(Modifier.statusBarsPadding().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.lambda_logo), null, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(appName, fontFamily = DisplayFont, fontStyle = FontStyle.Italic, fontSize = 22.sp, color = LC.Text)
                                Eyebrow("Personal OS")
                            }
                        }
                        // live timers
                        val w = data.activeWork
                        if (w != null || pomo.running) {
                            Spacer(Modifier.height(14.dp))
                            if (w != null) {
                                val ms = w.accumulatedMs + (w.runningSince?.let { now - it } ?: 0L)
                                LiveChip("Work", hhmmss(ms), w.runningSince != null) { go(Route.Work) }
                            }
                            if (pomo.running) {
                                val rem = pomo.endsAt?.let { maxOf(0L, it - now) } ?: pomo.remainingMs
                                Spacer(Modifier.height(6.dp))
                                LiveChip("Pomodoro", countdown(rem), true) { go(Route.Pomodoro) }
                            }
                        }
                    }
                    HorizontalDivider(color = LC.Border)
                    Column(Modifier.weight(1f, fill = false)) {
                        Route.values().forEach { r ->
                            NavigationDrawerItem(
                                label = { Text(r.label, fontSize = 14.sp) },
                                icon = { Icon(r.icon, null, Modifier.size(20.dp)) },
                                selected = route == r && !zenOpen,
                                onClick = { go(r) },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = LC.Purple.copy(alpha = 0.14f),
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor = LC.Primary, unselectedTextColor = LC.Text,
                                    selectedIconColor = LC.Primary, unselectedIconColor = LC.Muted
                                )
                            )
                        }
                        NavigationDrawerItem(
                            label = { Text("Zen focus mode", fontSize = 14.sp) },
                            icon = { Icon(Icons.Filled.AutoAwesome, null, Modifier.size(20.dp)) },
                            selected = false,
                            onClick = { openZen(null) },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color(0xFFD8B4FE), unselectedIconColor = Color(0xFFD8B4FE)
                            )
                        )
                    }
                    Spacer(Modifier.navigationBarsPadding().height(12.dp))
                }
            }
        ) {
            Column(Modifier.fillMaxSize()) {
                // top bar
                Row(
                    Modifier.fillMaxWidth().background(LC.Bg.copy(alpha = 0.92f)).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1f).clickable { go(Route.Dashboard) }, verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.lambda_logo), null, Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(10.dp))
                        Text(appName, fontFamily = DisplayFont, fontStyle = FontStyle.Italic, fontSize = 22.sp, color = LC.Text)
                    }
                    LIconButton(Icons.Filled.AutoAwesome, "Zen mode", { openZen(null) }, tint = Color(0xFFD8B4FE))
                    LIconButton(Icons.Filled.Menu, "Open menu", { scope.launch { drawerState.open() } }, tint = LC.Text)
                }
                Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
                    key(route) {
                        when (route) {
                            Route.Dashboard -> DashboardScreen(vm, onNavigate = { go(it) }, onZen = { openZen(it) })
                            Route.Planner -> PlannerScreen(vm)
                            Route.Work -> WorkScreen(vm)
                            Route.Pomodoro -> PomodoroScreen(vm)
                            Route.Academics -> AcademicsScreen(vm)
                            Route.Habits -> HabitsScreen(vm)
                            Route.DailyLog -> DailyLogScreen(vm)
                            Route.Analytics -> AnalyticsScreen(vm)
                            Route.Notes -> NotesScreen(vm)
                            Route.Goals -> GoalsScreen(vm)
                            Route.Settings -> SettingsScreen(vm)
                        }
                    }
                }
            }
        }

        if (zenOpen) {
            ZenScreen(vm, zenTask, onClose = { zenOpen = false })
        }
    }
}

@Composable
private fun LiveChip(label: String, value: String, running: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LC.Primary.copy(alpha = 0.1f)).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (running) LC.Emerald else LC.Amber))
        Spacer(Modifier.width(8.dp))
        Text(label, color = LC.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = LC.Primary, fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

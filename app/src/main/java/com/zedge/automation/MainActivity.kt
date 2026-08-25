package com.zedge.automation

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zedge.automation.config.AppConfig
import com.zedge.automation.ui.GlowDot
import com.zedge.automation.ui.screens.AiStudioScreen
import com.zedge.automation.ui.screens.DistributeScreen
import com.zedge.automation.ui.screens.HomeScreen
import com.zedge.automation.ui.screens.ScheduleScreen
import com.zedge.automation.ui.screens.SettingsScreen
import com.zedge.automation.ui.screens.StableAudioLoginScreen
import com.zedge.automation.ui.screens.UploadQueueScreen
import com.zedge.automation.ui.theme.AccentGradient
import com.zedge.automation.ui.theme.GlassBackground
import com.zedge.automation.ui.theme.GlassBorder
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.SkyBlueLight
import com.zedge.automation.ui.theme.SoftRed
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.ZedgeTheme
import com.zedge.automation.viewmodel.MainViewModel
import java.util.Locale

// Same tabs as the web dashboard's top nav, now as a floating glass dock.
// NOTE: plain data class + function (NOT sealed class objects) — sealed-class
// companion lists can contain nulls due to class-init ordering and crash at launch.
data class Tab(val route: String, val label: String, val icon: ImageVector)

private fun allTabs(): List<Tab> = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("upload", "Queue", Icons.Filled.CloudUpload),
    Tab("aistudio", "Studio", Icons.Filled.AutoFixHigh),
    Tab("schedule", "Calendar", Icons.Filled.CalendarMonth),
    Tab("distribute", "Distribute", Icons.Filled.Share)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZedgeTheme { ZedgeApp() }
        }
    }
}

@Composable
fun ZedgeApp(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val tabs = remember { allTabs() }
    val currentRoute = backStack?.destination?.route ?: "home"
    val activeProject by vm.activeProject.collectAsState()
    val navigationEvent by vm.navigationEvent.collectAsState()

    // Handle navigation events (e.g., auto-recovery from API limit)
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
            vm.clearNavigationEvent()
        }
    }

    // ── Back button handling ──
    // On the home tab (root destination) the system back would exit the app.
    // 1) If a bulk run is active, warn first — exiting kills the run midway.
    // 2) Otherwise require a double press within 2s ("press again to exit").
    val context = LocalContext.current
    val bulkStatuses by vm.bulkStatuses.collectAsState()
    val bulkRunning = bulkStatuses.any {
        it.status == "Pending" || it.status == "Composing" ||
            it.status == "Processing audio..." || it.status == "Generating metadata..."
    }
    var showExitDialog by remember { mutableStateOf(false) }
    var lastBackPressMs by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = currentRoute == "home") {
        if (bulkRunning) {
            showExitDialog = true
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressMs < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressMs = now
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Bulk generation running!", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Exiting now will stop the run midway — remaining ringtones won't be generated " +
                        "and Stable Audio credits may be wasted. Are you sure you want to exit?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    vm.stopBulkGeneration()
                    (context as? Activity)?.finish()
                }) { Text("Exit anyway", color = SoftRed) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Keep running") }
            }
        )
    }

    GlassBackground {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassHeader(
                activeProject = activeProject,
                onSettingsClick = { navController.navigate("settings") { launchSingleTop = true } },
                onProjectSelect = { key -> vm.connectToDatabase(key) }
            )
        },
        bottomBar = {
            FloatingGlassNav(tabs = tabs, currentRoute = currentRoute) { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            NavHost(
                navController    = navController,
                startDestination = "home",
                // Default: sub-screen slide-in from right
                enterTransition  = { slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)) },
                exitTransition   = { slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(200)) },
                popEnterTransition  = { slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(280)) },
                popExitTransition   = { slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200)) }
            ) {
                // ── Bottom-tab screens: scale+fade (feels like tab switch) ──
                val tabEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
                    scaleIn(tween(220), initialScale = 0.94f) + fadeIn(tween(220))
                }
                val tabExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
                    scaleOut(tween(180), targetScale = 0.96f) + fadeOut(tween(150))
                }

                composable("home",
                    enterTransition = tabEnter, exitTransition = tabExit,
                    popEnterTransition = tabEnter, popExitTransition = tabExit
                ) { HomeScreen(vm, onViewAll = { navController.navigate("upload") { launchSingleTop = true } }) }

                composable("upload",
                    enterTransition = tabEnter, exitTransition = tabExit,
                    popEnterTransition = tabEnter, popExitTransition = tabExit
                ) { UploadQueueScreen(vm) }

                composable("aistudio",
                    enterTransition = tabEnter, exitTransition = tabExit,
                    popEnterTransition = tabEnter, popExitTransition = tabExit
                ) { AiStudioScreen(vm) }

                composable("schedule",
                    enterTransition = tabEnter, exitTransition = tabExit,
                    popEnterTransition = tabEnter, popExitTransition = tabExit
                ) { ScheduleScreen(vm) }

                composable("distribute",
                    enterTransition = tabEnter, exitTransition = tabExit,
                    popEnterTransition = tabEnter, popExitTransition = tabExit
                ) { DistributeScreen(vm) }

                // ── Sub-screens: slide from right (default transitions apply) ──
                composable("settings") {
                    SettingsScreen(vm, onStableAudioLogin = {
                        navController.navigate("stable-audio-login")
                    })
                }
                composable("stable-audio-login") {
                    StableAudioLoginScreen(vm, onBack = { navController.popBackStack() })
                }
            }
        }
    }
    }
}

/**
 * New glass header — glowing bolt logo, spaced-out brand type,
 * frosted settings orb and a cyan project chip with live dot.
 */
@Composable
private fun GlassHeader(
    activeProject: String,
    onSettingsClick: () -> Unit,
    onProjectSelect: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0x29FFFFFF), Color(0x00FFFFFF))))
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Glowing logo orb
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(56.dp).background(
                        Brush.radialGradient(listOf(Color(0x66A855F7), Color.Transparent)),
                        CircleShape
                    )
                )
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentGradient)
                        .border(1.dp, Color(0x59FFFFFF), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "AUTOMATION HUB",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    maxLines = 1
                )
                Text(
                    "Unified Stream Console",
                    color = SkyBlueLight,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            // Frosted settings orb
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, GlassBorder, CircleShape)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            // Project selector chip — cyan glass with live dot
            Box {
                var expanded by remember { mutableStateOf(false) }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x2922D3EE))
                        .border(1.dp, Color(0x5922D3EE), RoundedCornerShape(20.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlowDot(MintGreen, 6)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        activeProject.uppercase(Locale.US),
                        color = SkyBlueLight,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = SkyBlueLight)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    AppConfig.FIREBASE_PROJECTS.keys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key.uppercase(Locale.US)) },
                            onClick = {
                                expanded = false
                                onProjectSelect(key)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating glass dock — replaces the old full-width Material NavigationBar.
 * The selected tab expands into a neon gradient pill with its label.
 */
@Composable
private fun FloatingGlassNav(
    tabs: List<Tab>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp, top = 4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xD9130C2B))
                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                // Spring bounce when a tab becomes selected
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessLow
                    ),
                    label = "navIconScale_${tab.route}"
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (selected) Modifier.background(AccentGradient).border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp)) else Modifier)
                        .clickable { onNavigate(tab.route) }
                        .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) Color.White else TextMuted,
                        modifier = Modifier.size((21 * iconScale).dp)
                    )
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tab.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

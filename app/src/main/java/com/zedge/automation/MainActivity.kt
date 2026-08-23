package com.zedge.automation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zedge.automation.config.AppConfig
import com.zedge.automation.ui.screens.AiStudioScreen
import com.zedge.automation.ui.screens.DistributeScreen
import com.zedge.automation.ui.screens.HomeScreen
import com.zedge.automation.ui.screens.ScheduleScreen
import com.zedge.automation.ui.screens.SettingsScreen
import com.zedge.automation.ui.screens.StableAudioLoginScreen
import com.zedge.automation.ui.screens.UploadQueueScreen
import com.zedge.automation.ui.theme.AccentGradient
import com.zedge.automation.ui.theme.HeaderDark
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.ZedgeTheme
import com.zedge.automation.viewmodel.MainViewModel
import java.util.Locale

// Same tabs as the web dashboard's top nav, now as a mobile bottom bar.
// NOTE: plain data class + function (NOT sealed class objects) — sealed-class
// companion lists can contain nulls due to class-init ordering and crash at launch.
data class Tab(val route: String, val label: String, val icon: ImageVector)

private fun allTabs(): List<Tab> = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("upload", "Queue", Icons.Filled.CloudUpload),
    Tab("aistudio", "AI Studio", Icons.Filled.AutoFixHigh),
    Tab("schedule", "Calendar", Icons.Filled.CalendarMonth),
    Tab("distribute", "Distribute", Icons.Filled.Share),
    Tab("settings", "Settings", Icons.Filled.Settings)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Custom "Automation Hub" style header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(HeaderDark)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(46.dp).background(AccentGradient, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Automation Hub",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        "Unified Stream Console",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            activeProject.uppercase(Locale.US),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AppConfig.FIREBASE_PROJECTS.keys.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(key.uppercase(Locale.US)) },
                                onClick = {
                                    expanded = false
                                    vm.connectToDatabase(key)
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = HeaderDark) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryPink,
                            selectedTextColor = PrimaryPink,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x33F27E9D)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavHost(navController, startDestination = "home") {
                composable("home") { HomeScreen(vm, onViewAll = { navController.navigate("upload") { launchSingleTop = true } }) }
                composable("upload") { UploadQueueScreen(vm) }
                composable("aistudio") { AiStudioScreen(vm) }
                composable("schedule") { ScheduleScreen(vm) }
                composable("distribute") { DistributeScreen(vm) }
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

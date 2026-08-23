package com.zedge.automation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.zedge.automation.ui.screens.UploadQueueScreen
import com.zedge.automation.ui.theme.HeaderDark
import com.zedge.automation.ui.theme.PrimaryPink
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.ZedgeTheme
import com.zedge.automation.viewmodel.MainViewModel

// Same tabs as the web dashboard's top nav, now as a mobile bottom bar.
// NOTE: plain data class + function (NOT sealed class objects) — sealed-class
// companion lists can contain nulls due to class-init ordering and crash at launch.
data class Tab(val route: String, val label: String, val icon: ImageVector)

private fun allTabs(): List<Tab> = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("upload", "Queue", Icons.Filled.CloudUpload),
    Tab("aistudio", "AI Studio", Icons.Filled.AutoFixHigh),
    Tab("schedule", "Schedule", Icons.Filled.CalendarMonth),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZedgeApp(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val tabs = remember { allTabs() }
    val currentRoute = backStack?.destination?.route ?: "home"
    val activeProject by vm.activeProject.collectAsState()

    Scaffold(
        topBar = {
            Column(Modifier.background(HeaderDark)) {
                TopAppBar(
                    title = { Text("\uD83C\uDFA7 Zedge Automation Publish", color = Color.White, style = MaterialTheme.typography.titleMedium) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderDark),
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = activeProject,
                                onValueChange = {},
                                readOnly = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor().padding(end = 8.dp).fillMaxWidth(0.42f)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                AppConfig.FIREBASE_PROJECTS.keys.forEach { key ->
                                    DropdownMenuItem(
                                        text = { Text(key) },
                                        onClick = {
                                            expanded = false
                                            vm.connectToDatabase(key)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
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
                            indicatorColor = Color(0x1AFF4757)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavHost(navController, startDestination = "home") {
                composable("home") { HomeScreen(vm) }
                composable("upload") { UploadQueueScreen(vm) }
                composable("aistudio") { AiStudioScreen(vm) }
                composable("schedule") { ScheduleScreen(vm) }
                composable("distribute") { DistributeScreen(vm) }
                composable("settings") { SettingsScreen(vm) }
            }
        }
    }
}

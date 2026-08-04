package com.example.radioapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.radioapp.data.StationRepository
import com.example.radioapp.player.RadioController
import com.example.radioapp.ui.components.MiniPlayer
import com.example.radioapp.ui.navigation.AppNavigation
import com.example.radioapp.ui.navigation.Screen
import java.io.File

class MainActivity : ComponentActivity() {
    
    private lateinit var radioController: RadioController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val crashFile = File(filesDir, "crash_log.txt")
        var previousCrash: String? = null
        if (crashFile.exists()) {
            previousCrash = crashFile.readText()
            crashFile.delete()
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                crashFile.writeText("CRASH on ${thread.name}:\n${throwable.stackTraceToString()}")
            } catch (e: Exception) {
                // Ignore if we can't write
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        val repository = StationRepository(this)
        radioController = RadioController(this)
        
        lifecycleScope.launch {
            radioController.sleepTimerExpired.collect {
                finishAndRemoveTask()
            }
        }
        
        setContent {
            val themeMode by repository.preferences.themeModeFlow.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val colorScheme = if (isDarkTheme) {
                androidx.compose.material3.darkColorScheme()
            } else {
                androidx.compose.material3.lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                var showCrashDialog by remember { mutableStateOf(previousCrash != null) }
                if (showCrashDialog) {
                    AlertDialog(
                        onDismissRequest = { showCrashDialog = false },
                        title = { Text("Previous Crash Log") },
                        text = { Text(previousCrash ?: "") },
                        confirmButton = {
                            Button(onClick = { showCrashDialog = false }) { Text("OK") }
                        }
                    )
                }
                RadioAppScaffold(repository, radioController)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        radioController.release()
    }
}

@Composable
fun RadioAppScaffold(repository: StationRepository, radioController: RadioController) {
    val navController = rememberNavController()
    
    LaunchedEffect(Unit) {
        repository.loadInitialStationsIfNeeded()
        val recents = repository.getRecentStations().firstOrNull()
        if (!recents.isNullOrEmpty()) {
            radioController.play(recents.first())
        }
    }

    val items = listOf(
        Triple(Screen.Browse.route, "Browse", Icons.Filled.Search),
        Triple(Screen.Favourites.route, "Favourites", Icons.Filled.Favorite),
        Triple(Screen.Settings.route, "Settings", Icons.Filled.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                MiniPlayer(radioController = radioController, repository = repository)
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AppNavigation(
            navController = navController,
            paddingValues = paddingValues,
            repository = repository,
            radioController = radioController
        )
    }
}

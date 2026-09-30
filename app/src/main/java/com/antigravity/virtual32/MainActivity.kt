package com.antigravity.virtual32

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import com.antigravity.virtual32.ui.screens.HomeScreen
import com.antigravity.virtual32.ui.screens.AnswersScreen
import com.antigravity.virtual32.ui.screens.SimulatorScreen
import com.antigravity.virtual32.ui.screens.SettingsScreen
import com.antigravity.virtual32.ui.theme.Virtual32Theme

enum class Screen {
    Home, Answers, Simulator, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Virtual32Theme {
                var currentScreen by remember { mutableStateOf(Screen.Home) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == Screen.Home,
                                onClick = { currentScreen = Screen.Home },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Answers,
                                onClick = { currentScreen = Screen.Answers },
                                icon = { Icon(Icons.Default.List, contentDescription = "Answers") },
                                label = { Text("Answers") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Simulator,
                                onClick = { currentScreen = Screen.Simulator },
                                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Simulator") },
                                label = { Text("Simulator") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Settings,
                                onClick = { currentScreen = Screen.Settings },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            Screen.Home -> HomeScreen()
                            Screen.Answers -> AnswersScreen(onBack = { currentScreen = Screen.Home })
                            Screen.Simulator -> SimulatorScreen(onBack = { currentScreen = Screen.Home })
                            Screen.Settings -> SettingsScreen(onBack = { currentScreen = Screen.Home })
                        }
                    }
                }
            }
        }
    }
}

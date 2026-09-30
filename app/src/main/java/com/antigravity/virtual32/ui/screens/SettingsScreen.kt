package com.antigravity.virtual32.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.virtual32.ui.components.DottedBackgroundBox

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToAiSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    
    val settings by viewModel.settings.collectAsState(initial = null)
    val stats by viewModel.usageStats.collectAsState()
    val context = LocalContext.current

    var exportIncludeKeys by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { 
            context.contentResolver.openOutputStream(it)?.use { stream ->
                viewModel.exportSettings(stream, includeKeys = exportIncludeKeys)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                viewModel.importSettings(stream)
            }
        }
    }

    DottedBackgroundBox(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Usage Stats Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Usage Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Photos today: ${stats.photosToday}")
                    Text("OK: ${stats.okCount} | Unclear: ${stats.unclearCount} | Error: ${stats.errorCount}")
                    Text("Avg Latency: ${stats.avgLatency} ms")
                    Text("Provider usage: ${stats.geminiCount} Gemini, ${stats.claudeCount} Claude")
                    Text("Rate limits hit (429): ${stats.rateLimitCount}")
                }
            }

            // Haptics
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Haptic Feedback (vibrate on ready)")
                    Switch(
                        checked = settings?.enableHaptics ?: false,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                    )
                }
            }

            // Navigation Buttons
            Button(
                onClick = onNavigateToAiSettings,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("AI & Prompt Settings", style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = onNavigateToDiagnostics,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Diagnostics & Self-test", style = MaterialTheme.typography.titleMedium)
            }

            // Backup Restore
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Export settings and prompts to a JSON file.", style = MaterialTheme.typography.bodyMedium)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exportIncludeKeys, onCheckedChange = { exportIncludeKeys = it })
                        Text("Include API keys in export", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportLauncher.launch("virtual32_backup.json") }, modifier = Modifier.weight(1f)) {
                            Text("Export Backup")
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) {
                            Text("Import Backup")
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(80.dp)) // Bottom nav padding
        }
    }
}

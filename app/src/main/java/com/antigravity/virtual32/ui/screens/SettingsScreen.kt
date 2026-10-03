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

            // Sessions Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Max photos per session (2-20)
                    Column {
                        val maxPages = settings?.maxSessionPages ?: 12
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Max photos per session", style = MaterialTheme.typography.bodyMedium)
                            Text("$maxPages", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = maxPages.toFloat(),
                            onValueChange = { viewModel.setMaxSessionPages(it.toInt()) },
                            valueRange = 2f..20f,
                            steps = 17
                        )
                    }

                    HorizontalDivider()

                    // Auto-submit after N seconds (Off/10/20/30/60)
                    Column {
                        val currentAutoSubmit = settings?.sessionAutoSubmitSec ?: 0
                        Text("Auto-submit timer", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Off", 10 to "10s", 20 to "20s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                                FilterChip(
                                    selected = currentAutoSubmit == sec,
                                    onClick = { viewModel.setSessionAutoSubmitSec(sec) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Downscale threshold toggle (15 MB downscale to 1600px)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Downscale large sessions (>15 MB)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Downscales photos to 1600px q85 if total size exceeds 15 MB before AI call. Gallery copies remain full resolution.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings?.downscaleSessionPayload ?: true,
                            onCheckedChange = { viewModel.setDownscaleSessionPayload(it) }
                        )
                    }

                    HorizontalDivider()

                    // Multi-photo instruction editor
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Multi-photo instruction", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { viewModel.resetMultiPhotoInstruction() }) {
                                Text("Reset to default")
                            }
                        }
                        OutlinedTextField(
                            value = settings?.multiPhotoInstruction ?: "",
                            onValueChange = { viewModel.setMultiPhotoInstruction(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Note: The JSON output contract (page mapping, question sorting, and format) remains locked and strictly enforced.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

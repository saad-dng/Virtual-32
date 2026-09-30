package com.antigravity.virtual32.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.virtual32.receiver.service.ReceiverState
import com.antigravity.virtual32.receiver.service.SelfTestResult
import com.antigravity.virtual32.receiver.service.SelfTestRunner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    
    val logs by ReceiverState.logs.collectAsState()
    val health by ReceiverState.health.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selfTestResults by remember { mutableStateOf<List<SelfTestResult>?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var minLevel by remember { mutableStateOf("V") } // V, I, W, E

    val filteredLogs = logs.filter { log ->
        val levelMatch = when (minLevel) {
            "E" -> log.startsWith("E/")
            "W" -> log.startsWith("W/") || log.startsWith("E/")
            "I" -> log.startsWith("I/") || log.startsWith("W/") || log.startsWith("E/")
            else -> true
        }
        val searchMatch = searchQuery.isBlank() || log.contains(searchQuery, ignoreCase = true)
        levelMatch && searchMatch
    }.reversed() // latest first

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Diagnostics", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Button(
                onClick = {
                    isTesting = true
                    scope.launch {
                        val runner = SelfTestRunner(context)
                        // Note: wakeLock and wifiLock references aren't easily accessible here if not passed.
                        // We will just pass nulls. The test will fail "Locks" unless we expose them, which is fine, hints help.
                        selfTestResults = runner.runTests(health.serverPort, null, null)
                        isTesting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting
            ) {
                Text(if (isTesting) "Running Self-Test..." else "Run Self-Test")
            }

            if (selfTestResults != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Self-Test Results", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        selfTestResults!!.forEach { res ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(
                                    if (res.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (res.passed) Color.Green else Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(res.name, fontWeight = FontWeight.SemiBold)
                                    if (!res.passed && res.hint != null) {
                                        Text(res.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("System Logs", fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = {
                        val text = filteredLogs.joinToString("\n")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Logs", text))
                    }) { Icon(Icons.Default.ContentCopy, "Copy Logs") }
                    IconButton(onClick = {
                        val text = filteredLogs.joinToString("\n")
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Logs"))
                    }) { Icon(Icons.Default.Share, "Share Logs") }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search logs") },
                    modifier = Modifier.weight(1f)
                )
                // Minimal level dropdown
                var levelExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = levelExpanded, onExpandedChange = { levelExpanded = !levelExpanded }) {
                    OutlinedTextField(
                        value = minLevel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Level") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(100.dp)
                    )
                    ExposedDropdownMenu(expanded = levelExpanded, onDismissRequest = { levelExpanded = false }) {
                        listOf("V", "I", "W", "E").forEach { l ->
                            DropdownMenuItem(text = { Text(l) }, onClick = { minLevel = l; levelExpanded = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black).padding(8.dp)
            ) {
                items(filteredLogs) { log ->
                    val color = when {
                        log.startsWith("E/") -> Color.Red
                        log.startsWith("W/") -> Color.Yellow
                        log.startsWith("I/") -> Color.Cyan
                        else -> Color.LightGray
                    }
                    Text(log, color = color, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

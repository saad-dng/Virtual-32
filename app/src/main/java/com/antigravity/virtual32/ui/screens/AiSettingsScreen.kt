package com.antigravity.virtual32.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.virtual32.receiver.ai.PromptBuilder
import com.antigravity.virtual32.settings.AiProvider
import com.antigravity.virtual32.ui.components.DottedBackgroundBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = viewModel()
) {
    BackHandler(onBack = onBack)
    val state by viewModel.uiState.collectAsState()
    
    var showPresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    
    var providerExpanded by remember { mutableStateOf(false) }
    var fallbackExpanded by remember { mutableStateOf(false) }
    var presetExpanded by remember { mutableStateOf(false) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showClaudeKey by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    viewModel.testPrompt(bytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DottedBackgroundBox(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            TopAppBar(
                title = { Text("AI & Prompt Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Provider Selection Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Active Providers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Select which AI service to use for answering questions. Fallback will be used if the primary provider encounters errors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Primary Provider Dropdown
                        ExposedDropdownMenuBox(
                            expanded = providerExpanded,
                            onExpandedChange = { providerExpanded = !providerExpanded }
                        ) {
                            OutlinedTextField(
                                value = state.settings.provider.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Primary Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                AiProvider.values().filter { it != AiProvider.NONE }.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption.name) },
                                        onClick = {
                                            viewModel.updateSettings { it.copy(provider = selectionOption) }
                                            providerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Fallback Dropdown
                        ExposedDropdownMenuBox(
                            expanded = fallbackExpanded,
                            onExpandedChange = { fallbackExpanded = !fallbackExpanded }
                        ) {
                            OutlinedTextField(
                                value = state.settings.fallbackProvider.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Fallback Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fallbackExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = fallbackExpanded,
                                onDismissRequest = { fallbackExpanded = false }
                            ) {
                                AiProvider.values().forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption.name) },
                                        onClick = {
                                            viewModel.updateSettings { it.copy(fallbackProvider = selectionOption) }
                                            fallbackExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Gemini Configuration Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Google Gemini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val isPrimary = state.settings.provider == AiProvider.GEMINI
                            val isFallback = state.settings.fallbackProvider == AiProvider.GEMINI
                            val badgeText = when {
                                isPrimary -> "Primary"
                                isFallback -> "Fallback"
                                else -> "Configured"
                            }
                            val badgeColor = when {
                                isPrimary -> MaterialTheme.colorScheme.primaryContainer
                                isFallback -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            SuggestionChip(
                                onClick = {},
                                label = { Text(badgeText, style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = badgeColor)
                            )
                        }

                        OutlinedTextField(
                            value = state.settings.geminiModel,
                            onValueChange = { m -> viewModel.updateSettings { it.copy(geminiModel = m.trim()) } },
                            label = { Text("Gemini Model") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick Model Suggestion Chips (Latest Gemini Multimodal Photo Models)
                        Text("Latest Multimodal Models (Photo Support):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "gemini-3.8-flash",
                                "gemini-3.8-pro",
                                "gemini-3.5-flash",
                                "gemini-3.5-pro",
                                "gemini-2.5-flash",
                                "gemini-2.5-pro"
                            ).forEach { modelName ->
                                FilterChip(
                                    selected = state.settings.geminiModel == modelName,
                                    onClick = { viewModel.updateSettings { it.copy(geminiModel = modelName) } },
                                    label = { Text(modelName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.settings.geminiKey,
                            onValueChange = { k -> viewModel.updateSettings { it.copy(geminiKey = k.trim()) } },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.settings.geminiKey.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSettings { it.copy(geminiKey = "") } }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Key")
                                        }
                                    }
                                    IconButton(onClick = {
                                        val clip = clipboardManager.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val text = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                                            if (text.isNotEmpty()) {
                                                viewModel.updateSettings { it.copy(geminiKey = text) }
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Key")
                                    }
                                    IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                        Icon(
                                            if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showGeminiKey) "Hide key" else "Show key"
                                        )
                                    }
                                }
                            }
                        )

                        Text(
                            "Get an API key from Google AI Studio (aistudio.google.com)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { viewModel.testGeminiKey() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isTestingGemini && state.settings.geminiKey.isNotBlank()
                        ) {
                            if (state.isTestingGemini) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing Gemini Key...")
                            } else {
                                Text("Test Gemini API Key")
                            }
                        }

                        if (state.geminiTestResult != null) {
                            val isSuccess = state.geminiTestResult!!.contains("verified", ignoreCase = true) || state.geminiTestResult!!.contains("OK", ignoreCase = true)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.geminiTestResult!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Claude Configuration Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Anthropic Claude", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val isPrimary = state.settings.provider == AiProvider.CLAUDE
                            val isFallback = state.settings.fallbackProvider == AiProvider.CLAUDE
                            val badgeText = when {
                                isPrimary -> "Primary"
                                isFallback -> "Fallback"
                                else -> "Configured"
                            }
                            val badgeColor = when {
                                isPrimary -> MaterialTheme.colorScheme.primaryContainer
                                isFallback -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            SuggestionChip(
                                onClick = {},
                                label = { Text(badgeText, style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = badgeColor)
                            )
                        }

                        OutlinedTextField(
                            value = state.settings.claudeModel,
                            onValueChange = { m -> viewModel.updateSettings { it.copy(claudeModel = m.trim()) } },
                            label = { Text("Claude Model") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick Model Suggestion Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022").forEach { modelName ->
                                FilterChip(
                                    selected = state.settings.claudeModel == modelName,
                                    onClick = { viewModel.updateSettings { it.copy(claudeModel = modelName) } },
                                    label = { Text(modelName.replace("claude-3-5-", ""), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.settings.claudeKey,
                            onValueChange = { k -> viewModel.updateSettings { it.copy(claudeKey = k.trim()) } },
                            label = { Text("Claude API Key") },
                            placeholder = { Text("sk-ant-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showClaudeKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.settings.claudeKey.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSettings { it.copy(claudeKey = "") } }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Key")
                                        }
                                    }
                                    IconButton(onClick = {
                                        val clip = clipboardManager.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val text = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                                            if (text.isNotEmpty()) {
                                                viewModel.updateSettings { it.copy(claudeKey = text) }
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Key")
                                    }
                                    IconButton(onClick = { showClaudeKey = !showClaudeKey }) {
                                        Icon(
                                            if (showClaudeKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showClaudeKey) "Hide key" else "Show key"
                                        )
                                    }
                                }
                            }
                        )

                        Text(
                            "Get an API key from Anthropic Console (console.anthropic.com)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { viewModel.testClaudeKey() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isTestingClaude && state.settings.claudeKey.isNotBlank()
                        ) {
                            if (state.isTestingClaude) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing Claude Key...")
                            } else {
                                Text("Test Claude API Key")
                            }
                        }

                        if (state.claudeTestResult != null) {
                            val isSuccess = state.claudeTestResult!!.contains("verified", ignoreCase = true) || state.claudeTestResult!!.contains("OK", ignoreCase = true)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.claudeTestResult!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Prompt Editor Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Prompt Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        // Preset Dropdown
                        ExposedDropdownMenuBox(
                            expanded = presetExpanded,
                            onExpandedChange = { presetExpanded = !presetExpanded }
                        ) {
                            OutlinedTextField(
                                value = state.activePreset?.name ?: "Custom",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Preset") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = presetExpanded,
                                onDismissRequest = { presetExpanded = false }
                            ) {
                                state.presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.name + if(preset.isBuiltIn) " (Built-in)" else "") },
                                        onClick = {
                                            viewModel.setActivePreset(preset.id)
                                            presetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = state.editedInstruction,
                            onValueChange = { viewModel.setInstruction(it) },
                            label = { Text("Instruction") },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            maxLines = 10
                        )
                        Text("${state.editedInstruction.length} chars", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (state.hasUnsavedChanges) {
                                Button(onClick = { 
                                    presetNameInput = state.activePreset?.name ?: ""
                                    showPresetDialog = true 
                                }) { Text("Save As...") }
                                OutlinedButton(onClick = { viewModel.resetToDefault() }) { Text("Discard") }
                            } else {
                                if (state.activePreset?.isBuiltIn == false) {
                                    OutlinedButton(onClick = { viewModel.deletePreset(state.activePreset!!.id) }) { Text("Delete") }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Locked output contract (appended automatically):", style = MaterialTheme.typography.labelMedium)
                        val contractText = if(state.settings.includeReasoning) PromptBuilder.LOCKED_CONTRACT_WITH_REASONING else PromptBuilder.LOCKED_CONTRACT
                        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                            Text(text = contractText, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                        
                        Button(
                            onClick = { photoPicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isTestingPrompt
                        ) {
                            if (state.isTestingPrompt) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Image...")
                            } else {
                                Text("Test Prompt (Choose Image)")
                            }
                        }
                        if (state.testPromptResult != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.testPromptResult!!,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
                
                // Options Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = state.settings.confidenceFlags,
                                onCheckedChange = { v -> viewModel.updateSettings { it.copy(confidenceFlags = v) } }
                            )
                            Text("Enable confidence flags")
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = state.settings.includeReasoning,
                                onCheckedChange = { v -> viewModel.updateSettings { it.copy(includeReasoning = v) } }
                            )
                            Text("Include short reasoning (<15 words)")
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = state.settings.pauseAi,
                                onCheckedChange = { v -> viewModel.updateSettings { it.copy(pauseAi = v) } }
                            )
                            Text("Pause AI (simulates errors)")
                        }
                        
                        OutlinedTextField(
                            value = state.settings.requestTimeoutSec.toString(),
                            onValueChange = { v -> 
                                v.toIntOrNull()?.let { num -> viewModel.updateSettings { it.copy(requestTimeoutSec = num) } }
                            },
                            label = { Text("Request Timeout (sec)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Save Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (presetNameInput.isNotBlank()) {
                        viewModel.savePreset(presetNameInput)
                        showPresetDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPresetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

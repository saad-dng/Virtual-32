package com.antigravity.virtual32.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.virtual32.receiver.ai.PromptBuilder
import com.antigravity.virtual32.settings.AiProvider
import com.antigravity.virtual32.ui.components.DottedBackgroundBox

class Last4PasswordVisualTransformation(val mask: Char = '\u2022') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val len = text.text.length
        val out = if (len <= 4) {
            mask.toString().repeat(len)
        } else {
            mask.toString().repeat(len - 4) + text.text.takeLast(4)
        }
        return TransformedText(AnnotatedString(out), OffsetMapping.Identity)
    }
}

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

    val context = LocalContext.current
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
                title = { Text("AI & Prompt", fontWeight = FontWeight.Bold) },
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
                // Providers Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Provider & Models", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        // Provider Dropdown
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
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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

                        if (state.settings.provider == AiProvider.GEMINI || state.settings.fallbackProvider == AiProvider.GEMINI) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Gemini Config", fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = state.settings.geminiModel,
                                onValueChange = { m -> viewModel.updateSettings { it.copy(geminiModel = m) } },
                                label = { Text("Gemini Model") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.settings.geminiKey,
                                onValueChange = { k -> viewModel.updateSettings { it.copy(geminiKey = k) } },
                                label = { Text("Gemini API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = Last4PasswordVisualTransformation()
                            )
                        }

                        if (state.settings.provider == AiProvider.CLAUDE || state.settings.fallbackProvider == AiProvider.CLAUDE) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Claude Config", fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = state.settings.claudeModel,
                                onValueChange = { m -> viewModel.updateSettings { it.copy(claudeModel = m) } },
                                label = { Text("Claude Model") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.settings.claudeKey,
                                onValueChange = { k -> viewModel.updateSettings { it.copy(claudeKey = k) } },
                                label = { Text("Claude API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = Last4PasswordVisualTransformation()
                            )
                        }
                        
                        Button(onClick = { viewModel.testKey() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Test Auth Key")
                        }
                        if (state.testKeyResult != null) {
                            Text("Result: ${state.testKeyResult}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
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
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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
                        
                        Button(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Test Prompt (Choose Image)")
                        }
                        if (state.testPromptResult != null) {
                            Text("Result: ${state.testPromptResult}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
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

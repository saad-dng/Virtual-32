package com.antigravity.virtual32.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.antigravity.virtual32.data.Batch
import com.antigravity.virtual32.data.AnswerEntity
import com.antigravity.virtual32.ui.components.DottedBackgroundBox
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnswersScreen(
    modifier: Modifier = Modifier,
    viewModel: AnswersViewModel = viewModel(),
    onNavigateToHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    var answerToEdit by remember { mutableStateOf<AnswerEntity?>(null) }
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val intent = Intent(context, com.antigravity.virtual32.receiver.service.ReceiverService::class.java).apply {
                action = com.antigravity.virtual32.receiver.service.ReceiverService.ACTION_PROCESS_GALLERY
                putExtra(com.antigravity.virtual32.receiver.service.ReceiverService.EXTRA_URI, uri.toString())
            }
            context.startService(intent)
        }
    }

    // Scroll to cursor
    LaunchedEffect(uiState.cursor) {
        if (uiState.answers.isNotEmpty() && uiState.cursor < uiState.answers.size && !uiState.isFilterLowConfidence) {
            listState.animateScrollToItem(uiState.cursor)
        }
    }

    DottedBackgroundBox(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Answers",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${uiState.count} answers - cycle ${uiState.cursor + 1}/${uiState.count}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SuggestionChip(
                            onClick = { },
                            label = { Text(uiState.answerMode.name) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = uiState.isFilterLowConfidence,
                            onClick = { viewModel.toggleFilter() },
                            label = { Text("Low Confidence") }
                        )
                        Row {
                            IconButton(onClick = onNavigateToHistory, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Default.History, "History") }
                            IconButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Default.PhotoLibrary, "Process from Gallery") }
                            IconButton(onClick = { viewModel.resetCycle() }, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Default.Refresh, "Reset Cycle") }
                            IconButton(onClick = { viewModel.clearList() }, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Default.Delete, "Clear List") }
                            IconButton(
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                onClick = {
                                val text = uiState.answers.joinToString(", ") { "${it.q}-${it.choice}" }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Answers", text))
                            }) { Icon(Icons.Default.ContentCopy, "Copy All") }
                            IconButton(
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                onClick = {
                                val text = uiState.answers.joinToString(", ") { "${it.q}-${it.choice}" }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Answers"))
                            }) { Icon(Icons.Default.Share, "Share") }
                        }
                    }

                    // Filter "from photo k"
                    if (uiState.availablePages.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedPhotoFilter == null,
                                    onClick = { viewModel.setPhotoFilter(null) },
                                    label = { Text("All photos") }
                                )
                            }
                            items(uiState.availablePages) { page ->
                                FilterChip(
                                    selected = uiState.selectedPhotoFilter == page,
                                    onClick = { viewModel.setPhotoFilter(page) },
                                    label = { Text("Photo $page") }
                                )
                            }
                        }
                    }
                }
            }

            // Warnings Banner (missing question numbers, unreadable photos, hint, Dismiss)
            if (uiState.warnings.isNotEmpty() && !uiState.isWarningsDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Session Warnings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = { viewModel.dismissWarnings() }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                        uiState.warnings.forEach { warning ->
                            Text(
                                "• $warning",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        uiState.warningHint?.let { hint ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Hint: $hint",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (uiState.answers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No answers yet. Capture a photo!", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Photo strip
                    if (uiState.recentBatches.isNotEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.recentBatches) { batch ->
                                    val photos = batch.getPhotoPathList()
                                    photos.forEach { path ->
                                        AsyncImage(
                                            model = File(path),
                                            contentDescription = "Batch photo",
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { fullScreenImage = path },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    items(uiState.answers.size) { i ->
                        val ans = uiState.answers[i]
                        val isCursor = i == uiState.cursor && !uiState.isFilterLowConfidence
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .combinedClickable(
                                    onClick = { viewModel.setCursor(i) },
                                    onLongClick = { answerToEdit = ans }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCursor) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Q${ans.q}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    // "photo k" chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = "Photo ${ans.page}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(ans.choice, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (ans.edited == true) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("Edited") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (ans.conf == "low") {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) { Text("Low Conf") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    
                                    val blinks = ans.choice[0] - 'A' + 1
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (b in 0 until blinks) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Blue))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (fullScreenImage != null) {
        Dialog(onDismissRequest = { fullScreenImage = null }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))
                .clickable { fullScreenImage = null }) {
                AsyncImage(
                    model = File(fullScreenImage!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
    
    if (answerToEdit != null) {
        var newChoice by remember { mutableStateOf(answerToEdit!!.choice) }
        AlertDialog(
            onDismissRequest = { answerToEdit = null },
            title = { Text("Edit Answer Q${answerToEdit!!.q}") },
            text = {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    listOf("A", "B", "C", "D", "E").forEach { choice ->
                        FilterChip(
                            selected = newChoice == choice,
                            onClick = { newChoice = choice },
                            label = { Text(choice) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.editAnswer(answerToEdit!!.id, newChoice)
                    answerToEdit = null
                }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { answerToEdit = null }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("Cancel") }
            }
        )
    }
}

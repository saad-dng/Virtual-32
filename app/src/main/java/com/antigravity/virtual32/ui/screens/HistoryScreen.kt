package com.antigravity.virtual32.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import android.content.Intent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.antigravity.virtual32.ui.components.DottedBackgroundBox
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(),
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    
    val batches by viewModel.batches.collectAsState(initial = emptyList())
    val selectedBatches by viewModel.selectedBatches.collectAsState()
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    DottedBackgroundBox(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedBatches.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, "Delete selected")
                        }
                        IconButton(onClick = { 
                            scope.launch {
                                val csv = viewModel.getCsvContent()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    putExtra(Intent.EXTRA_SUBJECT, "Virtual32 Batch History")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share CSV"))
                            }
                        }) {
                            Icon(Icons.Default.Share, "Export CSV")
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(batches) { batch ->
                    val isSelected = selectedBatches.contains(batch.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.toggleSelection(batch.id) },
                                onLongClick = { viewModel.toggleSelection(batch.id) }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (batch.photoPath != null) {
                                AsyncImage(
                                    model = File(batch.photoPath),
                                    contentDescription = "Thumbnail",
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(batch.createdAt))
                                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(batch.status, fontWeight = FontWeight.Bold, color = if (batch.status == "ok") Color.Green else Color.Red)
                                    if (batch.superseded) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge { Text("Superseded") }
                                    }
                                }
                                
                                Text("${batch.latencyMs} ms | ${batch.provider}", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Button(
                                onClick = { viewModel.reprocess(batch) },
                                enabled = batch.photoPath != null && !batch.superseded
                            ) {
                                Text("Reprocess")
                            }
                        }
                    }
                }
            }
        }
    }
}

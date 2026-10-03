package com.antigravity.virtual32.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import android.content.Intent
import androidx.compose.animation.animateContentSize
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.antigravity.virtual32.util.DownscaledThumbnail
import com.antigravity.virtual32.data.AnswerEntity
import com.antigravity.virtual32.data.Batch
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

    var batchDetailToView by remember { mutableStateOf<Batch?>(null) }
    var batchAnswers by remember { mutableStateOf<List<AnswerEntity>>(emptyList()) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(batches) { batch ->
                    val isSelected = selectedBatches.contains(batch.id)
                    val photoPaths = batch.getPhotoPathList()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .combinedClickable(
                                onClick = {
                                    if (selectedBatches.isNotEmpty()) {
                                        viewModel.toggleSelection(batch.id)
                                    } else {
                                        scope.launch {
                                            batchAnswers = viewModel.getAnswersForBatch(batch.id)
                                            batchDetailToView = batch
                                        }
                                    }
                                },
                                onLongClick = { viewModel.toggleSelection(batch.id) }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(batch.createdAt))
                                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            "${batch.pageCount} ${if (batch.pageCount == 1) "photo" else "photos"}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        batch.status,
                                        fontWeight = FontWeight.Bold,
                                        color = if (batch.status == "ok") Color.Green else Color.Red
                                    )
                                    if (batch.superseded) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge { Text("Superseded") }
                                    }
                                }
                            }

                            // Thumbnail strip
                            if (photoPaths.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(photoPaths) { path ->
                                        DownscaledThumbnail(
                                            imagePath = path,
                                            contentDescription = "Thumbnail",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { fullScreenImage = path },
                                            targetSize = 120
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${batch.latencyMs} ms | ${batch.provider} (${batch.model})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { viewModel.reprocess(batch) },
                                    enabled = photoPaths.isNotEmpty() && !batch.superseded,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Reprocess")
                                }
                            }

                            if (batch.status != "ok" && !batch.rawResponse.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    batch.rawResponse,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail dialog showing answers plus all photos when tapping a batch
    if (batchDetailToView != null) {
        val b = batchDetailToView!!
        val photos = b.getPhotoPathList()

        Dialog(onDismissRequest = { batchDetailToView = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Batch #${b.id} (${b.pageCount} ${if (b.pageCount == 1) "photo" else "photos"})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { batchDetailToView = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Text(
                        "${b.provider} - ${b.status} (${b.latencyMs} ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Photos:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photos) { path ->
                            DownscaledThumbnail(
                                imagePath = path,
                                contentDescription = "Photo",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { fullScreenImage = path },
                                targetSize = 150
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Answers (${batchAnswers.size}):", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(batchAnswers) { ans ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Q${ans.q}", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                "Photo ${ans.page}",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(ans.choice, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(ans.conf, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            viewModel.reprocess(b)
                            batchDetailToView = null
                        }) {
                            Text("Reprocess All Photos")
                        }
                    }
                }
            }
        }
    }

    if (fullScreenImage != null) {
        Dialog(onDismissRequest = { fullScreenImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { fullScreenImage = null }
            ) {
                AsyncImage(
                    model = File(fullScreenImage!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

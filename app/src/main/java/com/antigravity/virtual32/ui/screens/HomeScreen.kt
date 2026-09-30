package com.antigravity.virtual32.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.virtual32.ui.components.DottedBackgroundBox
import com.antigravity.virtual32.receiver.pipeline.PhotoPipelineImpl
import com.antigravity.virtual32.receiver.server.ReceiverHttpServer
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.receiver.server.LogBuffer
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.util.GalleryWriter
import com.antigravity.virtual32.util.PhotoCache
import androidx.compose.ui.platform.LocalContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val server = remember { 
        val db = AppDatabase.getDatabase(context)
        val answerStore = RoomAnswerStore(db.answerDao())
        val settingsRepo = SettingsRepository(context)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        val pipeline = PhotoPipelineImpl(
            settingsRepo = settingsRepo,
            answerStore = answerStore,
            okHttpClient = okHttpClient,
            galleryWriter = GalleryWriter(context),
            photoCache = PhotoCache(context),
            isNetworkAvailable = { true } // stub for acceptance test
        )
        ReceiverHttpServer(
            port = 5000, 
            pipeline = pipeline, 
            answerStore = answerStore, 
            logBuffer = LogBuffer()
        ) 
    }

    DottedBackgroundBox(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Receiver (Home)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Receiver HTTP server is ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { server.start() }) {
                Text("Start Server (Acceptance Test)")
            }
        }
    }
}

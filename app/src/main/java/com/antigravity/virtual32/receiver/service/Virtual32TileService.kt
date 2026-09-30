package com.antigravity.virtual32.receiver.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.N)
class Virtual32TileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        
        scope.launch {
            ReceiverState.health.collect {
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope.coroutineContext.cancelChildren()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = ReceiverState.health.value.isServiceRunning
        val intent = Intent(this, ReceiverService::class.java)
        
        if (isRunning) {
            stopService(intent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isRunning = ReceiverState.health.value.isServiceRunning
        
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRunning) "Receiver On" else "Receiver Off"
        tile.updateTile()
    }
}

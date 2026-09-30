package com.antigravity.virtual32.receiver.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.RoomAnswerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        if (action == "STOP") {
            context.stopService(Intent(context, ReceiverService::class.java))
            return
        }
        
        val db = AppDatabase.getDatabase(context)
        val answerStore = RoomAnswerStore(db.answerDao())
        
        CoroutineScope(Dispatchers.IO).launch {
            if (action == "NEXT") {
                answerStore.next()
            } else if (action == "REPEAT") {
                answerStore.repeat()
            }
        }
    }
}

package ngo.xnet.aiope.feature.chat.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class AgentAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("scheduledTaskId") ?: return
        val workRequest = OneTimeWorkRequestBuilder<AgentRunWorker>()
            .setInputData(workDataOf("scheduledTaskId" to taskId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

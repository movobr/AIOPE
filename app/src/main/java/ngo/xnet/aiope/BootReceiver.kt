package ngo.xnet.aiope

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ngo.xnet.aiope.feature.chat.engine.AgentRescheduleWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workRequest = OneTimeWorkRequestBuilder<AgentRescheduleWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "AgentRescheduleWorkerBoot",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            val serviceIntent = Intent(context, AiopeForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}

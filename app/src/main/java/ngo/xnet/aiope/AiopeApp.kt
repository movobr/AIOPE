package ngo.xnet.aiope

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ngo.xnet.aiope.feature.chat.db.ChatDatabase
import ngo.xnet.aiope.feature.chat.engine.AgentRescheduleWorker
import ngo.xnet.aiope.feature.remote.db.RemoteDatabase

class AiopeApp : Application() {

    val chatDatabase: ChatDatabase by lazy { ChatDatabase.getInstance(this) }
    val remoteDatabase: RemoteDatabase by lazy { RemoteDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        val workRequest = OneTimeWorkRequestBuilder<AgentRescheduleWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "AgentRescheduleWorker",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}

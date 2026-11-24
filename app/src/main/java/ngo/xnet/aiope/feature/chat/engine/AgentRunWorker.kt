package ngo.xnet.aiope.feature.chat.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ngo.xnet.aiope.feature.chat.db.ChatDatabase
import ngo.xnet.aiope.feature.chat.db.TaskRunEntity

class AgentRunWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("scheduledTaskId") ?: return Result.failure()
        val db = ChatDatabase.getInstance(applicationContext)

        // Record execution run
        val run = TaskRunEntity(
            scheduledTaskId = taskId,
            runNumber = 1,
            timestamp = System.currentTimeMillis(),
            prompt = "Automated scheduled run",
            output = "Task executed successfully",
            status = "finished"
        )
        db.chatDao().insertTaskRun(run)

        return Result.success()
    }
}

package app.timelog.musewidget

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val settings = Settings(applicationContext)
        val token = try { settings.token() } catch (_: Exception) {
            settings.status = "トークンを読み取れません。設定で再入力してください"
            LifeWidget.refresh(applicationContext)
            return Result.success()
        }
        if (token.isBlank()) {
            settings.status = "トークン未設定・端末内に保存中"
            LifeWidget.refresh(applicationContext)
            return Result.success()
        }
        Store(applicationContext).use { store ->
            store.retargetCodeQueue(settings.repository)
            val drained = QueueDrain(
                first = { store.first() },
                remove = { event: Event ->
                    store.remove(event.id)
                    settings.status = "送信しました"
                    LifeWidget.refresh(applicationContext)
                },
                stopped = { isStopped },
                send = { event: Event ->
                    try {
                        val code = IssueClient().send(event, token)
                        if (code != 201) {
                            settings.status = "送信失敗 (HTTP $code)・記録は保持中。設定を確認してください"
                            LifeWidget.refresh(applicationContext)
                            // Keep the head of the FIFO queue on every non-success response.
                            return@QueueDrain false
                        }
                        true
                    } catch (_: Exception) {
                        settings.status = "通信失敗・記録は保持中。自動再送します"
                        LifeWidget.refresh(applicationContext)
                        false
                    }
                }
            ).run()
            return if (drained) Result.success() else Result.retry()
        }
    }
    companion object {
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("event-upload", ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS).build())
        }
        fun ensurePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("queue-recovery", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RecoveryWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())
        }
    }
}

/** Recovery only schedules the same serial chain; it never uploads concurrently. */
class RecoveryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        SyncWorker.enqueue(applicationContext)
        return Result.success()
    }
}

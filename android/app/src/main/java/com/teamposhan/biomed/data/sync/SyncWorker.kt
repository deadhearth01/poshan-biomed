package com.teamposhan.biomed.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.teamposhan.biomed.PoshanApplication
import java.util.concurrent.TimeUnit

/**
 * Background sync. Runs only when a network is available and retries with backoff, so
 * field workers never have to press anything — data reaches the cloud on its own.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PoshanApplication ?: return Result.success()
        val container = app.container
        val report = runCatching {
            container.sync.syncAll(
                profile = container.repository.profile.value,
                children = container.repository.children.value,
                sessions = container.repository.sessions.value,
            )
        }.getOrElse {
            return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }

        if (report.attempted == 0 || report.failed == 0) return Result.success()

        // A schema/permission error will fail identically forever — retrying only burns
        // battery. Local data is never deleted, so the next periodic run will pick it up
        // once the server side is fixed.
        if (!report.retryable) return Result.failure()

        return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        const val PERIODIC_NAME = "poshan-periodic-sync"
        const val IMMEDIATE_NAME = "poshan-immediate-sync"

        /** Backoff caps out here; the 15-minute periodic job keeps trying after that. */
        private const val MAX_ATTEMPTS = 5

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Called on app start — also reschedules the periodic job so it survives reinstall. */
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)

            val periodic = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            wm.enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic,
            )
        }

        /** Called whenever connectivity returns or data changes. */
        fun syncSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}

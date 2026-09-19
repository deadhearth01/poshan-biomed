package com.teamposhan.biomed.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.teamposhan.biomed.PoshanApplication
import com.teamposhan.biomed.R
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.immunization.DoseStatus
import com.teamposhan.biomed.domain.immunization.ImmunizationSchedule
import java.util.concurrent.TimeUnit

/**
 * Reminds the caregiver when a vaccine dose falls due or goes overdue.
 *
 * A screening app that knows a dose is three weeks late and says nothing about it until the
 * caregiver happens to open the immunisation screen is withholding the most actionable
 * thing it knows. The schedule is already computed; this surfaces it.
 *
 * Checked daily rather than scheduled per dose: the child's age advances on its own, doses
 * get recorded between checks, and a single daily pass over a handful of children is far
 * cheaper than maintaining dozens of exact alarms that drift whenever data changes.
 */
class VaccineReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PoshanApplication ?: return Result.success()
        val container = app.container
        val children = container.repository.children.value
        if (children.isEmpty()) return Result.success()

        val notifier = VaccineNotifier(applicationContext)
        if (!notifier.canNotify()) return Result.success()

        children.forEach { child ->
            val ageDays = (AgeCalculator.chronologicalMonths(child) * 30.4375f).toInt()
            val given = container.repository.sessionsForChild(child.id)
                .flatMap { it.givenVaccines }
                .toSet()

            val states = ImmunizationSchedule.statusFor(ageDays, given)
            val overdue = states.filter { it.status == DoseStatus.OVERDUE }
            val dueSoon = states.filter { it.status == DoseStatus.DUE_SOON }

            when {
                overdue.isNotEmpty() -> notifier.notifyOverdue(child.id, child.name, overdue.map { it.dose.name })
                dueSoon.isNotEmpty() -> notifier.notifyDueSoon(child.id, child.name, dueSoon.map { it.dose.name })
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "poshan-vaccine-reminders"

        /**
         * Schedules the daily check. Safe to call on every app start — `KEEP` means an
         * existing schedule is left alone rather than restarted, which would otherwise
         * push the first run back by a day each time the app opened.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<VaccineReminderWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInitialDelay(2, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

/**
 * Builds and posts the reminder notifications.
 *
 * Wording matters here more than usual: a missed vaccine is a common, fixable thing, and a
 * notification that reads as an accusation gets dismissed and then ignored. These say what
 * is due and where to go, not what the caregiver failed to do.
 */
class VaccineNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_vaccines),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_vaccines_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun canNotify(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun notifyOverdue(childId: String, childName: String, doses: List<String>) {
        post(
            childId = childId,
            title = context.getString(R.string.notif_vaccine_overdue_title, childName),
            body = context.resources.getQuantityString(
                R.plurals.notif_vaccine_overdue_body,
                doses.size,
                doses.size,
                doses.take(3).joinToString(", "),
            ),
        )
    }

    fun notifyDueSoon(childId: String, childName: String, doses: List<String>) {
        post(
            childId = childId,
            title = context.getString(R.string.notif_vaccine_due_title, childName),
            body = context.resources.getQuantityString(
                R.plurals.notif_vaccine_due_body,
                doses.size,
                doses.size,
                doses.take(3).joinToString(", "),
            ),
        )
    }

    private fun post(childId: String, title: String, body: String) {
        if (!canNotify()) return

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = launch?.let {
            PendingIntent.getActivity(
                context,
                childId.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { pending?.let(::setContentIntent) }
            .build()

        // Keyed by child so a family with several children gets one notification each,
        // rather than later children silently replacing earlier ones.
        runCatching { manager.notify(childId.hashCode(), notification) }
    }

    private companion object {
        const val CHANNEL_ID = "poshan-vaccines"
    }
}

package com.teamposhan.biomed

import android.app.Application
import com.teamposhan.biomed.data.local.PoshanContainer
import com.teamposhan.biomed.data.remote.AppwriteService
import com.teamposhan.biomed.data.sync.NetworkMonitor
import com.teamposhan.biomed.data.sync.SyncWorker
import com.teamposhan.biomed.locale.AppLocale
import kotlinx.coroutines.runBlocking

class PoshanApplication : Application() {

    lateinit var container: PoshanContainer
        private set

    private var networkMonitor: NetworkMonitor? = null

    override fun onCreate() {
        super.onCreate()
        container = PoshanContainer(this)
        AppwriteService.init(this)

        // Restore the saved language before any UI is created.
        runCatching {
            val stored = runBlocking { container.prefs.currentLanguage() }
            if (stored.isNotBlank()) AppLocale.apply(stored)
        }

        // Auto-sync: periodic while online, plus an immediate attempt whenever
        // connectivity returns.
        SyncWorker.schedule(this)
        networkMonitor = NetworkMonitor(this).also { it.start() }

        // Daily check for vaccine doses that have fallen due or overdue. The schedule is
        // already computed on every immunisation screen; this is what makes the app say so
        // without waiting for the caregiver to go looking.
        com.teamposhan.biomed.data.reminder.VaccineReminderWorker.schedule(this)
    }

    override fun onTerminate() {
        networkMonitor?.stop()
        super.onTerminate()
    }
}

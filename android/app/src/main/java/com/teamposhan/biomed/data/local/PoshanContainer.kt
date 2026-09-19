package com.teamposhan.biomed.data.local

import android.content.Context
import com.teamposhan.biomed.data.remote.AppwriteSyncRepository
import com.teamposhan.biomed.data.remote.AuthRepository
import com.teamposhan.biomed.data.repo.PoshanRepository
import com.teamposhan.biomed.data.sync.NetworkMonitor

/**
 * Lightweight manual dependency container (no annotation processing required).
 */
class PoshanContainer(context: Context) {
    val prefs: PrefsStore = PrefsStore(context.applicationContext)
    val repository: PoshanRepository = PoshanRepository(context.applicationContext)
    val auth: AuthRepository = AuthRepository()
    val sync: AppwriteSyncRepository = AppwriteSyncRepository(auth)

    /** Connectivity check used to skip sync ticks that would certainly fail. */
    val network: NetworkMonitor = NetworkMonitor(context.applicationContext)
}

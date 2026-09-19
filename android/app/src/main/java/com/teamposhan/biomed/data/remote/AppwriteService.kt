package com.teamposhan.biomed.data.remote

import android.content.Context
import com.teamposhan.biomed.BuildConfig
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage

/**
 * Single Appwrite client for the process. The SDK persists the session cookie in
 * SharedPreferences, so login survives restarts without us storing any token.
 *
 * When APPWRITE_ENABLED is false (default until a project id is configured) the app runs
 * purely offline and every remote call is skipped by the repositories.
 */
object AppwriteService {

    @Volatile
    private var initialized = false

    lateinit var client: Client
        private set
    lateinit var account: Account
        private set
    lateinit var databases: Databases
        private set
    lateinit var storage: Storage
        private set

    val isConfigured: Boolean
        get() = BuildConfig.APPWRITE_ENABLED && BuildConfig.APPWRITE_PROJECT_ID.isNotBlank()

    /** Collection ids created in the Poshan Appwrite database. */
    object Collections {
        const val PROFILES = "profiles"
        const val CHILDREN = "children"
        const val ASSESSMENTS = "assessments"
        const val GROWTH = "growth_records"
        const val ATTENTION = "attention_records"
        const val VITAMIN_D = "vitamin_d_records"
    }

    val databaseId: String get() = BuildConfig.APPWRITE_DATABASE_ID

    val projectId: String get() = BuildConfig.APPWRITE_PROJECT_ID

    /**
     * Appwrite refuses every request when the endpoint is missing the API version segment,
     * so tolerate `https://host` and `https://host/` in the build config.
     */
    val endpoint: String
        get() {
            val raw = BuildConfig.APPWRITE_ENDPOINT.trim().trimEnd('/')
            return if (raw.endsWith("/v1")) raw else "$raw/v1"
        }

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            client = Client(context)
                .setEndpoint(endpoint)
                .setProject(BuildConfig.APPWRITE_PROJECT_ID)
            account = Account(client)
            databases = Databases(client)
            storage = Storage(client)
            initialized = true
        }
    }
}

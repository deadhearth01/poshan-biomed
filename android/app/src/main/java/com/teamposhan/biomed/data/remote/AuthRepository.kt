package com.teamposhan.biomed.data.remote

import com.teamposhan.biomed.data.model.UserProfile
import com.teamposhan.biomed.data.model.UserRole
import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Session
import io.appwrite.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Authentication facade. Uses Appwrite when configured, otherwise falls back to a fully
 * offline guest profile so the assessment flow always works.
 */
class AuthRepository {

    suspend fun guestProfile(role: UserRole): UserProfile = UserProfile(
        id = "local-" + UUID.randomUUID().toString().take(8),
        role = role,
        isGuest = true,
    )

    /**
     * Returns the Appwrite userId, creating an anonymous session when needed.
     * Existing sessions are reused. Throws on failure so the caller can show the real
     * reason instead of a generic "could not sync".
     */
    suspend fun ensureRemoteSession(): String? = withContext(Dispatchers.IO) {
        if (!AppwriteService.isConfigured) return@withContext null

        // A stored session is the happy path on every run after the first.
        runCatching { AppwriteService.account.get() }.getOrNull()?.let { return@withContext it.id }

        try {
            AppwriteService.account.createAnonymousSession().userId
        } catch (e: AppwriteException) {
            // Appwrite rejects anonymous creation while a session cookie is still active
            // ("Creation of anonymous users is prohibited when a session is active").
            // That means we already have a session whose account.get() merely hiccuped —
            // re-read it instead of reporting a bogus "could not create a session".
            val active = e.type == "user_session_already_exists" ||
                e.message?.contains("session is active", ignoreCase = true) == true
            if (active) {
                runCatching { AppwriteService.account.get() }.getOrNull()?.id
                    ?: throw IllegalStateException(describe(e))
            } else {
                throw IllegalStateException(describe(e), e)
            }
        }
    }

    /** Turns an Appwrite failure into something a field worker can act on. */
    private fun describe(e: AppwriteException): String {
        val msg = e.message?.takeIf { it.isNotBlank() } ?: "unknown error"
        val code = e.code
        val hint = when {
            code == 401 && e.type == "general_unauthorized_scope" ->
                " — the project does not allow anonymous sign-in; enable it in Auth → Settings."
            code == 401 -> " — check the Android platform registration for com.teamposhan.biomed."
            code == 404 -> " — project ${AppwriteService.projectId} not found at ${AppwriteService.endpoint}."
            code == 429 -> " — rate limited, sync will retry shortly."
            code == 503 || code == 500 -> " — the Appwrite server is unavailable, sync will retry."
            else -> ""
        }
        return "$msg (code $code)$hint"
    }

    suspend fun currentUser(): User<Map<String, Any>>? = withContext(Dispatchers.IO) {
        if (!AppwriteService.isConfigured) return@withContext null
        runCatching { AppwriteService.account.get() }.getOrNull()
    }

    suspend fun requestPhoneToken(phone: String): Result<String> = withContext(Dispatchers.IO) {
        if (!AppwriteService.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Appwrite not configured"))
        }
        runCatching {
            val token = AppwriteService.account.createPhoneToken(ID.unique(), phone)
            token.userId
        }
    }

    suspend fun confirmPhoneToken(userId: String, secret: String): Result<Session> =
        withContext(Dispatchers.IO) {
            if (!AppwriteService.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Appwrite not configured"))
            }
            runCatching { AppwriteService.account.createSession(userId, secret) }
        }

    suspend fun signInEmail(email: String, password: String): Result<Session> =
        withContext(Dispatchers.IO) {
            if (!AppwriteService.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Appwrite not configured"))
            }
            runCatching { AppwriteService.account.createEmailPasswordSession(email, password) }
        }

    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        if (!AppwriteService.isConfigured) return@withContext
        runCatching { AppwriteService.account.deleteSession("current") }
    }
}

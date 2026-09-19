package com.teamposhan.biomed.data.repo

import android.content.Context
import com.teamposhan.biomed.data.local.JsonFileStore
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

/**
 * Offline-first store for children, assessment sessions and the local profile.
 * All writes go to disk immediately so a screening can be interrupted at any point.
 */
class PoshanRepository(context: Context) {

    private val childStore = JsonFileStore(
        context, "children.json",
        ListSerializer(ChildProfile.serializer()), { emptyList() },
    )
    private val sessionStore = JsonFileStore(
        context, "sessions.json",
        ListSerializer(AssessmentSession.serializer()), { emptyList() },
    )
    private val profileStore = JsonFileStore(
        context, "profiles.json",
        ListSerializer(UserProfile.serializer()), { emptyList() },
    )

    private val _children = MutableStateFlow(childStore.read())
    val children: StateFlow<List<ChildProfile>> = _children.asStateFlow()

    private val _sessions = MutableStateFlow(sessionStore.read())
    val sessions: StateFlow<List<AssessmentSession>> = _sessions.asStateFlow()

    private val _profile = MutableStateFlow(profileStore.read().firstOrNull())
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    // ---- children -------------------------------------------------------

    fun child(id: String): ChildProfile? = _children.value.firstOrNull { it.id == id }

    fun addChild(child: ChildProfile) {
        _children.value = _children.value + child
        childStore.write(_children.value)
    }

    fun updateChild(child: ChildProfile) {
        _children.value = _children.value.map { if (it.id == child.id) child else it }
        childStore.write(_children.value)
    }

    fun removeChild(id: String) {
        _children.value = _children.value.filterNot { it.id == id }
        childStore.write(_children.value)
    }

    // ---- sessions -------------------------------------------------------

    fun session(id: String): AssessmentSession? = _sessions.value.firstOrNull { it.id == id }

    fun sessionsForChild(childId: String): List<AssessmentSession> =
        _sessions.value.filter { it.childId == childId }.sortedByDescending { it.startedAt }

    fun latestSessionForChild(childId: String): AssessmentSession? =
        sessionsForChild(childId).firstOrNull()

    fun newSession(childId: String): AssessmentSession {
        val session = AssessmentSession(id = UUID.randomUUID().toString(), childId = childId)
        upsertSession(session)
        return session
    }

    fun upsertSession(session: AssessmentSession) {
        val existing = _sessions.value.indexOfFirst { it.id == session.id }
        _sessions.value = if (existing >= 0) {
            _sessions.value.toMutableList().also { it[existing] = session }
        } else {
            _sessions.value + session
        }
        sessionStore.write(_sessions.value)
    }

    fun deleteSession(id: String) {
        _sessions.value = _sessions.value.filterNot { it.id == id }
        sessionStore.write(_sessions.value)
    }

    // ---- profile --------------------------------------------------------

    fun setProfile(profile: UserProfile) {
        _profile.value = profile
        profileStore.write(listOf(profile))
    }

    fun clearProfile() {
        _profile.value = null
        profileStore.write(emptyList())
    }
}

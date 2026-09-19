package com.teamposhan.biomed.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "poshan_prefs")

class PrefsStore(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val consented = booleanPreferencesKey("consented")
        val onboarded = booleanPreferencesKey("onboarded")
        val role = stringPreferencesKey("role")
        val activeChildId = stringPreferencesKey("active_child_id")
    }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.language] ?: "en" }
    val consented: Flow<Boolean> = context.dataStore.data.map { it[Keys.consented] ?: false }
    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboarded] ?: false }
    val role: Flow<String> = context.dataStore.data.map { it[Keys.role] ?: "" }
    val activeChildId: Flow<String> = context.dataStore.data.map { it[Keys.activeChildId] ?: "" }

    suspend fun currentLanguage(): String = language.first()
    suspend fun isOnboarded(): Boolean = onboarded.first()

    suspend fun setLanguage(code: String) = context.dataStore.edit { it[Keys.language] = code }
    suspend fun setConsented(value: Boolean) = context.dataStore.edit { it[Keys.consented] = value }
    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[Keys.onboarded] = value }
    suspend fun setRole(value: String) = context.dataStore.edit { it[Keys.role] = value }
    suspend fun setActiveChild(id: String) = context.dataStore.edit { it[Keys.activeChildId] = id }
}

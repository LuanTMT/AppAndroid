package com.example.firstapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPreferences {
    private val TOKEN_KEY = stringPreferencesKey("token")
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val REMEMBER_LOGIN_KEY = booleanPreferencesKey("remember_login")
    private val SAVED_EMAIL_KEY = stringPreferencesKey("saved_email")

    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun getToken(context: Context): String? {
        return context.dataStore.data.map { it[TOKEN_KEY] }.first()
    }

    suspend fun clearToken(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
        }
    }

    suspend fun saveUserId(context: Context, userId: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }

    suspend fun getUserId(context: Context): String? {
        return context.dataStore.data.map { it[USER_ID_KEY] }.first()
    }

    // Remember login toggle
    suspend fun setRememberLogin(context: Context, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REMEMBER_LOGIN_KEY] = remember
        }
    }

    suspend fun isRememberLogin(context: Context): Boolean {
        return context.dataStore.data.map { it[REMEMBER_LOGIN_KEY] ?: false }.first()
    }

    // Save email for prefill when remember is enabled
    suspend fun saveEmail(context: Context, email: String) {
        context.dataStore.edit { prefs ->
            prefs[SAVED_EMAIL_KEY] = email
        }
    }

    suspend fun getSavedEmail(context: Context): String? {
        return context.dataStore.data.map { it[SAVED_EMAIL_KEY] }.first()
    }

    suspend fun clearSavedLogin(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(SAVED_EMAIL_KEY)
            prefs[REMEMBER_LOGIN_KEY] = false
        }
    }
}
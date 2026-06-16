package com.example.vmsuser.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "plixo_auth")

class AuthTokenManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.map { it[KEY_TOKEN] }.first()

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(KEY_TOKEN) }
    }
}

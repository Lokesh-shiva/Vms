package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.ProfileRepository
import com.example.vmsuser.models.Notification
import com.example.vmsuser.models.WalletTransaction
import com.example.vmsuser.network.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repo = ProfileRepository()

    val user = UserSession.user

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions

    private val _walletBalance = MutableStateFlow(0)
    val walletBalance: StateFlow<Int> = _walletBalance

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                repo.getNotifications().onSuccess { _notifications.value = it }
            } catch (e: Exception) { Log.e("ProfileVM", "loadNotifications", e) }
        }
    }

    fun markNotificationRead(id: Int) {
        _notifications.value = _notifications.value.map { if (it.id == id) it.copy(read = true) else it }
        viewModelScope.launch {
            repo.markNotificationRead(id).onFailure { Log.w("ProfileVM", "markNotificationRead: ${it.message}") }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            try {
                repo.getWalletTransactions().onSuccess { _transactions.value = it }
            } catch (e: Exception) { Log.e("ProfileVM", "loadTransactions", e) }
        }
        viewModelScope.launch {
            try {
                repo.getWalletBalance().onSuccess { _walletBalance.value = it }
            } catch (e: Exception) { Log.e("ProfileVM", "loadWalletBalance", e) }
        }
    }

    fun updateProfile(
        name: String,
        region: String,
        username: String? = null,
        email: String? = null,
        dateOfBirth: String? = null,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _updating.value = true
            _updateError.value = null
            try {
                repo.updateProfile(name, region, username, email, dateOfBirth)
                    .onSuccess { onDone() }
                    .onFailure { _updateError.value = it.message ?: "Could not save profile." }
            } catch (e: Exception) {
                Log.e("ProfileVM", "updateProfile", e)
                _updateError.value = e.message ?: "Could not save profile."
            } finally {
                _updating.value = false
            }
        }
    }

    fun clearUpdateError() { _updateError.value = null }

}

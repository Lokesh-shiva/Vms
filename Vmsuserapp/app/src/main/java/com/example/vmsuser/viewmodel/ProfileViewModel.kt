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

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(mockTransactions())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions

    private val _walletBalance = MutableStateFlow(240)
    val walletBalance: StateFlow<Int> = _walletBalance

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
    }

    fun updateProfile(name: String, region: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.updateProfile(name, region).onSuccess { onDone() }
            } catch (e: Exception) { Log.e("ProfileVM", "updateProfile", e); onDone() }
        }
    }

    private fun mockTransactions() = listOf(
        WalletTransaction(1, "debit", -400, "Badminton match · Kanteerava", "2026-06-15"),
        WalletTransaction(2, "credit", 50, "Match completion bonus", "2026-06-14"),
        WalletTransaction(3, "debit", -500, "Cricket match · BBMP Ground", "2026-06-12"),
        WalletTransaction(4, "credit", 100, "Referral bonus", "2026-06-10"),
    )
}

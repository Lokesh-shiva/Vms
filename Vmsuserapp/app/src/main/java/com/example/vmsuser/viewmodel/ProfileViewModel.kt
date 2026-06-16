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

    private val _notifications = MutableStateFlow<List<Notification>>(mockNotifications())
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

    private fun mockNotifications() = listOf(
        Notification(1, "match_found", "Match found!", "Your Badminton match is confirmed for today 6 PM.", "2026-06-15T10:00:00", false),
        Notification(2, "society_invite", "Invite to IBaC", "You've been invited to Indiranagar Badminton Club.", "2026-06-14T14:00:00", true),
        Notification(3, "coin_earned", "Coins earned", "You earned 50 Plixo coins for completing a match.", "2026-06-13T18:00:00", true),
    )

    private fun mockTransactions() = listOf(
        WalletTransaction(1, "debit", -400, "Badminton match · Kanteerava", "2026-06-15"),
        WalletTransaction(2, "credit", 50, "Match completion bonus", "2026-06-14"),
        WalletTransaction(3, "debit", -500, "Cricket match · BBMP Ground", "2026-06-12"),
        WalletTransaction(4, "credit", 100, "Referral bonus", "2026-06-10"),
    )
}

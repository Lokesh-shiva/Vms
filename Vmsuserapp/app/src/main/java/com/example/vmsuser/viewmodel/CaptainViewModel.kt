package com.example.vmsuser.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.CaptainRepository
import com.example.vmsuser.models.CaptainApplication
import com.example.vmsuser.models.CaptainStats
import com.example.vmsuser.models.Match
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CaptainViewModel : ViewModel() {
    private val repo = CaptainRepository()

    private val _stats = MutableStateFlow(mockStats())
    val stats: StateFlow<CaptainStats> = _stats

    private val _applying = MutableStateFlow(false)
    val applying: StateFlow<Boolean> = _applying

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    private val _application = MutableStateFlow<CaptainApplication?>(null)
    val application: StateFlow<CaptainApplication?> = _application

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _updatingUpi = MutableStateFlow(false)
    val updatingUpi: StateFlow<Boolean> = _updatingUpi

    init { loadStats() }

    fun clearError() { _error.value = null }

    fun loadStats() {
        viewModelScope.launch {
            try {
                repo.getStats().onSuccess { _stats.value = it }
            } catch (e: Exception) { Log.e("CaptainVM", "loadStats", e) }
        }
    }

    fun apply(bio: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _applying.value = true
            _error.value = null
            repo.apply(bio)
                .onSuccess { _application.value = it; onSuccess() }
                .onFailure { _error.value = it.message ?: "Could not submit application." }
            _applying.value = false
        }
    }

    fun uploadKyc(contentResolver: ContentResolver, documentType: String, imageUri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uploading.value = true
            _error.value = null
            repo.uploadKyc(contentResolver, documentType, imageUri)
                .onSuccess { _application.value = it; onSuccess() }
                .onFailure { _error.value = it.message ?: "Upload failed. Try again." }
            _uploading.value = false
        }
    }

    fun loadApplicationStatus() {
        viewModelScope.launch {
            repo.getApplicationStatus()
                .onSuccess { _application.value = it }
                .onFailure { Log.w("CaptainVM", "loadApplicationStatus: ${it.message}") }
        }
    }

    /** Poll until the application leaves PENDING_REVIEW (approved/rejected) or the screen is left. */
    fun pollApplicationStatus() {
        viewModelScope.launch {
            repeat(60) {
                repo.getApplicationStatus().onSuccess { _application.value = it }
                if (_application.value?.status != "PENDING_REVIEW") return@launch
                delay(5000)
            }
        }
    }

    fun updatePayoutUpi(upiId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _updatingUpi.value = true
            _error.value = null
            repo.updatePayoutUpi(upiId)
                .onSuccess { loadStats(); onDone() }
                .onFailure { _error.value = it.message ?: "Could not update UPI ID." }
            _updatingUpi.value = false
        }
    }

    private fun mockStats() = CaptainStats(
        todayEarnings = 1200,
        weekEarnings = 8400,
        rating = 4.9f,
        matchesLed = 142,
        activeMatches = listOf(
            Match(1, "Badminton", "confirmed", "Kanteerava Annex", "Indiranagar", "Today 6 PM", price = 400),
            Match(2, "Cricket", "open", "BBMP Ground", "Koramangala", "Tomorrow 8 AM", price = 200),
        ),
    )
}

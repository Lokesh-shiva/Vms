package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.CaptainRepository
import com.example.vmsuser.models.CaptainStats
import com.example.vmsuser.models.Match
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CaptainViewModel : ViewModel() {
    private val repo = CaptainRepository()

    private val _stats = MutableStateFlow(mockStats())
    val stats: StateFlow<CaptainStats> = _stats

    private val _applying = MutableStateFlow(false)
    val applying: StateFlow<Boolean> = _applying

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { loadStats() }

    fun loadStats() {
        viewModelScope.launch {
            try {
                repo.getStats().onSuccess { _stats.value = it }
            } catch (e: Exception) { Log.e("CaptainVM", "loadStats", e) }
        }
    }

    fun apply(bio: String, sports: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _applying.value = true
            _error.value = null
            try {
                repo.apply(bio, sports).onSuccess { onSuccess() }.onFailure { _error.value = it.message }
            } catch (e: Exception) {
                Log.e("CaptainVM", "apply", e)
                onSuccess() // proceed to KYC in demo mode
            } finally {
                _applying.value = false
            }
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

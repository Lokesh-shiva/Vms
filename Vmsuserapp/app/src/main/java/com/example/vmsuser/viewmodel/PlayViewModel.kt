package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.MatchRepository
import com.example.vmsuser.models.Match
import com.example.vmsuser.models.QueueStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayViewModel : ViewModel() {
    private val repo = MatchRepository()

    private val _selectedSport = MutableStateFlow("Badminton")
    val selectedSport: StateFlow<String> = _selectedSport

    private val _selectedSkill = MutableStateFlow("Intermediate")
    val selectedSkill: StateFlow<String> = _selectedSkill

    private val _queueStatus = MutableStateFlow<QueueStatus?>(null)
    val queueStatus: StateFlow<QueueStatus?> = _queueStatus

    private val _activeMatch = MutableStateFlow<Match?>(null)
    val activeMatch: StateFlow<Match?> = _activeMatch

    private val _inQueue = MutableStateFlow(false)
    val inQueue: StateFlow<Boolean> = _inQueue

    private val _match = MutableStateFlow<Match?>(null)
    val match: StateFlow<Match?> = _match

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // Mock match history for UI
    val matchHistory = listOf(
        Match(id = 1, sport = "Badminton", status = "completed", groundName = "Kanteerava Annex", groundAddress = "Indiranagar, Bengaluru", scheduledAt = "2026-06-14T10:00:00", price = 400),
        Match(id = 2, sport = "Cricket", status = "completed", groundName = "BBMP Ground", groundAddress = "Koramangala", scheduledAt = "2026-06-12T08:00:00", price = 500),
    )

    fun selectSport(sport: String) { _selectedSport.value = sport }
    fun selectSkill(skill: String) { _selectedSkill.value = skill }

    fun joinQueue(onMatchFound: (Int) -> Unit = {}, onNavigate: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repo.joinQueue(_selectedSport.value, _selectedSkill.value)
                .onSuccess { status ->
                    _inQueue.value = true
                    _queueStatus.value = status
                    if (status.matchFound && status.matchId != null) {
                        onMatchFound(status.matchId)
                    } else {
                        onNavigate(_selectedSport.value)
                    }
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun leaveQueue(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.leaveQueue()
            } catch (e: Exception) {
                Log.e("PlayVM", "leaveQueue", e)
            } finally {
                _inQueue.value = false
                onDone()
            }
        }
    }

    fun loadMatch(matchId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repo.getMatch(matchId).onSuccess { _match.value = it }
                    .onFailure { Log.e("PlayVM", "loadMatch failed: ${it.message}") }
            } finally {
                _loading.value = false
            }
        }
    }

    fun pollQueueStatus() {
        viewModelScope.launch {
            repeat(60) {
                try {
                    repo.getQueueStatus().onSuccess { _queueStatus.value = it }
                } catch (e: Exception) {
                    Log.e("PlayVM", "pollStatus", e)
                }
                delay(3000)
            }
        }
    }
}

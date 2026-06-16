package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.MatchRepository
import com.example.vmsuser.models.Match
import com.example.vmsuser.models.QueueStatus
import com.example.vmsuser.network.RetrofitClient
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

    // Real price from backend pricing service (default ₹200)
    private val _price = MutableStateFlow(200)
    val price: StateFlow<Int> = _price

    // Real players in queue for selected sport + region
    private val _playersSearching = MutableStateFlow(0)
    val playersSearching: StateFlow<Int> = _playersSearching

    // Match history from API
    private val _matchHistory = MutableStateFlow<List<Match>>(emptyList())
    val matchHistory: StateFlow<List<Match>> = _matchHistory

    init {
        loadMatchHistory()
    }

    private val _maxPlayers = MutableStateFlow(2)
    val maxPlayers: StateFlow<Int> = _maxPlayers

    fun selectSport(sport: String) {
        _selectedSport.value = sport
        fetchPrice(sport)
    }

    fun selectSkill(skill: String) { _selectedSkill.value = skill }

    fun selectMaxPlayers(n: Int) { _maxPlayers.value = n }

    fun clearError() { _error.value = null }

    private fun fetchPrice(sport: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.api.getMatchmakingPrice(sport)
                if (res.success && res.data != null) {
                    _price.value = res.data.finalPrice
                    _playersSearching.value = res.data.playersSearching
                }
            } catch (e: Exception) {
                Log.w("PlayVM", "fetchPrice: ${e.message}")
            }
        }
    }

    private fun loadMatchHistory() {
        viewModelScope.launch {
            repo.getMyMatches()
                .onSuccess { _matchHistory.value = it }
                .onFailure { Log.w("PlayVM", "loadMatchHistory: ${it.message}") }
        }
    }

    fun joinQueue(onMatchFound: (Int) -> Unit = {}, onNavigate: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repo.joinQueue(_selectedSport.value, _selectedSkill.value, _maxPlayers.value)
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

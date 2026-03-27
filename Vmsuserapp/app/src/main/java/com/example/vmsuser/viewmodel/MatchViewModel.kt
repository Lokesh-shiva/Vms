package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.models.CartType
import com.example.vmsuser.models.CreateMatchRequest
import com.example.vmsuser.models.Match
import com.example.vmsuser.models.Region
import com.example.vmsuser.models.Timeslot
import com.example.vmsuser.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchViewModel(private val repository: MatchRepository) : ViewModel() {

    // ── Match list state ───────────────────────────────────────────────
    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ── Create state ──────────────────────────────────────────────────
    private val _createState = MutableStateFlow<CreateMatchUiState>(CreateMatchUiState.Idle)
    val createState: StateFlow<CreateMatchUiState> = _createState

    // ── Action feedback ───────────────────────────────────────────────
    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback

    // Filter state (for list screen)
    var filterSportId: Int? = null
    var filterRegionId: Int? = null

    init {
        loadMatches()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _matches.value = repository.getMatches(filterSportId, filterRegionId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createMatch(
        cartTypeId: Int,
        timeslotId: Int,
        regionId: Int,
        maxPlayers: Int,
        skillLevel: String?
    ) {
        viewModelScope.launch {
            _createState.value = CreateMatchUiState.Loading
            try {
                val request = CreateMatchRequest(
                    cart_type_id = cartTypeId,
                    timeslot_id = timeslotId,
                    region_id = regionId,
                    max_players = maxPlayers,
                    skill_level = skillLevel?.ifBlank { null }
                )
                val match = repository.createMatch(request)
                _createState.value = CreateMatchUiState.Success(match)
            } catch (e: Exception) {
                _createState.value = CreateMatchUiState.Error(e.message ?: "Failed to create match")
            }
        }
    }

    fun joinMatch(matchId: Int) {
        viewModelScope.launch {
            try {
                repository.joinMatch(matchId)
                _actionFeedback.value = "Joined match!"
                loadMatches()
            } catch (e: Exception) {
                _actionFeedback.value = e.message ?: "Failed to join match"
            }
        }
    }

    fun leaveMatch(matchId: Int) {
        viewModelScope.launch {
            try {
                repository.leaveMatch(matchId)
                _actionFeedback.value = "Left match."
                loadMatches()
            } catch (e: Exception) {
                _actionFeedback.value = e.message ?: "Failed to leave match"
            }
        }
    }

    fun clearCreateState() { _createState.value = CreateMatchUiState.Idle }
    fun clearFeedback() { _actionFeedback.value = null }
    fun clearError() { _error.value = null }
}

sealed class CreateMatchUiState {
    object Idle : CreateMatchUiState()
    object Loading : CreateMatchUiState()
    data class Success(val match: Match) : CreateMatchUiState()
    data class Error(val message: String) : CreateMatchUiState()
}

class MatchViewModelFactory(private val repository: MatchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

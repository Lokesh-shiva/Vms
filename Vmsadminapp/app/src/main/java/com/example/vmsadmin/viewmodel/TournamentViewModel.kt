package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TournamentRepository
import com.example.vmsadmin.models.CreateTournamentMatchRequest
import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.Tournament
import com.example.vmsadmin.models.TournamentMatch
import com.example.vmsadmin.models.TournamentRegistration
import com.example.vmsadmin.models.TournamentStanding
import com.example.vmsadmin.models.UpdateTournamentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TournamentUiState(
    val tournaments: List<Tournament> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet(),
    // Detail screen state (for the currently-selected tournament)
    val selectedTournament: Tournament? = null,
    val matches: List<TournamentMatch> = emptyList(),
    val standings: List<TournamentStanding> = emptyList(),
    val registrations: List<TournamentRegistration> = emptyList(),
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val schedulingMatch: Boolean = false,
    val recordingResultForMatchId: Int? = null,
)

class TournamentViewModel(private val repository: TournamentRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentUiState())
    val uiState: StateFlow<TournamentUiState> = _uiState.asStateFlow()

    init { loadTournaments() }

    fun loadTournaments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(
                    tournaments = repository.getTournaments(), isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load tournaments")
            }
        }
    }

    fun refreshTournaments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                _uiState.value = _uiState.value.copy(
                    tournaments = repository.getTournaments(), isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, error = e.message ?: "Refresh failed")
            }
        }
    }

    fun createTournament(request: CreateTournamentRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.createTournament(request)
                loadTournaments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to create tournament")
            }
        }
    }

    fun updateStatus(id: Int, status: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds + id)
            try {
                val updated = repository.updateTournament(id, UpdateTournamentRequest(status = status))
                _uiState.value = _uiState.value.copy(
                    tournaments = _uiState.value.tournaments.map { if (it.id == id) updated else it },
                    updatingIds = _uiState.value.updatingIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to update tournament"
                )
            }
        }
    }

    fun deleteTournament(id: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteTournament(id)
                _uiState.value = _uiState.value.copy(
                    tournaments = _uiState.value.tournaments.filter { it.id != id }
                )
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to delete tournament")
            }
        }
    }

    // ── Detail screen (matches / standings / registrations) ─────────────

    fun selectTournament(tournament: Tournament) {
        _uiState.value = _uiState.value.copy(selectedTournament = tournament)
        loadTournamentDetail(tournament.id)
    }

    fun loadTournamentDetail(tournamentId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoading = true, detailError = null)
            try {
                val matches = repository.getMatches(tournamentId)
                val standings = repository.getStandings(tournamentId)
                val registrations = repository.getRegistrations(tournamentId)
                _uiState.value = _uiState.value.copy(
                    matches = matches,
                    standings = standings,
                    registrations = registrations,
                    detailLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    detailLoading = false,
                    detailError = e.message ?: "Failed to load tournament detail",
                )
            }
        }
    }

    fun scheduleMatch(tournamentId: Int, request: CreateTournamentMatchRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(schedulingMatch = true)
            try {
                val match = repository.createMatch(tournamentId, request)
                _uiState.value = _uiState.value.copy(
                    matches = _uiState.value.matches + match,
                    schedulingMatch = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    schedulingMatch = false,
                    detailError = e.message ?: "Failed to schedule match",
                )
            }
        }
    }

    fun recordMatchResult(tournamentId: Int, matchId: Int, homeScore: Int, awayScore: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recordingResultForMatchId = matchId)
            try {
                val updated = repository.recordMatchResult(tournamentId, matchId, homeScore, awayScore)
                _uiState.value = _uiState.value.copy(
                    matches = _uiState.value.matches.map { if (it.id == matchId) updated else it },
                    recordingResultForMatchId = null,
                )
                // Standings change as a side effect of recording a result — refresh them.
                _uiState.value = _uiState.value.copy(standings = repository.getStandings(tournamentId))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    recordingResultForMatchId = null,
                    detailError = e.message ?: "Failed to record result",
                )
            }
        }
    }

    fun clearDetailError() {
        _uiState.value = _uiState.value.copy(detailError = null)
    }

}

class TournamentViewModelFactory(
    private val repository: TournamentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TournamentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TournamentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TournamentRepository
import com.example.vmsadmin.models.CreateTournamentRequest
import com.example.vmsadmin.models.Tournament
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
    val snackbar: String? = null
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
                _uiState.value = _uiState.value.copy(isRefreshing = false, snackbar = e.message ?: "Refresh failed")
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
                    updatingIds = _uiState.value.updatingIds - id,
                    snackbar = "Status updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    updatingIds = _uiState.value.updatingIds - id,
                    snackbar = e.message ?: "Failed to update tournament"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbar = null)
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

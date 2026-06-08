package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.SocietyRepository
import com.example.vmsadmin.models.Society
import com.example.vmsadmin.models.SocietyLeaderboardEntry
import com.example.vmsadmin.models.SocietyMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SocietyUiState(
    val societies: List<Society> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val actionInProgressIds: Set<Int> = emptySet(),
    // Detail panel state
    val selectedSociety: Society? = null,
    val members: List<SocietyMember> = emptyList(),
    val leaderboard: List<SocietyLeaderboardEntry> = emptyList(),
    val isDetailLoading: Boolean = false
)

class SocietyViewModel(private val repository: SocietyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SocietyUiState())
    val uiState: StateFlow<SocietyUiState> = _uiState.asStateFlow()

    init { loadSocieties() }

    fun loadSocieties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(
                    societies = repository.getSocieties(), isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = e.message ?: "Failed to load societies"
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                _uiState.value = _uiState.value.copy(
                    societies = repository.getSocieties(), isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false, error = e.message ?: "Refresh failed"
                )
            }
        }
    }

    fun deactivateSociety(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgressIds = _uiState.value.actionInProgressIds + id)
            try {
                val updated = repository.deactivateSociety(id)
                _uiState.value = _uiState.value.copy(
                    societies = _uiState.value.societies.map { if (it.id == id) updated else it },
                    actionInProgressIds = _uiState.value.actionInProgressIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionInProgressIds = _uiState.value.actionInProgressIds - id,
                    error = e.message ?: "Failed to deactivate society"
                )
            }
        }
    }

    fun deleteSociety(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgressIds = _uiState.value.actionInProgressIds + id)
            try {
                repository.deleteSociety(id)
                _uiState.value = _uiState.value.copy(
                    societies = _uiState.value.societies.filter { it.id != id },
                    actionInProgressIds = _uiState.value.actionInProgressIds - id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionInProgressIds = _uiState.value.actionInProgressIds - id,
                    error = e.message ?: "Failed to delete society"
                )
            }
        }
    }

    fun loadDetail(society: Society) {
        _uiState.value = _uiState.value.copy(
            selectedSociety = society, isDetailLoading = true,
            members = emptyList(), leaderboard = emptyList()
        )
        viewModelScope.launch {
            try {
                val members = repository.getSocietyMembers(society.id)
                val leaderboard = repository.getSocietyLeaderboard(society.id)
                _uiState.value = _uiState.value.copy(
                    members = members, leaderboard = leaderboard, isDetailLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDetailLoading = false, error = e.message ?: "Failed to load society detail"
                )
            }
        }
    }

    fun clearDetail() {
        _uiState.value = _uiState.value.copy(
            selectedSociety = null, members = emptyList(), leaderboard = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class SocietyViewModelFactory(
    private val repository: SocietyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SocietyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocietyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

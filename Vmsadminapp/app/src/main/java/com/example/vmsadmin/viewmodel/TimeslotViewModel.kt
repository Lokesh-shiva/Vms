package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TimeslotRepository
import com.example.vmsadmin.models.Timeslot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimeslotUiState(
    val timeslots: List<Timeslot> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTimeslot: Timeslot? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingTimeslot: Timeslot? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingTimeslot: Timeslot? = null,
    val updatingTimeslotIds: Set<Int> = emptySet()
)

class TimeslotViewModel(
    private val timeslotRepository: TimeslotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeslotUiState())
    val uiState: StateFlow<TimeslotUiState> = _uiState.asStateFlow()

    fun loadTimeslots() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val timeslots = timeslotRepository.getTimeslots()
                    .sortedBy { it.start_time }
                _uiState.value = _uiState.value.copy(
                    timeslots = timeslots,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun refreshTimeslots() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val timeslots = timeslotRepository.getTimeslots()
                    .sortedBy { it.start_time }
                _uiState.value = _uiState.value.copy(
                    timeslots = timeslots,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun addTimeslot(startTime: String, endTime: String, capacity: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                timeslotRepository.createTimeslot(startTime, endTime, capacity)
                _uiState.value = _uiState.value.copy(showAddDialog = false)
                loadTimeslots()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add timeslot"
                )
            }
        }
    }

    fun updateTimeslot(id: Int, startTime: String, endTime: String, capacity: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                timeslotRepository.updateTimeslot(
                    id = id,
                    startTime = startTime,
                    endTime = endTime,
                    capacity = capacity
                )
                _uiState.value = _uiState.value.copy(
                    showEditDialog = false,
                    editingTimeslot = null
                )
                loadTimeslots()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update timeslot"
                )
            }
        }
    }

    fun toggleTimeslot(id: Int, isActive: Boolean) {
        val originalList = _uiState.value.timeslots
        val updatedList = originalList.map {
            if (it.id == id) it.copy(is_active = isActive) else it
        }
        _uiState.value = _uiState.value.copy(
            timeslots = updatedList,
            updatingTimeslotIds = _uiState.value.updatingTimeslotIds + id,
            error = null
        )
        viewModelScope.launch {
            try {
                timeslotRepository.toggleTimeslotActive(id, isActive)
            } catch (e: Exception) {
                // Revert optimistic update on failure
                _uiState.value = _uiState.value.copy(
                    timeslots = originalList,
                    error = e.message ?: "Failed to toggle timeslot"
                )
            } finally {
                // Always clear the updating flag
                _uiState.value = _uiState.value.copy(
                    updatingTimeslotIds = _uiState.value.updatingTimeslotIds - id
                )
            }
        }
    }

    fun deleteTimeslot(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, error = null,
                showDeleteConfirm = false, deletingTimeslot = null
            )
            try {
                timeslotRepository.deleteTimeslot(id)
                loadTimeslots()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete timeslot"
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun dismissAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun showEditDialog(timeslot: Timeslot) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, editingTimeslot = timeslot)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingTimeslot = null)
    }

    fun showDeleteConfirm(timeslot: Timeslot) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true, deletingTimeslot = timeslot)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, deletingTimeslot = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class TimeslotViewModelFactory(
    private val timeslotRepository: TimeslotRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeslotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeslotViewModel(timeslotRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

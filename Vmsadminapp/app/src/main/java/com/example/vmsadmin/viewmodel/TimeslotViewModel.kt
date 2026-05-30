package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TimeslotRepository
import com.example.vmsadmin.models.Timeslot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimeslotUiState(
    val timeslots: List<Timeslot> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
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
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                timeslotRepository.createTimeslot(startTime, endTime, capacity)
                delay(200)
                loadTimeslots()
                _uiState.update { it.copy(successMessage = "Timeslot saved", showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add timeslot") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun updateTimeslot(id: Int, startTime: String, endTime: String, capacity: Int) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                timeslotRepository.updateTimeslot(
                    id = id,
                    startTime = startTime,
                    endTime = endTime,
                    capacity = capacity
                )
                delay(200)
                loadTimeslots()
                _uiState.update { it.copy(successMessage = "Timeslot updated", showEditDialog = false, editingTimeslot = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update timeslot") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
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
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, showDeleteConfirm = false, deletingTimeslot = null) }
            try {
                timeslotRepository.deleteTimeslot(id)
                delay(200)
                loadTimeslots()
                _uiState.update { it.copy(successMessage = "Timeslot deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete timeslot") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
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
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
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

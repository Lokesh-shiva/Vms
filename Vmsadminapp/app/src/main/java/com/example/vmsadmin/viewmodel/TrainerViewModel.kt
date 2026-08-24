package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TrainerRepository
import com.example.vmsadmin.models.Trainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainerUiState(
    val trainers: List<Trainer> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingTrainer: Trainer? = null,
    val updatingIds: Set<Int> = emptySet(),
)

class TrainerViewModel(private val repository: TrainerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainerUiState())
    val uiState: StateFlow<TrainerUiState> = _uiState.asStateFlow()

    fun loadTrainers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val trainers = repository.getTrainers()
                _uiState.update { it.copy(trainers = trainers, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load trainers") }
            }
        }
    }

    fun addTrainer(name: String, bio: String, specialties: String, rate: Double) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.createTrainer(name, bio, specialties, rate)
                loadTrainers()
                _uiState.update { it.copy(showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add trainer") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun updateTrainer(id: Int, name: String, bio: String, specialties: String, rate: Double) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.updateTrainer(id, name, bio, specialties, rate)
                loadTrainers()
                _uiState.update { it.copy(showEditDialog = false, editingTrainer = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update trainer") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun toggleTrainer(id: Int, isActive: Boolean) {
        val original = _uiState.value.trainers
        _uiState.update {
            it.copy(
                trainers = it.trainers.map { t -> if (t.id == id) t.copy(is_active = isActive) else t },
                updatingIds = it.updatingIds + id,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                repository.toggleTrainerActive(id, isActive)
                _uiState.update { it.copy(updatingIds = it.updatingIds - id) }
            } catch (e: Exception) {
                _uiState.update { it.copy(trainers = original, updatingIds = it.updatingIds - id, error = e.message ?: "Failed to toggle trainer") }
            }
        }
    }

    fun deleteTrainer(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.deleteTrainer(id)
                loadTrainers()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete trainer") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun dismissAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }
    fun showEditDialog(trainer: Trainer) { _uiState.update { it.copy(showEditDialog = true, editingTrainer = trainer) } }
    fun dismissEditDialog() { _uiState.update { it.copy(showEditDialog = false, editingTrainer = null) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}

class TrainerViewModelFactory(
    private val repository: TrainerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrainerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.UserManagementRepository
import com.example.vmsadmin.models.AppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserManagementState {
    object Loading : UserManagementState()
    data class Success(val users: List<AppUser>) : UserManagementState()
    data class Error(val message: String) : UserManagementState()
}

class UserManagementViewModel(
    private val repository: UserManagementRepository,
    private val currentUserId: Int?
) : ViewModel() {

    private val _state = MutableStateFlow<UserManagementState>(UserManagementState.Loading)
    val state: StateFlow<UserManagementState> = _state.asStateFlow()

    // Set of user IDs currently being mutated — used to disable rows in UI
    private val _pendingIds = MutableStateFlow<Set<Int>>(emptySet())
    val pendingIds: StateFlow<Set<Int>> = _pendingIds.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _state.value = UserManagementState.Loading
            try {
                val users = repository.getUsers()
                _state.value = UserManagementState.Success(users)
            } catch (e: Exception) {
                _state.value = UserManagementState.Error(e.message ?: "Failed to load users")
            }
        }
    }

    fun changeRole(id: Int, newRole: String) {
        if (id == currentUserId) return  // ViewModel-layer self-mutation guard
        viewModelScope.launch {
            _pendingIds.value = _pendingIds.value + id
            try {
                val updated = repository.updateRole(id, newRole)
                val current = (_state.value as? UserManagementState.Success)?.users ?: return@launch
                _state.value = UserManagementState.Success(current.map { if (it.id == id) updated else it })
            } catch (e: Exception) {
                _state.value = UserManagementState.Error(e.message ?: "Failed to change role")
            } finally {
                _pendingIds.value = _pendingIds.value - id
            }
        }
    }

    fun toggleActive(id: Int, currentActive: Boolean) {
        if (id == currentUserId) return  // ViewModel-layer self-mutation guard
        viewModelScope.launch {
            _pendingIds.value = _pendingIds.value + id
            try {
                val updated = repository.setActive(id, !currentActive)
                val current = (_state.value as? UserManagementState.Success)?.users ?: return@launch
                _state.value = UserManagementState.Success(current.map { if (it.id == id) updated else it })
            } catch (e: Exception) {
                _state.value = UserManagementState.Error(e.message ?: "Failed to toggle status")
            } finally {
                _pendingIds.value = _pendingIds.value - id
            }
        }
    }
}

class UserManagementViewModelFactory(
    private val repository: UserManagementRepository,
    private val currentUserId: Int?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserManagementViewModel(repository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

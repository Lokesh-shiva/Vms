package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.AuditLogRepository
import com.example.vmsadmin.models.AuditLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

data class AuditLogFilters(
    val action: String? = null,
    val actorUserId: Int? = null,
    val targetResourceType: String? = null,
    val startDate: String? = null, // YYYY-MM-DD
    val endDate: String? = null,   // YYYY-MM-DD
) {
    val isActive: Boolean
        get() = action != null || actorUserId != null || targetResourceType != null ||
            startDate != null || endDate != null
}

data class AuditLogUiState(
    val logs: List<AuditLogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val filters: AuditLogFilters = AuditLogFilters(),
    val endReached: Boolean = false,
)

class AuditLogViewModel(private val repository: AuditLogRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditLogUiState())
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    init { loadLogs() }

    private fun fetch(offset: Int, filters: AuditLogFilters) = viewModelScope.launch {
        try {
            val page = repository.getAuditLogs(
                limit = PAGE_SIZE,
                offset = offset,
                action = filters.action,
                actorUserId = filters.actorUserId,
                targetResourceType = filters.targetResourceType,
                startDate = filters.startDate,
                endDate = filters.endDate,
            )
            val combined = if (offset == 0) page else _uiState.value.logs + page
            _uiState.value = _uiState.value.copy(
                logs = combined,
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                error = null,
                endReached = page.size < PAGE_SIZE,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                error = e.message ?: "Failed to load audit logs",
            )
        }
    }

    fun loadLogs() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        fetch(offset = 0, filters = _uiState.value.filters)
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        fetch(offset = 0, filters = _uiState.value.filters)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || state.endReached) return
        _uiState.value = state.copy(isLoadingMore = true)
        fetch(offset = state.logs.size, filters = state.filters)
    }

    fun applyFilters(filters: AuditLogFilters) {
        _uiState.value = _uiState.value.copy(filters = filters, isLoading = true, error = null)
        fetch(offset = 0, filters = filters)
    }

    fun clearFilters() = applyFilters(AuditLogFilters())
}

class AuditLogViewModelFactory(
    private val repository: AuditLogRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuditLogViewModel(repository) as T
    }
}

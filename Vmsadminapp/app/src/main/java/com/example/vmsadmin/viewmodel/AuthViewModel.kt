package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.TokenManager
import com.example.vmsadmin.models.LoginRequest
import com.example.vmsadmin.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AuthViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    companion object {
        private val ADMIN_ROLES = setOf(
            "super_admin", "ops_manager", "ground_owner",
            "tournament_manager", "support", "finance", "csr_partner"
        )
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    val currentRole: StateFlow<String?> = tokenManager.roleFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val currentUserId: StateFlow<Int?> = tokenManager.userIdFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _debugRole = MutableStateFlow<String?>(null)

    /** The role actually used for UI — overridden by debug switcher when active. */
    val effectiveRole: StateFlow<String?> = currentRole
        .combine(_debugRole) { real, debug -> debug ?: real }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setDebugRole(role: String?) {
        _debugRole.value = role
    }

    fun login(phoneNumber: String, passwordHash: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val request = LoginRequest(phone = phoneNumber, password = passwordHash)
                val response = apiService.login(request)

                if (response.success && response.data != null) {
                    val role = response.data.role
                    if (role == null || role !in ADMIN_ROLES) {
                        _loginState.value = LoginState.Error("Access denied: this account does not have admin access.")
                    } else {
                        tokenManager.saveToken(response.data.access_token)
                        tokenManager.saveRole(role)
                        val userId = response.data.user_id
                        if (userId != null) {
                            tokenManager.saveUserId(userId)
                        }
                        _loginState.value = LoginState.Success
                    }
                } else {
                    _loginState.value = LoginState.Error(response.message ?: "Login failed")
                }
            } catch (e: HttpException) {
                _loginState.value = LoginState.Error("Login failed. Please try again.")
            } catch (e: IOException) {
                _loginState.value = LoginState.Error("Network error. Please check your connection.")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("An unexpected error occurred.")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            _debugRole.value = null
            tokenManager.clearSession()
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class AuthViewModelFactory(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(apiService, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

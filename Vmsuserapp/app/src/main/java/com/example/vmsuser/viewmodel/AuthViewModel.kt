package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.models.UiState
import com.example.vmsuser.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val registerState: StateFlow<UiState<Unit>> = _registerState.asStateFlow()

    fun login(phone: String, password: String) {
        if (_loginState.value is UiState.Loading) return
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                authRepository.login(phone, password)
                _loginState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(name: String, phone: String, password: String) {
        if (_registerState.value is UiState.Loading) return
        viewModelScope.launch {
            _registerState.value = UiState.Loading
            try {
                authRepository.register(name, phone, password)
                _registerState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _registerState.value = UiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = UiState.Idle
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

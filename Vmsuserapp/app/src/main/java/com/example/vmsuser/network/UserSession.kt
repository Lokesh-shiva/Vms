package com.example.vmsuser.network

import com.example.vmsuser.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSession {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    val currentUser get() = _user.value
    val isLoggedIn get() = _user.value != null
    val isCaptain get() = _user.value?.isCaptain == true
    val canCreateSociety get() = _user.value?.canCreateSociety == true

    fun setUser(user: User?) { _user.value = user }
    fun clear() { _user.value = null }
}

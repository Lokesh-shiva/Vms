package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.SocialRepository
import com.example.vmsuser.models.LeaderboardEntry
import com.example.vmsuser.models.Society
import com.example.vmsuser.models.SocietyMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {
    private val repo = SocialRepository()
    private val _societies = MutableStateFlow<List<Society>>(emptyList())
    val societies: StateFlow<List<Society>> = _societies
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _selected = MutableStateFlow<Society?>(null)
    val selected: StateFlow<Society?> = _selected
    private val _members = MutableStateFlow<List<SocietyMember>>(emptyList())
    val members: StateFlow<List<SocietyMember>> = _members
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard
    private val _joinedIds = MutableStateFlow<Set<Int>>(emptySet())
    val joinedIds: StateFlow<Set<Int>> = _joinedIds
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                repo.getSocieties().onSuccess { _societies.value = it }
            } catch (e: Exception) { Log.e("SocialVM", "load", e) }
            finally { _loading.value = false }
        }
    }

    fun select(id: Int) {
        viewModelScope.launch {
            try {
                repo.getSociety(id).onSuccess { _selected.value = it }
                    .onFailure { _selected.value = _societies.value.find { s -> s.id == id } }
                repo.getMembers(id).onSuccess { _members.value = it }
                repo.getLeaderboard(id).onSuccess { _leaderboard.value = it }
            } catch (e: Exception) {
                _selected.value = _societies.value.find { s -> s.id == id }
            }
        }
    }

    fun join(id: Int) {
        viewModelScope.launch {
            try { repo.join(id) } catch (e: Exception) { Log.e("SocialVM", "join", e) }
            _joinedIds.value = _joinedIds.value + id
        }
    }

    fun leave(id: Int) {
        viewModelScope.launch {
            try { repo.leave(id) } catch (e: Exception) { Log.e("SocialVM", "leave", e) }
            _joinedIds.value = _joinedIds.value - id
        }
    }

    fun create(name: String, sport: String, description: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.create(name, sport, description).onSuccess { onSuccess() }
                    .onFailure { _error.value = it.message }
            } catch (e: Exception) { _error.value = e.message }
        }
    }

}

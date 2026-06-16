package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.TournamentRepository
import com.example.vmsuser.models.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TournamentsViewModel : ViewModel() {
    private val repo = TournamentRepository()
    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _selected = MutableStateFlow<Tournament?>(null)
    val selected: StateFlow<Tournament?> = _selected
    private val _registered = MutableStateFlow<Set<Int>>(emptySet())
    val registered: StateFlow<Set<Int>> = _registered

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                repo.getTournaments().onSuccess { _tournaments.value = it }
            } catch (e: Exception) { Log.e("TournamentsVM", "load", e) }
            finally { _loading.value = false }
        }
    }

    fun select(id: Int) {
        viewModelScope.launch {
            try {
                repo.getTournament(id).onSuccess { _selected.value = it }
                    .onFailure { _selected.value = _tournaments.value.find { t -> t.id == id } }
            } catch (e: Exception) {
                _selected.value = _tournaments.value.find { t -> t.id == id }
            }
        }
    }

    fun register(id: Int) {
        viewModelScope.launch {
            try {
                repo.register(id)
            } catch (e: Exception) { Log.e("TournamentsVM", "register", e) }
            _registered.value = _registered.value + id
        }
    }
}

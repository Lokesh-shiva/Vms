package com.example.vmsuser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.data.TournamentRepository
import com.example.vmsuser.models.SportVoteResult
import com.example.vmsuser.models.SportVotesResponse
import com.example.vmsuser.models.Tournament
import com.example.vmsuser.models.TournamentStanding
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

    private val _registering = MutableStateFlow(false)
    val registering: StateFlow<Boolean> = _registering
    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError

    private val _votes = MutableStateFlow<List<SportVoteResult>>(emptyList())
    val votes: StateFlow<List<SportVoteResult>> = _votes
    private val _voteOptions = MutableStateFlow<List<String>>(emptyList())
    val voteOptions: StateFlow<List<String>> = _voteOptions
    private val _myVote = MutableStateFlow<String?>(null)
    val myVote: StateFlow<String?> = _myVote
    private val _totalVotes = MutableStateFlow(0)
    val totalVotes: StateFlow<Int> = _totalVotes
    private val _voteStatus = MutableStateFlow("NONE") // NONE | OPEN | CLOSED
    val voteStatus: StateFlow<String> = _voteStatus
    private val _voteClosesAt = MutableStateFlow<String?>(null)
    val voteClosesAt: StateFlow<String?> = _voteClosesAt
    private val _voteWinner = MutableStateFlow<String?>(null)
    val voteWinner: StateFlow<String?> = _voteWinner
    private val _votesLoading = MutableStateFlow(false)
    val votesLoading: StateFlow<Boolean> = _votesLoading
    private val _voteError = MutableStateFlow<String?>(null)
    val voteError: StateFlow<String?> = _voteError

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
            _registering.value = true
            _registerError.value = null
            repo.register(id)
                .onSuccess { _registered.value = _registered.value + id }
                .onFailure { e -> _registerError.value = e.message ?: "Failed to register." }
            _registering.value = false
        }
    }

    fun withdraw(id: Int) {
        viewModelScope.launch {
            _registering.value = true
            _registerError.value = null
            repo.withdraw(id)
                .onSuccess { _registered.value = _registered.value - id }
                .onFailure { e -> _registerError.value = e.message ?: "Failed to withdraw." }
            _registering.value = false
        }
    }

    fun clearRegisterError() { _registerError.value = null }

    private val _standings = MutableStateFlow<List<TournamentStanding>>(emptyList())
    val standings: StateFlow<List<TournamentStanding>> = _standings
    private val _standingsLoading = MutableStateFlow(false)
    val standingsLoading: StateFlow<Boolean> = _standingsLoading
    private val _standingsError = MutableStateFlow<String?>(null)
    val standingsError: StateFlow<String?> = _standingsError

    fun loadStandings(tournamentId: Int) {
        viewModelScope.launch {
            _standingsLoading.value = true
            _standingsError.value = null
            repo.getStandings(tournamentId)
                .onSuccess { _standings.value = it }
                .onFailure { e -> _standingsError.value = e.message ?: "Failed to load standings." }
            _standingsLoading.value = false
        }
    }

    private fun applyVoteState(state: SportVotesResponse) {
        _votes.value = state.results
        _voteOptions.value = state.options
        _myVote.value = state.myVote
        _totalVotes.value = state.totalVotes
        _voteStatus.value = state.status
        _voteClosesAt.value = state.closesAt
        _voteWinner.value = state.winnerSport
    }

    fun loadVotes() {
        viewModelScope.launch {
            _votesLoading.value = true
            _voteError.value = null
            repo.getVotes()
                .onSuccess { applyVoteState(it) }
                .onFailure { e -> _voteError.value = e.message ?: "Failed to load votes." }
            _votesLoading.value = false
        }
    }

    fun castVote(sport: String) {
        viewModelScope.launch {
            _votesLoading.value = true
            _voteError.value = null
            repo.castVote(sport)
                .onSuccess { applyVoteState(it) }
                .onFailure { e -> _voteError.value = e.message ?: "Failed to cast vote." }
            _votesLoading.value = false
        }
    }
}

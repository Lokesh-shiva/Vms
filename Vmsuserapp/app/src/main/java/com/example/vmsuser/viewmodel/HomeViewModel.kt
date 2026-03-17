package com.example.vmsuser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsuser.models.CartType
import com.example.vmsuser.models.Region
import com.example.vmsuser.models.Timeslot
import com.example.vmsuser.models.UiState
import com.example.vmsuser.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeData(
    val regions: List<Region> = emptyList(),
    val cartTypes: List<CartType> = emptyList(),
    val timeslots: List<Timeslot> = emptyList()
)

class HomeViewModel(private val locationRepository: LocationRepository) : ViewModel() {

    private val _homeState = MutableStateFlow<UiState<HomeData>>(UiState.Idle)
    val homeState: StateFlow<UiState<HomeData>> = _homeState.asStateFlow()

    fun loadHomeData() {
        if (_homeState.value is UiState.Loading) return
        viewModelScope.launch {
            _homeState.value = UiState.Loading
            try {
                val regions = locationRepository.getRegions()
                val cartTypes = locationRepository.getCartTypes()
                val timeslots = locationRepository.getTimeslots()
                _homeState.value = UiState.Success(
                    HomeData(regions = regions, cartTypes = cartTypes, timeslots = timeslots)
                )
            } catch (e: Exception) {
                _homeState.value = UiState.Error(e.message ?: "Failed to load data")
            }
        }
    }

    fun fetchRegions() {
        viewModelScope.launch {
            try {
                val regions = locationRepository.getRegions()
                val current = (_homeState.value as? UiState.Success)?.data ?: HomeData()
                _homeState.value = UiState.Success(current.copy(regions = regions))
            } catch (e: Exception) {
                _homeState.value = UiState.Error(e.message ?: "Failed to fetch regions")
            }
        }
    }

    fun fetchCartTypes() {
        viewModelScope.launch {
            try {
                val cartTypes = locationRepository.getCartTypes()
                val current = (_homeState.value as? UiState.Success)?.data ?: HomeData()
                _homeState.value = UiState.Success(current.copy(cartTypes = cartTypes))
            } catch (e: Exception) {
                _homeState.value = UiState.Error(e.message ?: "Failed to fetch cart types")
            }
        }
    }

    fun fetchTimeslots() {
        viewModelScope.launch {
            try {
                val timeslots = locationRepository.getTimeslots()
                val current = (_homeState.value as? UiState.Success)?.data ?: HomeData()
                _homeState.value = UiState.Success(current.copy(timeslots = timeslots))
            } catch (e: Exception) {
                _homeState.value = UiState.Error(e.message ?: "Failed to fetch timeslots")
            }
        }
    }
}

class HomeViewModelFactory(
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

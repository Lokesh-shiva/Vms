# Phase 01 Plan: Grounds Management

## Goal
Add a functional Grounds Management screen so ops staff can view all grounds, see status (AVAILABLE/BUSY), and toggle is_active — using existing patterns without architectural changes.

## Verification Criteria
- `GET /api/v1/grounds` is called on screen load
- Each ground shows: name, sport_id, status badge, is_active switch
- Toggling is_active calls `PUT /api/v1/grounds/{id}` with `{ "is_active": bool }`
- Switch is disabled while the update request is in-flight
- Error + Retry shown on network failure
- "Grounds" entry appears in ManageScreen and navigates to GroundsScreen
- No regressions to existing screens (Carts, Matches, Payments still work)

---

## Task 1 — Add Ground models to Models.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt`

Add after the existing `UpdateCartRequest` block:

```kotlin
@Serializable
data class Ground(
    val id: Int,
    val name: String,
    val sport_id: Int,
    val location_id: Int,
    val status: String,
    val is_active: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UpdateGroundRequest(
    val is_active: Boolean? = null
)
```

**Why separate model:** Grounds have domain-named fields (name, sport_id, location_id) that differ from Cart (label, cart_type_id, region_id). Mapping at the model layer keeps the rest of the code clean.

---

## Task 2 — Add ground endpoints to ApiService.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt`

Add imports at top:
```kotlin
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.models.UpdateGroundRequest
```

Add after the Match endpoints block:

```kotlin
// ── Ground endpoints ──────────────────────────────────────────────────
@GET("/api/v1/grounds")
suspend fun getGrounds(): ApiResponse<List<Ground>>

@PUT("/api/v1/grounds/{id}")
suspend fun updateGround(
    @Path("id") id: Int,
    @Body request: UpdateGroundRequest
): ApiResponse<Ground>
```

**Note:** Uses PUT (not PATCH) — matches backend `@router.put` definition.

---

## Task 3 — Create GroundRepository.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/GroundRepository.kt`

```kotlin
package com.example.vmsadmin.data

import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.models.UpdateGroundRequest
import com.example.vmsadmin.network.ApiService

class GroundRepository(private val apiService: ApiService) {

    suspend fun getGrounds(): List<Ground> {
        val response = apiService.getGrounds()
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to fetch grounds")
    }

    suspend fun toggleGround(id: Int, isActive: Boolean): Ground {
        val response = apiService.updateGround(id, UpdateGroundRequest(is_active = isActive))
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to update ground")
    }
}
```

---

## Task 4 — Create GroundViewModel.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/GroundViewModel.kt`

```kotlin
package com.example.vmsadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vmsadmin.data.GroundRepository
import com.example.vmsadmin.models.Ground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroundUiState(
    val grounds: List<Ground> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingIds: Set<Int> = emptySet()
)

class GroundViewModel(private val repository: GroundRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GroundUiState())
    val uiState: StateFlow<GroundUiState> = _uiState.asStateFlow()

    init {
        loadGrounds()
    }

    fun loadGrounds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val grounds = repository.getGrounds()
                _uiState.value = _uiState.value.copy(grounds = grounds, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load grounds"
                )
            }
        }
    }

    fun refreshGrounds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val grounds = repository.getGrounds()
                _uiState.value = _uiState.value.copy(grounds = grounds, isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Failed to refresh grounds"
                )
            }
        }
    }

    fun toggleGround(id: Int, isActive: Boolean) {
        val original = _uiState.value.grounds
        // Optimistic update
        _uiState.value = _uiState.value.copy(
            grounds = original.map { if (it.id == id) it.copy(is_active = isActive) else it },
            updatingIds = _uiState.value.updatingIds + id
        )
        viewModelScope.launch {
            try {
                repository.toggleGround(id, isActive)
                _uiState.value = _uiState.value.copy(updatingIds = _uiState.value.updatingIds - id)
            } catch (e: Exception) {
                // Rollback optimistic update
                _uiState.value = _uiState.value.copy(
                    grounds = original,
                    updatingIds = _uiState.value.updatingIds - id,
                    error = e.message ?: "Failed to update ground"
                )
            }
        }
    }
}

class GroundViewModelFactory(private val repository: GroundRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroundViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroundViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

---

## Task 5 — Create GroundsScreen.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/GroundsScreen.kt`

```kotlin
package com.example.vmsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.GroundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundsScreen(
    viewModel: GroundViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Grounds",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.grounds.isEmpty() -> {
                    // Shimmer skeleton
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            AppCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.width(160.dp).height(20.dp).shimmerEffect())
                                    Box(modifier = Modifier.width(50.dp).height(28.dp).shimmerEffect())
                                }
                                Spacer(Modifier.height(10.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).shimmerEffect())
                                Spacer(Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).shimmerEffect())
                            }
                        }
                    }
                }

                uiState.error != null && uiState.grounds.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            uiState.error ?: "Something went wrong",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadGrounds() }) { Text("Retry") }
                    }
                }

                uiState.grounds.isEmpty() -> {
                    Text(
                        "No grounds configured.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshGrounds() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.grounds, key = { it.id }) { ground ->
                                GroundCard(
                                    ground = ground,
                                    isUpdating = uiState.updatingIds.contains(ground.id),
                                    onToggle = { isActive -> viewModel.toggleGround(ground.id, isActive) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroundCard(
    ground: Ground,
    isUpdating: Boolean,
    onToggle: (Boolean) -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ground.name.ifBlank { "Ground #${ground.id}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                StatusBadge(status = ground.status)
            }
            Switch(
                checked = ground.is_active,
                onCheckedChange = onToggle,
                enabled = !isUpdating
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Sport ID: ${ground.sport_id}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Location ID: ${ground.location_id}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

## Task 6 — Add Grounds to ManageScreen (PlaceholderScreens.kt)

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/PlaceholderScreens.kt`

Add `onNavigateToGrounds: () -> Unit = {}` to `ManageScreen` parameters, and add a new `ManageCard` item in the LazyColumn:

```kotlin
// In ManageScreen function signature, add:
onNavigateToGrounds: () -> Unit = {},

// In LazyColumn, add BEFORE the Matches card:
item {
    ManageCard(
        title = "Grounds",
        subtitle = "View and manage sports grounds",
        icon = Icons.Outlined.LocationOn,
        enabled = true,
        onClick = onNavigateToGrounds
    )
}
```

`Icons.Outlined.LocationOn` is already imported.

---

## Task 7 — Wire Grounds in MainScreen.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/MainScreen.kt`

1. Add `groundViewModel: GroundViewModel` to `MainScreen` parameters
2. Add import: `import com.example.vmsadmin.viewmodel.GroundViewModel`
3. Pass `onNavigateToGrounds` to `ManageScreen`:
   ```kotlin
   onNavigateToGrounds = { navController.navigate("manage/grounds") }
   ```
4. Add route in NavHost:
   ```kotlin
   composable("manage/grounds") {
       GroundsScreen(
           viewModel = groundViewModel,
           onBack = { navController.popBackStack() }
       )
   }
   ```
5. Add import: `import com.example.vmsadmin.ui.screens.GroundsScreen`

---

## Task 8 — Wire GroundViewModel in AppNavigation.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/navigation/AppNavigation.kt`

1. Add `groundViewModel: GroundViewModel` to `AppNavigation` parameters
2. Pass `groundViewModel = groundViewModel` to `MainScreen`
3. Add import: `import com.example.vmsadmin.viewmodel.GroundViewModel`

---

## Task 9 — Wire GroundViewModel in MainActivity.kt

**File:** `Vmsadminapp/app/src/main/java/com/example/vmsadmin/MainActivity.kt`

1. Create `GroundRepository` instance (using `ApiClient.instance`)
2. Create `GroundViewModel` using `GroundViewModelFactory`
3. Pass `groundViewModel` to `AppNavigation`

Follow the exact same pattern as how `matchViewModel` / `MatchRepository` / `MatchViewModelFactory` are created.

---

## Execution Order

Tasks 1 → 2 → 3 → 4 → 5 (model/api/repo/viewmodel/screen — no dependencies between 3/4/5 after 1+2)
Tasks 6 → 7 → 8 → 9 (wiring — sequential, depends on screen existing from task 5)

Tasks 3, 4, and 5 can be done in parallel after Tasks 1 and 2.

---

## Acceptance Checklist
- [ ] `Ground` + `UpdateGroundRequest` in Models.kt
- [ ] `getGrounds()` + `updateGround()` in ApiService.kt
- [ ] `GroundRepository.kt` created
- [ ] `GroundViewModel.kt` created with optimistic toggle
- [ ] `GroundsScreen.kt` created: shimmer, error+retry, list, pull-refresh, switch toggle
- [ ] `ManageScreen` has "Grounds" card
- [ ] `MainScreen` accepts `groundViewModel` and has "manage/grounds" route
- [ ] `AppNavigation` passes `groundViewModel`
- [ ] `MainActivity` creates and passes `GroundViewModel`
- [ ] Build compiles without errors
- [ ] No regression in Carts, Matches, Payments screens

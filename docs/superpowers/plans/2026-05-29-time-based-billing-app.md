# Admin App — Time-Based Billing UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the admin app to the time-based billing backend — session start/end buttons, live elapsed timer on IN_PROGRESS bookings, AWAITING_TIME_PAYMENT state display, and time-rate + surge fields in the Pricing config form.

**Architecture:** Extend existing models, ApiService, repositories and ViewModels to consume the new `/start-session`, `/end-session`, and `/session-status` booking routes plus the updated `/fee-config` update endpoint. BookingsScreen gains a live timer composable for IN_PROGRESS cards and an info section for AWAITING_TIME_PAYMENT cards. FeeConfigScreen gains four new editable fields (matching_fee, rate_per_block, block_duration_minutes, max_duration_minutes) plus a surge toggle/multiplier in the edit dialog.

**Tech Stack:** Kotlin, Jetpack Compose, Retrofit/OkHttp, kotlinx.serialization, Java SimpleDateFormat (API-24-safe date parsing — no java.time).

---

## File Map

| File | Change |
|------|--------|
| `models/Models.kt` | Add session fields to `Booking`; `payment_type` to `Payment`; time-rate + surge fields to `FeeConfig`; extend `CreateFeeConfigRequest` + `UpdateFeeConfigRequest`; add `SessionStatus` |
| `network/ApiService.kt` | Add `startSession`, `endSession`, `getSessionStatus` |
| `data/BookingRepository.kt` | Add `startSession()`, `endSession()` |
| `data/FeeConfigRepository.kt` | Extend `updateFeeConfig()` + `createFeeConfig()` with new params |
| `viewmodel/BookingViewModel.kt` | Add `startSession()`, `endSession()` (replace old `startBooking`/`completeBooking` wiring) |
| `viewmodel/FeeConfigViewModel.kt` | Extend `addConfig()` + `updateConfig()` signatures |
| `ui/screens/BookingsScreen.kt` | Live timer on IN_PROGRESS cards; AWAITING_TIME_PAYMENT display; call `startSession`/`endSession` |
| `ui/screens/FeeConfigScreen.kt` | Add 6 new fields to Create/Edit dialog; show time-rate on card |

---

## Task 1: Update Models.kt

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt`

This task has no tests (pure data model, no logic). Build verifies correctness.

- [ ] **Step 1: Read the file first**

```bash
# Already read — see context above. File is at:
# Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt
```

- [ ] **Step 2: Add session fields to the `Booking` data class**

Replace the existing `Booking` data class (lines ~46–61):

```kotlin
@Serializable
data class Booking(
    val id: Int,
    val region_id: Int? = null,
    val cart_type_id: Int? = null,
    val timeslot_id: Int? = null,
    val status: String,
    val assigned_cart_id: Int? = null,
    val address: String? = null,
    val date: String? = null,
    val created_at: String? = null,
    // Display-friendly names
    val region_name: String? = null,
    val cart_type_name: String? = null,
    val timeslot_label: String? = null,
    val cart_label: String? = null,
    // Session / time-billing fields (null when session not started)
    val session_started_at: String? = null,
    val session_ended_at: String? = null,
    val session_minutes: Int? = null,
    val session_blocks: Int? = null,
    val time_bill_amount: Double? = null,
    val surge_multiplier_snapshot: Double? = null
)
```

- [ ] **Step 3: Add `payment_type` to the `Payment` data class**

Replace the existing `Payment` data class (lines ~64–74):

```kotlin
@Serializable
data class Payment(
    val id: Int,
    val booking_id: Int? = null,
    val provider: String? = null,
    val amount: Double? = null,
    val reference_code: String? = null,
    val transaction_id: String? = null,
    val status: String? = null,
    val payment_type: String? = null,   // "MATCHING_FEE" or "TIME_BILL"
    val created_at: String? = null,
    val updated_at: String? = null
)
```

- [ ] **Step 4: Add time-rate + surge fields to the `FeeConfig` data class**

Replace the existing `FeeConfig` data class (lines ~199–212):

```kotlin
@Serializable
data class FeeConfig(
    val id: Int,
    val region_id: Int,
    val cart_type_id: Int,
    val booking_fee: Double,
    val cancellation_fee_pct: Double,
    val platform_fee_pct: Double,
    val is_active: Boolean = true,
    val region_name: String? = null,
    val cart_type_name: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    // Time-based billing fields
    val matching_fee: Double = 0.0,
    val rate_per_block: Double = 0.0,
    val block_duration_minutes: Int = 45,
    val max_duration_minutes: Int = 180,
    val surge_enabled: Boolean = false,
    val surge_multiplier: Double = 1.0
)
```

- [ ] **Step 5: Extend `CreateFeeConfigRequest`**

Replace the existing `CreateFeeConfigRequest` (lines ~214–220):

```kotlin
@Serializable
data class CreateFeeConfigRequest(
    val region_id: Int,
    val cart_type_id: Int,
    val booking_fee: Double,
    val cancellation_fee_pct: Double,
    val platform_fee_pct: Double,
    val matching_fee: Double = 0.0,
    val rate_per_block: Double = 0.0,
    val block_duration_minutes: Int = 45,
    val max_duration_minutes: Int = 180
)
```

- [ ] **Step 6: Extend `UpdateFeeConfigRequest`**

Replace the existing `UpdateFeeConfigRequest` (lines ~222–229):

```kotlin
@Serializable
data class UpdateFeeConfigRequest(
    val booking_fee: Double? = null,
    val cancellation_fee_pct: Double? = null,
    val platform_fee_pct: Double? = null,
    val is_active: Boolean? = null,
    val matching_fee: Double? = null,
    val rate_per_block: Double? = null,
    val block_duration_minutes: Int? = null,
    val max_duration_minutes: Int? = null,
    val surge_enabled: Boolean? = null,
    val surge_multiplier: Double? = null
)
```

- [ ] **Step 7: Add `SessionStatus` model at end of file (before closing)**

```kotlin
@Serializable
data class SessionStatus(
    val booking_id: Int,
    val status: String,
    val running: Boolean,
    val elapsed_minutes: Int,
    val current_blocks: Int,
    val estimated_time_bill: Double
)
```

- [ ] **Step 8: Commit**

```bash
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/models/Models.kt
git commit -m "feat(app): extend models for time-billing — Booking session fields, Payment type, FeeConfig time-rate"
```

---

## Task 2: ApiService + BookingRepository + FeeConfigRepository

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/BookingRepository.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/FeeConfigRepository.kt`

No automated tests for Retrofit interfaces. Build + manual device test verifies.

- [ ] **Step 1: Read each file before editing**

Files already read in context (see above).

- [ ] **Step 2: Add session endpoints to ApiService.kt**

Add the following three methods to `ApiService` after the existing `cancelBooking` declaration (around line 69):

```kotlin
@POST("/api/v1/bookings/{booking_id}/start-session")
suspend fun startSession(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

@POST("/api/v1/bookings/{booking_id}/end-session")
suspend fun endSession(@Path("booking_id") bookingId: Int): ApiResponse<JsonElement>

@GET("/api/v1/bookings/{booking_id}/session-status")
suspend fun getSessionStatus(@Path("booking_id") bookingId: Int): ApiResponse<SessionStatus>
```

Also add the import at the top of the file:

```kotlin
import com.example.vmsadmin.models.SessionStatus
```

- [ ] **Step 3: Add `startSession` and `endSession` to BookingRepository.kt**

Append to `BookingRepository` class (after the existing `cancelBooking` method):

```kotlin
suspend fun startSession(bookingId: Int) {
    val response = apiService.startSession(bookingId)
    if (!response.success) {
        throw Exception(response.message ?: "Failed to start session")
    }
}

suspend fun endSession(bookingId: Int) {
    val response = apiService.endSession(bookingId)
    if (!response.success) {
        throw Exception(response.message ?: "Failed to end session")
    }
}
```

- [ ] **Step 4: Extend `FeeConfigRepository.updateFeeConfig()` with new params**

Replace the entire `updateFeeConfig` function in `FeeConfigRepository.kt`:

```kotlin
suspend fun updateFeeConfig(
    id: Int,
    bookingFee: Double? = null,
    cancellationFeePct: Double? = null,
    platformFeePct: Double? = null,
    isActive: Boolean? = null,
    matchingFee: Double? = null,
    ratePerBlock: Double? = null,
    blockDurationMinutes: Int? = null,
    maxDurationMinutes: Int? = null,
    surgeEnabled: Boolean? = null,
    surgeMultiplier: Double? = null
): FeeConfig {
    try {
        val response = apiService.updateFeeConfig(
            id = id,
            request = UpdateFeeConfigRequest(
                booking_fee = bookingFee,
                cancellation_fee_pct = cancellationFeePct,
                platform_fee_pct = platformFeePct,
                is_active = isActive,
                matching_fee = matchingFee,
                rate_per_block = ratePerBlock,
                block_duration_minutes = blockDurationMinutes,
                max_duration_minutes = maxDurationMinutes,
                surge_enabled = surgeEnabled,
                surge_multiplier = surgeMultiplier
            )
        )
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to update fee config")
    } catch (e: HttpException) {
        throw Exception(parseErrorDetail(e) ?: "Failed to update fee config")
    }
}
```

- [ ] **Step 5: Extend `FeeConfigRepository.createFeeConfig()` with new params**

Replace the entire `createFeeConfig` function:

```kotlin
suspend fun createFeeConfig(
    regionId: Int,
    cartTypeId: Int,
    bookingFee: Double,
    cancellationFeePct: Double,
    platformFeePct: Double,
    matchingFee: Double = 0.0,
    ratePerBlock: Double = 0.0,
    blockDurationMinutes: Int = 45,
    maxDurationMinutes: Int = 180
): FeeConfig {
    try {
        val response = apiService.createFeeConfig(
            CreateFeeConfigRequest(
                region_id = regionId,
                cart_type_id = cartTypeId,
                booking_fee = bookingFee,
                cancellation_fee_pct = cancellationFeePct,
                platform_fee_pct = platformFeePct,
                matching_fee = matchingFee,
                rate_per_block = ratePerBlock,
                block_duration_minutes = blockDurationMinutes,
                max_duration_minutes = maxDurationMinutes
            )
        )
        if (response.success && response.data != null) {
            return response.data
        }
        throw Exception(response.message ?: "Failed to create fee config")
    } catch (e: HttpException) {
        throw Exception(parseErrorDetail(e) ?: "Failed to create fee config")
    }
}
```

- [ ] **Step 6: Build to verify no compile errors**

```
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd Vmsadminapp
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/network/ApiService.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/BookingRepository.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/data/FeeConfigRepository.kt
git commit -m "feat(app): add startSession/endSession endpoints + extend FeeConfig repo with time-rate params"
```

---

## Task 3: BookingViewModel — session actions

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/BookingViewModel.kt`

- [ ] **Step 1: Read the file before editing**

Already read in context. The file is ~123 lines. It has `startBooking()` and `completeBooking()` methods that call the OLD `/start` and `/complete` endpoints.

- [ ] **Step 2: Add `startSession()` and `endSession()` methods**

Append before the closing brace of the `BookingViewModel` class (after `cancelBooking`):

```kotlin
fun startSession(bookingId: Int) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            bookingRepository.startSession(bookingId)
            loadBookings()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to start session"
            )
        }
    }
}

fun endSession(bookingId: Int) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            bookingRepository.endSession(bookingId)
            loadBookings()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to end session"
            )
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/BookingViewModel.kt
git commit -m "feat(app): add startSession + endSession to BookingViewModel"
```

---

## Task 4: BookingsScreen — live timer + AWAITING_TIME_PAYMENT

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/BookingsScreen.kt`

- [ ] **Step 1: Read the file before editing**

Already read. Key section is `BookingCard` (lines ~157–244). The `IN_PROGRESS` branch currently shows only an "End Session" button.

- [ ] **Step 2: Wire the dialogs to the new ViewModel methods**

In `BookingsScreen` (the parent composable), update the `onStart` and `onComplete` callbacks in the `BookingCard` call site:

Change:
```kotlin
onStart = {
    dialogTitle = "Start Session?"
    dialogMessage = "Mark booking #${booking.id} as in progress? The player has arrived at the ground."
    dialogAction = { viewModel.startBooking(booking.id) }
    showDialog = true
},
onComplete = {
    dialogTitle = "End Session?"
    dialogMessage = "Mark booking #${booking.id} as completed? The session has finished."
    dialogAction = { viewModel.completeBooking(booking.id) }
    showDialog = true
},
```

To:
```kotlin
onStart = {
    dialogTitle = "Start Session?"
    dialogMessage = "Mark booking #${booking.id} as in progress? The player has arrived at the ground."
    dialogAction = { viewModel.startSession(booking.id) }
    showDialog = true
},
onComplete = {
    dialogTitle = "End Session?"
    dialogMessage = "Mark booking #${booking.id} as completed? The session has finished."
    dialogAction = { viewModel.endSession(booking.id) }
    showDialog = true
},
```

- [ ] **Step 3: Add `elapsedSeconds` helper function at bottom of file**

Add this private top-level function after the `InfoRow` composable (after line ~266):

```kotlin
private fun elapsedSeconds(sessionStartedAt: String): Long {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val cleaned = sessionStartedAt.replace("T", " ").take(19)
        val startDate = sdf.parse(cleaned) ?: return 0L
        ((System.currentTimeMillis() - startDate.time) / 1000L).coerceAtLeast(0L)
    } catch (_: Exception) {
        0L
    }
}

@Composable
private fun LiveSessionTimer(sessionStartedAt: String) {
    var elapsed by remember { mutableStateOf(elapsedSeconds(sessionStartedAt)) }

    LaunchedEffect(sessionStartedAt) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            elapsed = elapsedSeconds(sessionStartedAt)
        }
    }

    val hours = elapsed / 3600
    val minutes = (elapsed % 3600) / 60
    val seconds = elapsed % 60
    val timeText = if (hours > 0)
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%02d:%02d".format(minutes, seconds)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Session Timer",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

- [ ] **Step 4: Update `BookingCard` to handle new states**

Replace the entire `BookingCard` composable (the private fun starting around line 157):

```kotlin
@Composable
private fun BookingCard(
    booking: Booking,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    AppCard {
        // Status badge + ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(status = booking.status)
            Text(
                text = "Booking #${booking.id}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoRow(label = "Region",   value = booking.region_name    ?: "Region ${booking.region_id ?: "-"}")
        InfoRow(label = "Sport",    value = booking.cart_type_name ?: "Sport ${booking.cart_type_id ?: "-"}")
        InfoRow(label = "Date",     value = booking.date           ?: "-")
        InfoRow(label = "Time",     value = booking.timeslot_label ?: "Slot ${booking.timeslot_id ?: "-"}")
        booking.cart_label?.let { InfoRow(label = "Ground", value = it) }

        val status = booking.status.uppercase()

        // Live timer for in-progress sessions
        if (status == "IN_PROGRESS" && booking.session_started_at != null) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            LiveSessionTimer(sessionStartedAt = booking.session_started_at)
        }

        // Time bill summary for awaiting payment
        if (status == "AWAITING_TIME_PAYMENT") {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            booking.session_minutes?.let { InfoRow(label = "Session Duration", value = "$it min") }
            booking.session_blocks?.let { InfoRow(label = "Blocks Used", value = it.toString()) }
            booking.time_bill_amount?.let {
                InfoRow(label = "Time Bill", value = "₹%.2f".format(it))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⏳ Awaiting time bill payment approval in Payments tab",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // Action buttons based on status
        if (status in listOf("PENDING_PAYMENT", "CONFIRMED", "IN_PROGRESS")) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (status) {
                    "PENDING_PAYMENT" -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Cancel") }
                    }
                    "CONFIRMED" -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Cancel") }
                        Button(
                            onClick = onStart,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Start Session") }
                    }
                    "IN_PROGRESS" -> {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("End Session") }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: Add missing import for `mutableStateOf` (if not already present)**

The file already imports `androidx.compose.runtime.*`. Check the import block — if `kotlinx.coroutines.delay` needs an explicit import, add it. Since we used `kotlinx.coroutines.delay` fully-qualified in `LiveSessionTimer`, no extra import is needed.

- [ ] **Step 6: Build to verify**

```
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/BookingsScreen.kt
git commit -m "feat(app): BookingsScreen — live session timer + AWAITING_TIME_PAYMENT state display"
```

---

## Task 5: FeeConfigScreen — time-rate + surge fields

**Files:**
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/FeeConfigViewModel.kt`
- Modify: `Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/FeeConfigScreen.kt`

- [ ] **Step 1: Read both files before editing**

Both already read in context.

- [ ] **Step 2: Extend `FeeConfigViewModel.addConfig()` with new params**

Replace the `addConfig` function in `FeeConfigViewModel.kt`:

```kotlin
fun addConfig(
    regionId: Int,
    cartTypeId: Int,
    bookingFee: Double,
    cancellationFeePct: Double,
    platformFeePct: Double,
    matchingFee: Double = 0.0,
    ratePerBlock: Double = 0.0,
    blockDurationMinutes: Int = 45,
    maxDurationMinutes: Int = 180
) {
    if (_uiState.value.isSubmitting) return
    viewModelScope.launch {
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        try {
            repository.createFeeConfig(
                regionId, cartTypeId, bookingFee, cancellationFeePct, platformFeePct,
                matchingFee, ratePerBlock, blockDurationMinutes, maxDurationMinutes
            )
            delay(200)
            loadConfigs()
            _uiState.update { it.copy(successMessage = "Fee config saved", showAddDialog = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to add fee config") }
        } finally {
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }
}
```

- [ ] **Step 3: Extend `FeeConfigViewModel.updateConfig()` with new params**

Replace the `updateConfig` function:

```kotlin
fun updateConfig(
    id: Int,
    bookingFee: Double,
    cancellationFeePct: Double,
    platformFeePct: Double,
    matchingFee: Double,
    ratePerBlock: Double,
    blockDurationMinutes: Int,
    maxDurationMinutes: Int,
    surgeEnabled: Boolean,
    surgeMultiplier: Double
) {
    if (_uiState.value.isSubmitting) return
    viewModelScope.launch {
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        try {
            repository.updateFeeConfig(
                id = id,
                bookingFee = bookingFee,
                cancellationFeePct = cancellationFeePct,
                platformFeePct = platformFeePct,
                matchingFee = matchingFee,
                ratePerBlock = ratePerBlock,
                blockDurationMinutes = blockDurationMinutes,
                maxDurationMinutes = maxDurationMinutes,
                surgeEnabled = surgeEnabled,
                surgeMultiplier = surgeMultiplier
            )
            delay(200)
            loadConfigs()
            _uiState.update {
                it.copy(
                    successMessage = "Configuration updated",
                    showEditDialog = false,
                    editingConfig = null
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to update fee config") }
        } finally {
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }
}
```

- [ ] **Step 4: Update `FeeConfigCard` to show time-rate info**

In `FeeConfigScreen.kt`, inside the `FeeConfigCard` composable, after the line:
```kotlin
FeeInfoRow(label = "Platform Fee", value = "%.1f%%".format(config.platform_fee_pct), alpha = contentAlpha)
```

Add:
```kotlin
if (config.matching_fee > 0 || config.rate_per_block > 0) {
    Spacer(modifier = Modifier.height(8.dp))
    FeeInfoRow(label = "Matching Fee", value = "₹%.2f".format(config.matching_fee), alpha = contentAlpha)
    FeeInfoRow(
        label = "Rate / Block",
        value = "₹%.2f / %dmin".format(config.rate_per_block, config.block_duration_minutes),
        alpha = contentAlpha
    )
    if (config.surge_enabled) {
        FeeInfoRow(
            label = "Surge",
            value = "%.1fx (active)".format(config.surge_multiplier),
            alpha = contentAlpha
        )
    }
}
```

- [ ] **Step 5: Update `FeeConfigFormDialog` signature to include time-rate + surge params**

Find the `FeeConfigFormDialog` composable signature (around line 488). It currently ends with:
```kotlin
isSubmitting: Boolean,
onConfirm: (regionId: Int, cartTypeId: Int, bookingFee: Double, cancellationPct: Double, platformPct: Double) -> Unit,
onDismiss: () -> Unit
```

Change it to:
```kotlin
isSubmitting: Boolean,
initialMatchingFee: String = "0.0",
initialRatePerBlock: String = "0.0",
initialBlockDuration: String = "45",
initialMaxDuration: String = "180",
initialSurgeEnabled: Boolean = false,
initialSurgeMultiplier: String = "1.0",
onConfirm: (regionId: Int, cartTypeId: Int, bookingFee: Double, cancellationPct: Double, platformPct: Double,
            matchingFee: Double, ratePerBlock: Double, blockDurationMinutes: Int, maxDurationMinutes: Int,
            surgeEnabled: Boolean, surgeMultiplier: Double) -> Unit,
onDismiss: () -> Unit
```

- [ ] **Step 6: Add state variables for new fields inside `FeeConfigFormDialog`**

Inside the `FeeConfigFormDialog` body, after the existing state declarations (after `var platformPct by remember ...`), add:

```kotlin
var matchingFee by remember { mutableStateOf(initialMatchingFee) }
var ratePerBlock by remember { mutableStateOf(initialRatePerBlock) }
var blockDuration by remember { mutableStateOf(initialBlockDuration) }
var maxDuration by remember { mutableStateOf(initialMaxDuration) }
var surgeEnabled by remember { mutableStateOf(initialSurgeEnabled) }
var surgeMultiplier by remember { mutableStateOf(initialSurgeMultiplier) }

var matchingFeeError by remember { mutableStateOf<String?>(null) }
var ratePerBlockError by remember { mutableStateOf<String?>(null) }
var blockDurationError by remember { mutableStateOf<String?>(null) }
var maxDurationError by remember { mutableStateOf<String?>(null) }
var surgeMultiplierError by remember { mutableStateOf<String?>(null) }
```

- [ ] **Step 7: Update `submit()` lambda inside `FeeConfigFormDialog` to validate and pass new fields**

Find the `fun submit()` lambda inside `FeeConfigFormDialog`. After the existing validation block (before `if (valid) {`), add:

```kotlin
val mFee = matchingFee.toDoubleOrNull()
if (mFee == null || mFee < 0) {
    matchingFeeError = "Enter a valid matching fee (>= 0)"
    valid = false
}
val rPB = ratePerBlock.toDoubleOrNull()
if (rPB == null || rPB < 0) {
    ratePerBlockError = "Enter a valid rate (>= 0)"
    valid = false
}
val bDur = blockDuration.toIntOrNull()
if (bDur == null || bDur <= 0) {
    blockDurationError = "Enter a positive number of minutes"
    valid = false
}
val mDur = maxDuration.toIntOrNull()
if (mDur == null || mDur <= 0) {
    maxDurationError = "Enter a positive number of minutes"
    valid = false
}
val sMult = surgeMultiplier.toDoubleOrNull()
if (surgeEnabled && (sMult == null || sMult < 1.0 || sMult > 3.0)) {
    surgeMultiplierError = "Multiplier must be between 1.0 and 3.0"
    valid = false
}
```

Change the `onConfirm(...)` call in `if (valid)` to:
```kotlin
if (valid) {
    keyboardController?.hide()
    onConfirm(
        selectedRegionId!!, selectedCartTypeId!!,
        fee!!, cancel!!, platform!!,
        mFee!!, rPB!!, bDur!!, mDur!!,
        surgeEnabled, sMult ?: 1.0
    )
}
```

- [ ] **Step 8: Add new form fields to the dialog's Column content**

Inside the `AlertDialog`'s `text = { Column(...) }` block, after the `platformPct` `OutlinedTextField` (which has `imeAction = ImeAction.Done`), add the time-rate section:

```kotlin
Spacer(modifier = Modifier.height(4.dp))
HorizontalDivider()
Spacer(modifier = Modifier.height(4.dp))
Text(
    "Time-Based Billing",
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary
)

OutlinedTextField(
    value = matchingFee,
    onValueChange = { matchingFee = it; matchingFeeError = null },
    label = { Text("Matching Fee (₹)") },
    singleLine = true,
    isError = matchingFeeError != null,
    supportingText = matchingFeeError?.let { err -> { Text(err) } },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
)

OutlinedTextField(
    value = ratePerBlock,
    onValueChange = { ratePerBlock = it; ratePerBlockError = null },
    label = { Text("Rate per Block (₹)") },
    singleLine = true,
    isError = ratePerBlockError != null,
    supportingText = ratePerBlockError?.let { err -> { Text(err) } },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
)

OutlinedTextField(
    value = blockDuration,
    onValueChange = { blockDuration = it; blockDurationError = null },
    label = { Text("Block Duration (minutes)") },
    singleLine = true,
    isError = blockDurationError != null,
    supportingText = blockDurationError?.let { err -> { Text(err) } },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
)

OutlinedTextField(
    value = maxDuration,
    onValueChange = { maxDuration = it; maxDurationError = null },
    label = { Text("Max Duration (minutes)") },
    singleLine = true,
    isError = maxDurationError != null,
    supportingText = maxDurationError?.let { err -> { Text(err) } },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
)

// Surge pricing — only in edit mode (not meaningful at creation time)
if (isEditMode) {
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Surge Pricing",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Enable Surge", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = surgeEnabled, onCheckedChange = { surgeEnabled = it })
    }
    if (surgeEnabled) {
        OutlinedTextField(
            value = surgeMultiplier,
            onValueChange = { surgeMultiplier = it; surgeMultiplierError = null },
            label = { Text("Surge Multiplier (1.0 – 3.0)") },
            singleLine = true,
            isError = surgeMultiplierError != null,
            supportingText = surgeMultiplierError?.let { err -> { Text(err) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
```

Add `Switch` import at the top of the file (with other material3 imports):
```kotlin
import androidx.compose.material3.Switch
```

- [ ] **Step 9: Update call sites of `FeeConfigFormDialog` in `FeeConfigScreen`**

There are two call sites: one in the Add dialog section and one in the Edit dialog section. Both call `FeeConfigFormDialog(...)`.

**Add dialog call site** — find the existing call and add the new params before `onConfirm`:
```kotlin
FeeConfigFormDialog(
    title = "Add Pricing Config",
    regions = regionState.regions,
    cartTypes = cartTypeState.cartTypes,
    existingConfigs = uiState.feeConfigs,
    isEditMode = false,
    initialRegionId = null,
    initialCartTypeId = null,
    initialBookingFee = "",
    initialCancellationPct = "",
    initialPlatformPct = "",
    isSubmitting = uiState.isSubmitting,
    // new params use defaults: initialMatchingFee = "0.0", etc.
    onConfirm = { regionId, cartTypeId, bookingFee, cancellationPct, platformPct,
                  matchingFee, ratePerBlock, blockDurationMinutes, maxDurationMinutes,
                  _, _ ->  // surge ignored on create
        viewModel.addConfig(
            regionId, cartTypeId, bookingFee, cancellationPct, platformPct,
            matchingFee, ratePerBlock, blockDurationMinutes, maxDurationMinutes
        )
    },
    onDismiss = { viewModel.dismissAddDialog() }
)
```

**Edit dialog call site** — find the existing call and update:
```kotlin
uiState.editingConfig?.let { config ->
    FeeConfigFormDialog(
        title = "Edit Pricing Config",
        regions = regionState.regions,
        cartTypes = cartTypeState.cartTypes,
        existingConfigs = uiState.feeConfigs,
        isEditMode = true,
        initialRegionId = config.region_id,
        initialCartTypeId = config.cart_type_id,
        initialBookingFee = config.booking_fee.toString(),
        initialCancellationPct = config.cancellation_fee_pct.toString(),
        initialPlatformPct = config.platform_fee_pct.toString(),
        isSubmitting = uiState.isSubmitting,
        initialMatchingFee = config.matching_fee.toString(),
        initialRatePerBlock = config.rate_per_block.toString(),
        initialBlockDuration = config.block_duration_minutes.toString(),
        initialMaxDuration = config.max_duration_minutes.toString(),
        initialSurgeEnabled = config.surge_enabled,
        initialSurgeMultiplier = config.surge_multiplier.toString(),
        onConfirm = { _, _, bookingFee, cancellationPct, platformPct,
                      matchingFee, ratePerBlock, blockDurationMinutes, maxDurationMinutes,
                      surgeEnabled, surgeMultiplier ->
            viewModel.updateConfig(
                id = config.id,
                bookingFee = bookingFee,
                cancellationFeePct = cancellationPct,
                platformFeePct = platformPct,
                matchingFee = matchingFee,
                ratePerBlock = ratePerBlock,
                blockDurationMinutes = blockDurationMinutes,
                maxDurationMinutes = maxDurationMinutes,
                surgeEnabled = surgeEnabled,
                surgeMultiplier = surgeMultiplier
            )
        },
        onDismiss = { viewModel.dismissEditDialog() }
    )
}
```

- [ ] **Step 10: Build to verify**

```
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` with 0 errors.

- [ ] **Step 11: Commit**

```bash
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/viewmodel/FeeConfigViewModel.kt
git add Vmsadminapp/app/src/main/java/com/example/vmsadmin/ui/screens/FeeConfigScreen.kt
git commit -m "feat(app): FeeConfigScreen — add time-rate + surge fields to pricing form"
```

---

## Task 6: Final Build + DEV_LOG

**Files:**
- Modify: `backend/DEV_LOG.md`

- [ ] **Step 1: Full clean build**

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd Vmsadminapp
.\gradlew clean assembleDebug
```

Expected: `BUILD SUCCESSFUL`. Note the APK path:
`Vmsadminapp/app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2: Append to backend/DEV_LOG.md**

Append the following entry (do NOT overwrite any existing content):

```markdown
---

## 2026-05-29 — Phase 02: Admin App — Time-Based Billing UI

### Summary
Wired the admin app to the time-based billing backend. Session start/end now uses the new `/start-session` and `/end-session` endpoints (metered billing flow). `IN_PROGRESS` booking cards show a live elapsed-time timer. `AWAITING_TIME_PAYMENT` cards display session duration, block count, and the time bill amount with a note to approve in Payments. Pricing (FeeConfig) edit dialog gains matching_fee, rate_per_block, block_duration_minutes, max_duration_minutes, and surge toggle/multiplier fields.

### Admin App — Modified Files

| File | Change |
|------|--------|
| `models/Models.kt` | Added session fields to `Booking` (session_started_at, session_ended_at, session_minutes, session_blocks, time_bill_amount, surge_multiplier_snapshot); `payment_type` to `Payment`; time-rate + surge fields to `FeeConfig`; extended `CreateFeeConfigRequest` + `UpdateFeeConfigRequest`; added `SessionStatus` |
| `network/ApiService.kt` | Added `startSession`, `endSession`, `getSessionStatus` endpoints |
| `data/BookingRepository.kt` | Added `startSession()`, `endSession()` |
| `data/FeeConfigRepository.kt` | Extended `createFeeConfig()` + `updateFeeConfig()` with time-rate + surge params |
| `viewmodel/BookingViewModel.kt` | Added `startSession()`, `endSession()` methods |
| `viewmodel/FeeConfigViewModel.kt` | Extended `addConfig()` + `updateConfig()` signatures for all time-rate + surge params |
| `ui/screens/BookingsScreen.kt` | Live `LiveSessionTimer` composable on IN_PROGRESS cards; AWAITING_TIME_PAYMENT info section; calls `startSession`/`endSession` ViewModel methods |
| `ui/screens/FeeConfigScreen.kt` | `FeeConfigFormDialog` extended with 6 new fields + surge toggle; card displays time-rate info; both Add + Edit call sites updated |

### Architectural Decisions
- **API-24-safe timer**: Uses `java.text.SimpleDateFormat` (not `java.time`) since `minSdk = 24` and `coreLibraryDesugaring` is not configured; SimpleDateFormat is safe to all API levels.
- **Surge only in Edit mode**: Surge is an operational control (flip live), not a config-time setting; Add dialog does not show surge fields.
- **AWAITING_TIME_PAYMENT is display-only**: No action button from BookingsScreen — the TIME_BILL payment appears in Payments tab where Finance/Admin approves it.
- **Old `/start` + `/complete` endpoints kept in ApiService**: Backward compatibility; new session endpoints are used for all metered bookings.
```

- [ ] **Step 3: Commit DEV_LOG**

```bash
git add backend/DEV_LOG.md
git commit -m "docs: DEV_LOG entry for admin app time-based billing UI"
```

---

## Self-Review

**Spec coverage:**
- ✅ Models updated: Booking session fields, Payment.payment_type, FeeConfig time-rate + surge
- ✅ ApiService: startSession, endSession, getSessionStatus added
- ✅ BookingRepository: startSession, endSession added
- ✅ FeeConfigRepository: extended with all new params
- ✅ BookingViewModel: startSession, endSession wired
- ✅ FeeConfigViewModel: updateConfig + addConfig extended
- ✅ BookingsScreen: live timer on IN_PROGRESS, AWAITING_TIME_PAYMENT info
- ✅ FeeConfigScreen: all 6 new fields + surge toggle in dialog; card shows time-rate info
- ✅ Build task with DEV_LOG

**Placeholder scan:** No TBDs, TODOs, or vague instructions. Every step has actual code.

**Type consistency:**
- `FeeConfigFormDialog.onConfirm` lambda extended consistently across Task 5 Steps 5, 7, 8, 9
- `updateConfig()` param names match between ViewModel (Task 5 Step 3) and Repository (Task 2 Step 4)
- `SessionStatus` defined in Task 1, imported in Task 2 Step 2

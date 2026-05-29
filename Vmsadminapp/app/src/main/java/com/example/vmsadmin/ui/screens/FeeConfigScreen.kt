package com.example.vmsadmin.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.CartType
import com.example.vmsadmin.models.FeeConfig
import com.example.vmsadmin.models.Region
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.FeeConfigViewModel
import com.example.vmsadmin.viewmodel.RegionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeConfigScreen(
    viewModel: FeeConfigViewModel,
    regionViewModel: RegionViewModel,
    cartTypeViewModel: CartTypeViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val regionState by regionViewModel.uiState.collectAsState()
    val cartTypeState by cartTypeViewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadConfigs()
        if (regionState.regions.isEmpty()) regionViewModel.loadRegions()
        if (cartTypeState.cartTypes.isEmpty()) cartTypeViewModel.loadCartTypes()
        visible = true
    }

    if (uiState.showAddDialog) {
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
            // new optional params use defaults (0.0, 0.0, "45", "180", false, "1.0")
            onConfirm = { regionId, cartTypeId, bookingFee, cancellationPct, platformPct,
                          matchingFee, ratePerBlock, blockDurationMinutes, maxDurationMinutes,
                          _, _ ->
                viewModel.addConfig(
                    regionId, cartTypeId, bookingFee, cancellationPct, platformPct,
                    matchingFee, ratePerBlock, blockDurationMinutes, maxDurationMinutes
                )
            },
            onDismiss = { viewModel.dismissAddDialog() }
        )
    }

    if (uiState.showEditDialog && uiState.editingConfig != null) {
        val config = uiState.editingConfig!!
        FeeConfigFormDialog(
            title = "Edit Pricing Config",
            regions = regionState.regions,
            cartTypes = cartTypeState.cartTypes,
            existingConfigs = uiState.feeConfigs,
            isEditMode = true,
            initialRegionId = config.region_id,
            initialCartTypeId = config.cart_type_id,
            initialBookingFee = config.booking_fee.toBigDecimal().stripTrailingZeros().toPlainString(),
            initialCancellationPct = config.cancellation_fee_pct.toBigDecimal().stripTrailingZeros().toPlainString(),
            initialPlatformPct = config.platform_fee_pct.toBigDecimal().stripTrailingZeros().toPlainString(),
            isSubmitting = uiState.isSubmitting,
            initialMatchingFee = config.matching_fee.toBigDecimal().stripTrailingZeros().toPlainString(),
            initialRatePerBlock = config.rate_per_block.toBigDecimal().stripTrailingZeros().toPlainString(),
            initialBlockDuration = config.block_duration_minutes.toString(),
            initialMaxDuration = config.max_duration_minutes.toString(),
            initialSurgeEnabled = config.surge_enabled,
            initialSurgeMultiplier = config.surge_multiplier.toBigDecimal().stripTrailingZeros().toPlainString(),
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

    if (uiState.showDeleteConfirm && uiState.deletingConfig != null) {
        val config = uiState.deletingConfig!!
        val regionName = config.region_name
            ?: regionState.regions.firstOrNull { it.id == config.region_id }?.name
            ?: "Region ${config.region_id}"
        val cartTypeName = config.cart_type_name
            ?: cartTypeState.cartTypes.firstOrNull { it.id == config.cart_type_id }?.name
            ?: "Type ${config.cart_type_id}"
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = {
                Text("Deactivate Fee Config", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to deactivate the fee configuration for \"$regionName - $cartTypeName\"? You can reactivate it later.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteConfig(config.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissDeleteConfirm() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        "Fee Configuration",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Fee Config")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.feeConfigs.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) {
                            AppCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .height(22.dp)
                                            .shimmerEffect()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(28.dp)
                                            .shimmerEffect()
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(14.dp)
                                        .shimmerEffect()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(14.dp)
                                        .shimmerEffect()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(14.dp)
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }

                uiState.error != null && uiState.feeConfigs.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "Something went wrong",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadConfigs() }) {
                            Text("Retry")
                        }
                    }
                }

                uiState.feeConfigs.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No fee configurations",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add your first fee config",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshConfigs() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                uiState.feeConfigs,
                                key = { _, config -> config.id }
                            ) { index, config ->
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(animationSpec = tween(400, delayMillis = index * 50)) +
                                        slideInVertically(
                                            initialOffsetY = { 50 },
                                            animationSpec = tween(400, delayMillis = index * 50)
                                        )
                                ) {
                                    val resolvedRegionName = config.region_name
                                        ?: regionState.regions.firstOrNull { it.id == config.region_id }?.name
                                    val resolvedCartTypeName = config.cart_type_name
                                        ?: cartTypeState.cartTypes.firstOrNull { it.id == config.cart_type_id }?.name
                                    FeeConfigCard(
                                        config = config,
                                        regionDisplayName = resolvedRegionName ?: "Region ${config.region_id}",
                                        cartTypeDisplayName = resolvedCartTypeName ?: "Type ${config.cart_type_id}",
                                        onEdit = { viewModel.showEditDialog(config) },
                                        onDelete = { viewModel.showDeleteConfirm(config) },
                                        onReactivate = { viewModel.reactivateConfig(config.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeConfigCard(
    config: FeeConfig,
    regionDisplayName: String,
    cartTypeDisplayName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReactivate: () -> Unit
) {
    val isActive = config.is_active
    val contentAlpha = if (isActive) 1f else 0.5f

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = regionDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cartTypeDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
            StatusBadge(status = if (isActive) "CONFIRMED" else "CANCELLED")
        }

        Spacer(modifier = Modifier.height(16.dp))
        FeeInfoRow(label = "Booking Fee", value = "₹%.2f".format(config.booking_fee), alpha = contentAlpha)
        FeeInfoRow(label = "Cancellation Fee", value = "%.1f%%".format(config.cancellation_fee_pct), alpha = contentAlpha)
        FeeInfoRow(label = "Platform Fee", value = "%.1f%%".format(config.platform_fee_pct), alpha = contentAlpha)
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

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Deactivate",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deactivate", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                OutlinedButton(
                    onClick = onReactivate,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Reactivate",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reactivate", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun FeeInfoRow(label: String, value: String, alpha: Float = 1f) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeeConfigFormDialog(
    title: String,
    regions: List<Region>,
    cartTypes: List<CartType>,
    existingConfigs: List<FeeConfig>,
    isEditMode: Boolean,
    initialRegionId: Int?,
    initialCartTypeId: Int?,
    initialBookingFee: String,
    initialCancellationPct: String,
    initialPlatformPct: String,
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
) {
    var selectedRegionId by remember { mutableStateOf(initialRegionId) }
    var selectedCartTypeId by remember { mutableStateOf(initialCartTypeId) }
    var bookingFee by remember { mutableStateOf(initialBookingFee) }
    var cancellationPct by remember { mutableStateOf(initialCancellationPct) }
    var platformPct by remember { mutableStateOf(initialPlatformPct) }

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

    var regionError by remember { mutableStateOf(false) }
    var cartTypeError by remember { mutableStateOf(false) }
    var bookingFeeError by remember { mutableStateOf<String?>(null) }
    var cancellationPctError by remember { mutableStateOf<String?>(null) }
    var platformPctError by remember { mutableStateOf<String?>(null) }
    var sumError by remember { mutableStateOf<String?>(null) }
    var duplicateError by remember { mutableStateOf<String?>(null) }

    var regionExpanded by remember { mutableStateOf(false) }
    var cartTypeExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedRegionName = regions.firstOrNull { it.id == selectedRegionId }?.name
    val selectedCartTypeName = cartTypes.firstOrNull { it.id == selectedCartTypeId }?.name

    // In add mode, filter cart types that already have a config for the selected region
    val availableCartTypes = if (!isEditMode && selectedRegionId != null) {
        val usedCartTypeIds = existingConfigs
            .filter { it.region_id == selectedRegionId }
            .map { it.cart_type_id }
            .toSet()
        cartTypes.filter { it.id !in usedCartTypeIds }
    } else {
        cartTypes
    }

    fun submit() {
        var valid = true

        if (!isEditMode) {
            if (selectedRegionId == null) {
                regionError = true; valid = false
            }
            if (selectedCartTypeId == null) {
                cartTypeError = true; valid = false
            }
            if (valid && existingConfigs.any {
                    it.region_id == selectedRegionId && it.cart_type_id == selectedCartTypeId
                }) {
                duplicateError = "Configuration already exists for this region and cart type"
                valid = false
            }
        }

        val fee = bookingFee.toDoubleOrNull()
        if (fee == null || fee < 0) {
            bookingFeeError = "Enter a valid fee (>= 0)"
            valid = false
        }

        val cancel = cancellationPct.toDoubleOrNull()
        if (cancel == null || cancel < 0 || cancel > 100) {
            cancellationPctError = "Enter a value between 0 and 100"
            valid = false
        }

        val platform = platformPct.toDoubleOrNull()
        if (platform == null || platform < 0 || platform > 100) {
            platformPctError = "Enter a value between 0 and 100"
            valid = false
        }

        if (valid && cancel != null && platform != null && (cancel + platform) > 100) {
            sumError = "Cancellation + Platform fees cannot exceed 100%"
            valid = false
        }

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
        if (rPB != null && rPB > 0 && (bDur == null || bDur <= 0)) {
            blockDurationError = "Required when rate per block is set"
            valid = false
        }
        val mDur = maxDuration.toIntOrNull()
        if (rPB != null && rPB > 0 && (mDur == null || mDur <= 0)) {
            maxDurationError = "Required when rate per block is set"
            valid = false
        }
        val sMult = surgeMultiplier.toDoubleOrNull()
        if (surgeEnabled && (sMult == null || sMult < 1.0 || sMult > 3.0)) {
            surgeMultiplierError = "Multiplier must be between 1.0 and 3.0"
            valid = false
        }

        if (valid) {
            keyboardController?.hide()
            onConfirm(
                selectedRegionId!!, selectedCartTypeId!!,
                fee!!, cancel!!, platform!!,
                mFee!!, rPB!!,
                bDur ?: 45, mDur ?: 180,
                surgeEnabled, sMult ?: 1.0
            )
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isEditMode) {
                    // Region dropdown
                    Box {
                        OutlinedButton(
                            onClick = { regionExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = selectedRegionName ?: "Select Region",
                                modifier = Modifier.weight(1f),
                                color = if (regionError)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select region")
                        }
                        DropdownMenu(
                            expanded = regionExpanded,
                            onDismissRequest = { regionExpanded = false },
                            modifier = Modifier.heightIn(max = 220.dp)
                        ) {
                            regions.forEach { region ->
                                DropdownMenuItem(
                                    text = { Text(region.name) },
                                    onClick = {
                                        selectedRegionId = region.id
                                        regionError = false
                                        duplicateError = null
                                        // Reset cart type when region changes
                                        selectedCartTypeId = null
                                        regionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (regionError) {
                        Text(
                            text = "Please select a region",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Cart Type dropdown
                    Box {
                        OutlinedButton(
                            onClick = { cartTypeExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = selectedCartTypeName ?: "Select Sport",
                                modifier = Modifier.weight(1f),
                                color = if (cartTypeError)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select cart type")
                        }
                        DropdownMenu(
                            expanded = cartTypeExpanded,
                            onDismissRequest = { cartTypeExpanded = false },
                            modifier = Modifier.heightIn(max = 220.dp)
                        ) {
                            if (availableCartTypes.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "All cart types configured",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    onClick = { cartTypeExpanded = false },
                                    enabled = false
                                )
                            } else {
                                availableCartTypes.forEach { cartType ->
                                    DropdownMenuItem(
                                        text = { Text(cartType.name) },
                                        onClick = {
                                            selectedCartTypeId = cartType.id
                                            cartTypeError = false
                                            duplicateError = null
                                            cartTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (cartTypeError) {
                        Text(
                            text = "Please select a cart type",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (duplicateError != null) {
                        Text(
                            text = duplicateError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Show read-only region/cart-type in edit mode
                    Text(
                        text = "Region: ${selectedRegionName ?: "Region $initialRegionId"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sport: ${selectedCartTypeName ?: "Type $initialCartTypeId"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = bookingFee,
                    onValueChange = {
                        bookingFee = it
                        bookingFeeError = null
                        sumError = null
                    },
                    label = { Text("Booking Fee (₹)") },
                    singleLine = true,
                    isError = bookingFeeError != null,
                    supportingText = bookingFeeError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = cancellationPct,
                    onValueChange = {
                        cancellationPct = it
                        cancellationPctError = null
                        sumError = null
                    },
                    label = { Text("Cancellation Fee (%)") },
                    singleLine = true,
                    isError = cancellationPctError != null,
                    supportingText = cancellationPctError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = platformPct,
                    onValueChange = {
                        platformPct = it
                        platformPctError = null
                        sumError = null
                    },
                    label = { Text("Platform Fee (%)") },
                    singleLine = true,
                    isError = platformPctError != null,
                    supportingText = platformPctError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (sumError != null) {
                    Text(
                        text = sumError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

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
                        Switch(checked = surgeEnabled, onCheckedChange = {
                            surgeEnabled = it
                            if (!it) surgeMultiplierError = null
                        })
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

                if (!isEditMode && (regions.isEmpty() || cartTypes.isEmpty())) {
                    Text(
                        text = "Load Regions and Cart Types before creating fee configs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isSubmitting && (if (!isEditMode) regions.isNotEmpty() && cartTypes.isNotEmpty() else true),
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                AnimatedContent(targetState = isSubmitting, label = "save") { submitting ->
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

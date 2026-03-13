package com.example.vmsadmin.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
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
    cartTypeViewModel: CartTypeViewModel
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

    LaunchedEffect(Unit) {
        viewModel.loadConfigs()
        if (regionState.regions.isEmpty()) regionViewModel.loadRegions()
        if (cartTypeState.cartTypes.isEmpty()) cartTypeViewModel.loadCartTypes()
        visible = true
    }

    if (uiState.showAddDialog) {
        FeeConfigFormDialog(
            title = "Add Fee Configuration",
            regions = regionState.regions,
            cartTypes = cartTypeState.cartTypes,
            existingConfigs = uiState.feeConfigs,
            isEditMode = false,
            initialRegionId = null,
            initialCartTypeId = null,
            initialBookingFee = "",
            initialCancellationPct = "",
            initialPlatformPct = "",
            onConfirm = { regionId, cartTypeId, bookingFee, cancellationPct, platformPct ->
                viewModel.addConfig(regionId, cartTypeId, bookingFee, cancellationPct, platformPct)
            },
            onDismiss = { viewModel.dismissAddDialog() }
        )
    }

    if (uiState.showEditDialog && uiState.editingConfig != null) {
        val config = uiState.editingConfig!!
        FeeConfigFormDialog(
            title = "Edit Fee Configuration",
            regions = regionState.regions,
            cartTypes = cartTypeState.cartTypes,
            existingConfigs = uiState.feeConfigs,
            isEditMode = true,
            initialRegionId = config.region_id,
            initialCartTypeId = config.cart_type_id,
            initialBookingFee = config.booking_fee.toBigDecimal().stripTrailingZeros().toPlainString(),
            initialCancellationPct = config.cancellation_fee_pct.toBigDecimal().stripTrailingZeros().toPlainString(),
            initialPlatformPct = config.platform_fee_pct.toBigDecimal().stripTrailingZeros().toPlainString(),
            onConfirm = { _, _, bookingFee, cancellationPct, platformPct ->
                viewModel.updateConfig(config.id, bookingFee, cancellationPct, platformPct)
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
                Text("Delete Fee Config", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete the fee configuration for \"$regionName - $cartTypeName\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteConfig(config.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete")
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
                                        onDelete = { viewModel.showDeleteConfirm(config) }
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
    onDelete: () -> Unit
) {
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cartTypeDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(status = if (config.is_active) "CONFIRMED" else "CANCELLED")
        }

        Spacer(modifier = Modifier.height(16.dp))
        FeeInfoRow(label = "Booking Fee", value = "₹%.2f".format(config.booking_fee))
        FeeInfoRow(label = "Cancellation Fee", value = "%.1f%%".format(config.cancellation_fee_pct))
        FeeInfoRow(label = "Platform Fee", value = "%.1f%%".format(config.platform_fee_pct))

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete", style = MaterialTheme.typography.labelMedium)
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
        }
    }
}

@Composable
private fun FeeInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
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
    onConfirm: (regionId: Int, cartTypeId: Int, bookingFee: Double, cancellationPct: Double, platformPct: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRegionId by remember { mutableStateOf(initialRegionId) }
    var selectedCartTypeId by remember { mutableStateOf(initialCartTypeId) }
    var bookingFee by remember { mutableStateOf(initialBookingFee) }
    var cancellationPct by remember { mutableStateOf(initialCancellationPct) }
    var platformPct by remember { mutableStateOf(initialPlatformPct) }

    var regionError by remember { mutableStateOf(false) }
    var cartTypeError by remember { mutableStateOf(false) }
    var bookingFeeError by remember { mutableStateOf<String?>(null) }
    var cancellationPctError by remember { mutableStateOf<String?>(null) }
    var platformPctError by remember { mutableStateOf<String?>(null) }
    var sumError by remember { mutableStateOf<String?>(null) }
    var duplicateError by remember { mutableStateOf<String?>(null) }

    var regionExpanded by remember { mutableStateOf(false) }
    var cartTypeExpanded by remember { mutableStateOf(false) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
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
                                text = selectedCartTypeName ?: "Select Cart Type",
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
                        text = "Cart Type: ${selectedCartTypeName ?: "Type $initialCartTypeId"}",
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                onClick = {
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
                        bookingFeeError = "Enter a valid fee (≥ 0)"
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

                    if (valid) {
                        onConfirm(
                            selectedRegionId!!,
                            selectedCartTypeId!!,
                            fee!!,
                            cancel!!,
                            platform!!
                        )
                    }
                },
                enabled = if (!isEditMode) regions.isNotEmpty() && cartTypes.isNotEmpty() else true,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

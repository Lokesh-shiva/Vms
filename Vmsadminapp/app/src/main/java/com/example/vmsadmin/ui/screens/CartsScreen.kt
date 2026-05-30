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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Cart
import com.example.vmsadmin.models.CartType
import com.example.vmsadmin.models.Region
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.CartTypeViewModel
import com.example.vmsadmin.viewmodel.CartViewModel
import com.example.vmsadmin.viewmodel.RegionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartsScreen(
    viewModel: CartViewModel,
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
        viewModel.loadCarts()
        if (regionState.regions.isEmpty()) {
            regionViewModel.loadRegions()
        }
        if (cartTypeState.cartTypes.isEmpty()) {
            cartTypeViewModel.loadCartTypes()
        }
        visible = true
    }

    if (uiState.showAddDialog) {
        CartFormDialog(
            title = "Add Cart",
            regions = regionState.regions,
            cartTypes = cartTypeState.cartTypes,
            initialLabel = "",
            initialRegionId = null,
            initialCartTypeId = null,
            isSubmitting = uiState.isSubmitting,
            onConfirm = { label, regionId, cartTypeId ->
                viewModel.addCart(label, regionId, cartTypeId)
            },
            onDismiss = { viewModel.dismissAddDialog() }
        )
    }

    if (uiState.showEditDialog && uiState.editingCart != null) {
        val cart = uiState.editingCart!!
        CartFormDialog(
            title = "Edit Cart",
            regions = regionState.regions,
            cartTypes = cartTypeState.cartTypes,
            initialLabel = cart.label ?: "",
            initialRegionId = cart.region_id,
            initialCartTypeId = cart.cart_type_id,
            isSubmitting = uiState.isSubmitting,
            onConfirm = { label, regionId, cartTypeId ->
                viewModel.updateCart(
                    id = cart.id,
                    label = label,
                    regionId = regionId,
                    cartTypeId = cartTypeId
                )
            },
            onDismiss = { viewModel.dismissEditDialog() }
        )
    }

    if (uiState.showDeleteConfirm && uiState.deletingCart != null) {
        val cart = uiState.deletingCart!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = {
                Text("Delete Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete \"${cartDisplayLabel(cart)}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCart(cart.id) },
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        "Carts",
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
                Icon(Icons.Default.Add, contentDescription = "Add Cart")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.carts.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                        .fillMaxWidth(0.6f)
                                        .height(14.dp)
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }

                uiState.error != null && uiState.carts.isEmpty() -> {
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
                        Button(onClick = { viewModel.loadCarts() }) {
                            Text("Retry")
                        }
                    }
                }

                uiState.carts.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No carts configured",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add your first cart",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCarts() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                uiState.carts,
                                key = { _, cart -> cart.id }
                            ) { index, cart ->
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(animationSpec = tween(400, delayMillis = index * 50)) +
                                        slideInVertically(
                                            initialOffsetY = { 50 },
                                            animationSpec = tween(400, delayMillis = index * 50)
                                        )
                                ) {
                                    val resolvedRegionName = cart.region_name
                                        ?: regionState.regions.firstOrNull { it.id == cart.region_id }?.name
                                    val resolvedCartTypeName = cart.cart_type_name
                                        ?: cartTypeState.cartTypes.firstOrNull { it.id == cart.cart_type_id }?.name
                                    CartCard(
                                        cart = cart,
                                        regionDisplayName = resolvedRegionName ?: "Region ${cart.region_id}",
                                        cartTypeDisplayName = resolvedCartTypeName ?: "Type ${cart.cart_type_id}",
                                        isUpdating = uiState.updatingCartIds.contains(cart.id),
                                        onEdit = { viewModel.showEditDialog(cart) },
                                        onDelete = { viewModel.showDeleteConfirm(cart) },
                                        onToggle = { isActive ->
                                            viewModel.toggleCart(cart.id, isActive)
                                        }
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
private fun CartCard(
    cart: Cart,
    regionDisplayName: String,
    cartTypeDisplayName: String,
    isUpdating: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val isActive = cart.is_active
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartDisplayLabel(cart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusBadge(status = cart.status)
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                enabled = !isUpdating,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Region: $regionDisplayName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cart Type: $cartTypeDisplayName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
                enabled = !isUpdating,
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
                enabled = !isUpdating,
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

private fun cartDisplayLabel(cart: Cart): String {
    return cart.label?.takeIf { it.isNotBlank() } ?: "CART-${cart.id}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartFormDialog(
    title: String,
    regions: List<Region>,
    cartTypes: List<CartType>,
    initialLabel: String,
    initialRegionId: Int?,
    initialCartTypeId: Int?,
    isSubmitting: Boolean,
    onConfirm: (label: String, regionId: Int, cartTypeId: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var selectedRegionId by remember { mutableStateOf(initialRegionId) }
    var selectedCartTypeId by remember { mutableStateOf(initialCartTypeId) }
    var labelError by remember { mutableStateOf(false) }
    var regionError by remember { mutableStateOf(false) }
    var cartTypeError by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }
    var cartTypeExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedRegionName = regions.firstOrNull { it.id == selectedRegionId }?.name
    val selectedCartTypeName = cartTypes.firstOrNull { it.id == selectedCartTypeId }?.name

    fun submit() {
        labelError = label.isBlank()
        regionError = selectedRegionId == null
        cartTypeError = selectedCartTypeId == null
        if (!labelError && !regionError && !cartTypeError) {
            keyboardController?.hide()
            onConfirm(label.trim(), selectedRegionId!!, selectedCartTypeId!!)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        labelError = it.isBlank()
                    },
                    label = { Text("Cart Label") },
                    placeholder = { Text("CART-1") },
                    singleLine = true,
                    isError = labelError,
                    supportingText = if (labelError) {
                        { Text("Cart label cannot be empty") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )

                Box {
                    OutlinedButton(
                        onClick = { regionExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = selectedRegionName ?: "Select Region",
                            modifier = Modifier.weight(1f),
                            color = if (regionError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
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

                Box {
                    OutlinedButton(
                        onClick = { cartTypeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = selectedCartTypeName ?: "Select Cart Type",
                            modifier = Modifier.weight(1f),
                            color = if (cartTypeError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select cart type")
                    }
                    DropdownMenu(
                        expanded = cartTypeExpanded,
                        onDismissRequest = { cartTypeExpanded = false },
                        modifier = Modifier.heightIn(max = 220.dp)
                    ) {
                        cartTypes.forEach { cartType ->
                            DropdownMenuItem(
                                text = { Text(cartType.name) },
                                onClick = {
                                    selectedCartTypeId = cartType.id
                                    cartTypeError = false
                                    cartTypeExpanded = false
                                }
                            )
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

                if (regions.isEmpty() || cartTypes.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Load Regions and Cart Types before creating carts.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isSubmitting && regions.isNotEmpty() && cartTypes.isNotEmpty(),
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

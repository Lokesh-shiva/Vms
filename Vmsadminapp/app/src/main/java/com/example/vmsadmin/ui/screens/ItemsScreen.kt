package com.example.vmsadmin.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vmsadmin.models.CartType
import com.example.vmsadmin.models.Item
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.ItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: ItemViewModel, onBack: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
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
        viewModel.loadItems()
        visible = true
    }

    // ── Add Item Dialog ──────────────────────────────────────────────
    if (uiState.showAddDialog) {
        ItemDialog(
            title = "Add Item",
            initialName = "",
            initialPrice = "",
            initialCartTypeId = null,
            initialDescription = "",
            initialImageUrl = "",
            cartTypes = uiState.cartTypes,
            isSubmitting = uiState.isSubmitting,
            onConfirm = { name, price, cartTypeId, description, imageUrl ->
                viewModel.addItem(name, price, cartTypeId, description, imageUrl)
            },
            onDismiss = { viewModel.dismissAddDialog() }
        )
    }

    // ── Edit Item Dialog ─────────────────────────────────────────────
    if (uiState.showEditDialog && uiState.editingItem != null) {
        val item = uiState.editingItem!!
        ItemDialog(
            title = "Edit Item",
            initialName = item.name,
            initialPrice = item.price.toString(),
            initialCartTypeId = item.cart_type_id,
            initialDescription = item.description ?: "",
            initialImageUrl = item.image_url ?: "",
            cartTypes = uiState.cartTypes,
            isSubmitting = uiState.isSubmitting,
            onConfirm = { name, price, cartTypeId, description, imageUrl ->
                viewModel.updateItem(item.id, name, price, cartTypeId, description, imageUrl)
            },
            onDismiss = { viewModel.dismissEditDialog() }
        )
    }

    // ── Delete Confirmation Dialog ───────────────────────────────────
    if (uiState.showDeleteConfirm && uiState.deletingItem != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = {
                Text("Delete Item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete \"${uiState.deletingItem!!.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteItem(uiState.deletingItem!!.id) },
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
                        "Items",
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
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
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
                                    Column {
                                        Box(modifier = Modifier.width(160.dp).height(20.dp).shimmerEffect())
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.width(80.dp).height(16.dp).shimmerEffect())
                                    }
                                    Box(modifier = Modifier.width(48.dp).height(28.dp).shimmerEffect())
                                }
                            }
                        }
                    }
                }

                uiState.error != null && uiState.items.isEmpty() -> {
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
                        Button(onClick = { viewModel.loadItems() }) {
                            Text("Retry")
                        }
                    }
                }

                uiState.cartTypes.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No items yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add your first item",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                else -> {
                    val itemsByCartType = uiState.items.groupBy { it.cart_type_id }
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshItems() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var globalIndex = 0
                            uiState.cartTypes.forEach { cartType ->
                                val groupItems = (itemsByCartType[cartType.id] ?: emptyList())
                                    .sortedBy { it.name }
                                val allAvailable = groupItems.isNotEmpty() && groupItems.all { it.is_available }
                                val isCategoryToggling = cartType.id in uiState.updatingCartTypeIds

                                item(key = "header_${cartType.id}") {
                                    val idx = globalIndex
                                    AnimatedVisibility(
                                        visible = visible,
                                        enter = fadeIn(animationSpec = tween(400, delayMillis = idx * 50)) +
                                                slideInVertically(
                                                    initialOffsetY = { 50 },
                                                    animationSpec = tween(400, delayMillis = idx * 50)
                                                )
                                    ) {
                                        CategoryHeader(
                                            cartType = cartType,
                                            itemCount = groupItems.size,
                                            allAvailable = allAvailable,
                                            isToggling = isCategoryToggling,
                                            onToggle = { isOn ->
                                                viewModel.toggleItemsByCartType(cartType.id, isOn)
                                            }
                                        )
                                    }
                                }
                                globalIndex++

                                itemsIndexed(
                                    groupItems,
                                    key = { _, item -> "item_${item.id}" }
                                ) { _, item ->
                                    val idx = globalIndex
                                    AnimatedVisibility(
                                        visible = visible,
                                        enter = fadeIn(animationSpec = tween(400, delayMillis = idx * 50)) +
                                                slideInVertically(
                                                    initialOffsetY = { 50 },
                                                    animationSpec = tween(400, delayMillis = idx * 50)
                                                )
                                    ) {
                                        ItemCard(
                                            item = item,
                                            isUpdating = item.id in uiState.updatingIds,
                                            onEdit = { viewModel.showEditDialog(item) },
                                            onDelete = { viewModel.showDeleteConfirm(item) },
                                            onToggle = { viewModel.toggleItem(item.id, !item.is_available) }
                                        )
                                    }
                                    globalIndex++
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
private fun CategoryHeader(
    cartType: CartType,
    itemCount: Int,
    allAvailable: Boolean,
    isToggling: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cartType.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$itemCount item${if (itemCount != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        if (itemCount > 0) {
            Text(
                text = if (allAvailable) "All active" else "Some inactive",
                style = MaterialTheme.typography.labelSmall,
                color = if (allAvailable) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 8.dp)
            )
            Switch(
                checked = allAvailable,
                onCheckedChange = onToggle,
                enabled = !isToggling && itemCount > 0,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: Item,
    isUpdating: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    AppCard {
        // ── Image area ───────────────────────────────────────────────
        val hasImage = !item.image_url.isNullOrBlank() && item.image_url.startsWith("http")
        if (hasImage) {
            AsyncImage(
                model = item.image_url,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No image",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Name + availability toggle ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹%.2f".format(item.price),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (item.is_available) "Available" else "Unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.is_available)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Switch(
                checked = item.is_available,
                onCheckedChange = { onToggle() },
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
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDialog(
    title: String,
    initialName: String,
    initialPrice: String,
    initialCartTypeId: Int?,
    initialDescription: String,
    initialImageUrl: String,
    cartTypes: List<CartType>,
    isSubmitting: Boolean,
    onConfirm: (name: String, price: Double, cartTypeId: Int, description: String?, imageUrl: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var price by remember { mutableStateOf(initialPrice) }
    var description by remember { mutableStateOf(initialDescription) }
    var imageUrl by remember { mutableStateOf(initialImageUrl) }
    var selectedCartType by remember {
        mutableStateOf(cartTypes.find { it.id == initialCartTypeId })
    }
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var cartTypeError by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submit() {
        val parsedPrice = price.toDoubleOrNull()
        nameError = name.isBlank()
        priceError = parsedPrice == null || parsedPrice <= 0
        cartTypeError = selectedCartType == null
        if (!nameError && !priceError && !cartTypeError) {
            keyboardController?.hide()
            onConfirm(
                name.trim(),
                parsedPrice!!,
                selectedCartType!!.id,
                description.ifBlank { null },
                imageUrl.ifBlank { null }
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
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Item Name") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Item name cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        priceError = it.toDoubleOrNull()?.let { v -> v <= 0 } ?: true
                    },
                    label = { Text("Price (₹)") },
                    singleLine = true,
                    isError = priceError,
                    supportingText = if (priceError) {
                        { Text("Enter a valid price greater than 0") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCartType?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cart Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        isError = cartTypeError,
                        supportingText = if (cartTypeError) {
                            { Text("Please select a cart type") }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        cartTypes.forEach { cartType ->
                            DropdownMenuItem(
                                text = { Text(cartType.name) },
                                onClick = {
                                    selectedCartType = cartType
                                    cartTypeError = false
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (optional)") },
                    placeholder = { Text("Paste image link (e.g., from Imgur)", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = !isSubmitting,
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

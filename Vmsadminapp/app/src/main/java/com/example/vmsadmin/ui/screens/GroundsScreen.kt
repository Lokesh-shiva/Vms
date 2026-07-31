package com.example.vmsadmin.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vmsadmin.models.AppUser
import com.example.vmsadmin.models.Ground
import com.example.vmsadmin.network.absoluteMediaUrl
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.ui.components.StatusBadge
import com.example.vmsadmin.ui.components.shimmerEffect
import com.example.vmsadmin.viewmodel.GroundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundsScreen(
    viewModel: GroundViewModel,
    currentUserRole: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val contentResolver = LocalContext.current.contentResolver

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
                                    isUploadingImage = uiState.uploadingImageIds.contains(ground.id),
                                    currentUserRole = currentUserRole,
                                    foundOwner = uiState.ownerSearchResult,
                                    ownerSearchLoading = uiState.ownerSearchLoading,
                                    ownerSearchError = uiState.ownerSearchError,
                                    onToggle = { isActive -> viewModel.toggleGround(ground.id, isActive) },
                                    onSearchOwner = { phone -> viewModel.searchOwnerByPhone(phone) },
                                    onAssignOwner = { gId, uId -> viewModel.assignOwner(gId, uId) },
                                    onUploadImage = { uri -> viewModel.uploadGroundImage(contentResolver, ground.id, uri) }
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
    isUploadingImage: Boolean,
    currentUserRole: String,
    foundOwner: AppUser?,
    ownerSearchLoading: Boolean,
    ownerSearchError: String?,
    onToggle: (Boolean) -> Unit,
    onSearchOwner: (String) -> Unit,
    onAssignOwner: (Int, Int) -> Unit,
    onUploadImage: (Uri) -> Unit,
) {
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onUploadImage) }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isUploadingImage,
                    ) { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (isUploadingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (!ground.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = absoluteMediaUrl(ground.image_url),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
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
        if (currentUserRole.lowercase() == "super_admin") {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "Assign Owner",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            ground.owner_user_id?.let {
                Text(
                    "Current owner ID: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            var ownerPhone by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ownerPhone,
                    onValueChange = { ownerPhone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSearchOwner(ownerPhone) },
                    enabled = ownerPhone.isNotBlank() && !isUpdating && !ownerSearchLoading
                ) {
                    if (ownerSearchLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Search")
                    }
                }
            }
            ownerSearchError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            foundOwner?.let { user ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Found: ${user.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onAssignOwner(ground.id, user.id) },
                    enabled = !isUpdating
                ) {
                    Text("Assign as Owner")
                }
            }
        }
    }
}

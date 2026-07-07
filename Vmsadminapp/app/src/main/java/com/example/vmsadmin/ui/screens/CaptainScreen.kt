package com.example.vmsadmin.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vmsadmin.models.Captain
import com.example.vmsadmin.viewmodel.CaptainViewModel
import com.example.vmsadmin.viewmodel.SearchStatus
import com.example.vmsadmin.viewmodel.UserManagementViewModel
import com.example.vmsadmin.ui.components.AppCard
import com.example.vmsadmin.viewmodel.RegionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptainScreen(
    viewModel: CaptainViewModel,
    userManagementViewModel: UserManagementViewModel,
    regionViewModel: RegionViewModel,
    currentUserRole: String = "",
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val regionState by regionViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var reviewTarget by remember { mutableStateOf<Captain?>(null) }
    var viewDocumentTarget by remember { mutableStateOf<Captain?>(null) }
    var payoutTarget by remember { mutableStateOf<Captain?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=All, 1=Pending Applications, 2=Payouts

    LaunchedEffect(Unit) {
        if (regionState.regions.isEmpty()) regionViewModel.loadRegions()
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) viewModel.loadPayouts()
    }

    payoutTarget?.let { captain ->
        MarkPaidDialog(
            captain = captain,
            onConfirm = { reference -> viewModel.markPaid(captain.id, reference); payoutTarget = null },
            onDismiss = { payoutTarget = null },
        )
    }

    LaunchedEffect(uiState.snackbar) {
        uiState.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    reviewTarget?.let { captain ->
        ReviewCaptainDialog(
            captain = captain,
            onApprove = { viewModel.reviewCaptain(captain.id, approve = true, reason = null); reviewTarget = null },
            onReject = { reason -> viewModel.reviewCaptain(captain.id, approve = false, reason = reason); reviewTarget = null },
            onViewDocument = { viewDocumentTarget = captain },
            onDismiss = { reviewTarget = null },
        )
    }

    viewDocumentTarget?.let { captain ->
        LaunchedEffect(captain.id) { viewModel.loadKycDocument(captain.id) }
        KycDocumentDialog(
            documentType = captain.kyc_document_type,
            bytes = uiState.kycDocumentBytes,
            loading = uiState.kycDocumentLoading,
            onDismiss = { viewDocumentTarget = null; viewModel.clearKycDocument() },
        )
    }

    if (showAddDialog) {
        AddCaptainDialog(
            userManagementViewModel = userManagementViewModel,
            regionNames = regionState.regions.associate { it.id to it.name },
            onConfirm = { userId, regionId ->
                viewModel.addCaptain(userId, regionId)
                showAddDialog = false
                userManagementViewModel.clearSearch()
            },
            onDismiss = {
                showAddDialog = false
                userManagementViewModel.clearSearch()
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Captains", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Captain")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; viewModel.setFilter(null) },
                    text = { Text("All Captains") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; viewModel.setFilter("PENDING_REVIEW") },
                    text = { Text("Pending Applications") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Payouts") },
                )
            }
            if (selectedTab == 2) {
                when {
                    uiState.payoutsLoading -> LoadingCaptains(Modifier)
                    uiState.payouts.isEmpty() -> EmptyCaptains(Modifier, message = "No pending payouts.")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.payouts, key = { it.id }) { captain ->
                            PayoutCard(captain = captain, onMarkPaid = { payoutTarget = captain })
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            } else {
                when {
                    uiState.isLoading -> LoadingCaptains(Modifier)
                    uiState.error != null -> ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.load() },
                        modifier = Modifier
                    )
                    uiState.captains.isEmpty() -> EmptyCaptains(Modifier)
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.captains, key = { it.id }) { captain ->
                            val regionName = regionState.regions.find { it.id == captain.region_id }?.name
                            CaptainCard(
                                captain = captain,
                                regionName = regionName,
                                canDelete = currentUserRole == "super_admin",
                                onStatusChange = { newStatus -> viewModel.updateStatus(captain.id, newStatus) },
                                onDelete = { viewModel.delete(captain.id) },
                                onReview = { reviewTarget = captain },
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptainCard(
    captain: Captain,
    regionName: String?,
    canDelete: Boolean,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit,
    onReview: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Captain") },
            text = { Text("Remove ${captain.name ?: "this captain"} from the captain roster?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val initials = captain.name
                    ?.split(" ")
                    ?.take(2)
                    ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    ?.joinToString("") ?: "?"
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = captain.name ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = captain.phone ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (regionName != null) {
                    Text(
                        text = regionName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    StatusChip(captain.status)
                    Text(
                        text = "★ ${"%.1f".format(captain.rating)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${captain.total_trips} trips",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (captain.status == "PENDING_REVIEW") {
                Button(onClick = onReview) { Text("Review") }
            } else {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        listOf("ACTIVE", "INACTIVE", "SUSPENDED").forEach { s ->
                            if (s != captain.status) {
                                DropdownMenuItem(
                                    text = { Text("Set $s") },
                                    onClick = { onStatusChange(s); showMenu = false }
                                )
                            }
                        }
                        if (canDelete) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { showDeleteConfirm = true; showMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "ACTIVE"         -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.primary
        "INACTIVE"       -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "SUSPENDED"      -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error
        "REJECTED"       -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error
        "PENDING_REVIEW" -> Color(0xFFFFF3CD) to Color(0xFF856404)
        else             -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCaptainDialog(
    userManagementViewModel: UserManagementViewModel,
    regionNames: Map<Int, String>,
    onConfirm: (userId: Int, regionId: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val searchResult by userManagementViewModel.searchResult.collectAsState()
    val searchStatus by userManagementViewModel.searchStatus.collectAsState()
    var phone by remember { mutableStateOf("") }
    var selectedRegionId by remember { mutableStateOf<Int?>(null) }
    var regionExpanded by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Captain") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        userManagementViewModel.searchByPhone(phone)
                    }),
                    trailingIcon = {
                        IconButton(onClick = {
                            keyboard?.hide()
                            userManagementViewModel.searchByPhone(phone)
                        }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search")
                        }
                    }
                )

                when (searchStatus) {
                    is SearchStatus.Idle -> Text(
                        "Enter the user's phone number and tap 🔍 to find them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    is SearchStatus.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is SearchStatus.NotFound -> Text(
                        "No user found with that number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    is SearchStatus.Error -> Text(
                        (searchStatus as SearchStatus.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    is SearchStatus.Found -> searchResult?.let { user ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(user.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        user.role.replace("_", " ").uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (regionNames.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = regionExpanded,
                                onExpandedChange = { regionExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = regionNames[selectedRegionId] ?: "No region",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Region (optional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(regionExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = regionExpanded,
                                    onDismissRequest = { regionExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("No region") },
                                        onClick = { selectedRegionId = null; regionExpanded = false }
                                    )
                                    regionNames.forEach { (id, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = { selectedRegionId = id; regionExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { searchResult?.let { onConfirm(it.id, selectedRegionId) } },
                enabled = searchStatus is SearchStatus.Found && searchResult != null
            ) { Text("Add Captain") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LoadingCaptains(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(5) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.height(14.dp).fillMaxWidth(0.4f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
                        Box(Modifier.height(11.dp).fillMaxWidth(0.3f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCaptains(modifier: Modifier = Modifier, message: String? = null) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))
            if (message != null) {
                Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("No captains yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap + to add one", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun ReviewCaptainDialog(
    captain: Captain,
    onApprove: () -> Unit,
    onReject: (reason: String) -> Unit,
    onViewDocument: () -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var showRejectInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Application") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(captain.name ?: "Unknown applicant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(captain.phone ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!captain.bio.isNullOrBlank()) {
                    Text(captain.bio, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Document: ${captain.kyc_document_type ?: "not uploaded"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (captain.kyc_document_url != null) {
                    OutlinedButton(onClick = onViewDocument) { Text("View Document") }
                }
                if (showRejectInput) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Rejection reason") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (showRejectInput) {
                Button(
                    onClick = { onReject(reason.trim()) },
                    enabled = reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Confirm Reject") }
            } else {
                Button(onClick = onApprove) { Text("Approve") }
            }
        },
        dismissButton = {
            if (showRejectInput) {
                OutlinedButton(onClick = { showRejectInput = false }) { Text("Back") }
            } else {
                OutlinedButton(onClick = { showRejectInput = true }) { Text("Reject") }
            }
        },
    )
}

@Composable
private fun KycDocumentDialog(
    documentType: String?,
    bytes: ByteArray?,
    loading: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(documentType ?: "KYC Document") },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator()
                    bytes != null -> {
                        val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                        if (bitmap != null) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "KYC document", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("Could not render document.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> Text("Document unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun PayoutCard(captain: Captain, onMarkPaid: () -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(captain.name ?: "Unknown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(captain.phone ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    captain.payout_upi_id ?: "No UPI ID on file",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (captain.payout_upi_id != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
                Text(
                    "₹${captain.wallet_balance ?: 0} owed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(onClick = onMarkPaid) { Text("Mark Paid") }
        }
    }
}

@Composable
private fun MarkPaidDialog(
    captain: Captain,
    onConfirm: (reference: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var reference by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark payout as sent") },
        text = {
            Column {
                Text(
                    "Confirming that ₹${captain.wallet_balance ?: 0} was sent to ${captain.name ?: "this captain"}" +
                        (captain.payout_upi_id?.let { " ($it)" } ?: "") + " via UPI/bank transfer.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference / txn ID (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(reference.trim().ifBlank { null }) }) { Text("Confirm") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

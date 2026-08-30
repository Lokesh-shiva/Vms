package com.example.vmsuser.ui.screens.captain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.models.LocationOption
import com.example.vmsuser.models.Society
import com.example.vmsuser.models.SportItem
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.CaptainViewModel

private val SPORT_PHOTOS = mapOf(
    "Badminton" to "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600&q=80",
    "Cricket" to "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=600&q=80",
    "Football" to "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&q=80",
)

@Composable
fun CaptainDashboardScreen(navController: NavController) {
    val vm: CaptainViewModel = viewModel()
    val stats by vm.stats.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active", "Create", "Earnings")
    var createVisibility by remember { mutableStateOf<String?>(null) }

    if (createVisibility != null) {
        CreateMatchDialog(
            visibility = createVisibility!!,
            vm = vm,
            onDismiss = { createVisibility = null },
            onDone = { match ->
                createVisibility = null
                navController.navigate(Screen.CaptainMatch.create(match.id))
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(PlixoInk).statusBarsPadding()) {
        Column(modifier = Modifier.background(PlixoInk).padding(20.dp)) {
            Text(
                "Captain Mode",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CaptainStatCard("₹${stats.todayEarnings}", "Today", Modifier.weight(1f))
                CaptainStatCard("₹${stats.weekEarnings}", "This week", Modifier.weight(1f))
                CaptainStatCard("${stats.rating}★", "Rating", Modifier.weight(1f))
                CaptainStatCard("${stats.matchesLed}", "Led", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.08f), PlixoShape.Pill)
                    .padding(4.dp),
            ) {
                tabs.forEachIndexed { i, tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PlixoShape.Pill)
                            .background(if (activeTab == i) PlixoLime else Color.Transparent)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { activeTab = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tab,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (activeTab == i) PlixoLimeFg else Color.White.copy(0.6f),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlixoBg, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        ) {
            when (activeTab) {
                0 -> ActiveMatchesTab(stats.activeMatches, navController)
                1 -> CreateMatchTab(
                    onSelect = { key ->
                        if (key == "TOURNAMENT") navController.navigate(Screen.Tournaments.route)
                        else createVisibility = key
                    },
                )
                2 -> EarningsTab(navController, stats.walletBalance, stats.kycStatus)
            }
        }
    }
}

@Composable
private fun ActiveMatchesTab(matches: List<com.example.vmsuser.models.Match>, navController: NavController) {
    if (matches.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 60.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.EventBusy, null, tint = PlixoText3, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text("No active matches", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlixoText)
            Spacer(Modifier.height(2.dp))
            Text(
                "Create a match from the Create tab to get started.",
                fontFamily = PlusJakartaSans, fontSize = 12.5.sp, color = PlixoText2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(matches) { match ->
            val photoUrl = SPORT_PHOTOS[match.sport]
                ?: "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=600&q=80"
            Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card)) {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                        PlixoPill(match.sport, bg = sportColor(match.sport), fg = Color.White)
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        PlixoPill(
                            if (match.status == "confirmed") "Confirmed" else "Open",
                            bg = if (match.status == "confirmed") BlockMintBg else BlockSkyBg,
                            fg = if (match.status == "confirmed") BlockMintFg else BlockSkyFg,
                        )
                    }
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(match.groundName, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PlixoText)
                            Text(match.scheduledAt, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                        }
                        Text(
                            "₹${match.price * 6}",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PlixoPrimary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlixoButton(
                            "Manage",
                            onClick = { navController.navigate(Screen.CaptainMatch.create(match.id)) },
                            variant = PlixoButtonVariant.Primary,
                            fullWidth = false,
                            modifier = Modifier.weight(1f),
                        )
                        PlixoButton(
                            "Message",
                            onClick = { navController.navigate(Screen.ChatThread.create(match.id.toString())) },
                            variant = PlixoButtonVariant.Soft,
                            fullWidth = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun CreateMatchTab(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        listOf(
            Triple("OPEN", "Open match" to "Anyone can join via the app", PlixoPrimaryLight),
            Triple("SOCIETY", "Society match" to "Exclusive to your society members", BlockSkyBg),
            Triple("TOURNAMENT", "Tournament" to "Browse or register for an official tournament", BlockLilacBg),
            Triple("PRIVATE", "Private" to "Invite-only with a code", PlixoSurface2),
        ).forEach { (key, titleDesc, bg) ->
            val (title, desc) = titleDesc
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg, PlixoShape.Card)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(key) }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
                    Text(desc, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = PlixoText3)
            }
        }
    }
}

@Composable
private fun EarningsTab(navController: NavController, walletBalance: Int, kycStatus: String?) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlixoInk, PlixoShape.Card)
                .padding(24.dp),
        ) {
            Column {
                Text("Wallet balance", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = Color.White.copy(0.6f))
                Text(
                    "₹$walletBalance",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(12.dp))
                PlixoButton(
                    "View full earnings",
                    onClick = { navController.navigate(Screen.CaptainEarnings.route) },
                    variant = PlixoButtonVariant.Lime,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        VerificationCard(kycStatus = kycStatus, onUpload = { navController.navigate(Screen.KycUpload.route) })
    }
}

@Composable
private fun VerificationCard(kycStatus: String?, onUpload: () -> Unit) {
    val (label, description, showUpload) = when (kycStatus) {
        "APPROVED" -> Triple("Verified", "Your ID document has been approved.", false)
        "PENDING" -> Triple("Under review", "We're checking your uploaded document.", false)
        "REJECTED" -> Triple("Rejected", "Your document was rejected — upload a new one.", true)
        else -> Triple("Not submitted", "Upload an ID document so admins can verify you.", true)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlixoSurface, PlixoShape.Card)
            .padding(20.dp),
    ) {
        Column {
            Text("Verification", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoText)
            Spacer(Modifier.height(4.dp))
            Text(label, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlixoText)
            Spacer(Modifier.height(2.dp))
            Text(description, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
            if (showUpload) {
                Spacer(Modifier.height(12.dp))
                PlixoButton("Upload ID document", onClick = onUpload)
            }
        }
    }
}

@Composable
private fun CaptainStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(0.07f), PlixoShape.SmCard)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        Text(label, fontFamily = PlusJakartaSans, fontSize = 10.sp, color = Color.White.copy(0.5f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMatchDialog(
    visibility: String,
    vm: CaptainViewModel,
    onDismiss: () -> Unit,
    onDone: (com.example.vmsuser.models.Match) -> Unit,
) {
    val creating by vm.creatingMatch.collectAsState()
    val error by vm.createMatchError.collectAsState()

    var sports by remember { mutableStateOf<List<SportItem>>(emptyList()) }
    var locations by remember { mutableStateOf<List<LocationOption>>(emptyList()) }
    var mySocieties by remember { mutableStateOf<List<Society>>(emptyList()) }
    var selectedSport by remember { mutableStateOf<SportItem?>(null) }
    var selectedLocation by remember { mutableStateOf<LocationOption?>(null) }
    var selectedSociety by remember { mutableStateOf<Society?>(null) }
    var maxPlayers by remember { mutableIntStateOf(6) }
    var sportExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var societyExpanded by remember { mutableStateOf(false) }
    var createdMatch by remember { mutableStateOf<com.example.vmsuser.models.Match?>(null) }

    LaunchedEffect(Unit) {
        try {
            val sportsRes = RetrofitClient.api.getSports()
            if (sportsRes.success && sportsRes.data != null) sports = sportsRes.data.filter { it.isActive }
        } catch (_: Exception) {}
        try {
            val locRes = RetrofitClient.api.getLocations()
            if (locRes.success && locRes.data != null) locations = locRes.data
        } catch (_: Exception) {}
        if (visibility == "SOCIETY") {
            com.example.vmsuser.data.SocialRepository().getSocieties().onSuccess { list ->
                mySocieties = list.filter { it.isMember }
            }
        }
    }

    val title = when (visibility) {
        "OPEN" -> "Create open match"
        "SOCIETY" -> "Create society match"
        else -> "Create private match"
    }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text(title, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            if (createdMatch != null) {
                // Success step — only reached for PRIVATE, where the invite code must be shown
                // before leaving this dialog (nothing else in the app displays it).
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, null, tint = PlixoPrimary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Match created!", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoText)
                    Spacer(Modifier.height(10.dp))
                    Text("Share this code so others can join:", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        createdMatch?.inviteCode ?: "—",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        letterSpacing = 4.sp,
                        color = PlixoPrimary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(expanded = sportExpanded, onExpandedChange = { sportExpanded = it }) {
                        OutlinedTextField(
                            value = selectedSport?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sport") },
                            placeholder = { Text(if (sports.isEmpty()) "Loading…" else "Select sport") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded) },
                            singleLine = true,
                        )
                        ExposedDropdownMenu(expanded = sportExpanded && sports.isNotEmpty(), onDismissRequest = { sportExpanded = false }) {
                            sports.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedSport = s; sportExpanded = false })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = locationExpanded, onExpandedChange = { locationExpanded = it }) {
                        OutlinedTextField(
                            value = selectedLocation?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Area") },
                            placeholder = { Text(if (locations.isEmpty()) "Loading…" else "Select area") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                            singleLine = true,
                        )
                        ExposedDropdownMenu(expanded = locationExpanded && locations.isNotEmpty(), onDismissRequest = { locationExpanded = false }) {
                            locations.forEach { l ->
                                DropdownMenuItem(text = { Text(l.name) }, onClick = { selectedLocation = l; locationExpanded = false })
                            }
                        }
                    }
                    if (visibility == "SOCIETY") {
                        ExposedDropdownMenuBox(expanded = societyExpanded, onExpandedChange = { societyExpanded = it }) {
                            OutlinedTextField(
                                value = selectedSociety?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Society") },
                                placeholder = { Text(if (mySocieties.isEmpty()) "No societies found" else "Select society") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = societyExpanded) },
                                singleLine = true,
                            )
                            ExposedDropdownMenu(expanded = societyExpanded && mySocieties.isNotEmpty(), onDismissRequest = { societyExpanded = false }) {
                                mySocieties.forEach { s ->
                                    DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedSociety = s; societyExpanded = false })
                                }
                            }
                        }
                        if (mySocieties.isEmpty()) {
                            Text(
                                "You need to be a member of a society to create a society match.",
                                fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Max players", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PlixoText)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            IconButton(onClick = { if (maxPlayers > 2) maxPlayers-- }, enabled = maxPlayers > 2) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                            }
                            Text("$maxPlayers", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoText)
                            IconButton(onClick = { if (maxPlayers < 22) maxPlayers++ }, enabled = maxPlayers < 22) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase")
                            }
                        }
                    }
                    error?.let {
                        Text(it, fontFamily = PlusJakartaSans, fontSize = 12.5.sp, color = PlixoDanger)
                    }
                }
            }
        },
        confirmButton = {
            if (createdMatch != null) {
                TextButton(onClick = { onDone(createdMatch!!) }) { Text("Continue") }
            } else {
                val canSubmit = selectedSport != null && selectedLocation != null && !creating &&
                    (visibility != "SOCIETY" || selectedSociety != null)
                TextButton(
                    onClick = {
                        vm.createMatch(
                            cartTypeId = selectedSport!!.id,
                            regionId = selectedLocation!!.id,
                            maxPlayers = maxPlayers,
                            visibility = visibility,
                            societyId = selectedSociety?.id,
                        ) { match ->
                            if (visibility == "PRIVATE") createdMatch = match else onDone(match)
                        }
                    },
                    enabled = canSubmit,
                ) { Text(if (creating) "Creating…" else "Create") }
            }
        },
        dismissButton = {
            if (createdMatch == null) {
                TextButton(onClick = onDismiss, enabled = !creating) { Text("Cancel") }
            }
        },
    )
}

package com.example.vmsuser.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.config.FeatureFlags
import com.example.vmsuser.data.MatchRepository
import com.example.vmsuser.models.OpenMatch
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.network.absoluteMediaUrl
import com.example.vmsuser.network.registerFcmToken
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel
import com.example.vmsuser.viewmodel.TournamentsViewModel
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
fun HomeScreen(navController: NavController) {
    val user by UserSession.user.collectAsState()
    val tournamentsVm: TournamentsViewModel = viewModel()
    val tournaments by tournamentsVm.tournaments.collectAsState()
    val nextTournament = tournaments.firstOrNull { it.status.uppercase() == "UPCOMING" || it.status.uppercase() == "ONGOING" }

    val profileVm: ProfileViewModel = viewModel()
    val walletBalance by profileVm.walletBalance.collectAsState()
    LaunchedEffect(Unit) { if (FeatureFlags.WALLET) profileVm.loadTransactions() }

    var openMatches by remember { mutableStateOf<List<OpenMatch>>(emptyList()) }
    LaunchedEffect(Unit) {
        if (FeatureFlags.OPEN_MATCHES) {
            MatchRepository().getOpenMatches().onSuccess { openMatches = it.take(3) }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op — FCM still registers the token either way; only tray display is affected */ }

    LaunchedEffect(Unit) {
        if (FeatureFlags.NOTIFICATIONS) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { registerFcmToken(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var locationOpen by remember { mutableStateOf(false) }
    var region by remember { mutableStateOf(user?.city?.takeIf { it.isNotBlank() } ?: user?.region?.takeIf { it.isNotBlank() } ?: "My area") }
    val areas = listOf("Indiranagar", "Koramangala", "HSR Layout", "Whitefield", "Jayanagar")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlixoBg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
                .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = PlixoInk.copy(0.06f))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(0.92f), Color.White.copy(0.78f))
                    )
                )
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.95f), Color.White.copy(0.4f))), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        greeting(),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = PlixoText2,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { locationOpen = !locationOpen }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = PlixoPrimary, modifier = Modifier.size(15.dp))
                        Text(
                            region,
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PlixoText,
                            letterSpacing = (-0.4).sp,
                        )
                        Icon(
                            if (locationOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            null,
                            tint = PlixoText3,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                // Location dropdown
                if (locationOpen) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 52.dp)
                            .width(190.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = PlixoSurface,
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                "YOUR AREA",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = PlixoText3,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 4.dp),
                            )
                            areas.forEach { area ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (region == area) PlixoPrimaryLight else Color.Transparent)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            region = area; locationOpen = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        area,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (region == area) PlixoPrimary else PlixoText,
                                    )
                                    if (region == area) Icon(Icons.Filled.Check, null, tint = PlixoPrimary, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (FeatureFlags.NOTIFICATIONS) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PlixoSurface)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                navController.navigate(Screen.Notifications.route)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Notifications, "Notifications", tint = PlixoText2, modifier = Modifier.size(22.dp))
                    }
                }
                PlixoAvatar(
                    name = user?.name?.takeIf { it.isNotBlank() } ?: "Player",
                    imageUrl = absoluteMediaUrl(user?.profilePhotoUrl),
                    size = 44.dp,
                    onClick = { navController.navigate(Screen.Profile.route) },
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))

            // ── Photo hero ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        navController.navigate(Screen.Play.route)
                    }
                    .shadow(elevation = 0.dp),
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=900&q=80",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x0D16151F),
                                    0.42f to Color(0x1916151F),
                                    1.0f to Color(0xD116151F),
                                )
                            )
                        )
                )
                // Flash btn top-right — glass circle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(18.dp)
                        .size(44.dp)
                        .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(0.3f))
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(0.28f), Color.White.copy(0.10f))
                            )
                        )
                        .border(1.dp, Color.White.copy(0.30f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.FlashOn, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                // Bottom CTA
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                ) {
                    Text(
                        "Ready to\nplay today?",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 38.sp,
                        color = Color.White,
                        letterSpacing = (-1.4).sp,
                        lineHeight = 39.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(PlixoShape.Button)
                                .background(PlixoLime)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    navController.navigate(Screen.Play.route)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.FlashOn, null, tint = PlixoLimeFg, modifier = Modifier.size(19.dp))
                                Text("Instant Match", fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PlixoLimeFg)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    navController.navigate(Screen.Play.route)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.ArrowForward, null, tint = PlixoInk, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── Stat blocks (real user data) ──────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = "${user?.matchStreak ?: 0}",
                    suffix = "days",
                    label = "Play streak",
                    color = StatCardColor.Lime,
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = "${((user?.winRate ?: 0f) * 100).toInt()}",
                    suffix = "%",
                    label = "Win rate",
                    color = StatCardColor.Lilac,
                    icon = Icons.Filled.TrendingUp,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))

            // ── Coins strip ──────────────────────────────────────────────────
            if (FeatureFlags.WALLET) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0xFFFBEFD8).copy(0.5f), spotColor = Color(0xFFFBEFD8).copy(0.3f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFEFAF3).copy(0.96f), Color(0xFFFBF3E4).copy(0.88f))
                            )
                        )
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.9f), Color(0xFFFBEFD8).copy(0.5f))), RoundedCornerShape(24.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            navController.navigate(Screen.Wallet.route)
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFFFBEFD8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🪙", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$walletBalance coins",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = PlixoText,
                            letterSpacing = (-0.4).sp,
                        )
                        Text(
                            "Earned by completing matches",
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                            color = PlixoText2,
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = PlixoText3, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Quick tiles ──────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(
                    icon = Icons.Filled.Group,
                    label = "Groups",
                    sub = "Browse",
                    bg = BlockSkyBg,
                    fg = BlockSkyFg,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Societies.route) },
                )
                if (FeatureFlags.WALLET) {
                    QuickTile(
                        icon = Icons.Filled.AccountBalanceWallet,
                        label = "Wallet",
                        sub = "$walletBalance coins",
                        bg = BlockMintBg,
                        fg = BlockMintFg,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Wallet.route) },
                    )
                } else {
                    val openCount = tournaments.count { it.status == "open" || it.status == "upcoming" }
                    QuickTile(
                        icon = Icons.Filled.EmojiEvents,
                        label = "Tournaments",
                        sub = if (openCount > 0) "$openCount open" else "Browse",
                        bg = BlockMintBg,
                        fg = BlockMintFg,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Tournaments.route) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // ── Up next ───────────────────────────────────────────────────────
        }

        if (FeatureFlags.OPEN_MATCHES) {
            SectionHeader(
                title = "Open matches nearby",
                action = "See all",
                onAction = { navController.navigate(Screen.OpenMatches.route) },
            )
            if (openMatches.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    openMatches.forEach { match ->
                        OpenMatchHomeCard(
                            match = match,
                            onClick = { navController.navigate(Screen.OpenMatches.route) },
                        )
                    }
                }
            } else {
                HomeEmptyState(
                    icon = Icons.Filled.Group,
                    title = "No open matches nearby",
                    subtitle = "Start one from the Play tab and others can join.",
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        SectionHeader(
            title = "Up next",
            action = "All cups",
            onAction = { navController.navigate(Screen.Tournaments.route) },
        )
        if (nextTournament != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(188.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(PlixoInk)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        tournamentsVm.select(nextTournament.id)
                        navController.navigate(Screen.TournamentDetail.create(nextTournament.id))
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlixoPill(
                            if (nextTournament.status == "open") "REG OPEN" else "STARTS SOON",
                            bg = PlixoLime.copy(alpha = 0.95f),
                            fg = PlixoLimeFg,
                        )
                        PlixoPill(nextTournament.sport, bg = Color.White.copy(0.14f), fg = Color.White)
                    }
                    Column {
                        Text(
                            nextTournament.name,
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White,
                            letterSpacing = (-0.8).sp,
                            lineHeight = 26.sp,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (nextTournament.prizePool.isNotBlank()) {
                                Text("${nextTournament.prizePool} prize", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PlixoLime)
                            }
                            Text(
                                "${nextTournament.registeredTeams}/${nextTournament.maxTeams} teams",
                                fontFamily = PlusJakartaSans,
                                fontSize = 13.sp,
                                color = Color.White.copy(0.8f),
                            )
                        }
                    }
                }
            }
        } else {
            HomeEmptyState(
                icon = Icons.Filled.EmojiEvents,
                title = "No tournaments yet",
                subtitle = "Open tournaments will show up here.",
            )
        }
        Spacer(Modifier.height(20.dp))

        // ── Captain CTA ───────────────────────────────────────────────────────
        if (FeatureFlags.CAPTAIN_ONBOARDING) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(PlixoInk)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    navController.navigate(Screen.Captain.route)
                }
                .padding(20.dp),
        ) {
            // Decorative circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(PlixoLime.copy(alpha = 0.12f))
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PlixoLime),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Stars, null, tint = PlixoLimeFg, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Become a Captain",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = (-0.4).sp,
                    )
                    Text(
                        "Organise matches · earn ₹150–500/session",
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(0.6f),
                    )
                }
                Icon(Icons.Filled.ArrowForward, null, tint = PlixoLime, modifier = Modifier.size(22.dp))
            }
        }
        } // end FeatureFlags.CAPTAIN_ONBOARDING
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(PlixoSurface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(label, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlixoText, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(1.dp))
        Text(sub, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
    }
}

@Composable
private fun OpenMatchHomeCard(match: OpenMatch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PlixoSurface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BlockSkyBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.SportsSoccer, null, tint = BlockSkyFg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                match.sport,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PlixoText,
            )
            Text(
                match.groundName.ifBlank { match.regionName },
                fontFamily = PlusJakartaSans,
                fontSize = 12.5.sp,
                color = PlixoText2,
            )
        }
        PlixoPill(
            "${match.joinedPlayers}/${match.maxPlayers}",
            bg = BlockMintBg,
            fg = BlockMintFg,
        )
    }
}

@Composable
private fun HomeEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(PlixoSurface)
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PlixoSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = PlixoText3, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
    }
}

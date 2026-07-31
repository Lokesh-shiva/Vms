package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.config.FeatureFlags
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.AuthTokenManager
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.network.absoluteMediaUrl
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileScreen(navController: NavController) {
    val vm: ProfileViewModel = viewModel()
    val user by vm.user.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Stats") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val name = user?.name?.takeIf { it.isNotBlank() } ?: "Player"
    val region = (user?.city ?: user?.region)?.takeIf { it.isNotBlank() } ?: "—"
    val matchesPlayed = user?.matchesPlayed ?: 0
    val winRate = user?.winRate ?: 0f
    val wins = (matchesPlayed * winRate).toInt()
    val streak = user?.matchStreak ?: 0
    val isCaptain = user?.isCaptain ?: false
    val level = user?.level ?: 1
    val xp = user?.xpPoints ?: 0
    val xpNext = ((xp / 500) + 1) * 500

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlixoBg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
        ) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Profile",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = PlixoText,
                    letterSpacing = (-0.8).sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(PlixoSurface)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { navController.navigate(Screen.Settings.route) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Settings, null, tint = PlixoText2, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Avatar + info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PlixoSurface)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PlixoAvatar(name = name, imageUrl = absoluteMediaUrl(user?.profilePhotoUrl), size = 64.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PlixoText, letterSpacing = (-0.5).sp)
                    Text(region, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlixoPill("Level $level", bg = PlixoPrimaryLight, fg = PlixoPrimary)
                        if (isCaptain) PlixoPill("Captain", bg = PlixoLime, fg = PlixoLimeFg)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PlixoSurface2)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { navController.navigate(Screen.EditProfile.route) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("Edit", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PlixoText2)
                }
            }
            Spacer(Modifier.height(12.dp))

            // XP progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PlixoSurface)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$xp XP", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
                    Text("Level ${level + 1} · $xpNext XP", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(PlixoSurface2)) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(xp.toFloat() / xpNext)
                            .clip(RoundedCornerShape(99.dp))
                            .background(PlixoPrimary),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PlixoSurface)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MiniStat("$matchesPlayed", "Matches")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(PlixoBorder))
                MiniStat("$wins", "Wins")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(PlixoBorder))
                MiniStat("${(winRate * 100).toInt()}%", "Win rate")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(PlixoBorder))
                MiniStat("$streak 🔥", "Streak")
            }
            Spacer(Modifier.height(16.dp))

            // Inner tabs: Stats / Badges / History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(PlixoSurface2)
                    .padding(4.dp),
            ) {
                listOf("Stats", "Badges", "History").forEach { t ->
                    val active = tab == t
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) PlixoSurface else Color.Transparent)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tab = t }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            t,
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (active) PlixoText else PlixoText2,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Tab content
            when (tab) {
                "Stats" -> StatsTab()
                "Badges" -> BadgesTab()
                "History" -> HistoryTab()
            }

            Spacer(Modifier.height(20.dp))

            // Menu sections — only features with backend support are shown
            MenuSection(
                items = buildList {
                    add(Triple(Icons.Outlined.EmojiEvents, "My tournaments") { navController.navigate(Screen.Tournaments.route) })
                    if (FeatureFlags.WALLET) add(Triple(Icons.Outlined.AccountBalanceWallet, "Wallet") { navController.navigate(Screen.Wallet.route) })
                    add(Triple(Icons.Outlined.Group, "My groups") { navController.navigate(Screen.Societies.route) })
                },
            )
            Spacer(Modifier.height(12.dp))
            val secondSection = buildList {
                // Captain dashboard for existing captains; onboarding hidden when unsupported
                if (isCaptain && FeatureFlags.CAPTAIN_DASHBOARD) {
                    add(Triple(Icons.Outlined.Stars, "Captain dashboard") { navController.navigate(Screen.Captain.route) })
                } else if (!isCaptain && FeatureFlags.CAPTAIN_ONBOARDING) {
                    add(Triple(Icons.Outlined.Stars, "Captain mode") { navController.navigate(Screen.BecomeACaptain.route) })
                }
                if (FeatureFlags.CAPTAIN_ONBOARDING) {
                    add(Triple(Icons.Outlined.VerifiedUser, "Verification / KYC") { navController.navigate(Screen.KycStatus.route) })
                }
                if (FeatureFlags.NOTIFICATIONS) {
                    add(Triple(Icons.Outlined.Notifications, "Notifications") { navController.navigate(Screen.Notifications.route) })
                }
                add(Triple(Icons.Outlined.Settings, "Settings") { navController.navigate(Screen.Settings.route) })
            }
            if (secondSection.isNotEmpty()) {
                MenuSection(items = secondSection, highlight = 0)
                Spacer(Modifier.height(12.dp))
            }
            MenuSection(
                items = listOf(
                    Triple(Icons.Outlined.Logout, "Sign out") {
                        scope.launch {
                            AuthTokenManager(context).clearToken()
                            UserSession.clear()
                            navController.navigate(Screen.Phone.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                ),
                danger = true,
            )
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(value, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlixoText)
        Text(label, fontFamily = PlusJakartaSans, fontSize = 11.sp, color = PlixoText2, letterSpacing = 0.sp)
    }
}

@Composable
private fun StatsTab() {
    ProfileEmptyState(
        icon = Icons.Outlined.BarChart,
        title = "No sport stats yet",
        subtitle = "Play a few matches and your per-sport record will show here.",
    )
}

@Composable
private fun BadgesTab() {
    ProfileEmptyState(
        icon = Icons.Outlined.MilitaryTech,
        title = "No badges yet",
        subtitle = "Earn badges by winning matches and hitting streaks.",
    )
}

@Composable
private fun HistoryTab() {
    ProfileEmptyState(
        icon = Icons.Outlined.History,
        title = "No match history",
        subtitle = "Your recent matches will appear here.",
    )
}

@Composable
private fun ProfileEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PlixoSurface).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(PlixoSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = PlixoText3, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}

@Composable
private fun MenuSection(
    items: List<Triple<ImageVector, String, () -> Unit>>,
    highlight: Int = -1,
    danger: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PlixoSurface),
    ) {
        items.forEachIndexed { i, (icon, label, action) ->
            if (i > 0) Box(modifier = Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(PlixoBorder))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { action() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                val iconBg = when {
                    danger -> PlixoDangerLight
                    highlight == i -> PlixoPrimaryLight
                    else -> PlixoSurface2
                }
                val iconFg = when {
                    danger -> PlixoDanger
                    highlight == i -> PlixoPrimary
                    else -> PlixoText2
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = iconFg, modifier = Modifier.size(19.dp))
                }
                Text(
                    label,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (danger) PlixoDanger else PlixoText,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ChevronRight, null, tint = PlixoText3, modifier = Modifier.size(18.dp))
            }
        }
    }
}

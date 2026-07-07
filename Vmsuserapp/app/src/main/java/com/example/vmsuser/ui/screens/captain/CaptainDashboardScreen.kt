package com.example.vmsuser.ui.screens.captain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.vmsuser.navigation.Screen
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
                1 -> CreateMatchTab(navController)
                2 -> EarningsTab(navController, stats.walletBalance)
            }
        }
    }
}

@Composable
private fun ActiveMatchesTab(matches: List<com.example.vmsuser.models.Match>, navController: NavController) {
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
private fun CreateMatchTab(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        listOf(
            Triple("Open match", "Anyone can join via the app", PlixoPrimaryLight),
            Triple("Society match", "Exclusive to your society members", BlockSkyBg),
            Triple("Tournament", "Official Plixo tournament format", BlockLilacBg),
            Triple("Private", "Invite-only with a code", PlixoSurface2),
        ).forEach { (title, desc, bg) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg, PlixoShape.Card)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
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
private fun EarningsTab(navController: NavController, walletBalance: Int) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
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

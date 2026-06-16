package com.example.vmsuser.ui.screens.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.config.FeatureFlags
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.PlayViewModel

private val SPORT_PHOTOS = mapOf(
    "Badminton" to "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=900&q=80",
    "Cricket" to "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=900&q=80",
    "Football" to "https://images.unsplash.com/photo-1553778263-73a83bab9b0c?w=900&q=80",
    "Basketball" to "https://images.unsplash.com/photo-1546519638405-a9f57abb306f?w=900&q=80",
    "Tennis" to "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=900&q=80",
)

@Composable
fun ActiveMatchScreen(navController: NavController, matchId: Int) {
    val vm: PlayViewModel = viewModel()
    val match by vm.match.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    var checkedIn by remember { mutableStateOf(false) }
    var showCantMake by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) { vm.loadMatch(matchId) }

    if (showCantMake) {
        AlertDialog(
            onDismissRequest = { showCantMake = false },
            title = {
                Text("Can't make it?", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Text(
                    "Leaving after the match is confirmed may add a ghost strike to your record.",
                    fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                }) {
                    Text("Yes, leave", color = PlixoDanger, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCantMake = false }) {
                    Text("Stay", color = PlixoPrimary, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (loading && match == null) {
        Box(modifier = Modifier.fillMaxSize().background(PlixoBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PlixoPrimary)
        }
        return
    }

    val m = match
    val sport = m?.sport ?: "Match"
    val groundName = m?.groundName?.takeIf { it.isNotBlank() } ?: "Venue TBD"
    val groundAddress = m?.groundAddress?.takeIf { it.isNotBlank() } ?: ""
    val captainName = m?.captainName?.takeIf { it.isNotBlank() } ?: "Captain"
    val playerCount = m?.playerIds?.size ?: 0
    val photoUrl = SPORT_PHOTOS[sport] ?: SPORT_PHOTOS["Badminton"]!!

    // Format scheduledAt (ISO 8601 "2026-06-14T18:00:00") into readable label
    val timeLabel = m?.scheduledAt?.let { iso ->
        try {
            val parts = iso.split("T")
            val date = parts[0]
            val timePart = parts.getOrNull(1)?.take(5) ?: ""
            val hour = timePart.substringBefore(":").toIntOrNull() ?: 0
            val minute = timePart.substringAfter(":").toIntOrNull() ?: 0
            val ampm = if (hour < 12) "AM" else "PM"
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            val timeStr = "%d:%02d %s".format(h12, minute, ampm)
            "$date · $timeStr"
        } catch (e: Exception) { iso }
    } ?: "Time TBD"

    Column(
        modifier = Modifier.fillMaxSize().background(PlixoBg).verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent))))
            Row(
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                ) {
                    Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Active Match", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            }
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                PlixoPill("$sport · $timeLabel", bg = PlixoLime, fg = PlixoLimeFg)
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Location card
            Row(
                modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(44.dp).background(PlixoPrimaryLight, PlixoShape.SmCard), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LocationOn, null, tint = PlixoPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(groundName, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
                    if (groundAddress.isNotBlank()) {
                        Text(groundAddress, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                    }
                }
            }

            // Time card
            Row(
                modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(44.dp).background(PlixoPrimaryLight, PlixoShape.SmCard), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Schedule, null, tint = PlixoPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(timeLabel, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
                    Text("Arrive 10 minutes early", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                }
            }

            // Captain card
            Row(
                modifier = Modifier.fillMaxWidth().background(PlixoInk, PlixoShape.Card).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlixoAvatar(name = captainName, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(captainName, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text("Your captain", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                if (FeatureFlags.CHAT) {
                    IconButton(
                        onClick = { navController.navigate(Screen.Chat.route) },
                        modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    ) {
                        Icon(Icons.Filled.ChatBubble, "Message", tint = PlixoLime, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Players card
            if (playerCount > 0) {
                Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                    Text(
                        "Players ($playerCount)",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PlixoText2,
                    )
                    Spacer(Modifier.height(10.dp))
                    val playerLabels = m?.playerIds?.mapIndexed { i, _ -> "P${i + 1}" } ?: emptyList()
                    AvatarStack(names = playerLabels, size = 38.dp)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (!checkedIn) {
                PlixoButton(label = "Check in", onClick = { checkedIn = true }, variant = PlixoButtonVariant.Primary)
                Spacer(Modifier.height(8.dp))
                PlixoButton(label = "Can't make it", onClick = { showCantMake = true }, variant = PlixoButtonVariant.DangerSoft)
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().background(BlockMintBg, PlixoShape.Card).padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, null, tint = BlockMintFg, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Checked in! See you on the court.", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlockMintFg)
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

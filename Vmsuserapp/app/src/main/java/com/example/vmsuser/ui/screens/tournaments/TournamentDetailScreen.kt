package com.example.vmsuser.ui.screens.tournaments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowBackIosNew
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.TournamentsViewModel

@Composable
fun TournamentDetailScreen(navController: NavController, id: Int) {
    val vm: TournamentsViewModel = viewModel()
    val selected by vm.selected.collectAsState()
    val registered by vm.registered.collectAsState()
    val registering by vm.registering.collectAsState()
    val registerError by vm.registerError.collectAsState()

    LaunchedEffect(id) { vm.select(id) }

    val t = selected ?: return

    val photoUrl = when (t.sport) {
        "Cricket" -> "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=900&q=80"
        "Football" -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=900&q=80"
        else -> "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=900&q=80"
    }
    val isRegistered = t.id in registered

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).verticalScroll(rememberScrollState())) {
        // Hero
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x44000000), Color(0xDD16151F)), startY = 80f)))
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp).size(40.dp).background(Color.Black.copy(0.3f), RoundedCornerShape(12.dp)),
            ) {
                Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlixoPill(t.sport, bg = sportColor(t.sport), fg = Color.White)
                    PlixoPill(t.prizePool, bg = BlockLimeBg, fg = BlockLimeFg)
                }
                Spacer(Modifier.height(8.dp))
                Text(t.name, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White, letterSpacing = (-0.5).sp)
                Text("by ${t.organizerName}", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = Color.White.copy(0.65f))
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Info cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(Icons.Filled.LocationOn, t.location, Modifier.weight(1f))
                InfoChip(Icons.Filled.CalendarToday, t.startDate, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(Icons.Filled.Group, "${t.registeredTeams}/${t.maxTeams} teams", Modifier.weight(1f))
                InfoChip(Icons.Filled.CurrencyRupee, "₹${t.entryFee} entry", Modifier.weight(1f))
            }

            // Teams progress
            Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Teams registered", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PlixoText2)
                    Text("${t.registeredTeams}/${t.maxTeams}", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PlixoText)
                }
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(PlixoSurface2, PlixoShape.Pill)) {
                    val progress = t.registeredTeams.toFloat() / t.maxTeams.coerceAtLeast(1)
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(PlixoPrimary, PlixoShape.Pill))
                }
            }

            // About
            Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                Text("About", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlixoText)
                Spacer(Modifier.height(6.dp))
                Text(t.description, fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText2, lineHeight = 20.sp)
            }

            // Register CTA
            if (registerError != null) {
                Text(registerError!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
            }
            if (isRegistered) {
                Column(modifier = Modifier.fillMaxWidth().background(BlockMintBg, PlixoShape.Card).padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.CheckCircle, null, tint = BlockMintFg, modifier = Modifier.size(24.dp))
                            Text("You're registered!", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BlockMintFg)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (registering) "Withdrawing…" else "Withdraw registration",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = BlockMintFg.copy(alpha = 0.75f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !registering) { vm.withdraw(t.id) },
                    )
                }
            } else {
                PlixoButton(
                    if (registering) "Registering…" else "Register for ${t.name} · ₹${t.entryFee}",
                    onClick = { vm.register(t.id) },
                    enabled = !registering,
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(PlixoSurface, PlixoShape.SmCard).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = PlixoPrimary, modifier = Modifier.size(16.dp))
        Text(text, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText, fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}

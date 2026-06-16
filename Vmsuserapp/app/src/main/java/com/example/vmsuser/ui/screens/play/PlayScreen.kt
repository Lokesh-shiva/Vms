package com.example.vmsuser.ui.screens.play

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
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
import com.example.vmsuser.config.FeatureFlags
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.PlayViewModel

private val SPORTS = listOf(
    Triple("Badminton", "🏸", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=400&q=80"),
    Triple("Cricket", "🏏", "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=400&q=80"),
    Triple("Football", "⚽", "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=400&q=80"),
    Triple("Tennis", "🎾", "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=400&q=80"),
    Triple("Basketball", "🏀", "https://images.unsplash.com/photo-1518063319789-7217e6706b04?w=400&q=80"),
    Triple("Pickleball", "🏓", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=400&q=80"),
    Triple("Table Tennis", "🏓", "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=400&q=80"),
    Triple("Running", "🏃", "https://images.unsplash.com/photo-1526232761682-d26e03ac148e?w=400&q=80"),
)

private val SKILLS = listOf("Beginner", "Mid", "Pro")

private data class SportInfo(val players: Int, val wait: Int, val price: Int)
private val SPORT_INFO = mapOf(
    "Cricket" to SportInfo(5, 10, 120),
    "Football" to SportInfo(12, 8, 80),
    "Badminton" to SportInfo(2, 15, 200),
    "Basketball" to SportInfo(4, 20, 60),
    "Tennis" to SportInfo(1, 30, 350),
    "Pickleball" to SportInfo(3, 12, 150),
    "Table Tennis" to SportInfo(4, 8, 100),
    "Running" to SportInfo(8, 5, 50),
)

@Composable
fun PlayScreen(navController: NavController) {
    val vm: PlayViewModel = viewModel()
    val selectedSport by vm.selectedSport.collectAsState()
    val selectedSkill by vm.selectedSkill.collectAsState()
    val loading by vm.loading.collectAsState()

    val info = SPORT_INFO[selectedSport] ?: SportInfo(3, 15, 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlixoBg)
            .statusBarsPadding(),
    ) {
        // TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Play",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = PlixoText,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    "Jump into the live queue",
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    color = PlixoText2,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Sport grid header ──────────────────────────────────────────
            Text(
                "Choose your sport",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PlixoText,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(12.dp))

            // 2-col sport photo grid
            SPORTS.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { (sport, _, photoUrl) ->
                        val active = sport == selectedSport
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    3.dp,
                                    if (active) PlixoPrimary else Color.Transparent,
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    vm.selectSport(sport)
                                },
                        ) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = if (active)
                                                arrayOf(0f to Color(0x1A7C5CFF), 1f to Color(0x9E7C5CFF))
                                            else
                                                arrayOf(0f to Color(0x0D16151F), 1f to Color(0x9E16151F))
                                        )
                                    )
                            )
                            Text(
                                sport,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                            )
                            if (active) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(9.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(PlixoLime),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = PlixoLimeFg, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(10.dp))

            // Skill level
            Text(
                "Skill level",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PlixoText,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SKILLS.forEach { skill ->
                    val active = skill == selectedSkill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) PlixoInk else PlixoSurface2)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.selectSkill(skill) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            skill,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (active) Color.White else PlixoText2,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    Triple("${info.players}", "Searching", false),
                    Triple("${info.wait}m", "Est. wait", false),
                    Triple("₹${info.price}", "Per game", true),
                ).forEach { (value, label, isPrimary) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PlixoSurface)
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            value,
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = if (isPrimary) PlixoPrimary else PlixoText,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(label, fontFamily = PlusJakartaSans, fontSize = 11.sp, color = PlixoText3)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // CTA
            PlixoButton(
                label = if (loading) "Joining…" else "Join $selectedSport queue · ₹${info.price}",
                onClick = {
                    vm.joinQueue { sport -> navController.navigate(Screen.Queue.create(sport)) }
                },
                enabled = !loading,
            )
            if (FeatureFlags.OPEN_MATCHES) {
                Spacer(Modifier.height(10.dp))
                PlixoButton(
                    label = "Browse open matches nearby",
                    onClick = { navController.navigate(Screen.OpenMatches.route) },
                    variant = PlixoButtonVariant.Ghost,
                )
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

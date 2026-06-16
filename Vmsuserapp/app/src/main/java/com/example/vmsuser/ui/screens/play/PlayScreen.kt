package com.example.vmsuser.ui.screens.play

import android.Manifest
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
import androidx.compose.material.icons.filled.Check
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
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.PlayViewModel

// Local display metadata — photo per sport name (case-insensitive key lookup)
private val SPORT_PHOTO = mapOf(
    "badminton"    to "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=400&q=80",
    "cricket"      to "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=400&q=80",
    "football"     to "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=400&q=80",
    "tennis"       to "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=400&q=80",
    "basketball"   to "https://images.unsplash.com/photo-1518063319789-7217e6706b04?w=400&q=80",
    "volleyball"   to "https://images.unsplash.com/photo-1547919307-1ecb10702e6f?w=400&q=80",
    "table tennis" to "https://images.unsplash.com/photo-1534158914592-062992fbe900?w=400&q=80",
    "running"      to "https://images.unsplash.com/photo-1526232761682-d26e03ac148e?w=400&q=80",
)
private val FALLBACK_PHOTO = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=400&q=80"
fun sportPhoto(name: String) = SPORT_PHOTO[name.lowercase()] ?: FALLBACK_PHOTO

// Max-players options — labels shown in chips
private val PLAYER_OPTIONS = listOf(2, 4, 6, 11)

private val SKILLS = listOf("Beginner", "Intermediate", "Advanced")

@Composable
fun PlayScreen(navController: NavController) {
    val vm: PlayViewModel = viewModel()
    val selectedSport by vm.selectedSport.collectAsState()
    val selectedSkill by vm.selectedSkill.collectAsState()
    val maxPlayers by vm.maxPlayers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var sportNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var sportsLoading by remember { mutableStateOf(true) }

    // Location permission launcher — request before starting the session
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Whether granted or not, proceed — GPS is used later for arrival check
        vm.clearError()
        vm.joinQueue { sport -> navController.navigate(Screen.Queue.create(sport)) }
    }

    LaunchedEffect(Unit) {
        try {
            val res = RetrofitClient.api.getSports()
            if (res.success && res.data != null) {
                val names = res.data.filter { it.isActive }.map { it.name }
                sportNames = names
                if (selectedSport.isBlank() || selectedSport !in names) {
                    names.firstOrNull()?.let { vm.selectSport(it) }
                } else {
                    vm.selectSport(selectedSport)
                }
            }
        } catch (_: Exception) {}
        sportsLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlixoBg)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Play Now",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = PlixoText,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    "Pick your game and we'll find you players",
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

            // ── Sport ────────────────────────────────────────────────────
            Text(
                "Sport",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PlixoText,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(12.dp))

            if (sportsLoading) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PlixoPrimary, modifier = Modifier.size(28.dp))
                }
            } else {
                sportNames.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { sport ->
                            val active = sport == selectedSport
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(92.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(3.dp, if (active) PlixoPrimary else Color.Transparent, RoundedCornerShape(20.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        vm.selectSport(sport)
                                    },
                            ) {
                                AsyncImage(
                                    model = sportPhoto(sport),
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
            }

            Spacer(Modifier.height(10.dp))

            // ── Players needed ───────────────────────────────────────────
            Text(
                "Players needed",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PlixoText,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "How many total players should this session have?",
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                color = PlixoText3,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PLAYER_OPTIONS.forEach { n ->
                    val active = n == maxPlayers
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) PlixoInk else PlixoSurface2)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                vm.selectMaxPlayers(n)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$n",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (active) Color.White else PlixoText2,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            // ── Skill level ──────────────────────────────────────────────
            Text(
                "Your skill level",
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
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                vm.selectSkill(skill)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            skill,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (active) Color.White else PlixoText2,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // ── Error ────────────────────────────────────────────────────
            error?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEDED))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(msg, color = Color(0xFFB00020), fontSize = 13.sp, fontFamily = PlusJakartaSans)
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── CTA ──────────────────────────────────────────────────────
            PlixoButton(
                label = if (loading) "Starting…" else "Play Now",
                onClick = {
                    vm.clearError()
                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                enabled = !loading && selectedSport.isNotBlank(),
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

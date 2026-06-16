package com.example.vmsuser.ui.screens.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.data.MatchRepository
import com.example.vmsuser.models.OpenMatch
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OpenMatchesScreen(navController: NavController) {
    val repo = remember { MatchRepository() }
    val scope = rememberCoroutineScope()

    var matches by remember { mutableStateOf<List<OpenMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var joiningId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        repo.getOpenMatches()
            .onSuccess { matches = it }
            .onFailure { error = it.message }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlixoBg)
            .statusBarsPadding(),
    ) {
        PlixoTopBar(title = "Open Matches", onBack = { navController.popBackStack() })

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PlixoPrimary)
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        error ?: "Something went wrong",
                        color = PlixoText2,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                    )
                }
            }
            matches.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No open matches near you", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PlixoText)
                        Spacer(Modifier.height(8.dp))
                        Text("Be the first — tap Play and create one!", fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText2)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(matches, key = { it.id }) { match ->
                        OpenMatchCard(
                            match = match,
                            joining = joiningId == match.id,
                            onJoin = {
                                joiningId = match.id
                                scope.launch {
                                    repo.joinOpenMatch(match.id)
                                        .onSuccess { joined ->
                                            navController.navigate(
                                                Screen.ActiveMatch.create(joined.id)
                                            )
                                        }
                                        .onFailure { error = it.message }
                                    joiningId = null
                                }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun OpenMatchCard(match: OpenMatch, joining: Boolean, onJoin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlixoSurface, PlixoShape.Card),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            AsyncImage(
                model = sportPhoto(match.sport),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                PlixoPill(
                    text = match.sport.ifBlank { "Match" },
                    bg = sportColor(match.sport),
                    fg = Color.White,
                )
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PlixoText3, modifier = Modifier.size(14.dp))
                Text(
                    match.regionName.ifBlank { "Nearby" },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = PlixoText,
                )
            }
            if (match.creatorName.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Started by ${match.creatorName}",
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                    color = PlixoText3,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = PlixoText3, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${match.joinedPlayers}/${match.maxPlayers} joined",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = PlixoText2,
                )
                Spacer(Modifier.weight(1f))
                PlixoButton(
                    label = if (joining) "Joining…" else "Join",
                    onClick = onJoin,
                    variant = PlixoButtonVariant.Soft,
                    fullWidth = false,
                    enabled = !joining,
                    modifier = Modifier.width(100.dp),
                )
            }
        }
    }
}

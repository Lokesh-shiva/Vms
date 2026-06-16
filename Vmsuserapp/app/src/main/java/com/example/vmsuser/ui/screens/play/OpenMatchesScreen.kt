package com.example.vmsuser.ui.screens.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*

private data class OpenMatch(
    val id: Int,
    val sport: String,
    val ground: String,
    val time: String,
    val filled: Int,
    val total: Int,
    val price: Int,
    val photoUrl: String,
)

private val MOCK_MATCHES = listOf(
    OpenMatch(
        id = 1,
        sport = "Cricket",
        ground = "BBMP Ground Koramangala",
        time = "Today 4:00 PM",
        filled = 8,
        total = 11,
        price = 200,
        photoUrl = "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=600&q=80",
    ),
    OpenMatch(
        id = 2,
        sport = "Football",
        ground = "Bangalore Football Stadium",
        time = "Today 6:00 PM",
        filled = 12,
        total = 22,
        price = 150,
        photoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&q=80",
    ),
    OpenMatch(
        id = 3,
        sport = "Badminton",
        ground = "Kanteerava Annex",
        time = "Tomorrow 7:00 AM",
        filled = 2,
        total = 4,
        price = 400,
        photoUrl = "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600&q=80",
    ),
)

@Composable
fun OpenMatchesScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlixoBg)
            .statusBarsPadding(),
    ) {
        PlixoTopBar(title = "Open Matches", onBack = { navController.popBackStack() })
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(MOCK_MATCHES) { match ->
                OpenMatchCard(
                    match = match,
                    onJoin = { navController.navigate(Screen.ActiveMatch.create(match.id)) },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun OpenMatchCard(match: OpenMatch, onJoin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlixoSurface, PlixoShape.Card),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            AsyncImage(
                model = match.photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                PlixoPill(
                    text = match.sport,
                    bg = sportColor(match.sport),
                    fg = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = PlixoText3,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    match.ground,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = PlixoText,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = PlixoText3,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    match.time,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    color = PlixoText2,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Group,
                    contentDescription = null,
                    tint = PlixoText3,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "${match.filled}/${match.total}",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = PlixoText2,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₹${match.price}/player",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PlixoText,
                )
                Spacer(Modifier.weight(1f))
                PlixoButton(
                    label = "Join",
                    onClick = onJoin,
                    variant = PlixoButtonVariant.Soft,
                    fullWidth = false,
                    modifier = Modifier.width(100.dp),
                )
            }
        }
    }
}

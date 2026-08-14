package com.example.vmsuser.ui.screens.tournaments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.TournamentsViewModel

private val SPORT_PHOTOS = mapOf(
    "Cricket" to "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=700&q=80",
    "Football" to "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=700&q=80",
    "Badminton" to "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=700&q=80",
    "Basketball" to "https://images.unsplash.com/photo-1518063319789-7217e6706b04?w=700&q=80",
    "Tennis" to "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=700&q=80",
    "Pickleball" to "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=700&q=80",
)

private val VOTE_BAR_COLORS = listOf(
    Color(0xFF7C5CFF), Color(0xFF0E9488), Color(0xFF86922A), Color(0xFFC0392B),
    Color(0xFF2F6BD6), Color(0xFFD6842F),
)

@Composable
fun TournamentsScreen(navController: NavController) {
    val vm: TournamentsViewModel = viewModel()
    val tournaments by vm.tournaments.collectAsState()
    val registered by vm.registered.collectAsState()
    val loading by vm.loading.collectAsState()
    var tab by remember { mutableStateOf("Browse") }
    val tabs = listOf("Browse", "Vote", "My cups")

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        // TopBar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tournaments", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = PlixoText, letterSpacing = (-0.8).sp)
                Text("Compete across Vizag", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
            }
        }

        // Inner tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(50))
                .background(PlixoSurface2)
                .padding(4.dp),
        ) {
            tabs.forEach { t ->
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
        Spacer(Modifier.height(8.dp))

        when (tab) {
            "Browse" -> BrowseTab(tournaments, registered, loading, navController, vm)
            "Vote" -> VoteTab(vm)
            "My cups" -> MyCupsTab(tournaments.filter { it.id in registered }, navController, vm)
        }
    }
}

@Composable
private fun BrowseTab(
    tournaments: List<com.example.vmsuser.models.Tournament>,
    registered: Set<Int>,
    loading: Boolean,
    navController: NavController,
    vm: TournamentsViewModel,
) {
    val open = tournaments.filter { it.status.uppercase() == "UPCOMING" || it.status.uppercase() == "ONGOING" }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Prize hero card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(PlixoInk)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 24.dp, y = (-24).dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(PlixoLime.copy(alpha = 0.1f))
                )
                Column {
                    Text("Open tournaments", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = Color.White.copy(0.6f))
                    Text("${open.size}", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color.White, letterSpacing = (-1).sp)
                    Text("accepting registrations now", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoLime)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (open.isEmpty()) {
            item {
                TournamentsEmptyState(
                    loading = loading,
                    title = if (loading) "Loading tournaments…" else "No tournaments open",
                    subtitle = if (loading) "" else "Check back soon — new cups open regularly.",
                )
            }
        } else {
            items(open) { t ->
                val photoUrl = SPORT_PHOTOS[t.sport] ?: "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=700&q=80"
                TournamentCard(
                    tournament = t,
                    photoUrl = photoUrl,
                    isRegistered = t.id in registered,
                    onClick = { vm.select(t.id); navController.navigate(Screen.TournamentDetail.create(t.id)) },
                    onRegister = { vm.register(t.id) },
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun formatClosesAt(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val parsed = java.time.LocalDateTime.parse(iso)
        parsed.format(java.time.format.DateTimeFormatter.ofPattern("d MMM, h:mm a"))
    } catch (_: Exception) { iso }
}

@Composable
private fun VoteTab(vm: TournamentsViewModel) {
    val votes by vm.votes.collectAsState()
    val voteOptions by vm.voteOptions.collectAsState()
    val myVote by vm.myVote.collectAsState()
    val totalVotes by vm.totalVotes.collectAsState()
    val status by vm.voteStatus.collectAsState()
    val closesAt by vm.voteClosesAt.collectAsState()
    val winner by vm.voteWinner.collectAsState()
    val loading by vm.votesLoading.collectAsState()
    val error by vm.voteError.collectAsState()

    LaunchedEffect(Unit) { vm.loadVotes() }

    val closed = status == "CLOSED"
    val votesBySport = votes.associateBy { it.sport }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PlixoInk)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    when {
                        status == "NONE" -> "No vote right now"
                        closed -> "Voting closed"
                        else -> "Voting closes ${formatClosesAt(closesAt)}"
                    },
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.5.sp,
                    color = Color.White.copy(0.6f),
                )
                Text("$totalVotes total votes", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
            }
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(PlixoLime.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (closed) Icons.Filled.Lock else Icons.Filled.Flag, null, tint = PlixoLime, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (closed) "Voting has closed for this round." else "The most-voted sport becomes the next city-wide tournament. Tap a sport to cast or change your vote.",
            fontFamily = PlusJakartaSans,
            fontSize = 13.5.sp,
            color = PlixoText2,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text(error!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger, modifier = Modifier.padding(bottom = 10.dp))
        }

        if (loading && voteOptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PlixoPrimary, modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
            }
        } else if (status == "NONE") {
            TournamentsEmptyState(
                loading = false,
                title = "No vote round active",
                subtitle = "Check back once the next voting round opens.",
            )
        } else {
            voteOptions.forEachIndexed { index, sport ->
                val sportVotes = votesBySport[sport]?.votes ?: 0
                val pct = if (totalVotes > 0) sportVotes * 100 / totalVotes else 0
                val voted = myVote == sport
                val isWinner = closed && winner == sport
                val color = VOTE_BAR_COLORS[index % VOTE_BAR_COLORS.size]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PlixoSurface)
                        .border(2.dp, if (voted || isWinner) PlixoPrimary else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !loading && !closed,
                        ) { vm.castVote(sport) }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                            Text(sport, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PlixoText)
                            if (isWinner) PlixoPill("Winner 🏆", bg = PlixoPrimaryLight, fg = PlixoPrimary)
                            else if (voted) PlixoPill("Your vote", bg = PlixoPrimaryLight, fg = PlixoPrimary)
                        }
                        Text("$pct%", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (voted || isWinner) PlixoPrimary else PlixoText)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(PlixoSurface2)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(pct / 100f)
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (voted || isWinner) PlixoPrimary else PlixoSurface3),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun MyCupsTab(
    myTournaments: List<com.example.vmsuser.models.Tournament>,
    navController: NavController,
    vm: TournamentsViewModel,
) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (myTournaments.isEmpty()) {
            item {
                TournamentsEmptyState(
                    loading = false,
                    title = "No tournaments yet",
                    subtitle = "Register for a tournament to see it here.",
                )
            }
        } else {
            items(myTournaments) { t ->
                val photoUrl = SPORT_PHOTOS[t.sport] ?: "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=700&q=80"
                TournamentCard(
                    tournament = t,
                    photoUrl = photoUrl,
                    isRegistered = true,
                    onClick = { vm.select(t.id); navController.navigate(Screen.TournamentDetail.create(t.id)) },
                    onRegister = {},
                )
            }
        }
    }
}

@Composable
private fun TournamentsEmptyState(loading: Boolean, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) {
            CircularProgressIndicator(color = PlixoPrimary, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
        } else {
            Icon(Icons.Filled.EmojiEvents, null, tint = PlixoText3, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = PlixoText)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TournamentCard(
    tournament: com.example.vmsuser.models.Tournament,
    photoUrl: String,
    isRegistered: Boolean,
    onClick: () -> Unit,
    onRegister: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PlixoSurface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(128.dp)) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(0f to Color(0xD116151F), 0.55f to Color(0x3316151F), 1f to Color(0x1A16151F))
                        )
                    )
            )
            Row(modifier = Modifier.align(Alignment.TopStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val (statusLabel, statusBg, statusFg) = when (tournament.status.uppercase()) {
                    "UPCOMING" -> Triple("REG OPEN", PlixoLime.copy(0.95f), PlixoLimeFg)
                    "ONGOING" -> Triple("LIVE", Color(0xFFF0535F).copy(0.95f), Color.White)
                    "COMPLETED" -> Triple("ENDED", Color(0xFF888888).copy(0.95f), Color.White)
                    else -> Triple("STARTS SOON", Color(0xFF2F6BD6).copy(0.95f), Color.White)
                }
                PlixoPill(statusLabel, bg = statusBg, fg = statusFg)
            }
            Column(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalAlignment = Alignment.End) {
                Text("Prize pool", fontFamily = PlusJakartaSans, fontSize = 10.sp, color = Color.White.copy(0.7f))
                Text(tournament.prizePool, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoLime)
            }
            Text(
                tournament.name,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.White,
                letterSpacing = (-0.5).sp,
                lineHeight = 21.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tournament.startDate, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                Text(
                    "${tournament.registeredTeams}/${tournament.maxTeams} teams · entry ₹${tournament.entryFee}",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = PlixoText,
                )
            }
            Spacer(Modifier.height(11.dp))
            val pct = if (tournament.maxTeams > 0) tournament.registeredTeams.toFloat() / tournament.maxTeams else 0f
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(PlixoSurface2)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pct.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (pct >= 1f) PlixoDanger else PlixoPrimary),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (isRegistered) "You're registered ✓" else "${tournament.maxTeams - tournament.registeredTeams} spots left",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (isRegistered) Color(0xFF2E9E5B) else PlixoText3,
                )
                if (!isRegistered) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(PlixoInk)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRegister() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("Register", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

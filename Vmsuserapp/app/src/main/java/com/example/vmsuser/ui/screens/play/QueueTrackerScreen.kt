package com.example.vmsuser.ui.screens.play

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.PlixoButton
import com.example.vmsuser.ui.components.PlixoButtonVariant
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.PlayViewModel
import kotlinx.coroutines.delay

@Composable
fun QueueTrackerScreen(navController: NavController, sport: String) {
    val vm: PlayViewModel = viewModel()
    val queueStatus by vm.queueStatus.collectAsStateWithLifecycle()
    val sessionCancelled by vm.sessionCancelled.collectAsStateWithLifecycle()
    var waitSeconds by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.pollQueueStatus()
    }

    // Navigate when backend confirms match found
    LaunchedEffect(queueStatus?.matchFound, queueStatus?.matchId) {
        val status = queueStatus
        if (status?.matchFound == true && status.matchId != null) {
            navController.navigate(Screen.ActiveMatch.create(status.matchId)) {
                popUpTo(Screen.Queue.route.replace("{sport}", sport)) { inclusive = true }
            }
        }
    }

    // Backend auto-cancelled this session (no players joined in time)
    LaunchedEffect(sessionCancelled) {
        if (sessionCancelled) {
            Toast.makeText(context, "No players found nearby — session ended. Try again later.", Toast.LENGTH_LONG).show()
            vm.clearSessionCancelled()
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            waitSeconds++
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "p1",
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 400), RepeatMode.Restart),
        label = "p2",
    )
    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 800), RepeatMode.Restart),
        label = "p3",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlixoSurface)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            listOf(
                pulse1 to 0.15f,
                pulse2 to 0.1f,
                pulse3 to 0.07f,
            ).forEach { (scale, alpha) ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(PlixoPrimary),
                )
            }
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(PlixoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("🏟️", fontSize = 32.sp)
            }
        }
        Spacer(Modifier.height(32.dp))

        val playerCount = queueStatus?.playersSearching?.takeIf { it > 0 } ?: 0
        Text(
            "$playerCount",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 52.sp,
            color = PlixoText,
            letterSpacing = (-2).sp,
        )
        Text(
            if (playerCount == 1) "player joined (you)" else "players joined",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = PlixoText2,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your $sport session is open · waiting for others to join",
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            color = PlixoText3,
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).background(PlixoSurface2, PlixoShape.SmCard).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val mins = waitSeconds / 60
                val secs = waitSeconds % 60
                Text(
                    "%02d:%02d".format(mins, secs),
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = PlixoText,
                )
                Text("waiting", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
            }
            Column(
                modifier = Modifier.weight(1f).background(PlixoPrimaryLight, PlixoShape.SmCard).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val estWait = queueStatus?.estimatedWaitSeconds
                val label = if (estWait != null && estWait > 0) {
                    val m = estWait / 60
                    val s = estWait % 60
                    if (m > 0) "~${m}m" else "~${s}s"
                } else "—"
                Text(
                    label,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = PlixoPrimaryDark,
                )
                Text(
                    "est. arrival",
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    color = PlixoPrimaryDark.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)) {
            PlixoButton(
                label = "Cancel session",
                onClick = { vm.leaveQueue { navController.popBackStack() } },
                variant = PlixoButtonVariant.DangerSoft,
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

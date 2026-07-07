package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.models.Notification
import com.example.vmsuser.notifications.notificationDeepLinkRoute
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun NotificationsScreen(navController: NavController) {
    val vm: ProfileViewModel = viewModel()
    val notifications by vm.notifications.collectAsState()

    LaunchedEffect(Unit) { vm.loadNotifications() }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Notifications", onBack = { navController.popBackStack() })
        LazyColumn {
            items(notifications) { n ->
                NotificationRow(n, onClick = {
                    vm.markNotificationRead(n.id)
                    val matchId = n.data["match_id"]?.jsonPrimitive?.intOrNull
                    navController.navigate(notificationDeepLinkRoute(n.type, matchId))
                })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun NotificationRow(n: Notification, onClick: () -> Unit) {
    val icon = when (n.type) {
        "MATCH_FOUND" -> Icons.Filled.SportsScore
        "SESSION_CANCELLED" -> Icons.Filled.Cancel
        "CAPTAIN_APPROVED" -> Icons.Filled.Star
        "CAPTAIN_REJECTED" -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.Notifications
    }
    val iconBg = when (n.type) {
        "MATCH_FOUND" -> PlixoPrimaryLight
        "CAPTAIN_APPROVED" -> BlockLimeBg
        "CAPTAIN_REJECTED", "SESSION_CANCELLED" -> BlockSkyBg
        else -> PlixoSurface2
    }
    val iconFg = when (n.type) {
        "MATCH_FOUND" -> PlixoPrimary
        "CAPTAIN_APPROVED" -> BlockLimeFg
        "CAPTAIN_REJECTED", "SESSION_CANCELLED" -> BlockSkyFg
        else -> PlixoText2
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!n.read) PlixoPrimaryLight.copy(0.3f) else PlixoSurface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(iconBg, PlixoShape.Icon),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconFg, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(n.title, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
            Spacer(Modifier.height(2.dp))
            Text(n.body, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2, lineHeight = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(n.createdAt.take(10), fontFamily = PlusJakartaSans, fontSize = 11.sp, color = PlixoText3)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 74.dp)
            .height(1.dp)
            .background(PlixoBorder),
    )
}

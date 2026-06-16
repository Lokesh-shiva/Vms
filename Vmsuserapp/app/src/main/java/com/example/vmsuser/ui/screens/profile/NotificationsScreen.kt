package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
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
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel

@Composable
fun NotificationsScreen(navController: NavController) {
    val vm: ProfileViewModel = viewModel()
    val notifications by vm.notifications.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Notifications", onBack = { navController.popBackStack() })
        LazyColumn {
            items(notifications) { n ->
                NotificationRow(n)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun NotificationRow(n: Notification) {
    val icon = when (n.type) {
        "match_found" -> Icons.Filled.SportsScore
        "society_invite" -> Icons.Filled.Group
        "coin_earned" -> Icons.Filled.MonetizationOn
        else -> Icons.Filled.Notifications
    }
    val iconBg = when (n.type) {
        "match_found" -> PlixoPrimaryLight
        "society_invite" -> BlockSkyBg
        "coin_earned" -> BlockLimeBg
        else -> PlixoSurface2
    }
    val iconFg = when (n.type) {
        "match_found" -> PlixoPrimary
        "society_invite" -> BlockSkyFg
        "coin_earned" -> BlockLimeFg
        else -> PlixoText2
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!n.read) PlixoPrimaryLight.copy(0.3f) else PlixoSurface)
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

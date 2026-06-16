package com.example.vmsuser.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.SocialViewModel

@Composable
fun SocietiesScreen(navController: NavController) {
    val vm: SocialViewModel = viewModel()
    val societies by vm.societies.collectAsState()
    val joinedIds by vm.joinedIds.collectAsState()
    val canCreate = UserSession.canCreateSociety

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(
            title = "Societies",
            actions = {
                if (canCreate) {
                    IconButton(onClick = { navController.navigate(Screen.CreateSociety.route) }) {
                        Icon(Icons.Filled.Add, "Create society", tint = PlixoPrimary)
                    }
                }
            }
        )
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(societies) { s ->
                val color = sportColor(s.sport)
                val isMember = s.isMember || s.id in joinedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlixoSurface, PlixoShape.Card)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            vm.select(s.id)
                            navController.navigate(Screen.SocietyDetail.create(s.id))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Sport color circle
                    Box(modifier = Modifier.size(48.dp).clip(PlixoShape.Icon).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(s.sport.take(1), fontFamily = BricolageGrotesque, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.name, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Group, null, tint = PlixoText3, modifier = Modifier.size(12.dp))
                            Text("${s.memberCount} members", fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText2)
                        }
                    }
                    if (isMember) {
                        PlixoPill("Joined", bg = BlockMintBg, fg = BlockMintFg)
                    } else {
                        PlixoButton(
                            label = "Join",
                            onClick = { vm.join(s.id) },
                            variant = PlixoButtonVariant.Soft,
                            fullWidth = false,
                            modifier = Modifier.wrapContentWidth(),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

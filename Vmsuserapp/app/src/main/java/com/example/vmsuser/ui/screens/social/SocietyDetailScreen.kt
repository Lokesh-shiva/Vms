package com.example.vmsuser.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.models.SocietyMember
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.SocialViewModel

@Composable
fun SocietyDetailScreen(navController: NavController, id: Int) {
    val vm: SocialViewModel = viewModel()
    val selected by vm.selected.collectAsState()
    val members by vm.members.collectAsState()
    val leaderboard by vm.leaderboard.collectAsState()
    val joinedIds by vm.joinedIds.collectAsState()

    LaunchedEffect(id) { vm.select(id) }
    val s = selected ?: return
    val isMember = s.isMember || id in joinedIds
    val color = sportColor(s.sport)

    LazyColumn(modifier = Modifier.fillMaxSize().background(PlixoBg)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().background(PlixoInk).statusBarsPadding().padding(bottom = 24.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    PlixoTopBar(title = "", onBack = { navController.popBackStack() })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp).background(color.copy(0.2f), PlixoShape.Icon), contentAlignment = Alignment.Center) {
                            Text(s.sport.take(1), fontFamily = BricolageGrotesque, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = color)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(s.name, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White, letterSpacing = (-0.5).sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Group, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(14.dp))
                                Text("${s.memberCount} members · ${s.sport}", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = Color.White.copy(0.6f))
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // About
                Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                    Text("About", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlixoText)
                    Spacer(Modifier.height(6.dp))
                    Text(s.description, fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText2, lineHeight = 20.sp)
                }
                // Members preview
                if (members.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                        Text("Members", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlixoText)
                        Spacer(Modifier.height(12.dp))
                        AvatarStack(names = members.map { it.name }, size = 40.dp)
                    }
                } else if (s.memberCount > 0) {
                    Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                        Text("Members", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlixoText)
                        Spacer(Modifier.height(8.dp))
                        Text("${s.memberCount} members", fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText2)
                    }
                }
                // Leaderboard
                if (leaderboard.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                        Text("Leaderboard", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlixoText)
                        Spacer(Modifier.height(10.dp))
                        leaderboard.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "#${index + 1}",
                                    fontFamily = BricolageGrotesque,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PlixoText3,
                                    modifier = Modifier.width(32.dp),
                                )
                                Text(
                                    entry.name.ifBlank { "Player #${entry.userId}" },
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = PlixoText,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${entry.totalPoints} pts",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = color,
                                )
                            }
                        }
                    }
                }
                // Join/Leave CTA
                if (isMember) {
                    PlixoButton("Leave society", onClick = { vm.leave(id); navController.popBackStack() }, variant = PlixoButtonVariant.DangerSoft)
                } else {
                    PlixoButton("Join ${s.name}", onClick = { vm.join(id) })
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

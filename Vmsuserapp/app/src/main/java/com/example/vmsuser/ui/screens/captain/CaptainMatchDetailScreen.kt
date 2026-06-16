package com.example.vmsuser.ui.screens.captain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*

@Composable
fun CaptainMatchDetailScreen(navController: NavController, matchId: Int) {
    val players = listOf("Aarav Mehta", "Priya Sharma", "Karthik R", "Divya M", "Rohan V", "Sana K")
    var checkedIn by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Match #$matchId", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PlixoSurface, PlixoShape.Card)
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.LocationOn, null, tint = PlixoPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        "Kanteerava Annex · Today 6 PM",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PlixoText,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PlixoSurface, PlixoShape.Card)
                    .padding(16.dp),
            ) {
                Text(
                    "Players",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PlixoText,
                )
                Spacer(Modifier.height(12.dp))
                players.forEach { player ->
                    val checked = player in checkedIn
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlixoAvatar(name = player, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            player,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = PlixoText,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = checked,
                            onCheckedChange = {
                                checkedIn = if (it) checkedIn + player else checkedIn - player
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PlixoPrimary,
                            ),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                label = { Text("Match notes") },
                placeholder = { Text("Any issues or observations…") },
                shape = PlixoShape.Input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlixoPrimary,
                    unfocusedBorderColor = PlixoBorder,
                    focusedContainerColor = PlixoSurface2,
                    unfocusedContainerColor = PlixoSurface2,
                ),
            )

            if (finished) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BlockMintBg, PlixoShape.Card)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Match finished! ✓",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BlockMintFg,
                    )
                }
            } else {
                PlixoButton("Finish match & record result", onClick = { finished = true })
            }
        }
    }
}

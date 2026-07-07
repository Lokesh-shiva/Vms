package com.example.vmsuser.ui.screens.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.CaptainViewModel

private val ALL_SPORTS = listOf("Cricket", "Football", "Badminton", "Basketball", "Tennis", "Volleyball")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptainApplicationScreen(navController: NavController) {
    val vm: CaptainViewModel = viewModel()
    val applying by vm.applying.collectAsState()
    val error by vm.error.collectAsState()
    var step by remember { mutableIntStateOf(1) }
    var selectedSports by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bio by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding()) {
        PlixoTopBar(
            title = "Captain Application",
            onBack = { if (step > 1) step-- else navController.popBackStack() },
        )
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(4.dp)
                .background(PlixoSurface2, PlixoShape.Pill),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(step / 3f)
                    .fillMaxHeight()
                    .background(PlixoPrimary, PlixoShape.Pill),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            when (step) {
                1 -> {
                    Text(
                        "Which sports do\nyou captain?",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = PlixoText,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 30.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ALL_SPORTS.forEach { sport ->
                            SportChip(sport, selected = sport in selectedSports) { s ->
                                selectedSports = if (s in selectedSports) selectedSports - s else selectedSports + s
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    PlixoButton("Continue", onClick = { step = 2 }, enabled = selectedSports.isNotEmpty())
                }
                2 -> {
                    Text(
                        "Tell us about\nyourself",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = PlixoText,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 30.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Why do you want to be a Plixo captain?",
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        color = PlixoText2,
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        label = { Text("Your bio") },
                        placeholder = { Text("e.g. I've been playing cricket for 10 years and love organising matches…") },
                        shape = PlixoShape.Input,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlixoPrimary,
                            unfocusedBorderColor = PlixoBorder,
                            focusedContainerColor = PlixoSurface2,
                            unfocusedContainerColor = PlixoSurface2,
                        ),
                        maxLines = 5,
                    )
                    Spacer(Modifier.height(24.dp))
                    PlixoButton("Continue", onClick = { step = 3 }, enabled = bio.length >= 30)
                }
                3 -> {
                    Text(
                        "Review &\nSubmit",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = PlixoText,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 30.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PlixoSurface2, PlixoShape.Card)
                            .padding(16.dp),
                    ) {
                        Text(
                            "Sports: ${selectedSports.joinToString(", ")}",
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                            color = PlixoText2,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Bio: $bio", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = agreedToTerms,
                            onCheckedChange = { agreedToTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = PlixoPrimary),
                        )
                        Text(
                            "I agree to the Captain Code of Conduct",
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                            color = PlixoText2,
                        )
                    }
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(error!!, color = PlixoDanger, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    PlixoButton(
                        label = if (applying) "Submitting…" else "Submit application",
                        onClick = {
                            vm.apply(bio) {
                                navController.navigate(Screen.KycIntro.route)
                            }
                        },
                        enabled = agreedToTerms && !applying,
                    )
                }
            }
        }
    }
}

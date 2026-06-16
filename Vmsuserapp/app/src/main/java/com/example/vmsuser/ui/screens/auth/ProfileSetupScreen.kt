package com.example.vmsuser.ui.screens.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.data.AuthRepository
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import kotlinx.coroutines.launch

private val ALL_SPORTS = listOf(
    "Cricket", "Football", "Badminton", "Basketball", "Tennis",
    "Swimming", "Volleyball", "Running", "Pickleball", "Table Tennis",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(navController: NavController) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var sports by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }

    val totalSteps = 2
    val progress by animateFloatAsState(
        targetValue = step.toFloat() / totalSteps,
        animationSpec = tween(400),
        label = "progress",
    )

    fun submit() {
        loading = true
        errorMsg = null
        scope.launch {
            repo.completeProfile(
                name = name.trim(),
                dateOfBirth = dob.trim().ifBlank { null },
                city = city.trim(),
                sportPreferences = sports.toList(),
                profilePhotoUrl = null,
            ).onSuccess { user ->
                UserSession.setUser(user)
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }.onFailure { e ->
                errorMsg = e.message ?: "Could not save profile. Try again."
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding(),
    ) {
        PlixoTopBar(
            title = "",
            onBack = { if (step == 1) navController.popBackStack() else step = 1 },
            actions = {
                Text(
                    "$step/$totalSteps",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PlixoText3,
                )
            }
        )

        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(5.dp)
                .background(PlixoSurface2, PlixoShape.Pill),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .background(PlixoPrimary, PlixoShape.Pill),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()).padding(24.dp),
        ) {
            if (step == 1) {
                // ── Step 1: Name + DOB + City ─────────────────────────
                Text(
                    "Set up your profile",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = PlixoText,
                    letterSpacing = (-0.8).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text("Tell us a little about yourself", fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText2)
                Spacer(Modifier.height(26.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full name") },
                    placeholder = { Text("e.g. Aarav Mehta") },
                    shape = PlixoShape.Input,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Date of birth · optional") },
                    placeholder = { Text("DD / MM / YYYY") },
                    shape = PlixoShape.Input,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("City / area") },
                    placeholder = { Text("e.g. Indiranagar, Bengaluru") },
                    shape = PlixoShape.Input,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    singleLine = true,
                )

                Spacer(Modifier.height(26.dp))
                PlixoButton(
                    "Continue",
                    onClick = { step = 2 },
                    enabled = name.isNotBlank() && city.isNotBlank(),
                )

            } else {
                // ── Step 2: Sport picker ───────────────────────────────
                Text(
                    "Pick your sports",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = PlixoText,
                    letterSpacing = (-0.8).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "We'll match you with the right players nearby",
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    color = PlixoText2,
                )
                Spacer(Modifier.height(24.dp))

                ALL_SPORTS.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { sport ->
                            SportChip(
                                sport = sport,
                                selected = sport in sports,
                                onSelect = { sports = if (it in sports) sports - it else sports + it },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(22.dp))

                if (sports.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(PlixoPrimaryLight, PlixoShape.SmCard).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✓", fontSize = 16.sp, color = PlixoPrimary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${sports.size} sport${if (sports.size > 1) "s" else ""} selected",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = PlixoPrimaryDark,
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
                    Spacer(Modifier.height(12.dp))
                }

                PlixoButton(
                    if (loading) "Saving…" else "Start playing",
                    onClick = { if (!loading) submit() },
                    enabled = sports.isNotEmpty() && !loading,
                )
                Spacer(Modifier.height(10.dp))
                PlixoButton(
                    "Skip for now",
                    onClick = { if (!loading) submit() },
                    variant = PlixoButtonVariant.Ghost,
                    enabled = !loading,
                )
            }
        }
    }
}

@Composable
private fun RowScope.SportChip(sport: String, selected: Boolean, onSelect: (String) -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .background(if (selected) PlixoInk else PlixoSurface2, PlixoShape.SmCard)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(sport) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            sport,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (selected) Color.White else PlixoText2,
        )
    }
}

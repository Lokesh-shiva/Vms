package com.example.vmsuser.ui.screens.auth

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.data.AuthRepository
import com.example.vmsuser.models.LocationOption
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.network.lastKnownLocation
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ALL_SPORTS = listOf(
    "Cricket", "Football", "Badminton", "Basketball", "Tennis",
    "Swimming", "Volleyball", "Running", "Pickleball", "Table Tennis",
)

private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private const val MIN_AGE_YEARS = 13

private fun isoDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(millis))

private fun displayDate(iso: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
    SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsed!!)
} catch (_: Exception) { iso }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(navController: NavController) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf("") }
    var areaExpanded by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf<List<LocationOption>>(emptyList()) }
    var sports by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository() }
    val context = LocalContext.current

    val usernameValid = username.isBlank() || USERNAME_REGEX.matches(username)
    val emailValid = email.isBlank() || EMAIL_REGEX.matches(email)

    var detectingLocation by remember { mutableStateOf(false) }
    var locationSuggestion by remember { mutableStateOf<LocationOption?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        photoUri = uri
        photoError = null
        uploadingPhoto = true
        scope.launch {
            repo.uploadProfilePhoto(context.contentResolver, uri)
                .onSuccess { user -> profilePhotoUrl = user.profilePhotoUrl }
                .onFailure { e -> photoError = e.message ?: "Couldn't upload photo. Try again." }
            uploadingPhoto = false
        }
    }

    fun detectNearestLocation() {
        val fix = lastKnownLocation(context)
        if (fix == null) {
            locationError = "Couldn't get your location. Pick your area manually."
            detectingLocation = false
            return
        }
        scope.launch {
            try {
                val res = RetrofitClient.api.getNearestLocations(fix.first, fix.second)
                val nearest = res.data?.firstOrNull()
                if (res.success && nearest != null) {
                    locationSuggestion = nearest
                } else {
                    locationError = "No serviceable area found near you yet."
                }
            } catch (_: Exception) {
                locationError = "Couldn't detect your area. Pick it manually."
            }
            detectingLocation = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            detectNearestLocation()
        } else {
            detectingLocation = false
            locationError = "Location permission denied. Pick your area manually."
        }
    }

    LaunchedEffect(Unit) {
        try {
            val res = RetrofitClient.api.getLocations()
            if (res.success && res.data != null) locations = res.data
        } catch (_: Exception) {}
    }

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
                username = username.trim(),
                email = email.trim().ifBlank { null },
                dateOfBirth = dob.trim().ifBlank { null },
                city = city.trim(),
                sportPreferences = sports.toList(),
                profilePhotoUrl = profilePhotoUrl,
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
                Spacer(Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(PlixoSurface2)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !uploadingPhoto,
                            ) { photoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (photoUri != null) {
                            AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.Person, null, tint = PlixoText3, modifier = Modifier.size(44.dp))
                        }
                        if (uploadingPhoto) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            }
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PlixoPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (photoError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        photoError!!,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        color = PlixoDanger,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(20.dp))

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
                    value = username,
                    onValueChange = { username = it.filter { c -> !c.isWhitespace() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    placeholder = { Text("e.g. aarav_10") },
                    shape = PlixoShape.Input,
                    isError = username.isNotBlank() && !usernameValid,
                    supportingText = {
                        if (username.isNotBlank() && !usernameValid) {
                            Text("3-20 characters: letters, numbers, underscore only.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        errorBorderColor = PlixoDanger,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email · optional") },
                    placeholder = { Text("you@example.com") },
                    shape = PlixoShape.Input,
                    isError = email.isNotBlank() && !emailValid,
                    supportingText = {
                        if (email.isNotBlank() && !emailValid) {
                            Text("Enter a valid email address.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlixoPrimary,
                        unfocusedBorderColor = PlixoBorder,
                        errorBorderColor = PlixoDanger,
                        focusedContainerColor = PlixoSurface2,
                        unfocusedContainerColor = PlixoSurface2,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showDatePicker = true },
                ) {
                    OutlinedTextField(
                        value = if (dob.isBlank()) "" else displayDate(dob),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Date of birth") },
                        placeholder = { Text("Used to find age-appropriate matches") },
                        shape = PlixoShape.Input,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = PlixoBorder,
                            disabledContainerColor = PlixoSurface2,
                            disabledTextColor = PlixoText,
                            disabledLabelColor = PlixoText3,
                            disabledPlaceholderColor = PlixoText3,
                        ),
                        singleLine = true,
                    )
                }
                if (showDatePicker) {
                    val maxMillis = remember {
                        Calendar.getInstance().apply { add(Calendar.YEAR, -MIN_AGE_YEARS) }.timeInMillis
                    }
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = maxMillis,
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
                        },
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { dob = isoDate(it) }
                                showDatePicker = false
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        },
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !detectingLocation,
                    ) {
                        locationError = null
                        detectingLocation = true
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.MyLocation, null, tint = PlixoPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (detectingLocation) "Detecting your area…" else "Use my current location",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PlixoPrimary,
                    )
                }
                if (locationError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(locationError!!, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoText3)
                }
                locationSuggestion?.let { suggestion ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(PlixoPrimaryLight, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Detected: ${suggestion.name}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = PlixoPrimaryDark,
                            )
                            Text(
                                "Not right? Pick another area below.",
                                fontFamily = PlusJakartaSans,
                                fontSize = 11.sp,
                                color = PlixoText3,
                            )
                        }
                        TextButton(onClick = {
                            city = suggestion.name
                            locationSuggestion = null
                        }) { Text("Use this") }
                    }
                }
                Spacer(Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = areaExpanded,
                    onExpandedChange = { areaExpanded = it },
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Your area") },
                        placeholder = { Text(if (locations.isEmpty()) "Loading areas…" else "Select your area") },
                        shape = PlixoShape.Input,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlixoPrimary,
                            unfocusedBorderColor = PlixoBorder,
                            focusedContainerColor = PlixoSurface2,
                            unfocusedContainerColor = PlixoSurface2,
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded) },
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = areaExpanded && locations.isNotEmpty(),
                        onDismissRequest = { areaExpanded = false },
                    ) {
                        locations.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc.name) },
                                onClick = { city = loc.name; areaExpanded = false },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                PlixoButton(
                    "Continue",
                    onClick = { step = 2 },
                    enabled = name.isNotBlank() && city.isNotBlank() &&
                        username.isNotBlank() && usernameValid && emailValid,
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

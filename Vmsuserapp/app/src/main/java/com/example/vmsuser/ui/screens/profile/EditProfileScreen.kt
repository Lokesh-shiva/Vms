package com.example.vmsuser.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.data.AuthRepository
import com.example.vmsuser.models.LocationOption
import com.example.vmsuser.network.RetrofitClient
import com.example.vmsuser.network.absoluteMediaUrl
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
fun EditProfileScreen(navController: NavController) {
    val vm: ProfileViewModel = viewModel()
    val user by vm.user.collectAsStateWithLifecycle()
    val updating by vm.updating.collectAsState()
    val updateError by vm.updateError.collectAsState()

    var name by remember { mutableStateOf(user?.name ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var dob by remember { mutableStateOf(user?.dateOfBirth ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf(user?.city ?: user?.region ?: "") }
    var areaExpanded by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf<List<LocationOption>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authRepo = remember { AuthRepository() }

    var photoUrl by remember { mutableStateOf(user?.profilePhotoUrl) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }

    val usernameValid = username.isBlank() || USERNAME_REGEX.matches(username)
    val emailValid = email.isBlank() || EMAIL_REGEX.matches(email)

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        photoError = null
        uploadingPhoto = true
        scope.launch {
            authRepo.uploadProfilePhoto(context.contentResolver, uri)
                .onSuccess { photoUrl = it.profilePhotoUrl }
                .onFailure { e -> photoError = e.message ?: "Couldn't upload photo. Try again." }
            uploadingPhoto = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            val res = RetrofitClient.api.getLocations()
            if (res.success && res.data != null) locations = res.data
        } catch (_: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding()) {
        PlixoTopBar(title = "Edit Profile", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(PlixoSurface2)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !uploadingPhoto,
                        ) { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    val resolvedPhotoUrl = absoluteMediaUrl(photoUrl)
                    if (resolvedPhotoUrl != null) {
                        AsyncImage(model = resolvedPhotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, null, tint = PlixoText3, modifier = Modifier.size(40.dp))
                    }
                    if (uploadingPhoto) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(PlixoPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
            }
            if (photoError != null) {
                Spacer(Modifier.height(6.dp))
                Text(photoError!!, fontFamily = PlusJakartaSans, fontSize = 12.sp, color = PlixoDanger, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(22.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full name") },
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
                val initialMillis = remember {
                    try {
                        if (dob.isNotBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dob)!!.time else maxMillis
                    } catch (_: Exception) { maxMillis }
                }
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialMillis,
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

            ExposedDropdownMenuBox(
                expanded = areaExpanded,
                onExpandedChange = { areaExpanded = it },
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Your area") },
                    placeholder = { Text(if (locations.isEmpty()) "Loading areas…" else "Select your area") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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

            if (updateError != null) {
                Spacer(Modifier.height(12.dp))
                Text(updateError!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
            }

            Spacer(Modifier.height(26.dp))
            PlixoButton(
                label = if (updating) "Saving…" else "Save changes",
                onClick = {
                    vm.updateProfile(
                        name = name,
                        region = city,
                        username = username.trim().ifBlank { null },
                        email = email.trim().ifBlank { null },
                        dateOfBirth = dob.ifBlank { null },
                    ) {
                        navController.popBackStack()
                    }
                },
                enabled = name.isNotBlank() && !updating && usernameValid && emailValid,
            )
        }
    }
}

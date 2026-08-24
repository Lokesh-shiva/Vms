package com.example.vmsuser.ui.screens.trainers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.absoluteMediaUrl
import com.example.vmsuser.ui.components.PlixoButton
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.TrainerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerDetailScreen(navController: NavController, id: Int) {
    val parentEntry = remember(navController) { navController.getBackStackEntry("trainer_graph") }
    val vm: TrainerViewModel = viewModel(parentEntry)

    val trainer by vm.selectedTrainer.collectAsState()
    val booking by vm.booking.collectAsState()
    val error by vm.bookingError.collectAsState()

    LaunchedEffect(id) { vm.selectTrainer(id) }

    var sessionDate by remember { mutableStateOf("") }
    var sessionTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val t = trainer ?: run {
        Box(modifier = Modifier.fillMaxSize().background(PlixoBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PlixoPrimary)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).verticalScroll(rememberScrollState())) {
        PlixoTopBar(title = "Coach", onBack = { navController.popBackStack() })

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val url = absoluteMediaUrl(t.imageUrl)
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(PlixoSurface2),
                    contentAlignment = Alignment.Center,
                ) {
                    if (url != null) {
                        AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.Person, null, tint = PlixoText3, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(t.name, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PlixoText)
                    if (t.specialties.isNotBlank()) {
                        Text(t.specialties, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
                    }
                    Text(
                        "₹${"%.0f".format(t.ratePerSession)} / session",
                        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PlixoPrimary,
                    )
                }
            }

            if (t.bio.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                    Text("About", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoText)
                    Spacer(Modifier.height(6.dp))
                    Text(t.bio, fontFamily = PlusJakartaSans, fontSize = 13.5.sp, color = PlixoText2, lineHeight = 19.sp)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().background(PlixoSurface, PlixoShape.Card).padding(16.dp)) {
                Text("Book a session", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoText)
                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().clickable(enabled = !booking) { showDatePicker = true }) {
                    OutlinedTextField(
                        value = if (sessionDate.isBlank()) "" else displayDate(sessionDate),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Date") },
                        placeholder = { Text("Pick a date") },
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
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().clickable(enabled = !booking) { showTimePicker = true }) {
                    OutlinedTextField(
                        value = sessionTime,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Time") },
                        placeholder = { Text("Pick a time") },
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

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
                }

                Spacer(Modifier.height(16.dp))
                PlixoButton(
                    if (booking) "Booking…" else "Book for ₹${"%.0f".format(t.ratePerSession)}",
                    onClick = {
                        vm.bookSession(t.id, sessionDate, sessionTime) { result ->
                            navController.navigate(Screen.TrainerBookingPayment.create(result.id)) {
                                popUpTo(Screen.Trainers.route)
                            }
                        }
                    },
                    enabled = !booking && sessionDate.isNotBlank() && sessionTime.isNotBlank(),
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }

    if (showDatePicker) {
        val minMillis = remember { Calendar.getInstance().timeInMillis }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = minMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis - 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { sessionDate = isoDate(it) }
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 18, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    sessionTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

private fun isoDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(millis))

private fun displayDate(iso: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
    SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsed!!)
} catch (_: Exception) { iso }

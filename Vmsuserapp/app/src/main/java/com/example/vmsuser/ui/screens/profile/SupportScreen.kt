package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.models.Dispute
import com.example.vmsuser.ui.components.PlixoButton
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.SupportViewModel

@Composable
fun SupportScreen(navController: NavController) {
    val vm: SupportViewModel = viewModel()
    val tickets by vm.tickets.collectAsState()
    val loading by vm.loading.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val error by vm.error.collectAsState()
    var showNewTicketForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadTickets() }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Help & Support", onBack = { navController.popBackStack() })

        if (showNewTicketForm) {
            NewTicketForm(
                submitting = submitting,
                error = error,
                onSubmit = { title, description ->
                    vm.raiseTicket(title, description) { showNewTicketForm = false }
                },
                onCancel = { showNewTicketForm = false; vm.clearError() },
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                PlixoButton("Raise a new ticket", onClick = { showNewTicketForm = true })
                Spacer(Modifier.height(20.dp))
                Text(
                    "My Tickets",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PlixoText,
                )
                Spacer(Modifier.height(12.dp))

                when {
                    loading && tickets.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlixoPrimary)
                    }
                    tickets.isEmpty() -> Text(
                        "No support tickets yet.",
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        color = PlixoText2,
                    )
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(tickets, key = { it.id }) { ticket -> TicketCard(ticket) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewTicketForm(
    submitting: Boolean,
    error: String?,
    onSubmit: (title: String, description: String) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "What's going on?",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = PlixoText,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            placeholder = { Text("e.g. Payment not reflecting") },
            shape = PlixoShape.Input,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlixoPrimary,
                unfocusedBorderColor = PlixoBorder,
                focusedContainerColor = PlixoSurface2,
                unfocusedContainerColor = PlixoSurface2,
            ),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            label = { Text("Description") },
            placeholder = { Text("Tell us what happened, with as much detail as you can.") },
            shape = PlixoShape.Input,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlixoPrimary,
                unfocusedBorderColor = PlixoBorder,
                focusedContainerColor = PlixoSurface2,
                unfocusedContainerColor = PlixoSurface2,
            ),
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        PlixoButton(
            if (submitting) "Submitting…" else "Submit ticket",
            onClick = { onSubmit(title.trim(), description.trim()) },
            enabled = title.isNotBlank() && description.trim().length >= 10 && !submitting,
        )
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onCancel, enabled = !submitting) { Text("Cancel") }
    }
}

@Composable
private fun TicketCard(ticket: Dispute) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlixoSurface2, PlixoShape.Card)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                ticket.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PlixoText,
                modifier = Modifier.weight(1f),
            )
            TicketStatusBadge(ticket.status)
        }
        Spacer(Modifier.height(6.dp))
        Text(ticket.description, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2, lineHeight = 18.sp)
        if (!ticket.resolutionNote.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Resolution: ${ticket.resolutionNote}",
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                color = PlixoText2,
            )
        }
        ticket.createdAt?.let {
            Spacer(Modifier.height(6.dp))
            Text(it.take(10), fontFamily = PlusJakartaSans, fontSize = 11.sp, color = PlixoText3)
        }
    }
}

@Composable
private fun TicketStatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "RESOLVED", "CLOSED" -> BlockLimeBg to BlockLimeFg
        "IN_PROGRESS" -> BlockSkyBg to BlockSkyFg
        else -> PlixoPrimaryLight to PlixoPrimary
    }
    Box(modifier = Modifier.background(bg, PlixoShape.Pill).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(status, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = fg)
    }
}

package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.models.DisputeMessage
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.SupportViewModel
import kotlinx.coroutines.launch

@Composable
fun TicketDetailScreen(navController: NavController, ticketId: Int) {
    val vm: SupportViewModel = viewModel()
    val messages by vm.messages.collectAsState()
    val messagesError by vm.messagesError.collectAsState()
    val sending by vm.sending.collectAsState()
    val tickets by vm.tickets.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val selfId = UserSession.currentUser?.id

    LaunchedEffect(ticketId) { vm.openThread(ticketId) }
    DisposableEffect(ticketId) { onDispose { vm.stopPolling() } }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    val ticket = tickets.find { it.id == ticketId }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg)) {
        Box(modifier = Modifier.background(PlixoSurface).statusBarsPadding()) {
            PlixoTopBar(title = ticket?.title ?: "Ticket", onBack = { navController.popBackStack() })
        }
        if (messagesError != null && messages.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(messagesError ?: "", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ticket != null) {
                item { TicketSummary(ticket.description) }
            }
            items(messages, key = { it.id }) { msg -> MessageBubble(msg, isSelf = msg.senderId != null && msg.senderId == selfId) }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(PlixoSurface).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).background(PlixoSurface2, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = TextStyle(fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText),
                cursorBrush = SolidColor(PlixoPrimary),
                enabled = !sending,
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Reply…", fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText3)
                    inner()
                },
            )
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = { if (text.isNotBlank() && !sending) { vm.sendMessage(ticketId, text); text = "" } },
                modifier = Modifier.size(44.dp).background(if (text.isNotBlank() && !sending) PlixoPrimary else PlixoSurface2, RoundedCornerShape(14.dp)),
            ) {
                Icon(Icons.Filled.Send, "Send", tint = if (text.isNotBlank() && !sending) Color.White else PlixoText3, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TicketSummary(description: String) {
    Box(
        modifier = Modifier.fillMaxWidth().background(PlixoSurface2, RoundedCornerShape(14.dp)).padding(14.dp),
    ) {
        Text(description, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2, lineHeight = 18.sp)
    }
}

@Composable
private fun MessageBubble(msg: DisputeMessage, isSelf: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start) {
            if (!isSelf) {
                Text(
                    if (msg.senderRole != null && msg.senderRole != "user") "${msg.senderName} · Support" else msg.senderName,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                    color = PlixoText3,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .background(if (isSelf) PlixoPrimary else PlixoSurface, RoundedCornerShape(18.dp, 18.dp, if (isSelf) 4.dp else 18.dp, if (isSelf) 18.dp else 4.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(msg.body, fontFamily = PlusJakartaSans, fontSize = 14.sp, color = if (isSelf) Color.White else PlixoText)
            }
            Text(
                msg.createdAt?.takeLast(8)?.take(5) ?: "",
                fontFamily = PlusJakartaSans,
                fontSize = 10.sp,
                color = PlixoText3,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

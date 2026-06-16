package com.example.vmsuser.ui.screens.chat

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
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatThreadScreen(navController: NavController, threadId: String) {
    val vm: ChatViewModel = viewModel()
    val messages by vm.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(threadId) { vm.loadThread(threadId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    val thread = vm.threads.collectAsState().value.find { it.id == threadId }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg)) {
        Box(modifier = Modifier.background(PlixoSurface).statusBarsPadding()) {
            PlixoTopBar(title = thread?.name ?: "Chat", onBack = { navController.popBackStack() })
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { msg ->
                MessageBubble(msg.text, msg.fromName, msg.isSelf, msg.timestamp)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        // Input bar
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
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Type a message…", fontFamily = PlusJakartaSans, fontSize = 14.sp, color = PlixoText3)
                    inner()
                },
            )
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = { if (text.isNotBlank()) { vm.sendMessage(text); text = "" } },
                modifier = Modifier.size(44.dp).background(if (text.isNotBlank()) PlixoPrimary else PlixoSurface2, RoundedCornerShape(14.dp)),
            ) {
                Icon(Icons.Filled.Send, "Send", tint = if (text.isNotBlank()) Color.White else PlixoText3, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(text: String, fromName: String, isSelf: Boolean, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start) {
            if (!isSelf) {
                Text(fromName, fontFamily = PlusJakartaSans, fontSize = 11.sp, color = PlixoText3, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            Box(
                modifier = Modifier
                    .background(if (isSelf) PlixoPrimary else PlixoSurface, RoundedCornerShape(18.dp, 18.dp, if (isSelf) 4.dp else 18.dp, if (isSelf) 18.dp else 4.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text, fontFamily = PlusJakartaSans, fontSize = 14.sp, color = if (isSelf) Color.White else PlixoText)
            }
            Text(time, fontFamily = PlusJakartaSans, fontSize = 10.sp, color = PlixoText3, modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
        }
    }
}

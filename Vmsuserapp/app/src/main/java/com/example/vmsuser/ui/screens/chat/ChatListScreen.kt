package com.example.vmsuser.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.ChatViewModel

@Composable
fun ChatListScreen(navController: NavController) {
    val vm: ChatViewModel = viewModel()
    val threads by vm.threads.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Chats")
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(threads) { thread ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlixoSurface)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            vm.loadThread(thread.id)
                            navController.navigate(Screen.ChatThread.create(thread.id))
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlixoAvatar(name = thread.name, size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(thread.name, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlixoText)
                        Spacer(Modifier.height(2.dp))
                        Text(thread.lastMessage, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2, maxLines = 1)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(thread.lastMessageTime, fontFamily = PlusJakartaSans, fontSize = 11.sp, color = PlixoText3)
                        if (thread.unreadCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier.size(20.dp).background(PlixoPrimary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(thread.unreadCount.toString(), fontSize = 11.sp, color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().padding(start = 82.dp).height(1.dp).background(PlixoBorder))
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

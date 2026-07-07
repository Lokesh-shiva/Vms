package com.example.vmsuser.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.UserSession
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.components.SectionHeader
import com.example.vmsuser.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete account?",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            },
            text = {
                Text(
                    "This will permanently delete your account and all your data. This action cannot be undone.",
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    color = PlixoText2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    UserSession.clear()
                    navController.navigate(Screen.Phone.route) { popUpTo(0) { inclusive = true } }
                }) {
                    Text("Delete", color = PlixoDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Settings", onBack = { navController.popBackStack() })
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            SectionHeader("General")
            SettingsItem(Icons.Outlined.Notifications, "Notifications") {}
            SettingsItem(Icons.Outlined.Lock, "Privacy") {}
            SettingsItem(Icons.Outlined.Language, "Language") {}

            SectionHeader("Support")
            SettingsItem(Icons.Outlined.Info, "About Plixo") {}
            SettingsItem(Icons.Outlined.Description, "Terms & Privacy") {}
            SettingsItem(Icons.Outlined.HelpOutline, "Help & Support") {
                navController.navigate(Screen.Support.route)
            }

            SectionHeader("Account")
            SettingsItem(Icons.Outlined.Logout, "Sign out", tint = PlixoDanger) {
                UserSession.clear()
                navController.navigate(Screen.Phone.route) { popUpTo(0) { inclusive = true } }
            }
            SettingsItem(Icons.Outlined.DeleteForever, "Delete account", tint = PlixoDanger) {
                showDeleteDialog = true
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    tint: Color = PlixoText2,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlixoSurface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (tint == PlixoDanger) PlixoDanger else PlixoText,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ChevronRight, null, tint = PlixoText3, modifier = Modifier.size(18.dp))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp)
            .height(1.dp)
            .background(PlixoBorder),
    )
}

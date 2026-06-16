package com.example.vmsuser.ui.screens.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*

@Composable
fun KycUploadScreen(navController: NavController) {
    var idUploaded by remember { mutableStateOf(false) }
    var selfieUploaded by remember { mutableStateOf(false) }
    var addressUploaded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding()) {
        PlixoTopBar(title = "Upload Documents", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            UploadSlot("Government ID", "Aadhaar, PAN, or Passport", idUploaded) { idUploaded = !idUploaded }
            UploadSlot("Selfie Photo", "Clear front-facing photo", selfieUploaded) { selfieUploaded = !selfieUploaded }
            UploadSlot("Address Proof", "Utility bill or bank statement", addressUploaded) { addressUploaded = !addressUploaded }
            Spacer(Modifier.height(12.dp))
            PlixoButton(
                label = "Submit for review",
                onClick = { navController.navigate(Screen.KycSubmitted.route) },
                enabled = idUploaded && selfieUploaded && addressUploaded,
            )
        }
    }
}

@Composable
private fun UploadSlot(title: String, subtitle: String, uploaded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (uploaded) BlockMintBg else PlixoSurface2, PlixoShape.Card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (uploaded) BlockMintFg.copy(0.15f) else PlixoBorder, PlixoShape.Icon),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (uploaded) Icons.Filled.CheckCircle else Icons.Outlined.CloudUpload,
                null,
                tint = if (uploaded) BlockMintFg else PlixoText3,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (uploaded) BlockMintFg else PlixoText,
            )
            Text(
                subtitle,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                color = if (uploaded) BlockMintFg.copy(0.7f) else PlixoText2,
            )
        }
        TextButton(onClick = onToggle) {
            Text(
                if (uploaded) "Change" else "Upload",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (uploaded) BlockMintFg else PlixoPrimary,
            )
        }
    }
}

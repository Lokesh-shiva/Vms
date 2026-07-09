package com.example.vmsuser.ui.screens.kyc

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.ui.components.*
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.CaptainViewModel

private val DOCUMENT_TYPES = listOf("AADHAAR" to "Aadhaar", "PAN" to "PAN Card", "DRIVING_LICENSE" to "Driving License")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycUploadScreen(navController: NavController) {
    val vm: CaptainViewModel = viewModel()
    val uploading by vm.uploading.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    var documentType by remember { mutableStateOf("AADHAAR") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    Column(modifier = Modifier.fillMaxSize().background(PlixoSurface).statusBarsPadding()) {
        PlixoTopBar(title = "Upload ID Document", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "We use this to verify you're really you before activating your captain account.",
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                color = PlixoText2,
                lineHeight = 19.sp,
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
            ) {
                OutlinedTextField(
                    value = DOCUMENT_TYPES.first { it.first == documentType }.second,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Document type") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = PlixoShape.Input,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    DOCUMENT_TYPES.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { documentType = value; typeExpanded = false })
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (imageUri != null) 200.dp else 140.dp)
                    .background(PlixoSurface2, PlixoShape.Card)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        imagePicker.launch("image/*")
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null) {
                    AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CloudUpload, null, tint = PlixoText3, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to select a photo", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoText2)
                    }
                }
            }
            if (imageUri != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = PlixoPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Photo selected", fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoPrimary)
                }
            }

            if (error != null) {
                Text(error!!, color = PlixoDanger, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }

            Spacer(Modifier.height(6.dp))
            PlixoButton(
                label = if (uploading) "Uploading…" else "Submit for review",
                onClick = {
                    val uri = imageUri ?: return@PlixoButton
                    vm.uploadKyc(context.contentResolver, documentType, uri) {
                        navController.navigate(Screen.KycSubmitted.route)
                    }
                },
                enabled = imageUri != null && !uploading,
            )
        }
    }
}

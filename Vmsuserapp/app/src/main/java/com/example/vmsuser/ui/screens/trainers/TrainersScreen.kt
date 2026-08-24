package com.example.vmsuser.ui.screens.trainers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsTennis
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
import com.example.vmsuser.models.Trainer
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.network.absoluteMediaUrl
import com.example.vmsuser.ui.components.PlixoTopBar
import com.example.vmsuser.ui.theme.*
import com.example.vmsuser.viewmodel.TrainerViewModel

@Composable
fun TrainersScreen(navController: NavController) {
    val parentEntry = remember(navController) { navController.getBackStackEntry("trainer_graph") }
    val vm: TrainerViewModel = viewModel(parentEntry)

    val trainers by vm.trainers.collectAsState()
    val loading by vm.trainersLoading.collectAsState()
    val error by vm.trainersError.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PlixoBg).statusBarsPadding()) {
        PlixoTopBar(title = "Coaches", onBack = { navController.popBackStack() })

        when {
            loading && trainers.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PlixoPrimary)
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, fontFamily = PlusJakartaSans, fontSize = 13.sp, color = PlixoDanger)
                }
            }
            trainers.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SportsTennis, null, tint = PlixoText3, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No coaches available yet", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PlixoText)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(trainers) { trainer ->
                        TrainerCard(
                            trainer = trainer,
                            onClick = {
                                vm.selectTrainer(trainer.id)
                                navController.navigate(Screen.TrainerDetail.create(trainer.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerCard(trainer: Trainer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PlixoSurface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val url = absoluteMediaUrl(trainer.imageUrl)
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(PlixoSurface2),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.Person, null, tint = PlixoText3, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(trainer.name, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PlixoText)
            if (trainer.specialties.isNotBlank()) {
                Text(trainer.specialties, fontFamily = PlusJakartaSans, fontSize = 12.5.sp, color = PlixoText2, maxLines = 1)
            }
        }
        Text(
            "₹${"%.0f".format(trainer.ratePerSession)}/session",
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = PlixoPrimary,
        )
    }
}

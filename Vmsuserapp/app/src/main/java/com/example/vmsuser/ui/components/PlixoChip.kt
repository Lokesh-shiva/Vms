package com.example.vmsuser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vmsuser.ui.theme.*

@Composable
fun SportChip(sport: String, selected: Boolean, onSelect: (String) -> Unit) {
    val color = sportColor(sport)
    val bg = if (selected) color else PlixoSurface2
    val fg = if (selected) Color.White else PlixoText2
    val border = if (selected) Color.Transparent else PlixoBorder

    Row(
        modifier = Modifier
            .clip(PlixoShape.Chip)
            .background(bg)
            .border(1.dp, border, PlixoShape.Chip)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onSelect(sport) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(sport, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg)
    }
}

@Composable
fun SkillLevelChip(level: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) PlixoInk else PlixoSurface2
    val fg = if (selected) Color.White else PlixoText2
    Box(
        modifier = Modifier
            .clip(PlixoShape.SmCard)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(level, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = fg)
    }
}

@Composable
fun PlixoPill(text: String, bg: Color = PlixoSurface2, fg: Color = PlixoText2) {
    Box(
        modifier = Modifier
            .clip(PlixoShape.Pill)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = fg)
    }
}

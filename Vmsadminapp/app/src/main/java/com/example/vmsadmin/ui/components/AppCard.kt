package com.example.vmsadmin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Glassmorphism effect: translucent background with a subtle gradient border
    val glassBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp, // soft shadow
            pressedElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            // Translucent background using theme surface for glass effect
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) 
        ),
        border = BorderStroke(1.dp, glassBrush)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.02f)) // subtle inner glow
                .padding(20.dp),
            content = content
        )
    }
}

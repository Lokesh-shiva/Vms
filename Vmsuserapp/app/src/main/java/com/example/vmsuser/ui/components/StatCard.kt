package com.example.vmsuser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vmsuser.ui.theme.*

enum class StatCardColor { Lime, Mint, Sky, Peach, Lilac, Rose }

fun StatCardColor.bg(): Color = when (this) {
    StatCardColor.Lime  -> BlockLimeBg
    StatCardColor.Mint  -> BlockMintBg
    StatCardColor.Sky   -> BlockSkyBg
    StatCardColor.Peach -> BlockPeachBg
    StatCardColor.Lilac -> BlockLilacBg
    StatCardColor.Rose  -> BlockRoseBg
}

fun StatCardColor.fg(): Color = when (this) {
    StatCardColor.Lime  -> BlockLimeFg
    StatCardColor.Mint  -> BlockMintFg
    StatCardColor.Sky   -> BlockSkyFg
    StatCardColor.Peach -> BlockPeachFg
    StatCardColor.Lilac -> BlockLilacFg
    StatCardColor.Rose  -> BlockRoseFg
}

@Composable
fun StatCard(
    value: String,
    label: String,
    color: StatCardColor,
    icon: ImageVector? = null,
    suffix: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(10.dp, PlixoShape.Card, ambientColor = color.fg().copy(0.2f), spotColor = color.fg().copy(0.15f))
            .clip(PlixoShape.Card)
            .background(
                Brush.verticalGradient(
                    listOf(color.bg().copy(0.95f), color.bg().copy(0.82f))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(0.75f), Color.White.copy(0.25f))),
                PlixoShape.Card,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.fg().copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color.fg(), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
        if (suffix != null) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = color.fg(), letterSpacing = (-1).sp)) {
                        append(value)
                    }
                    withStyle(SpanStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color.fg())) {
                        append(" $suffix")
                    }
                },
                lineHeight = 30.sp,
            )
        } else {
            Text(
                text = value,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = color.fg(),
                letterSpacing = (-1).sp,
                lineHeight = 30.sp,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = color.fg().copy(alpha = 0.72f),
        )
    }
}

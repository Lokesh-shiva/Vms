package com.example.vmsuser.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vmsuser.ui.theme.*

enum class PlixoButtonVariant { Primary, Dark, Lime, Soft, Outline, Ghost, Danger, DangerSoft }

@Composable
fun PlixoButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PlixoButtonVariant = PlixoButtonVariant.Primary,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    val (bg, fg, borderColor) = when (variant) {
        PlixoButtonVariant.Primary    -> Triple(PlixoPrimary, Color.White, null)
        PlixoButtonVariant.Dark       -> Triple(PlixoInk, Color.White, null)
        PlixoButtonVariant.Lime       -> Triple(PlixoLime, PlixoLimeFg, null)
        PlixoButtonVariant.Soft       -> Triple(PlixoPrimaryLight, PlixoPrimary, null)
        PlixoButtonVariant.Outline    -> Triple(Color.Transparent, PlixoPrimary, PlixoPrimary)
        PlixoButtonVariant.Ghost      -> Triple(Color.Transparent, PlixoText2, null)
        PlixoButtonVariant.Danger     -> Triple(PlixoDanger, Color.White, null)
        PlixoButtonVariant.DangerSoft -> Triple(PlixoDangerLight, PlixoDanger, null)
    }
    val finalBg = if (enabled) bg else PlixoSurface3
    val finalFg = if (enabled) fg else PlixoText3

    val baseModifier = if (fullWidth) modifier.fillMaxWidth() else modifier
    val borderedModifier = if (borderColor != null)
        baseModifier.border(1.5.dp, if (enabled) borderColor else PlixoBorder, PlixoShape.Button)
    else baseModifier

    Button(
        onClick = onClick,
        modifier = borderedModifier.height(56.dp),
        enabled = enabled,
        shape = PlixoShape.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = finalBg,
            contentColor = finalFg,
            disabledContainerColor = finalBg.copy(alpha = 0.5f),
            disabledContentColor = finalFg.copy(alpha = 0.5f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = finalFg,
        )
    }
}

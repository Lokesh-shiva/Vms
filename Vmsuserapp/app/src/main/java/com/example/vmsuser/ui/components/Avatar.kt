package com.example.vmsuser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.vmsuser.ui.theme.*

@Composable
fun PlixoAvatar(
    name: String,
    imageUrl: String? = null,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
) {
    val initials = name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(PlixoPrimaryLight)
            .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = initials,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.35f).sp,
                color = PlixoPrimaryDark,
            )
        }
    }
}

@Composable
fun AvatarStack(names: List<String>, size: Dp = 34.dp, max: Int = 4) {
    val visible = names.take(max)
    Row {
        visible.forEachIndexed { i, name ->
            Box(modifier = Modifier.offset(x = (-i * (size.value * 0.4f)).dp)) {
                PlixoAvatar(name = name, size = size)
            }
        }
        if (names.size > max) {
            Box(
                modifier = Modifier
                    .offset(x = (-(visible.size) * (size.value * 0.4f)).dp)
                    .size(size)
                    .clip(CircleShape)
                    .background(PlixoSurface3),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+${names.size - max}",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.3f).sp,
                    color = PlixoText2,
                )
            }
        }
    }
}

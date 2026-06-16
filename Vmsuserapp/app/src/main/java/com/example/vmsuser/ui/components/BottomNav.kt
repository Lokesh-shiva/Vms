package com.example.vmsuser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.vmsuser.config.FeatureFlags
import com.example.vmsuser.navigation.Screen
import com.example.vmsuser.navigation.parentTabRoute
import com.example.vmsuser.ui.theme.*

data class NavTab(
    val route: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
    val label: String,
)

private val GlassInk = Color(0xFF16151F)

@Composable
fun PlixoBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isCaptain: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tabs = buildList {
        add(NavTab(Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home, "Home"))
        add(NavTab(Screen.Play.route, Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow, "Play"))
        add(NavTab(Screen.Tournaments.route, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "Tournaments"))
        if (isCaptain && FeatureFlags.CAPTAIN_DASHBOARD) {
            add(NavTab(Screen.Captain.route, Icons.Filled.Star, Icons.Outlined.Star, "Captain"))
        }
        if (FeatureFlags.CHAT) {
            add(NavTab(Screen.Chat.route, Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "Chat"))
        }
        add(NavTab(Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.PersonOutline, "Profile"))
    }

    val activeTabRoute = parentTabRoute(currentRoute)

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center,
    ) {
        // Outer glow shadow + thick refractive glass slab
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 36.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = PlixoPrimary.copy(0.28f),
                    spotColor = PlixoInk.copy(0.45f),
                )
                .clip(RoundedCornerShape(50))
                // Glass base: deep translucent dark so the background bleeds through
                .background(GlassInk.copy(alpha = 0.72f))
                // Refractive edge: bright top-left highlight fading to a dark bottom rim
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.10f),
                        )
                    ),
                    shape = RoundedCornerShape(50),
                ),
        ) {
            // Top shimmer — the glass light reflection / refraction band
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.06f),
                            )
                        )
                    )
            )
            // Diagonal sheen for the refractive feel
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            0.0f to Color.White.copy(alpha = 0.10f),
                            0.45f to Color.Transparent,
                            1.0f to PlixoPrimary.copy(alpha = 0.10f),
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val isActive = activeTabRoute == tab.route
                    val iconColor by animateColorAsState(
                        targetValue = if (isActive) PlixoLimeFg else Color.White.copy(alpha = 0.55f),
                        animationSpec = tween(200),
                        label = "navIconColor_${tab.label}",
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .then(
                                if (isActive) Modifier.shadow(12.dp, CircleShape, spotColor = PlixoLime.copy(0.6f))
                                else Modifier
                            )
                            .clip(CircleShape)
                            .then(
                                if (isActive) Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(PlixoLime, PlixoLime.copy(0.82f))
                                    )
                                ) else Modifier.background(Color.White.copy(alpha = 0.06f))
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onNavigate(tab.route) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isActive) tab.iconSelected else tab.iconUnselected,
                            contentDescription = tab.label,
                            tint = iconColor,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            }
        }
    }
}

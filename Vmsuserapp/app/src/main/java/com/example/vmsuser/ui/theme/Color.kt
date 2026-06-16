package com.example.vmsuser.ui.theme

import androidx.compose.ui.graphics.Color

// Core palette
val PlixoPrimary = Color(0xFF7C5CFF)
val PlixoPrimaryLight = Color(0xFFEDE8FF)
val PlixoPrimaryMid = Color(0xFFC9BBFF)
val PlixoPrimaryDark = Color(0xFF5733E0)
val PlixoLime = Color(0xFFD9F26B)
val PlixoLimeFg = Color(0xFF37410A)
val PlixoInk = Color(0xFF16151F)
val PlixoBg = Color(0xFFF1F0F7)
val PlixoSurface = Color(0xFFFFFFFF)
val PlixoSurface2 = Color(0xFFF4F3FA)
val PlixoSurface3 = Color(0xFFE9E7F3)
val PlixoText = Color(0xFF16151F)
val PlixoText2 = Color(0xFF6B6979)
val PlixoText3 = Color(0xFFA4A2B2)
val PlixoBorder = Color(0xFFECEAF4)
val PlixoDanger = Color(0xFFF0535F)
val PlixoDangerLight = Color(0xFFFFE3E6)

// Color blocks for stat cards
val BlockLimeBg = Color(0xFFD9F26B)
val BlockLimeFg = Color(0xFF37410A)
val BlockMintBg = Color(0xFFBFEFD2)
val BlockMintFg = Color(0xFF0C5132)
val BlockSkyBg  = Color(0xFFBFDBFF)
val BlockSkyFg  = Color(0xFF13386E)
val BlockPeachBg = Color(0xFFFFD7BC)
val BlockPeachFg = Color(0xFF7A3A12)
val BlockLilacBg = Color(0xFFE4D6FF)
val BlockLilacFg = Color(0xFF4A2796)
val BlockRoseBg  = Color(0xFFFFD2DC)
val BlockRoseFg  = Color(0xFF8E1F3C)

// Sport colors
val SportCricket    = Color(0xFF2E9E5B)
val SportFootball   = Color(0xFF2F6BD6)
val SportBadminton  = Color(0xFF7C5CFF)
val SportBasketball = Color(0xFFE0732B)
val SportTennis     = Color(0xFF86922A)
val SportSwimming   = Color(0xFF1499B8)
val SportVolleyball = Color(0xFFC2417C)
val SportRunning    = Color(0xFF5B6470)
val SportPickleball = Color(0xFF0E9488)
val SportTableTennis = Color(0xFFC0392B)

fun sportColor(sport: String): Color = when (sport.lowercase()) {
    "cricket" -> SportCricket
    "football" -> SportFootball
    "badminton" -> SportBadminton
    "basketball" -> SportBasketball
    "tennis" -> SportTennis
    "swimming" -> SportSwimming
    "volleyball" -> SportVolleyball
    "running" -> SportRunning
    "pickleball" -> SportPickleball
    "table tennis" -> SportTableTennis
    else -> PlixoPrimary
}

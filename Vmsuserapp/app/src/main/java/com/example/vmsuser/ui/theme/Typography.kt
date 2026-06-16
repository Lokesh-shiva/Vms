package com.example.vmsuser.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.vmsuser.R

// Note: TTF files must be placed in res/font/ by the user.
// Names: bricolage_grotesque_regular.ttf, bricolage_grotesque_bold.ttf,
//        bricolage_grotesque_extrabold.ttf
//        plus_jakarta_sans_regular.ttf, plus_jakarta_sans_medium.ttf,
//        plus_jakarta_sans_semibold.ttf, plus_jakarta_sans_bold.ttf
// If fonts are missing, Android will fall back to system serif/sans-serif.

val BricolageGrotesque = try {
    FontFamily(
        Font(R.font.bricolage_grotesque_regular, FontWeight.Normal),
        Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
        Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
    )
} catch (e: Exception) {
    FontFamily.Serif
}

val PlusJakartaSans = try {
    FontFamily(
        Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    )
} catch (e: Exception) {
    FontFamily.SansSerif
}

val PlixoTypography = Typography(
    displayLarge = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, letterSpacing = (-2.5).sp),
    displayMedium = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 38.sp, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

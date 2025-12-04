package com.menagerie.bakers.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getTypography(fontFamily: FontFamily): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontWeight = FontWeight.Light,
            fontSize = 45.sp,
            letterSpacing = (-1.5).sp,
            fontFamily = fontFamily
        ),
        headlineMedium = TextStyle(
            fontWeight = FontWeight.Light,
            fontSize = 35.sp,
            letterSpacing = (-0.5).sp,
            fontFamily = fontFamily
        ),
        headlineSmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 30.sp,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        displayLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 25.sp,
            letterSpacing = 0.25.sp,
            fontFamily = fontFamily
        ),
        displayMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        displaySmall = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            letterSpacing = 0.15.sp,
            fontFamily = fontFamily
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp,
            fontFamily = fontFamily
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            letterSpacing = 0.25.sp,
            fontFamily = fontFamily
        ),
        bodySmall = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 1.25.sp,
            fontFamily = fontFamily
        ),
        labelSmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            fontFamily = fontFamily
        ),
    )
}
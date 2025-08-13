package com.example.firstapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun FirstAPPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = GreenPrimary,
            onPrimary = Color.White,
            background = GreenDark,
            onBackground = GreenLight,
            surface = GreenDark,
        )
    } else {
        lightColorScheme(
            primary = GreenPrimary, // màu main
            onPrimary = Color.White, //màu chữ
            background = GreenLight, // màu nền
            onBackground = GreenDark, // chữ nền
            surface = Color(0xFFE8F5E8).copy(alpha = 0f) , // màu nền bề mặt
            // onSurface = GreenDark // chữ nền bề mặt
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
package com.example.firstapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun FirstAPPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = GreenPrimary,
            onPrimary = GreenLight,
            secondary = GreenDark,
            onSecondary = GreenLight,
            background = GreenDark,
            onBackground = GreenLight,
            surface = GreenDark,
            onSurface = GreenLight
        )
    } else {
        lightColorScheme(
            primary = GreenPrimary,
            onPrimary = GreenLight,
            secondary = GreenDark,
            onSecondary = GreenLight,
            background = GreenLight,
            onBackground = GreenDark,
            surface = GreenLight,
            onSurface = GreenDark
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
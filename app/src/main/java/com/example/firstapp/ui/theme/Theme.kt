package com.example.firstapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun FirstAPPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme(
            primary = YellowPrimary,
            onPrimary = YellowDark,
            secondary = YellowLight,
            onSecondary = YellowDark,
            background = YellowDark,
            onBackground = YellowLight,
            surface = YellowDark,
            onSurface = YellowLight
        )
        else -> lightColorScheme(
            primary = YellowPrimary,
            onPrimary = YellowDark,
            secondary = YellowLight,
            onSecondary = YellowDark,
            background = YellowLight,
            onBackground = YellowDark,
            surface = YellowLight,
            onSurface = YellowDark
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
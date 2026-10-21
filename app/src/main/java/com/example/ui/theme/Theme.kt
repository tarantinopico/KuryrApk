package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.ThemeMode

@Composable
fun KuryrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useMaterialYou: Boolean = false,
    accentColorHex: Long = 0xFF1E40AF,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == ThemeMode.AMOLED

    val customAccent = Color(accentColorHex)
    val accentContainerLight = customAccent.copy(alpha = 0.14f)
    val accentContainerDark = customAccent.copy(alpha = 0.28f)

    val baseScheme = when {
        useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = if (accentColorHex == 0xFF1E40AF) RoyalBluePrimaryDark else customAccent,
            onPrimary = if (customAccent.luminance() > 0.5f) Color(0xFF0F172A) else Color.White,
            primaryContainer = if (accentColorHex == 0xFF1E40AF) RoyalBlueContainerDark else accentContainerDark,
            onPrimaryContainer = RoyalBlueOnContainerDark,
            secondary = SlateSecondaryDark,
            onSecondary = SlateOnSecondaryDark,
            secondaryContainer = SlateContainerDark,
            onSecondaryContainer = SlateOnContainerDark,
            background = if (isAmoled) AmoledBackground else EditorialBackgroundDark,
            surface = if (isAmoled) AmoledSurface else EditorialSurfaceDark,
            surfaceVariant = if (isAmoled) AmoledSurfaceVariant else EditorialSurfaceVariantDark,
            outline = EditorialOutlineDark
        )
        else -> lightColorScheme(
            primary = customAccent,
            onPrimary = Color.White,
            primaryContainer = if (accentColorHex == 0xFF1E40AF) RoyalBlueContainerLight else accentContainerLight,
            onPrimaryContainer = RoyalBlueOnContainerLight,
            secondary = SlateSecondaryLight,
            onSecondary = SlateOnSecondaryLight,
            secondaryContainer = SlateContainerLight,
            onSecondaryContainer = SlateOnContainerLight,
            background = EditorialBackgroundLight,
            surface = EditorialSurfaceLight,
            surfaceVariant = EditorialSurfaceVariantLight,
            outline = EditorialOutlineLight
        )
    }

    val finalColorScheme = if (isAmoled) {
        baseScheme.copy(
            background = AmoledBackground,
            surface = AmoledSurface,
            surfaceContainer = AmoledSurfaceVariant
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}

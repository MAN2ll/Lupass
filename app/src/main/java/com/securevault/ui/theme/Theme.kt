package com.securevault.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SecureVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6C9EFF),
            primaryContainer = Color(0xFF1A3A8A),
            secondary = Color(0xFF4DD0E1),
            tertiary = Color(0xFFFFB74D),
            background = Color(0xFF0A0E1A),
            surface = Color(0xFF121828),
            surfaceVariant = Color(0xFF1E2640),
            onSurface = Color(0xFFE8EAF6),
            onSurfaceVariant = Color(0xFF9EA8C8),
            error = Color(0xFFFF6B6B),
            outline = Color(0xFF2E3A58)
        ),
        content = content
    )
}

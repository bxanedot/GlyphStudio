package com.sfsymbols.ui.theme

import androidx.compose.runtime.Composable
import dev.nucleusframework.macoscompose.theme.AccentColor
import dev.nucleusframework.macoscompose.theme.GlassType
import dev.nucleusframework.macoscompose.theme.MacosTheme

/**
 * App theme wrapper using a clean, warm macOS palette and solid surfaces.
 */
@Composable
public fun AppTheme(
    darkTheme: Boolean = true,
    accent: AccentColor = AccentColor.Blue,
    content: @Composable () -> Unit
) {
    MacosTheme(
        darkTheme = darkTheme,
        accentColor = accent,
        liquidGlass = false,
        glassType = GlassType.Tinted,
    ) {
        content()
    }
}

package com.sfsymbols.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import dev.nucleusframework.macoscompose.theme.AccentColor
import dev.nucleusframework.macoscompose.theme.GlassType
import dev.nucleusframework.macoscompose.theme.MacosTheme

/** macOS theme that follows the system light/dark setting, with Liquid Glass surfaces. */
@Composable
public fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.Blue,
    content: @Composable () -> Unit
) {
    MacosTheme(
        darkTheme = darkTheme,
        accentColor = accent,
        liquidGlass = true,
        glassType = GlassType.Tinted,
    ) {
        content()
    }
}

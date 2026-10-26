package com.sfsymbols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sfsymbols.data.SfSymbolIconResolver
import com.sfsymbols.data.SfSymbolsCatalog
import com.sfsymbols.data.SymbolMode
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.theme.MacosTheme

@Composable
public fun ThemeSelector(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MacosTheme.colorScheme
    val moon = remember { SfSymbolsCatalog.all.firstOrNull { it.appleName == "moon.fill" }?.let { SfSymbolIconResolver.resolve(it, SymbolMode.Monochrome) } }
    val sun = remember { SfSymbolsCatalog.all.firstOrNull { it.appleName == "sun.max.fill" }?.let { SfSymbolIconResolver.resolve(it, SymbolMode.Monochrome) } }
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(cs.surface.copy(alpha = 0.72f)).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ThemeOption("Dark", moon, darkTheme, { onThemeChange(true) })
        ThemeOption("Light", sun, !darkTheme, { onThemeChange(false) })
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MacosTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .then(if (selected) Modifier.background(cs.accent) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (selected) cs.onAccent else cs.textSecondary, modifier = Modifier.size(14.dp))
        Text(label, color = if (selected) cs.onAccent else cs.textSecondary)
    }
}

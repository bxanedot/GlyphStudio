package com.sfsymbols.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The GlyphStudio logo as a vector, so the title bar mark and window icon stay
 * sharp at any size. Same 512-unit geometry as docs/logo.svg and
 * scripts/render_icons.py (which regenerates the installer icons).
 */
internal val AppLogo: ImageVector by lazy {
    ImageVector.Builder("GlyphStudio", 512.dp, 512.dp, 512f, 512f).apply {
        path(fill = SolidColor(Color(0xFF2E3D52))) {
            moveTo(112f, 0f)
            horizontalLineTo(400f)
            arcTo(112f, 112f, 0f, false, true, 512f, 112f)
            verticalLineTo(400f)
            arcTo(112f, 112f, 0f, false, true, 400f, 512f)
            horizontalLineTo(112f)
            arcTo(112f, 112f, 0f, false, true, 0f, 400f)
            verticalLineTo(112f)
            arcTo(112f, 112f, 0f, false, true, 112f, 0f)
            close()
        }
        // Three equal pills, 88 wide and 16 apart, centred in a 296x296 box.
        for (x in listOf(108f, 212f, 316f)) {
            path(fill = SolidColor(Color(0xFFE8EDF2))) {
                moveTo(x, 152f)
                arcTo(44f, 44f, 0f, false, true, x + 88f, 152f)
                verticalLineTo(360f)
                arcTo(44f, 44f, 0f, false, true, x, 360f)
                close()
            }
        }
    }.build()
}

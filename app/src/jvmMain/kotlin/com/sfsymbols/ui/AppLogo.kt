package com.sfsymbols.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * The GlyphStudio logo as a vector, so the title-bar mark and window icon stay
 * sharp at any size: an Apple-style squircle with a navy gradient and three
 * capsules. Same 512-unit geometry as scripts/render_icons.py (which also
 * writes docs/logo.svg and the installer icons); keep the two in sync.
 */
internal val AppLogo: ImageVector by lazy {
    ImageVector.Builder("GlyphStudio", 512.dp, 512.dp, 512f, 512f).apply {
        path(fill = Brush.verticalGradient(listOf(Color(0xFF4A6385), Color(0xFF1E2A3C)), 0f, 512f)) {
            // Superellipse |x|^5 + |y|^5 = 1, about Apple's continuous-corner icon shape.
            for (i in 0 until 256) {
                val t = 2 * PI * i / 256
                val x = 256 + 256 * sign(cos(t)) * abs(cos(t)).pow(0.4)
                val y = 256 + 256 * sign(sin(t)) * abs(sin(t)).pow(0.4)
                if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
            }
            close()
        }
        // Three equal capsules, 88 wide and 16 apart, each with a shadow 10 units below.
        val pills = listOf(108f, 212f, 316f)
        for (x in pills) path(fill = SolidColor(Color(0x5A080E18))) { capsule(x, 118f) }
        val pillFill = Brush.verticalGradient(listOf(Color.White, Color(0xFFD3DCE7)), 108f, 404f)
        for (x in pills) path(fill = pillFill) { capsule(x, 108f) }
    }.build()
}

private fun PathBuilder.capsule(x: Float, top: Float) {
    moveTo(x, top + 44f)
    arcTo(44f, 44f, 0f, false, true, x + 88f, top + 44f)
    verticalLineTo(top + 252f)
    arcTo(44f, 44f, 0f, false, true, x, top + 252f)
    close()
}

package com.composables.sfsymbols

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import com.composables.sfsymbols.monochrome.SFChevronDown
import com.composables.sfsymbols.monochrome.SFCircleFill
import com.composables.sfsymbols.monochrome.SFIphone
import com.composables.sfsymbols.monochrome.SFRectangleFill
import org.junit.Assert.assertEquals
import org.junit.Test

class SfIconSizingTest {
    @Test
    fun wideTallAndSquareIconsScaleUniformlyInFixedSizeSlots() {
        val vectors = listOf(
            SfSymbols.Monochrome.SFChevronDown,
            SfSymbols.Monochrome.SFRectangleFill,
            SfSymbols.Monochrome.SFIphone,
            SfSymbols.Monochrome.SFCircleFill
        )
        val slots = listOf(Size(16f, 16f), Size(24f, 24f), Size(28f, 28f), Size(56f, 56f), Size(40f, 24f))

        for (vector in vectors) {
            val intrinsicSize = Size(vector.defaultWidth.value, vector.defaultHeight.value)
            for (slot in slots) {
                // Material Icon fits the painter's intrinsic size into its layout slot.
                // VectorPainter then scales the original viewport to that fitted size.
                val fit = ContentScale.Fit.computeScaleFactor(intrinsicSize, slot)
                val scaleX = intrinsicSize.width * fit.scaleX / vector.viewportWidth
                val scaleY = intrinsicSize.height * fit.scaleY / vector.viewportHeight
                assertEquals("${vector.name} stretches in $slot", scaleX, scaleY, 0.00001f)
            }
        }
    }

    @Test
    fun everyGeneratedVariantPreservesItsAspectRatioAndDefaultSize() {
        for (symbol in SfSymbolsCatalog.all) {
            for (dualtone in listOf(false, true)) {
                val packageName = if (dualtone) "dualtone" else "monochrome"
                val receiver = if (dualtone) SfSymbols.Dualtone else SfSymbols.Monochrome
                val fileClass = Class.forName("com.composables.sfsymbols.$packageName.${symbol.pascalName}Kt")
                val vector = fileClass.getMethod("get${symbol.pascalName}", receiver.javaClass)
                    .invoke(null, receiver) as ImageVector

                assertEquals(
                    "${vector.name} has mismatched intrinsic and viewport proportions",
                    vector.defaultWidth.value / vector.viewportWidth,
                    vector.defaultHeight.value / vector.viewportHeight,
                    0.00001f
                )
                assertEquals(
                    "${vector.name} exceeds the default icon size",
                    24f,
                    maxOf(vector.defaultWidth.value, vector.defaultHeight.value),
                    0.00001f
                )
            }
        }
    }
}

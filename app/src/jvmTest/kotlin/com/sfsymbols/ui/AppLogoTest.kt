package com.sfsymbols.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import org.jetbrains.skia.EncodedImageFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppLogoTest {
    @Test fun `vector logo matches the icon geometry`() {
        val scene = ImageComposeScene(512, 512, Density(1f)) { Image(AppLogo, null, Modifier.fillMaxSize()) }
        val img = try {
            ImageIO.read(ByteArrayInputStream(scene.render(0L).encodeToData(EncodedImageFormat.PNG)!!.bytes))
        } finally { scene.close() }
        // Brightness (0-255) of the opaque pixel at (x, y); pills are near white, the squircle is navy.
        fun light(x: Int, y: Int): Int {
            val c = img.getRGB(x, y)
            assertEquals(0xFF, c ushr 24, "($x, $y) is opaque")
            return ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) / 3
        }
        assertEquals(0, img.getRGB(8, 8) ushr 24, "squircle corner is transparent")
        assertTrue(light(256, 40) < 120, "background")
        assertTrue(light(256, 40) > light(256, 470), "background gradient darkens downward")
        assertTrue(light(152, 256) > 200, "first pill")
        assertTrue(light(204, 256) < 120, "gap between pills")
        assertTrue(light(402, 256) > 200, "third pill is as wide as the others")
        assertTrue(light(410, 256) < 120, "right of the third pill")
    }
}

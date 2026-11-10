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

class AppLogoTest {
    @Test fun `vector logo matches the icon geometry`() {
        val scene = ImageComposeScene(512, 512, Density(1f)) { Image(AppLogo, null, Modifier.fillMaxSize()) }
        val img = try {
            ImageIO.read(ByteArrayInputStream(scene.render(0L).encodeToData(EncodedImageFormat.PNG)!!.bytes))
        } finally { scene.close() }
        fun at(x: Int, y: Int) = img.getRGB(x, y).toUInt().toString(16)
        assertEquals(0, img.getRGB(3, 3) ushr 24, "rounded corner is transparent")
        assertEquals("ff2e3d52", at(256, 40), "background")
        assertEquals("ffe8edf2", at(152, 256), "first pill")
        assertEquals("ff2e3d52", at(204, 256), "gap between pills")
        assertEquals("ffe8edf2", at(402, 256), "third pill is as wide as the others")
        assertEquals("ff2e3d52", at(410, 256), "right of the third pill")
    }
}

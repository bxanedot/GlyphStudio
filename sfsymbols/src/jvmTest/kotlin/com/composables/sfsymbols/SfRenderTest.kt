package com.composables.sfsymbols

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
class SfRenderTest {
    private val out = File("build/sf-preview").also { it.mkdirs() }

    private fun render(px: Int, nanos: Long = 0L, content: @androidx.compose.runtime.Composable () -> Unit): BufferedImage {
        val scene = ImageComposeScene(px, px, Density(1f), content = content)
        try {
            val img = scene.render(nanos)
            val bytes = img.encodeToData(EncodedImageFormat.PNG)!!.bytes
            return ImageIO.read(ByteArrayInputStream(bytes))
        } finally {
            scene.close()
        }
    }

    /** Alpha-bounding box of drawn pixels: [left, top, right, bottom] inclusive, or null if empty. */
    private fun bbox(img: BufferedImage): IntArray? {
        var l = img.width; var t = img.height; var r = -1; var b = -1
        for (y in 0 until img.height) for (x in 0 until img.width) {
            if ((img.getRGB(x, y) ushr 24) > 40) {
                if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y
            }
        }
        return if (r < 0) null else intArrayOf(l, t, r, b)
    }

    @Test
    fun catalogLoads() {
        assertTrue(SfSymbol.all.size > 8000)
        assertEquals("heart.fill", SfSymbol["heart.fill"].name)
        assertEquals("heart.fill", SfSymbol["SFHeartFill"].name)
        assertTrue(SfSymbol.search("wifi").isNotEmpty())
    }

    /** Every symbol, rendered through the real composable, must keep Apple's exact aspect ratio. */
    @Test
    fun noSymbolIsStretchedOrClipped() {
        val px = 160
        val bad = ArrayList<String>()
        var empty = 0
        val subject = if (System.getenv("SF_FULL") != null) SfSymbol.all else SfSymbol.all.filterIndexed { i, _ -> i % 8 == 0 }
        for (s in subject) {
            val b = s.bounds(SfWeight.Regular)
            val img = render(px) { SfSymbolIcon(s, size = 144.dp) }
            val bb = bbox(img)
            if (bb == null) { empty++; bad += "${s.name}: EMPTY"; continue }
            val w = bb[2] - bb[0] + 1
            val h = bb[3] - bb[1] + 1
            val scale = 144f / maxOf(b.width, b.height)
            val ew = b.width * scale
            val eh = b.height * scale
            // anti-aliased edge pixels add at most ~1.5px per side
            if (abs(w - ew) > 3f || abs(h - eh) > 3f) bad += "${s.name}: got ${w}x$h expected %.1fx%.1f".format(ew, eh)
        }
        File(out, "aspect-report.txt").writeText("checked=${subject.size} empty=$empty bad=${bad.size}\n" + bad.joinToString("\n"))
        assertTrue("deformed/empty symbols: ${bad.size}, first: ${bad.take(5)}", bad.isEmpty())
    }


    private class Frame(val top: Int, val left: Int, val right: Int, val ink: Int, val alpha: Long)

    private fun measure(img: BufferedImage): Frame {
        var t = img.height; var l = img.width; var r = -1; var ink = 0; var a = 0L
        for (y in 0 until img.height) for (x in 0 until img.width) {
            val al = img.getRGB(x, y) ushr 24
            if (al > 40) { ink++; if (y < t) t = y; if (x < l) l = x; if (x > r) r = x }
            a += al
        }
        return Frame(t, l, r, ink, a)
    }

    /** Plays [effect] and returns one measurement per requested millisecond mark. */
    private fun play(symbol: String, effect: SfSymbolEffect, marks: List<Int>, repeat: SfEffectRepeat? = null): List<Frame> {
        val scene = ImageComposeScene(160, 160, Density(1f)) {
            SfSymbolIcon(
                SfSymbol[symbol],
                modifier = Modifier.layout { m, c -> val pl = m.measure(c); layout(c.maxWidth, c.maxHeight) { pl.place(30, 40) } },
                size = 100.dp, effect = effect, repeat = repeat,
            )
        }
        try {
            val out = ArrayList<Frame>()
            var now = 0
            scene.render(0L)
            for (m in marks) {
                while (now < m) { now += 8; val img = scene.render(now * 1_000_000L); if (now >= m) out += measure(ImageIO.read(ByteArrayInputStream(img.encodeToData(EncodedImageFormat.PNG)!!.bytes))) }
            }
            return out
        } finally { scene.close() }
    }

    @Test
    fun effectsActuallyAnimate() {
        // Bounce: rises at ~217 ms, returns by 700 ms
        val b = play("heart.fill", SfSymbolEffect.Bounce(), listOf(40, 216, 700))
        assertTrue("bounce should lift the symbol: ${b.map { it.top }}", b[1].top < b[0].top - 8 && b[2].top >= b[0].top - 2)
        // Appear: fades/scales in over 250 ms
        val ap = play("heart.fill", SfSymbolEffect.Appear(), listOf(16, 400))
        assertTrue("appear ink ${ap.map { it.alpha }}", ap[0].alpha < ap[1].alpha / 2)
        // Pulse: dims to 30 % at 1 s, restored at 2 s
        val pu = play("wifi", SfSymbolEffect.Pulse(), listOf(16, 1000, 2100))
        assertTrue("pulse alpha ${pu.map { it.alpha }}", pu[1].alpha < pu[0].alpha * 0.6 && pu[2].alpha > pu[0].alpha * 0.9)
        // Scale up: 1.25x
        val sc = play("heart.fill", SfSymbolEffect.Scale(), listOf(16, 400))
        assertTrue("scale ink ${sc.map { it.ink }}", sc[1].ink > sc[0].ink * 1.4)
        // Scale with Apple's preview timing: grow 0.25 s, hold 0.5 s, return 0.25 s
        val sr = play("heart.fill", SfSymbolEffect.Scale(returns = true), listOf(16, 500, 1150))
        assertTrue("scale returns ${sr.map { it.ink }}", sr[1].ink > sr[0].ink * 1.4 && sr[2].ink < sr[1].ink * 0.8)
        // Breathe: peaks at 1.5 s
        val br = play("heart.fill", SfSymbolEffect.Breathe(), listOf(16, 1500))
        assertTrue("breathe ink ${br.map { it.ink }}", br[1].ink > br[0].ink * 1.25)
        // Wiggle: moves the bell
        val wg = play("bell.fill", SfSymbolEffect.Wiggle(), listOf(16, 90))
        assertTrue("wiggle ${wg.map { it.left to it.top }}", wg[0].left != wg[1].left || wg[0].top != wg[1].top || wg[0].ink != wg[1].ink)
        // Variable colour: wifi lights up then returns
        val vc = play("wifi", SfSymbolEffect.VariableColor(), listOf(16, 500, 1500))
        assertTrue("vc alpha ${vc.map { it.alpha }}", vc[1].alpha != vc[0].alpha)
        // Draw on grows the ink
        val dr = play("checkmark", SfSymbolEffect.DrawOn(), listOf(16, 200, 1200))
        assertTrue("draw ink ${dr.map { it.ink }}", dr[0].ink < dr[1].ink && dr[1].ink < dr[2].ink)
        // the checkmark is drawn from its left end: early on the right tip is still missing
        val ck = play("checkmark", SfSymbolEffect.DrawOn(), listOf(100, 1200))
        assertTrue("draw order ${ck.map { it.left to it.right }}", ck[0].right < ck[1].right - 15 && kotlin.math.abs(ck[0].left - ck[1].left) <= 6)
    }

    @Test
    fun drawSecondsMatchesApple() {
        // Apple: 8 frames for a point, 60 frames at 581.79 pt, at 60 fps
        assertEquals(8f / 60f, drawSeconds(0f), 0.002f)
        assertEquals(1.0f, drawSeconds(581.79f), 0.01f)
        assertTrue(drawSeconds(100f) in 0.2f..0.8f)
    }


    @Test
    fun metadataFromApple() {
        val heartFill = SfSymbol["heart.fill"]
        assertTrue(heartFill.releases["iOS"]?.isNotEmpty() == true)
        assertEquals("heart", heartFill.base?.name)
        assertTrue(SfSymbol["heart"].variants.any { it.name == "heart.fill" })
        val weather = SfSymbol.allCategories.first { it.key == "weather" }
        assertTrue(weather.title("de") != weather.label || weather.title("de").isNotEmpty())
        assertEquals("Wetter", weather.title("de"))
        assertTrue(SfSymbol["wifi"].layers.any { it.tags.isNotEmpty() })
        assertEquals("video.badge.questionmark.ar", SfSymbol["video.badge.questionmark"].localized("ar").name)
        assertTrue(SfSymbol.search("planner").isNotEmpty()) // from Apple's enhanced keywords
    }

    @Test
    fun opticalScalesDifferInSize() {
        for (n in listOf("heart.fill", "wifi", "gearshape")) {
            val s = SfSymbol[n]
            val sm = s.bounds(SfWeight.Regular, SfScale.Small).width
            val md = s.bounds(SfWeight.Regular, SfScale.Medium).width
            val lg = s.bounds(SfWeight.Regular, SfScale.Large).width
            assertTrue("$n small<medium<large: $sm $md $lg", sm < md && md < lg)
        }
    }

    @Test
    fun gradientMatchesApplesAlgorithm() {
        // mid-lightness blue: lighter stop first, original last
        val blue = Color(0xFF007AFF)
        val (a, b) = sfAutomaticGradient(blue)
        assertEquals(blue, b)
        assertTrue(a.green > blue.green && a.blue >= blue.blue)
        // very light colour: original first, darkened second
        val (w0, w1) = sfAutomaticGradient(Color(0xFFF2F2F7))
        assertEquals(Color(0xFFF2F2F7), w0)
        assertTrue(w1.red < w0.red)
    }

    @Test
    fun variableValueLightsLayersInOrder() {
        val wifi = SfSymbol["wifi"]
        val none = sfVariableLevels(wifi, 0f)
        val all = sfVariableLevels(wifi, 1f)
        val mid = sfVariableLevels(wifi, 0.5f)
        assertTrue(none.alpha.all { it < 1f })
        assertTrue(all.alpha.all { it == 1f })
        assertTrue(mid.alpha.count { it == 1f } in 1..2)
    }

    @Test
    fun rtlMirrorsFlaggedSymbols() {
        val sym = SfSymbol.all.first { it.mirrorsInRtl && it.bounds().let { b -> b.width > 0 } && !it.isLocaleVariant }
        fun cx(dir: androidx.compose.ui.unit.LayoutDirection): Double {
            val scene = ImageComposeScene(160, 160, Density(1f), layoutDirection = dir) {
                SfSymbolIcon(sym, modifier = Modifier.layout { m, c -> val p = m.measure(c); layout(c.maxWidth, c.maxHeight) { p.place(30, 30) } }, size = 100.dp)
            }
            val img = ImageIO.read(ByteArrayInputStream(scene.render(0L).encodeToData(EncodedImageFormat.PNG)!!.bytes)); scene.close()
            var sx = 0.0; var n = 0
            for (y in 0 until img.height) for (x in 0 until img.width) if ((img.getRGB(x, y) ushr 24) > 40) { sx += x; n++ }
            return sx / n
        }
        val ltr = cx(androidx.compose.ui.unit.LayoutDirection.Ltr)
        val rtl = cx(androidx.compose.ui.unit.LayoutDirection.Rtl)
        // mirrored about the 100 px wide slot starting at x=30 (slot centre 80 -> no: the flip is about the slot's own centre)
        assertTrue("ltr=$ltr rtl=$rtl for ${sym.name}", kotlin.math.abs(ltr - rtl) > 0.5 || sym.bounds().width < 2f)
    }

    @Test
    fun magicReplaceAndModesRenderWithoutError() {
        var s by androidx.compose.runtime.mutableStateOf(SfSymbol["checkmark.circle.fill"])
        val scene = ImageComposeScene(120, 120, Density(1f)) {
            SfSymbolIcon(s, size = 80.dp, mode = SfRenderingMode.Multicolor, gradient = true, scale = SfScale.Large,
                variableValue = 0.7f, transition = SfSymbolTransition.MagicReplace)
        }
        scene.render(0L)
        s = SfSymbol["xmark.circle.fill"]
        for (ms in 16..600 step 32) scene.render(ms * 1_000_000L)
        for (dir in SfReplaceDirection.values()) {
            val sc = ImageComposeScene(120, 120, Density(1f)) {
                SfSymbolIcon(s, size = 80.dp, transition = SfSymbolTransition.Replace, replaceDirection = dir)
            }
            sc.render(0L); s = SfSymbol["checkmark.circle.fill"]
            for (ms in 16..600 step 32) sc.render(ms * 1_000_000L)
            s = SfSymbol["xmark.circle.fill"]; sc.close()
        }
        scene.close()
    }

    @Test
    fun writePreviews() {
        val names = listOf(
            "heart.fill", "star", "wifi", "cloud.sun.rain.fill", "gearshape", "checkmark", "arrow.clockwise",
            "plus.circle.fill", "bell.fill", "speaker.wave.3", "battery.100percent", "camera.fill",
            "figure.stand", "macbook", "rectangle.split.3x1", "airpodspro", "house.fill", "trash", "magnifyingglass", "person.crop.circle.fill",
        )
        for (n in names) {
            val s = SfSymbol.named(n) ?: continue
            for (w in listOf(SfWeight.Ultralight, SfWeight.Regular, SfWeight.Black)) {
                val img = render(96) { SfSymbolIcon(s, size = 80.dp, weight = w) }
                ImageIO.write(img, "png", File(out, "w-$n-${w.name}.png"))
            }
            for (m in SfRenderingMode.values()) {
                val img = render(96) {
                    SfSymbolIcon(s, size = 80.dp, mode = m, tint = Color(0xFF1C1C1E), palette = listOf(Color(0xFFFF3B30), Color(0xFF007AFF), Color(0xFF34C759)))
                }
                ImageIO.write(img, "png", File(out, "m-$n-${m.name}.png"))
            }
        }
    }
}

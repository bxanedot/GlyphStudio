# Symbol Engine — Weights, Layers, Rendering Modes & Effects

The original `SfSymbols.Dualtone.X` / `SfSymbols.Monochrome.X` properties are static `ImageVector`s and are
unchanged. This page covers the second API, a real symbol engine built on `SfSymbol` + `SfSymbolIcon`:

| | `ImageVector` API | Symbol engine |
|---|---|---|
| Symbols | 7,007 | **8,526** (current SF Symbols release) |
| Names | numeric-mangled (`SF00Circle`) | real Apple names (`"0.circle"`) + the old names still resolve |
| Weights | 1 | **9** (Ultralight … Black), exact variable-font interpolation |
| Scales | 1 | Small / Medium / Large optical variants |
| Rendering modes | dualtone / mono | Monochrome, Hierarchical, Palette, Multicolor |
| Layers | flattened | Apple's real layer tree (knock-outs, groups, pulse/rotate/variable flags) |
| Animation | none | 11 effects + symbol transitions |
| Dependencies | `compose.ui` | `compose.ui` only — no Material, no foundation, no animation lib |

It runs anywhere `compose.ui` does: Android Compose and Compose Multiplatform (desktop, iOS, web).

## Use

```kotlin
SfSymbolIcon(
    symbol = SfSymbol["wifi"],
    size = 32.dp,                       // larger side of the symbol
    weight = SfWeight.Semibold,
    mode = SfRenderingMode.Hierarchical,
    tint = Color(0xFF007AFF),
    effect = SfSymbolEffect.VariableColor(),
    repeat = SfEffectRepeat.Continuous,
)
```

Symbols keep Apple's exact proportions at every weight: a single uniform scale is used, and the ink is centred
on its true curve bounds.

### Finding symbols

```kotlin
SfSymbol["heart.fill"]            // by Apple name (throws if unknown)
SfSymbol.named("SFHeartFill")     // also accepts the old API name and Apple aliases; null if unknown
SfSymbol.all                      // 8,526 symbols
SfSymbol.search("arrow")          // names + Apple's search keywords
SfSymbol.search("", SfSymbol.allCategories.first { it.key == "weather" })
symbol.legacyName                 // "SFHeartFill" — property name in the old API
symbol.since, symbol.keywords, symbol.categories, symbol.layers, symbol.hasExactLayers
```

### More from Apple's data

```kotlin
symbol.releases              // {"iOS": "16.0", "macOS": "13.0", ...} for the release it shipped in
symbol.hierarchicalSince     // release key from which hierarchical rendering exists
symbol.variants / symbol.base // heart <-> heart.fill
symbol.localized("ar")       // Apple's Arabic / RTL / Hindi / Japanese ... variant, or the symbol itself
symbol.layers[0].tags        // Apple's semantic layer tags
SfSymbol.allCategories.first { it.key == "weather" }.title("de")   // "Wetter" (38 languages)
SfSymbol.search("planner")   // searches Apple's search + enhanced keyword sets
```

### Parameters of `SfSymbolIcon`

| Parameter | Meaning |
|---|---|
| `weight` | `SfWeight.Ultralight … Black` |
| `scale` | `SfScale.Small / Medium / Large`: Apple's optically-adjusted outlines, drawn smaller / larger than Medium for the same `size` |
| `gradient` | Apple's automatic gradient: lighter at the top (darker for very light colours), computed with the SF Symbols app's own formula |
| `variableValue` | 0..1 fill for variable symbols (wifi bars, speaker waves); variable-draw layers draw partially |
| `mirrorInRtl` | flips symbols Apple marks as mirroring in right-to-left layouts (default on) |
| `mode` | `Monochrome`, `Hierarchical` (primary 100 % / secondary 50 % / tertiary 25 %), `Palette` (`palette = listOf(...)`), `Multicolor` (Apple's own layer colours; `tint` for tint-coloured layers) |
| `effect` | an `SfSymbolEffect`, or `null` |
| `repeat` | `null` = the effect's natural behaviour; `Once`, `Times(n)`, `Continuous` |
| `trigger` | discrete effects replay whenever this value changes (a counter, a state value…) |
| `active` | stops continuous effects when `false`; for state effects it applies (`true`) or reverts (`false`) them |
| `transition` | `None`, `Replace`, `MagicReplace` (layers sharing an Apple tag and outline structure morph, the rest fade) |

## Effects

| Effect | Notes |
|---|---|
| `Appear(scope)` / `Disappear(scope)` | scale + fade; hold their end state |
| `Bounce(direction, scope)` | up/down, whole symbol or staggered by layer |
| `Pulse(scope)` | dims the symbol's pulse layers (all layers when none are flagged) |
| `VariableColor(style, reversing, hideInactiveLayers)` | lights layers in Apple's per-symbol order (`Iterative` / `Cumulative`) |
| `Scale(up, returns)` | 1.25x up / 0.5x down; holds the state, or with `returns = true` plays Apple's preview timing (0.25 s in, 0.5 s hold, 0.25 s back) |
| `Wiggle(direction, continuous)` | `null` = the symbol's own preferred direction (left/right/up/down/clockwise/… or a custom angle) |
| `Rotate(direction, scope)` | rotates the rotatable layers (whole symbol otherwise) around Apple's rotation anchor |
| `Breathe(pulsing)` | continuous scale (+ fade) |
| `DrawOn(scope)` / `DrawOff(scope)` | strokes grow outward from the stroke's start point; layers without a stroke fade |

`SfEffectScope.ByLayer` staggers layers 80 ms apart.

```kotlin
var taps by remember { mutableStateOf(0) }
SfSymbolIcon(
    symbol = SfSymbol["bell.fill"],
    size = 28.dp,
    effect = SfSymbolEffect.Wiggle(),
    trigger = taps,                    // wiggles on every change
)
```

State effects:

```kotlin
SfSymbolIcon(SfSymbol["checkmark"], effect = SfSymbolEffect.DrawOn(), active = done)
```

### Symbol transitions

```kotlin
SfSymbolIcon(
    symbol = if (playing) SfSymbol["pause.fill"] else SfSymbol["play.fill"],
    transition = SfSymbolTransition.MagicReplace,   // morphs when both outlines share a structure
)
```

## Using a symbol where an `ImageVector` is required

```kotlin
Icon(SfSymbol["gearshape"].toImageVector(SfWeight.Medium), contentDescription = null)
```

This is a static monochrome vector (no layers or animation).

## Notes

- **Where the motion comes from:** durations, easing curves (cubic-Bezier), keyframe values and layer stagger
  are Apple's own, taken from the SF Symbols app: Bounce (0.217 s + 0.217 s + 0.267 s), Appear 0.25 s, Disappear
  0.167 s, Pulse 2 s down to 30 %, Wiggle five 0.167 s segments (10°/-8°/6°/-5° or 0.06/-0.04/0.03/-0.025 em; continuous wiggle 5° / 0.025 em),
  Rotate 360° in 2 s, Breathe 1.2x over 3 s, Variable Color 0.333 s per layer, Scale 1.25x (up) / 0.5x (down).
  Offsets (Bounce, Wiggle) are in em, i.e. relative to the symbol's point size, as in Apple's recipes.
- **Draw On/Off** uses Apple's duration formula (0.13 s for a short stroke up to 1.0 s for a long one, from the
  stroke's length), its easing curves and its draw-rail data: each stroke is revealed by distance travelled through
  the shape from the rail's start tie, so it follows corners and curves (wifi arcs open from the centre, a
  checkmark runs left to the corner and on to the tip). Layers without a stroke scale and fade like
  Appear/Disappear. `ByLayer` draws the layers one after another.
- **Replace / MagicReplace** use Apple's replace recipes (`replaceDirection`): `DownUp` shrinks the old symbol to 0.5x
  over 0.25 s (curve 0.75, 0, 0.8, 1) and then grows the new one 0.5x to 1x over 0.25 s (curve 0.2, 0, 0.25, 1);
  `OffUp` removes the old symbol at once and grows/fades the new one in over 0.25 s; `UpUp` grows the old symbol to
  1.25x (curve 0.33, 0, 0.83, 0.83) while it fades and grows the new one in at the same time. The opacity pairing
  of the fades and UpUp's incoming motion are inferred from the record layout. MagicReplace morphs layers that share a tag and outline structure.
- **Still approximate:** the reveal is computed on a 112×112 raster by distance through the shape; Apple clips each
  stroke's outline contour between its rail ties with `RB::Path::ClipStroke`, whose code is inside `CoreGlyphsLib` but
  hasn't been ported: it is a full geometric stroker (`Clipper::run` splits each segment at keyframes, then emits
  outline pieces, corner joins, bisections and caps), driven by Swift-side tie/keyframe building. The keyframes themselves are built by the SF Symbols app's export step (`DrawEditableSymbol.tiesForSubpath`,
  `ClipStrokeKeyframeSourceBuilder.merge`) and are not stored in the font tables, so only the ties are available here.
  The raster reveal is the substitute. Wiggle `Forward`/`Backward` follow the
  layout direction as in Apple's code, and custom wiggle angles use Apple's screen convention (270° is up).
- 162 symbols use merged outlines whose layer data doesn't map onto the glyph; they render correctly but as one
  layer, so by-layer effects treat them as a single piece (`symbol.hasExactLayers == false`).
- Small/Large scales are stored exactly for all three weight masters (an affine fit plus integer residuals).
- Geometry is decoded lazily from compact packed blocks; keep symbol access on the UI thread.
- Compiles for JVM, Android, JS and wasm; the iOS targets weren't built (needs macOS).
- `preview.html` (generated locally) is a live browser preview of every symbol, weight, mode and effect.

# Usage Guide — SF Symbols for Jetpack Compose

This document details implementation patterns, styling conventions, and architectural best practices for utilizing SF Symbols Compose in production Jetpack Compose and Compose Multiplatform codebases.

---

## Table of Contents
1. [Basic Symbol Implementation](#1-basic-symbol-implementation)
2. [Dualtone vs Monochrome Variants](#2-dualtone-vs-monochrome-variants)
3. [Material 3 Integration](#3-material-3-integration)
4. [Styling and Modifiers](#4-styling-and-modifiers)
5. [In-App Search and Runtime Catalog](#5-in-app-search-and-runtime-catalog)
6. [Low-Level Canvas Drawing](#6-low-level-canvas-drawing)
7. [Compose Multiplatform Compatibility](#7-compose-multiplatform-compatibility)

---

## 1. Basic Symbol Implementation

All vector symbols are exposed via the `SfSymbols` namespace with type-safe autocompletion:

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFHeartFill
import com.composables.sfsymbols.monochrome.SFStarFill

@Composable
fun BasicExample() {
    // Dualtone symbol with custom tint
    Icon(
        imageVector = SfSymbols.Dualtone.SFHeartFill,
        contentDescription = "Favorite",
        tint = Color(0xFFFF2D55),
        modifier = Modifier.size(32.dp)
    )

    // Monochrome symbol
    Icon(
        imageVector = SfSymbols.Monochrome.SFStarFill,
        contentDescription = "Star",
        tint = Color(0xFFFF9500),
        modifier = Modifier.size(24.dp)
    )
}
```

---

## 2. Dualtone vs Monochrome Variants

### Dualtone (Default)
Dualtone symbols contain layered alpha channels (`0.21` secondary opacity and `0.85` primary opacity). Applying a `tint` color automatically propagates the tint across all opacity layers:

```kotlin
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFCheckmarkCircleFill

Icon(
    imageVector = SfSymbols.Dualtone.SFCheckmarkCircleFill,
    contentDescription = "Completed",
    tint = Color(0xFF007AFF)
)
```

### Monochrome
Monochrome symbols render uniform, single-layer vectors without internal opacity divisions:

```kotlin
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFCheckmarkCircleFill

Icon(
    imageVector = SfSymbols.Monochrome.SFCheckmarkCircleFill,
    contentDescription = "Completed",
    tint = Color(0xFF007AFF)
)
```

---

## 3. Material 3 Integration

### Buttons & Action Components
```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFPlus
import com.composables.sfsymbols.dualtone.SFTrashFill

@Composable
fun ActionComponents() {
    Button(onClick = { /* Handle action */ }) {
        Icon(
            imageVector = SfSymbols.Dualtone.SFTrashFill,
            contentDescription = null
        )
        Text(text = " Remove Entry")
    }

    FloatingActionButton(onClick = { /* Handle create */ }) {
        Icon(
            imageVector = SfSymbols.Dualtone.SFPlus,
            contentDescription = "Create"
        )
    }
}
```

### Navigation Bars
```kotlin
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFHouseFill
import com.composables.sfsymbols.dualtone.SFMagnifyingglass
import com.composables.sfsymbols.dualtone.SFPersonFill

@Composable
fun NavigationBarExample() {
    var selectedTab by remember { mutableStateOf(0) }

    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = { Icon(SfSymbols.Dualtone.SFHouseFill, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = { Icon(SfSymbols.Dualtone.SFMagnifyingglass, contentDescription = "Search") },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = { Icon(SfSymbols.Dualtone.SFPersonFill, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}
```

---

## 4. Styling and Modifiers

Symbols preserve their original aspect ratio, with their longest side set to `24.dp` by default.
With Material 3 `Icon`, `Modifier.size(24.dp)` defines a square layout slot; wide and tall symbols
fit inside that slot without stretching. Use a smaller size, such as `20.dp`, to reduce the icon.
When drawing a vector directly on a Canvas, fit it uniformly to the available space as well.

Icons accept all standard Compose layout and drawing modifiers:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFBellFill

@Composable
fun NotificationAvatar() {
    Icon(
        imageVector = SfSymbols.Dualtone.SFBellFill,
        contentDescription = "Notifications",
        tint = Color(0xFF007AFF),
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF007AFF).copy(alpha = 0.12f))
            .clickable { /* Trigger action */ }
            .padding(10.dp)
    )
}
```

---

## 5. In-App Search and Runtime Catalog

The `SfSymbolsCatalog` object provides indexed runtime lookup for all 7,007 symbols:

```kotlin
import com.composables.sfsymbols.SfSymbolsCatalog

// Search across symbol names, Apple names, and categories
val searchResults = SfSymbolsCatalog.search("connectivity")

// Direct identifier lookup
val symbolMetadata = SfSymbolsCatalog.findByAppleName("wifi.slash")
// symbolMetadata?.pascalName -> "SFWifiSlash"
// symbolMetadata?.categories -> ["Connectivity"]
// symbolMetadata?.isRestricted -> false
```

---

## 6. Low-Level Canvas Drawing

Render vectors directly onto custom Compose `Canvas` contexts:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.dualtone.SFHeartFill

@Composable
fun CanvasRenderedSymbol() {
    val vectorPainter = rememberVectorPainter(image = SfSymbols.Dualtone.SFHeartFill)

    Canvas(modifier = Modifier.size(64.dp)) {
        val intrinsicSize = vectorPainter.intrinsicSize
        val scale = minOf(size.width / intrinsicSize.width, size.height / intrinsicSize.height)
        val fittedSize = Size(intrinsicSize.width * scale, intrinsicSize.height * scale)
        translate((size.width - fittedSize.width) / 2f, (size.height - fittedSize.height) / 2f) {
            with(vectorPainter) {
                draw(size = fittedSize)
            }
        }
    }
}
```

---

## 7. Compose Multiplatform Compatibility

The underlying `ImageVector` definitions are completely agnostic of the underlying operating system and run on:
- Android (`minSdk = 21`)
- Desktop JVM (Windows, macOS, Linux)
- iOS via Compose Multiplatform
- Web via Kotlin/Wasm and Kotlin/JS

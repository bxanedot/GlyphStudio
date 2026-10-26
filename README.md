<div align="center">
  <img src="app/src/jvmMain/resources/app_icon.png" alt="Glyph Studio app icon" width="112" />
  <h1>Glyph Studio</h1>
  <p><strong>Explore SF Symbols. Build with them in Compose.</strong></p>
  <p>A symbol catalog for desktop and a reusable icon library for Jetpack Compose and Kotlin Multiplatform.</p>

  <a href="https://github.com/bxanedot/GlyphStudio/releases"><img src="https://img.shields.io/github/v/release/bxanedot/GlyphStudio?display_name=tag&sort=semver&style=flat-square" alt="Latest release" /></a>
  <a href="https://github.com/bxanedot/GlyphStudio/actions/workflows/release.yml"><img src="https://img.shields.io/github/actions/workflow/status/bxanedot/GlyphStudio/release.yml?style=flat-square&label=installers" alt="Installer builds" /></a>
  <img src="https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.4.20" />
  <img src="https://img.shields.io/badge/Compose_Multiplatform-1.12.1-4285F4?style=flat-square" alt="Compose Multiplatform 1.12.1" />
</div>

---

## One project, two tools

| Glyph Studio Catalog | Compose Symbols |
| --- | --- |
| Search and browse more than 7,000 symbols | Add vector symbols to Compose interfaces |
| Preview symbols, switch rendering styles, and save favorites | Use in Android, desktop, and Kotlin Multiplatform projects |
| Copy symbol names and ready-to-use snippets | Tune weight, scale, color, and animation |

## Get Glyph Studio

Download the latest installer from [Releases](https://github.com/bxanedot/GlyphStudio/releases/latest):

- **Windows:** `.msi`
- **macOS:** `.dmg`
- **Linux:** `.deb`

Installers are built on their target operating systems by GitHub Actions when a `v*` tag is pushed. macOS downloads are unsigned unless signing secrets are configured.

## Use the Compose library

The root build already includes `:sfsymbols`. To use it from another project, place the module beside your app module and add it to the build:

```kotlin
// settings.gradle.kts
include(":sfsymbols")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":sfsymbols"))
}
```

Then use a symbol in your UI:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbol
import com.composables.sfsymbols.SfSymbolIcon
import com.composables.sfsymbols.SfWeight

@Composable
fun FavoriteAction() {
    SfSymbolIcon(
        symbol = SfSymbol["heart.fill"],
        contentDescription = "Favorite",
        size = 24.dp,
        weight = SfWeight.Semibold,
    )
}
```

See the [installation guide](docs/INSTALLATION.md), [usage guide](docs/USAGE.md), and [animation guide](docs/ANIMATION.md).

## Build from source

Requires JDK 21 or newer.

```powershell
./gradlew.bat :app:run
./gradlew.bat :sfsymbols:build
```

On macOS or Linux, use `./gradlew` with the same tasks.

## Apple SF Symbols

Glyph Studio is an independent project and is not affiliated with Apple. SF Symbols and related artwork are Apple property; follow Apple's [SF Symbols usage terms](https://developer.apple.com/design/human-interface-guidelines/sf-symbols/) and restrictions. The symbols must not be used in app icons, logos, or other trademark use.

## Project layout

```text
app/         Desktop catalog app
sfsymbols/   Compose Multiplatform symbol library
docs/        Library guides
```

**Developed by [bxane](https://github.com/bxanedot).**

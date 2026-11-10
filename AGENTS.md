# AGENTS.md

Guide for coding agents working in this repo. Human-facing docs are in [README.md](README.md) and [docs/](docs/).

## What this is

Two Gradle modules in one build:

| Module | What | Targets |
| --- | --- | --- |
| `sfsymbols/` | Compose Multiplatform SF Symbols library (`com.composables.sfsymbols`), published through JitPack | Android, JVM, iOS, JS, Wasm |
| `app/` | GlyphStudio desktop catalog (`com.sfsymbols`), plus a headless MCP server | JVM desktop only |

`app` depends on `:sfsymbols` for every icon and the symbol catalog. It has no icon data of its own.

## Commands

JDK 21+. On Windows use `gradlew.bat`, elsewhere `./gradlew`.

| Task | Command | Time (warm) |
| --- | --- | --- |
| App tests (search, MCP, view model, logo) | `./gradlew :app:jvmTest` | ~15 s |
| Library tests (headless rendering) | `./gradlew :sfsymbols:jvmTest` | ~20 s warm |
| Compile the library for JVM only | `./gradlew :sfsymbols:compileKotlinJvm` | ~1.5 min cold |
| Run the desktop app | `./gradlew :app:run` | |
| Build the MCP server | `./gradlew :app:assembleMcp` (output: `app/build/mcp/`) | |
| MCP smoke test over real stdio | `python scripts/test_mcp.py` (after `assembleMcp`) | |
| Regenerate the app icons | `python scripts/render_icons.py` (needs Pillow) | |
| Installers | `./gradlew :app:packageMsi` / `packageDmg` / `packageDeb` (on that OS) | |

Prefer the JVM-only tasks. `./gradlew build` compiles the library for every target, which takes a long time and needs the Android SDK. The 14k generated files are why `gradle.properties` sets a 6.5 GB heap and in-process Kotlin compilation. Leave those settings alone.

## Generated code: don't hand-edit

| Path | Contents |
| --- | --- |
| `sfsymbols/src/commonMain/kotlin/com/composables/sfsymbols/monochrome/` and `dualtone/` | 7,007 files each, one `ImageVector` property per symbol |
| `sfsymbols/src/commonMain/kotlin/com/composables/sfsymbols/core/data/` | Compressed glyph blobs and catalog tables for the `SfSymbol` engine |
| `sfsymbols/src/commonMain/kotlin/com/composables/sfsymbols/SfSymbolsCatalog.kt` | The 7,007-line name/category table (hand-written code at the bottom) |

These paths are listed in `.ignore`, so ripgrep-based search skips them. To find a symbol, don't search them: use `SfSymbolsCatalog.findByAppleName("heart.fill")` / `findByPascalName("SFHeartFill")`, the MCP `get_symbol` tool, or `rg --no-ignore`. The generator (`scripts/sf_encode.py`, named in the file headers) is **not in this repo**, so a change to symbol data can't be regenerated here. Treat that data as read-only.

## Naming

- Apple name `heart.fill` → Pascal name `SFHeartFill` → property `SfSymbols.Dualtone.SFHeartFill` / `SfSymbols.Monochrome.SFHeartFill`, imported from `com.composables.sfsymbols.dualtone.SFHeartFill` (or `.monochrome.`).
- The engine API is `SfSymbol["heart.fill"]`, drawn with `SfSymbolIcon(...)` (weights, scales, rendering modes, effects). See [docs/ANIMATION.md](docs/ANIMATION.md).

## App map (`app/src/jvmMain/kotlin/`)

| File | Role |
| --- | --- |
| `com/sfsymbols/Main.kt` | Entry point. `--mcp` runs the MCP server on stdio; otherwise it opens the window |
| `com/sfsymbols/viewmodel/CatalogViewModel.kt` | All UI state (Compose `mutableStateOf`, no Android ViewModel) |
| `com/sfsymbols/data/FuzzySearch.kt` | Ranked, typo-tolerant search; tags come from `resources/symbol_tags.txt` via `SymbolTags.kt` |
| `com/sfsymbols/data/SfSymbolIconResolver.kt` | Looks up a symbol's `ImageVector` by name through JVM reflection on the library's facade classes |
| `com/sfsymbols/data/PinnedSymbols.kt` | Favorites, saved to `~/.sf-symbols-catalog/pins.json` |
| `com/sfsymbols/mcp/McpServer.kt` | MCP over stdio (JSON-RPC 2.0) with tools `search_symbols` and `get_symbol` |
| `com/sfsymbols/ui/*` | macOS-styled UI (`dev.nucleusframework` compose-macos-ui); `AppLogo.kt` is the vector logo |
| `com/sfsymbols/util/ComposeSnippet.kt` | Builds the "Copy Code" snippet; covered by `CatalogViewModelTest` |

## Using the MCP server from an agent

The installed app is the server: launching it with `--mcp` serves MCP on stdio instead of opening a window. Point your client at the installed app (**Settings → MCP** in the app shows this config with the exact path):

```json
{ "mcpServers": { "glyphstudio": {
  "command": "<installed app>",
  "args": ["--mcp"]
} } }
```

Default `<installed app>` paths:

| OS | Path |
| --- | --- |
| Windows (per-user install) | `%LOCALAPPDATA%\GlyphStudio\GlyphStudio.exe` (write it with `/` or `\\` in JSON) |
| macOS | `/Applications/GlyphStudio.app/Contents/MacOS/GlyphStudio` |
| Linux (.deb) | `/opt/glyphstudio/bin/GlyphStudio` |

Without the app installed, build the server from source with `./gradlew :app:assembleMcp` and use `"command": "java"`, `"args": ["-Djava.awt.headless=true", "-cp", "/abs/path/app/build/mcp/lib/*", "com.sfsymbols.MainKt", "--mcp"]` (Java 21+).

`search_symbols(query, category?, limit?)` returns ranked matches. `get_symbol(code)` returns one symbol with its categories, the `SfSymbols.Dualtone/Monochrome` expression and the import for each variant.

## Conventions

- Kotlin official style. Public API uses explicit `public` (the library is published).
- The logo geometry lives in two places: `ui/AppLogo.kt` and `scripts/render_icons.py`. If you change one, change the other and rerun the script.
- Commit identity: past commits use `bxanedot <332934531+bxanedot@users.noreply.github.com>`.
- SF Symbols are Apple property. Don't use them in app icons or logos (see README).

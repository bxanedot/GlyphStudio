package com.sfsymbols.data

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.sfsymbols.SfSymbolMetadata
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.SfSymbolsCatalog
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves an [ImageVector] for a metadata entry in the requested [SymbolMode].
 *
 * Each icon in the `:sfsymbols` library is a top-level extension property on
 * `SfSymbols.Dualtone` / `SfSymbols.Monochrome`, which compiles to a facade
 * class `com.composables.sfsymbols.<mode>.<PascalName>Kt` with a static getter
 * `get<PascalName>(scope)`. We look these up once per symbol via JVM reflection
 * and cache the resulting vectors in memory.
 */
public object SfSymbolIconResolver {

    private data class Key(val pascalName: String, val mode: SymbolMode)

    private val cache = ConcurrentHashMap<Key, ImageVector>()

    public fun resolve(meta: SfSymbolMetadata, mode: SymbolMode): ImageVector? {
        val key = Key(meta.pascalName, mode)
        cache[key]?.let { return it }
        val (pkg, scope) = when (mode) {
            SymbolMode.Dualtone -> "dualtone" to SfSymbols.Dualtone
            SymbolMode.Monochrome -> "monochrome" to SfSymbols.Monochrome
        }
        return runCatching {
            val facade = Class.forName("com.composables.sfsymbols.$pkg.${meta.pascalName}Kt")
            val getter = facade.methods.firstOrNull { it.name == "get${meta.pascalName}" && it.parameterCount == 1 }
                ?: return null
            (getter.invoke(null, scope) as? ImageVector)?.also { cache[key] = it }
        }.getOrNull()
    }

    /** Looks up a symbol by its Apple name (e.g. "heart.fill"). */
    public fun resolve(appleName: String, mode: SymbolMode): ImageVector? =
        SfSymbolsCatalog.findByAppleName(appleName)?.let { resolve(it, mode) }
}

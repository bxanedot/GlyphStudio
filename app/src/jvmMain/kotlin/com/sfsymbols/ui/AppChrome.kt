package com.sfsymbols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.sfsymbols.data.SymbolMode
import com.sfsymbols.loadAppIcon
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.PopupButton
import dev.nucleusframework.macoscompose.components.SearchField
import dev.nucleusframework.macoscompose.components.Surface
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.components.TitleBar
import dev.nucleusframework.macoscompose.icons.Icon
import dev.nucleusframework.macoscompose.icons.LucideLayoutGrid
import dev.nucleusframework.macoscompose.icons.LucideList
import dev.nucleusframework.macoscompose.theme.GlassMaterialSize
import dev.nucleusframework.macoscompose.theme.MacosTheme

private val GRID_SIZE_OPTIONS = listOf("50%", "70%", "100%", "130%", "160%")
private val GRID_SIZE_SCALES = listOf(0.5f, 0.7f, 1f, 1.3f, 1.6f)

@Composable
public fun AppChrome(viewModel: CatalogViewModel, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TitleBar(title = { Text(viewModel.selectedCategory ?: if (viewModel.onlyPinned) "Favorites" else "All Symbols") })
        BrowserHeader(viewModel)
        Row(Modifier.fillMaxWidth().weight(1f)) {
            SidebarPane(viewModel, Modifier.fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                GalleryToolbar(viewModel)
                SymbolGrid(viewModel = viewModel, modifier = Modifier.weight(1f).fillMaxWidth())
            }
            DetailPane(viewModel, Modifier.width(290.dp).fillMaxHeight().padding(top = 10.dp, end = 10.dp, bottom = 10.dp))
        }
    }
}

@Composable
private fun BrowserHeader(viewModel: CatalogViewModel) {
    val cs = MacosTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandMark(Modifier.size(28.dp))
        Text("GlyphStudio", color = cs.textPrimary)
        Spacer(Modifier.weight(1f))
        SearchField(
            value = viewModel.query,
            onValueChange = viewModel::search,
            placeholder = "Search symbols",
            modifier = Modifier.width(260.dp),
        )
        ThemeSelector(viewModel.darkTheme, viewModel::updateDarkTheme)
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    val painter: Painter = androidx.compose.runtime.remember { loadAppIcon() }
    androidx.compose.foundation.Image(painter, contentDescription = "GlyphStudio", modifier = modifier)
}

@Composable
private fun GalleryToolbar(viewModel: CatalogViewModel) {
    val cs = MacosTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("${viewModel.filteredSymbols.size} symbols", color = cs.textSecondary)
        Spacer(Modifier.weight(1f))
        PopupButton(
            items = listOf("Dualtone", "Monochrome"),
            selectedIndex = if (viewModel.mode == SymbolMode.Dualtone) 0 else 1,
            onSelectedChange = { viewModel.updateMode(if (it == 0) SymbolMode.Dualtone else SymbolMode.Monochrome) },
            modifier = Modifier.width(136.dp),
            itemText = { it },
        )
        PopupButton(
            items = GRID_SIZE_OPTIONS,
            selectedIndex = nearestGridIndex(viewModel.gridScale),
            onSelectedChange = { viewModel.updateGridScale(GRID_SIZE_SCALES[it]) },
            modifier = Modifier.width(88.dp),
            itemText = { it },
        )
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(cs.surface.copy(alpha = 0.65f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LucideLayoutGrid, modifier = Modifier.size(30.dp).padding(8.dp).clickable { if (viewModel.listView) viewModel.toggleListView() })
            Icon(LucideList, modifier = Modifier.size(30.dp).padding(8.dp).clickable { if (!viewModel.listView) viewModel.toggleListView() })
        }
    }
}

private fun nearestGridIndex(scale: Float): Int = GRID_SIZE_SCALES.indices.minByOrNull { kotlin.math.abs(GRID_SIZE_SCALES[it] - scale) } ?: 2

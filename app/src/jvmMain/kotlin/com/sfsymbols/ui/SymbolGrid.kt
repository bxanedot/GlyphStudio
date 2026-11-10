package com.sfsymbols.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbolMetadata
import com.sfsymbols.data.SfSymbolIconResolver
import com.sfsymbols.data.SymbolMode
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.ScrollbarState
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.components.VerticalScrollbar
import dev.nucleusframework.macoscompose.theme.MacosTheme

private val FAVORITE_RED = Color(0xFFE0245E)

/**
 * Center pane: grid (or list) of symbols for the current filter/mode. Grid cell
 * size is scaled by [CatalogViewModel.gridScale]; each item has a favorite toggle.
 */
@Composable
public fun SymbolGrid(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    if (viewModel.listView) {
        SymbolList(viewModel, modifier)
        return
    }
    val gridState = rememberLazyGridState()
    val scrollbarState = remember { GridScrollbarState(gridState) }
    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = (110f * viewModel.gridScale).dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(viewModel.filteredSymbols, key = { it.appleName }) { meta ->
                SymbolCard(
                    meta = meta,
                    mode = viewModel.mode,
                    favorite = viewModel.isFavorite(meta),
                    selected = viewModel.selectedSymbol?.appleName == meta.appleName,
                    onClick = { viewModel.selectSymbol(meta) },
                    onToggleFavorite = { viewModel.toggleFavorite(meta) },
                )
            }
        }
        VerticalScrollbar(
            state = scrollbarState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
        )
    }
}

/** List-view alternative to the adaptive grid: one row per symbol. */
@Composable
private fun SymbolList(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scrollbarState = remember { LazyListScrollbarState(listState) }
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            lazyItems(viewModel.filteredSymbols, key = { it.appleName }) { meta ->
                SymbolListRow(
                    meta = meta,
                    mode = viewModel.mode,
                    favorite = viewModel.isFavorite(meta),
                    selected = viewModel.selectedSymbol?.appleName == meta.appleName,
                    onClick = { viewModel.selectSymbol(meta) },
                    onToggleFavorite = { viewModel.toggleFavorite(meta) },
                )
            }
        }
        VerticalScrollbar(
            state = scrollbarState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
        )
    }
}

/** Adapts a [LazyListState] to the macos-ui [ScrollbarState] interface. */
internal class LazyListScrollbarState(
    private val state: LazyListState,
) : ScrollbarState {
    override val scrollOffsetPx: Float
        get() = state.firstVisibleItemScrollOffset.toFloat() + state.firstVisibleItemIndex * itemHeight()

    override val maxScrollPx: Float
        get() {
            val li = state.layoutInfo
            if (li.totalItemsCount == 0) return 0f
            return (li.totalItemsCount * itemHeight() - li.viewportSize.height).coerceAtLeast(0).toFloat()
        }

    override val isScrollInProgress: Boolean
        get() = state.isScrollInProgress

    private fun itemHeight(): Int = state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.size } ?: 0

    override suspend fun scrollTo(px: Float) {
        val h = itemHeight()
        if (h == 0) return
        val index = (px / h).toInt().coerceAtLeast(0)
        state.scrollToItem(index, (px - index * h).toInt().coerceAtLeast(0))
    }
}

/**
 * Adapts a [LazyGridState] to the macos-ui [ScrollbarState] interface so the
 * native vertical scrollbar can drive the adaptive symbol grid.
 */
internal class GridScrollbarState(
    private val state: LazyGridState,
) : ScrollbarState {
    override val scrollOffsetPx: Float
        get() = state.layoutInfo.viewportStartOffset.toFloat()

    private fun columns(): Int = state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.column + 1 } ?: 1

    private fun rowHeight(): Int = state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.size.height } ?: 0

    override val maxScrollPx: Float
        get() {
            val li = state.layoutInfo
            val cols = columns()
            val rh = rowHeight()
            if (rh == 0) return 0f
            val rows = (li.totalItemsCount + cols - 1) / cols
            return (rows * rh - li.viewportSize.height).coerceAtLeast(0).toFloat()
        }

    override val isScrollInProgress: Boolean
        get() = state.isScrollInProgress

    override suspend fun scrollTo(px: Float) {
        val cols = columns()
        val rh = rowHeight()
        if (rh == 0) return
        val row = (px / rh).toInt().coerceAtLeast(0)
        state.scrollToItem(row * cols, scrollOffset = (px - row * rh).toInt().coerceAtLeast(0))
    }
}

/** Heart toggle shared by the grid, list and inspector; drawn with the SF heart symbols. */
@Composable
internal fun FavoriteIcon(favorite: Boolean, inactiveTint: Color, modifier: Modifier = Modifier) {
    val vector = remember(favorite) {
        SfSymbolIconResolver.resolve(if (favorite) "heart.fill" else "heart", SymbolMode.Monochrome)
    } ?: return
    Icon(
        imageVector = vector,
        contentDescription = if (favorite) "Remove favorite" else "Add favorite",
        tint = if (favorite) FAVORITE_RED else inactiveTint,
        modifier = modifier,
    )
}

@Composable
private fun SymbolCard(
    meta: SfSymbolMetadata,
    mode: SymbolMode,
    favorite: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val cs = MacosTheme.colorScheme
    val vector = remember(meta, mode) { SfSymbolIconResolver.resolve(meta, mode) } ?: return

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val shape = RoundedCornerShape(12.dp)
    val bg by animateColorAsState(
        when {
            selected -> cs.accent
            hovered -> cs.textPrimary.copy(alpha = 0.10f)
            else -> cs.textPrimary.copy(alpha = 0.05f)
        },
        tween(150),
        label = "card-bg",
    )
    // SF Symbols app style: borderless rounded tile, name underneath.
    Column(
        modifier = Modifier
            .hoverable(interactionSource)
            .clickable(indication = null, interactionSource = interactionSource, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(shape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            // Square bounds: Icon paints with ContentScale.Fit, keeping the true aspect ratio.
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = if (selected) cs.onAccent else cs.textPrimary,
                modifier = Modifier.fillMaxSize(0.42f),
            )

            // Favorite heart (top-end corner, own click area); shown on hover or when set.
            if (favorite || hovered) {
                val heartBg by animateColorAsState(
                    if (favorite) FAVORITE_RED.copy(alpha = 0.2f) else Color.Transparent,
                    tween(150),
                    label = "heart-bg",
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(heartBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleFavorite,
                        )
                        .padding(4.dp),
                ) {
                    FavoriteIcon(favorite, cs.textTertiary, Modifier.size(16.dp))
                }
            }
        }
        Text(
            text = meta.appleName,
            color = cs.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun SymbolListRow(
    meta: SfSymbolMetadata,
    mode: SymbolMode,
    favorite: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val cs = MacosTheme.colorScheme
    val vector = remember(meta, mode) { SfSymbolIconResolver.resolve(meta, mode) } ?: return
    val shape = RoundedCornerShape(8.dp)
    val bg by animateColorAsState(
        if (selected) cs.accent.copy(alpha = 0.12f) else Color.Transparent,
        tween(150),
        label = "row-bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(shape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = cs.textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = meta.appleName,
            color = cs.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        FavoriteIcon(
            favorite = favorite,
            inactiveTint = cs.textTertiary,
            modifier = Modifier
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleFavorite,
                )
                .padding(4.dp)
                .size(18.dp),
        )
    }
}

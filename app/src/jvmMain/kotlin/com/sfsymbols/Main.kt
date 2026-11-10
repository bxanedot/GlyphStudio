package com.sfsymbols

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPosition
import com.sfsymbols.mcp.McpServer
import com.sfsymbols.ui.AppLogo
import com.sfsymbols.ui.SfSymbolsCatalogApp
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.macoscompose.window.MacosDecoratedWindow

fun main(args: Array<String>) {
    if ("--mcp" in args) {
        McpServer().runBlockingUntilExit()
        return
    }
    launchCatalog()
}

private fun launchCatalog() = nucleusApplication {
    MacosDecoratedWindow(
        onCloseRequest = { exitApplication() },
        title = "GlyphStudio",
        icon = rememberVectorPainter(AppLogo),
        state = WindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1280.dp, 820.dp),
        ),
        minimumSize = DpSize(1000.dp, 680.dp),
    ) {
        SfSymbolsCatalogApp()
    }
}

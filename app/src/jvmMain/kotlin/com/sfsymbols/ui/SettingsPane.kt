package com.sfsymbols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sfsymbols.util.openUrl
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.GroupBox
import dev.nucleusframework.macoscompose.components.NavigationButtons
import dev.nucleusframework.macoscompose.components.PushButton
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.components.TitleBar
import dev.nucleusframework.macoscompose.components.TitleBarButtonGroup
import dev.nucleusframework.macoscompose.components.TitleBarGroupButton
import dev.nucleusframework.macoscompose.icons.Icon
import dev.nucleusframework.macoscompose.icons.LucideChevronLeft
import dev.nucleusframework.macoscompose.theme.MacosTheme
import com.sfsymbols.mcp.mcpClientConfig

private const val DEVELOPER = "bxane"
private const val GITHUB_URL = "https://github.com/bxanedot"

/** Settings screen: MCP setup for AI agents and developer links. */
@Composable
public fun SettingsPane(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    val cs = MacosTheme.colorScheme
    // jpackage sets this to the installed launcher; it is absent when running from Gradle.
    val mcpConfig = remember { mcpClientConfig(System.getProperty("jpackage.app-path")) }
    Column(modifier = modifier.fillMaxSize()) {
        TitleBar(
            navigationActions = {
                NavigationButtons(
                    onBack = { viewModel.navigate(CatalogViewModel.Screen.Catalog) },
                    onForward = {},
                    backEnabled = true,
                    forwardEnabled = false,
                )
            },
            title = {
                Text("Settings")
            },
            actions = {
                TitleBarButtonGroup {
                    TitleBarGroupButton(onClick = { viewModel.navigate(CatalogViewModel.Screen.Catalog) }) {
                        Icon(LucideChevronLeft, modifier = Modifier.size(12.dp))
                    }
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            GroupBox(label = "MCP (AI agent access)", modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI agents can search the catalog by running GlyphStudio as an MCP server. " +
                        "Add the configuration below to your agent's MCP settings. It points at this " +
                        "installed app, so there is nothing else to build or install.",
                    color = cs.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(text = "Available tools", color = cs.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "• search_symbols(query, category?, limit?) — fuzzy, ranked search\n" +
                        "• get_symbol(code) — resolve a pascal or apple name to metadata",
                    color = cs.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(text = "MCP client config", color = cs.textPrimary, fontWeight = FontWeight.SemiBold)
                SelectionContainer {
                    Text(
                        text = mcpConfig,
                        color = cs.textSecondary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                PushButton(onClick = { viewModel.copyMcpConfig(mcpConfig) }) {
                    Text("Copy configuration")
                }
                viewModel.badgeText?.let { message ->
                    Spacer(Modifier.height(6.dp))
                    Text(message, color = cs.textSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            GroupBox(label = "Developer", modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinkRow(label = "Name", value = DEVELOPER)
                    LinkRow(label = "GitHub", value = GITHUB_URL, url = GITHUB_URL)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(text = "Developed by $DEVELOPER", color = cs.textTertiary)
        }
    }
}

@Composable
private fun LinkRow(label: String, value: String, url: String? = null) {
    val cs = MacosTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = url != null) {
                if (url != null) openUrl(url)
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = cs.textTertiary, modifier = Modifier.width(88.dp))
        Text(
            text = value,
            color = cs.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (url != null) {
            Text(text = "↗", color = cs.accent)
        }
    }
}

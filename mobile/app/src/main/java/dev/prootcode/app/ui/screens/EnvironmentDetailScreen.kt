package dev.prootcode.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.ui.terminal.TerminalContent
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentDetailScreen(
    environmentId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val environment =
        remember(environmentId) {
            EnvironmentRegistry(File(context.filesDir, "environments.json"))
                .load()
                .firstOrNull { it.id == environmentId }
        }
    if (environment == null) {
        LaunchedEffect(environmentId) { onBack() }
        return
    }

    var selectedTab by remember { mutableStateOf(TAB_TERMINAL) }
    val tabs =
        remember {
            listOf(
                R.string.detail_tab_terminal,
                R.string.detail_tab_packages,
                R.string.detail_tab_code_server,
            )
        }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(environment.name) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.terminal_back))
                    }
                },
            )
        },
        bottomBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = index == selectedTab,
                        onClick = { selectedTab = index },
                        text = { Text(stringResource(label)) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            TAB_TERMINAL ->
                TerminalContent(
                    environment = environment,
                    modifier = Modifier.padding(padding),
                )
            else ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.detail_coming_soon))
                }
        }
    }
}

private const val TAB_TERMINAL = 0

package dev.prootcode.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
) {
    var theme by remember { mutableStateOf(ThemeChoice.System) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.terminal_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeChoice.entries.forEach { choice ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .selectable(
                                selected = theme == choice,
                                role = Role.RadioButton,
                                onClick = { theme = choice },
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = theme == choice,
                        onClick = null,
                    )
                    Text(
                        text = stringResource(choice.label),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            TextButton(
                onClick = onAbout,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_about))
            }
        }
    }
}

private enum class ThemeChoice(
    val label: Int,
) {
    System(R.string.settings_theme_system),
    Light(R.string.settings_theme_light),
    Dark(R.string.settings_theme_dark),
}

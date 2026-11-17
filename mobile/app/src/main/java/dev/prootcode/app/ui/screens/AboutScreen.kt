package dev.prootcode.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.prootcode.app.BuildConfig
import dev.prootcode.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    val notice by
        produceState("") {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.assets
                            .open("NOTICE.txt")
                            .bufferedReader()
                            .use { it.readText() }
                    }.getOrDefault("")
                }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.terminal_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionHeader(stringResource(R.string.about_version_section))
            GroupedCard {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.about_version_label)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.about_licenses)) },
                    trailingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickable { showLicenses = true },
                )
            }

            SectionHeader(stringResource(R.string.about_community))
            GroupedCard {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.about_github)) },
                    trailingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickable { openUrl(context, REPOSITORY_URL) },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.about_releases)) },
                    trailingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickable { openUrl(context, RELEASES_URL) },
                )
            }
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.about_licenses)) },
            text = {
                Column(
                    modifier =
                        Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text(stringResource(R.string.licenses_close))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun GroupedCard(content: @Composable () -> Unit) {
    Card {
        Column { content() }
    }
}

private fun openUrl(
    context: Context,
    url: String,
) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private const val REPOSITORY_URL = "https://github.com/abrahdev/ProotCode"
private const val RELEASES_URL = "https://github.com/abrahdev/ProotCode/releases"

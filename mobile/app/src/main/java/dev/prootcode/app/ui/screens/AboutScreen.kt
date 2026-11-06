package dev.prootcode.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
    val noticeText by
        produceState<String?>(null) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.assets
                            .open("NOTICE.txt")
                            .bufferedReader()
                            .use { it.readText() }
                    }.getOrNull()
                }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
            Text(text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
            TextButton(
                onClick = { showLicenses = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.about_licenses))
            }
            TextButton(
                onClick = { openUrl(context, GITHUB_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.about_github))
            }
            TextButton(
                onClick = { openUrl(context, RELEASES_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.about_releases))
            }
        }
    }

    if (showLicenses) {
        LicensesDialog(
            text = noticeText.orEmpty(),
            onDismiss = { showLicenses = false },
        )
    }
}

@Composable
private fun LicensesDialog(
    text: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.licenses_close))
            }
        },
        title = { Text(stringResource(R.string.licenses_title)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

private fun openUrl(
    context: Context,
    url: String,
) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private const val GITHUB_URL = "https://github.com/abrahdev/ProotCode"
private const val RELEASES_URL = "https://github.com/abrahdev/ProotCode/releases"

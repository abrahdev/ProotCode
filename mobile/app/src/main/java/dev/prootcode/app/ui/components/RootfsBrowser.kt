package dev.prootcode.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun listRootfsEntries(
    root: File,
    path: String,
): List<FileEntry> {
    val directory = if (path == "/") root else File(root, path.trimStart('/'))
    if (!directory.isDirectory) return emptyList()
    return directory
        .listFiles()
        .orEmpty()
        .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        .map { file ->
            FileEntry(
                name = file.name,
                path = if (path == "/") "/${file.name}" else "$path/${file.name}",
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isFile) file.length() else null,
                mounted = file.absolutePath.contains("/root/mounts"),
            )
        }
}

@Composable
fun RootfsBrowser(
    root: File,
    onOpenFile: (FileEntry) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var path by remember(root) { mutableStateOf(HOME_PATH) }
    var refresh by remember(root) { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<FileEntry?>(null) }
    var pendingShare by remember { mutableStateOf<FileEntry?>(null) }
    val saved = stringResource(R.string.files_saved)
    val deleted = stringResource(R.string.files_deleted)
    val failed = stringResource(R.string.files_operation_failed)

    val shareLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            val entry = pendingShare
            pendingShare = null
            if (uri != null && entry != null) {
                val source = File(root, entry.path.trimStart('/'))
                val ok =
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            source.inputStream().use { input -> input.copyTo(output) }
                        }
                    }.isSuccess
                onMessage(if (ok) saved else failed)
            }
        }

    val entries by
        produceState(initialValue = emptyList<FileEntry>(), path, root, refresh) {
            value = withContext(Dispatchers.IO) { listRootfsEntries(root, path) }
        }

    FileBrowser(
        path = path,
        entries = entries,
        onNavigate = { path = it },
        onOpenFile = onOpenFile,
        onDeleteFile = { pendingDelete = it },
        onShareFile = { entry ->
            pendingShare = entry
            shareLauncher.launch(entry.name)
        },
        parentLabel = stringResource(R.string.files_up),
        modifier = modifier,
    )

    val deleteEntry = pendingDelete
    if (deleteEntry != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.files_delete)) },
            text = {
                Text(stringResource(R.string.files_delete_text, deleteEntry.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        val removed =
                            runCatching { File(root, deleteEntry.path.trimStart('/')).delete() }
                                .getOrDefault(false)
                        if (removed) {
                            refresh += 1
                            onMessage(deleted)
                        } else {
                            onMessage(failed)
                        }
                    },
                ) {
                    Text(stringResource(R.string.files_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private const val HOME_PATH = "/root"

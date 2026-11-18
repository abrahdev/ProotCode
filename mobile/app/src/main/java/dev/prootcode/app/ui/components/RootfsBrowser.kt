package dev.prootcode.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
) {
    var path by remember(root) { mutableStateOf("/") }
    val entries by
        produceState(initialValue = emptyList<FileEntry>(), path, root) {
            value = withContext(Dispatchers.IO) { listRootfsEntries(root, path) }
        }
    FileBrowser(
        path = path,
        entries = entries,
        onNavigate = { path = it },
        onOpenFile = {},
        parentLabel = stringResource(R.string.files_up),
        modifier = modifier,
    )
}

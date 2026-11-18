package dev.prootcode.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R

@Composable
fun ClosableTabRow(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onNew: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (titles.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            val selectedIndex = selected.coerceIn(0, titles.lastIndex)
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier.weight(1f),
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
            ) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                if (index == selectedIndex) {
                                    IconButton(
                                        onClick = { onClose(index) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = stringResource(
                                                R.string.terminal_close_session,
                                            ),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
        if (onNew != null) {
            IconButton(onClick = onNew) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.terminal_new_session_desc),
                )
            }
        }
    }
}

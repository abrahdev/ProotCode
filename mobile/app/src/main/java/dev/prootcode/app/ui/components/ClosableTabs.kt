package dev.prootcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R

@Composable
fun ClosableTabRow(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (titles.isEmpty()) {
        Row(modifier = modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
        }
        return
    }

    val selectedIndex = selected.coerceIn(0, titles.lastIndex)
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(titles, key = { _, title -> title }) { index, title ->
            val active = index == selectedIndex
            val tabColor = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(tabColor)
                        .clickable { onSelect(index) }
                        .padding(
                            start = 12.dp,
                            end = if (active && titles.size > 1) 2.dp else 12.dp,
                            top = 6.dp,
                            bottom = 6.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    style = MaterialTheme.typography.labelLarge,
                )
                if (active && titles.size > 1) {
                    IconButton(
                        onClick = { onClose(index) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.terminal_close_session),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

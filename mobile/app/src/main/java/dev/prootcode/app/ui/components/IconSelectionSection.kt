package dev.prootcode.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R

@Composable
fun IconSelectionSection(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAll by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxWidth()) {
        IconOptionGrid(
            selectedId = selectedId,
            onSelect = onSelect,
        )
        TextButton(onClick = { showAll = !showAll }) {
            Text(
                stringResource(
                    if (showAll) R.string.icons_fewer else R.string.icons_more,
                ),
            )
        }
        if (showAll) {
            LabeledTextField(
                label = stringResource(R.string.icons_search),
                value = query,
                onValueChange = { query = it },
            )
            IconPickerGrid(
                icons =
                    ProotIcons.ALL.filter {
                        query.isBlank() || it.id.contains(query.trim().lowercase())
                    },
                onSelect = onSelect,
                modifier = Modifier.height(320.dp),
            )
        }
    }
}

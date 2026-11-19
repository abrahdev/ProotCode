package dev.prootcode.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconSelectionSection(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        LabeledTextField(
            label = stringResource(R.string.icons_search),
            value = query,
            onValueChange = { query = it },
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProotIcons.categories().forEach { name ->
                FilterChip(
                    selected = category == name,
                    onClick = { category = name },
                    label = { Text(name) },
                )
            }
        }
        val filtered =
            ProotIcons.ALL.filter { option ->
                val matchesQuery =
                    query.isBlank() ||
                        option.label.contains(query.trim(), ignoreCase = true) ||
                        option.id.contains(query.trim(), ignoreCase = true)
                val matchesCategory = category == "All" || option.category == category
                matchesQuery && matchesCategory
            }
        IconPickerGrid(
            icons = filtered,
            selectedId = selectedId,
            onSelect = onSelect,
            modifier = Modifier.height(248.dp).padding(top = 8.dp),
        )
    }
}

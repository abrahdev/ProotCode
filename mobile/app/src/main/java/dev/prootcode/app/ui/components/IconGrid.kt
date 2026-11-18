package dev.prootcode.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class IconOption(
    val id: String,
    val icon: ImageVector,
)

object ProotIcons {
    val ALL =
        listOf(
            IconOption("home", Icons.Outlined.Home),
            IconOption("star", Icons.Outlined.Star),
            IconOption("cloud", Icons.Outlined.Cloud),
            IconOption("folder", Icons.Outlined.Folder),
            IconOption("build", Icons.Outlined.Build),
            IconOption("lock", Icons.Outlined.Lock),
            IconOption("bolt", Icons.Outlined.Bolt),
            IconOption("school", Icons.Outlined.School),
            IconOption("work", Icons.Outlined.Work),
            IconOption("storage", Icons.Outlined.Storage),
            IconOption("memory", Icons.Outlined.Memory),
            IconOption("public", Icons.Outlined.Public),
            IconOption("science", Icons.Outlined.Science),
            IconOption("terminal", Icons.Outlined.Terminal),
            IconOption("phone", Icons.Outlined.PhoneAndroid),
            IconOption("code", Icons.Outlined.Code),
            IconOption("server", Icons.Outlined.Dns),
            IconOption("device", Icons.Outlined.DeveloperBoard),
            IconOption("computer", Icons.Outlined.Computer),
            IconOption("android", Icons.Outlined.Android),
            IconOption("backup", Icons.Outlined.Backup),
            IconOption("tree", Icons.Outlined.AccountTree),
            IconOption("download", Icons.Outlined.Download),
            IconOption("key", Icons.Outlined.Key),
            IconOption("person", Icons.Outlined.Person),
        )

    fun resolve(id: String): ImageVector? =
        when (id) {
            "default" -> null
            else -> ALL.firstOrNull { it.id == id }?.icon ?: Icons.Outlined.Category
        }

    fun label(id: String): String = id.replace('_', ' ')
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconOptionGrid(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<IconOption> = ProotIcons.ALL.take(5),
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            Column(
                modifier =
                    Modifier
                        .width(64.dp)
                        .selectable(
                            selected = option.id == selectedId,
                            role = Role.RadioButton,
                            onClick = { onSelect(option.id) },
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MarkBox(
                    text = "",
                    icon = option.icon,
                    selected = option.id == selectedId,
                    size = 56.dp,
                )
                Text(
                    text = ProotIcons.label(option.id),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun IconPickerGrid(
    icons: List<IconOption>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(icons, key = { it.id }) { option ->
            Surface(
                onClick = { onSelect(option.id) },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = ProotIcons.label(option.id),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.ScreenShare
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Wifi
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
    val label: String = id.replace('_', ' ').replaceFirstChar { it.uppercase() },
    val category: String = "",
)

object ProotIcons {
    const val GENERAL = "General"
    const val DEVELOPMENT = "Development"
    const val DATA_FILES = "Data & files"
    const val DEVICE_NETWORK = "Device & network"
    const val MEDIA_DESIGN = "Media & design"
    const val PLACES_MORE = "Places & more"

    val CATEGORIES =
        listOf(GENERAL, DEVELOPMENT, DATA_FILES, DEVICE_NETWORK, MEDIA_DESIGN, PLACES_MORE)

    private const val ALL_CATEGORY = "All"

    val ALL =
        listOf(
            IconOption("home", Icons.Outlined.Home, "Home", GENERAL),
            IconOption("star", Icons.Outlined.Star, "Star", GENERAL),
            IconOption("favorite", Icons.Outlined.Favorite, "Favorite", GENERAL),
            IconOption("search", Icons.Outlined.Search, "Search", GENERAL),
            IconOption("settings", Icons.Outlined.Settings, "Settings", GENERAL),
            IconOption("info", Icons.Outlined.Info, "Info", GENERAL),
            IconOption("help", Icons.AutoMirrored.Outlined.Help, "Help", GENERAL),
            IconOption("notifications", Icons.Outlined.Notifications, "Notifications", GENERAL),
            IconOption("person", Icons.Outlined.Person, "Person", GENERAL),
            IconOption("group", Icons.Outlined.Group, "Group", GENERAL),
            IconOption("check", Icons.Outlined.Check, "Check", GENERAL),
            IconOption("close", Icons.Outlined.Close, "Close", GENERAL),
            IconOption("code", Icons.Outlined.Code, "Code", DEVELOPMENT),
            IconOption("terminal", Icons.Outlined.Terminal, "Terminal", DEVELOPMENT),
            IconOption("build", Icons.Outlined.Build, "Build", DEVELOPMENT),
            IconOption("bug", Icons.Outlined.BugReport, "Bug report", DEVELOPMENT),
            IconOption("api", Icons.Outlined.Api, "API", DEVELOPMENT),
            IconOption("source", Icons.Outlined.Source, "Source", DEVELOPMENT),
            IconOption("data_object", Icons.Outlined.DataObject, "Data object", DEVELOPMENT),
            IconOption("memory", Icons.Outlined.Memory, "Memory", DEVELOPMENT),
            IconOption("dns", Icons.Outlined.Dns, "DNS", DEVELOPMENT),
            IconOption("computer", Icons.Outlined.Computer, "Computer", DEVELOPMENT),
            IconOption("developer_board", Icons.Outlined.DeveloperBoard, "Dev board", DEVELOPMENT),
            IconOption("devices", Icons.Outlined.Devices, "Devices", DEVELOPMENT),
            IconOption("storage", Icons.Outlined.Storage, "Storage", DATA_FILES),
            IconOption("folder", Icons.Outlined.Folder, "Folder", DATA_FILES),
            IconOption("folder_open", Icons.Outlined.FolderOpen, "Folder open", DATA_FILES),
            IconOption("folder_shared", Icons.Outlined.FolderShared, "Shared folder", DATA_FILES),
            IconOption("description", Icons.Outlined.Description, "Document", DATA_FILES),
            IconOption("content_copy", Icons.Outlined.ContentCopy, "Copy", DATA_FILES),
            IconOption("backup", Icons.Outlined.Backup, "Backup", DATA_FILES),
            IconOption("cloud", Icons.Outlined.Cloud, "Cloud", DATA_FILES),
            IconOption("cloud_download", Icons.Outlined.CloudDownload, "Cloud download", DATA_FILES),
            IconOption("cloud_upload", Icons.Outlined.CloudUpload, "Cloud upload", DATA_FILES),
            IconOption("sd_card", Icons.Outlined.SdCard, "SD card", DATA_FILES),
            IconOption("download", Icons.Outlined.Download, "Download", DATA_FILES),
            IconOption("phone_android", Icons.Outlined.PhoneAndroid, "Phone", DEVICE_NETWORK),
            IconOption("android", Icons.Outlined.Android, "Android", DEVICE_NETWORK),
            IconOption("tablet", Icons.Outlined.Tablet, "Tablet", DEVICE_NETWORK),
            IconOption("laptop", Icons.Outlined.Laptop, "Laptop", DEVICE_NETWORK),
            IconOption("watch", Icons.Outlined.Watch, "Watch", DEVICE_NETWORK),
            IconOption("wifi", Icons.Outlined.Wifi, "Wi-Fi", DEVICE_NETWORK),
            IconOption("bluetooth", Icons.Outlined.Bluetooth, "Bluetooth", DEVICE_NETWORK),
            IconOption("network_check", Icons.Outlined.NetworkCheck, "Network", DEVICE_NETWORK),
            IconOption("router", Icons.Outlined.Router, "Router", DEVICE_NETWORK),
            IconOption("usb", Icons.Outlined.Usb, "USB", DEVICE_NETWORK),
            IconOption("cast", Icons.Outlined.Cast, "Cast", DEVICE_NETWORK),
            IconOption("screen_share", Icons.AutoMirrored.Outlined.ScreenShare, "Screen share", DEVICE_NETWORK),
            IconOption("palette", Icons.Outlined.Palette, "Palette", MEDIA_DESIGN),
            IconOption("brush", Icons.Outlined.Brush, "Brush", MEDIA_DESIGN),
            IconOption("image", Icons.Outlined.Image, "Image", MEDIA_DESIGN),
            IconOption("movie", Icons.Outlined.Movie, "Movie", MEDIA_DESIGN),
            IconOption("music_note", Icons.Outlined.MusicNote, "Music", MEDIA_DESIGN),
            IconOption("videocam", Icons.Outlined.Videocam, "Video", MEDIA_DESIGN),
            IconOption("play_arrow", Icons.Outlined.PlayArrow, "Play", MEDIA_DESIGN),
            IconOption("color_lens", Icons.Outlined.ColorLens, "Colors", MEDIA_DESIGN),
            IconOption("text_fields", Icons.Outlined.TextFields, "Text", MEDIA_DESIGN),
            IconOption("draw", Icons.Outlined.Draw, "Draw", MEDIA_DESIGN),
            IconOption("camera", Icons.Outlined.Camera, "Camera", MEDIA_DESIGN),
            IconOption("public", Icons.Outlined.Public, "Web", PLACES_MORE),
            IconOption("language", Icons.Outlined.Language, "Language", PLACES_MORE),
            IconOption("map", Icons.Outlined.Map, "Map", PLACES_MORE),
            IconOption("location_on", Icons.Outlined.LocationOn, "Location", PLACES_MORE),
            IconOption("school", Icons.Outlined.School, "School", PLACES_MORE),
            IconOption("science", Icons.Outlined.Science, "Science", PLACES_MORE),
            IconOption("work", Icons.Outlined.Work, "Work", PLACES_MORE),
            IconOption("calendar_month", Icons.Outlined.CalendarMonth, "Calendar", PLACES_MORE),
            IconOption("today", Icons.Outlined.Today, "Today", PLACES_MORE),
            IconOption("key", Icons.Outlined.Key, "Key", PLACES_MORE),
            IconOption("lock", Icons.Outlined.Lock, "Lock", PLACES_MORE),
            IconOption("security", Icons.Outlined.Security, "Security", PLACES_MORE),
            IconOption("bookmark", Icons.Outlined.Bookmark, "Bookmark", PLACES_MORE),
        )

    fun resolve(id: String): ImageVector? = ALL.firstOrNull { it.id == id }?.icon

    fun label(id: String): String = ALL.firstOrNull { it.id == id }?.label ?: id.replace('_', ' ')

    fun categories(): List<String> = listOf(ALL_CATEGORY) + CATEGORIES
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconOptionGrid(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<IconOption> = ProotIcons.ALL.take(6),
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
                    text = option.label,
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
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(icons, key = { it.id }) { option ->
            val isSelected = option.id == selectedId
            Surface(
                onClick = { onSelect(option.id) },
                shape = MaterialTheme.shapes.medium,
                border =
                    BorderStroke(
                        1.dp,
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                modifier = Modifier.size(44.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.label,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

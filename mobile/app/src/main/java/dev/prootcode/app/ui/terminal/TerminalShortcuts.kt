package dev.prootcode.app.ui.terminal

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

enum class ShortcutKind { MODIFIER, KEY, TEXT }

data class TerminalShortcut(
    val id: String,
    val label: String,
    val payload: String,
    val kind: ShortcutKind,
    val enabled: Boolean = true,
    val custom: Boolean = false,
)

object TerminalShortcuts {
    private const val ESC = "\u001b"

    val DEFAULTS =
        listOf(
            TerminalShortcut("esc", "Esc", ESC, ShortcutKind.KEY),
            TerminalShortcut("tab", "Tab", "\t", ShortcutKind.KEY),
            TerminalShortcut("ctrl", "Ctrl", "", ShortcutKind.MODIFIER),
            TerminalShortcut("alt", "Alt", "", ShortcutKind.MODIFIER),
            TerminalShortcut("up", "↑", "$ESC[A", ShortcutKind.KEY),
            TerminalShortcut("down", "↓", "$ESC[B", ShortcutKind.KEY),
            TerminalShortcut("left", "←", "$ESC[D", ShortcutKind.KEY),
            TerminalShortcut("right", "→", "$ESC[C", ShortcutKind.KEY),
            TerminalShortcut("slash", "/", "/", ShortcutKind.TEXT),
            TerminalShortcut("dash", "-", "-", ShortcutKind.TEXT),
            TerminalShortcut("pipe", "|", "|", ShortcutKind.TEXT),
            TerminalShortcut("tilde", "~", "~", ShortcutKind.TEXT),
        )
}

class TerminalShortcutStore(
    private val preferences: SharedPreferences,
) {
    fun load(): List<TerminalShortcut> {
        val raw = preferences.getString(KEY, null) ?: return TerminalShortcuts.DEFAULTS
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length())
                .mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val kind =
                        runCatching { ShortcutKind.valueOf(item.optString("kind")) }.getOrNull()
                            ?: return@mapNotNull null
                    TerminalShortcut(
                        id = item.optString("id"),
                        label = item.optString("label"),
                        payload = item.optString("payload"),
                        kind = kind,
                        enabled = item.optBoolean("enabled", true),
                        custom = item.optBoolean("custom", false),
                    )
                }.ifEmpty { TerminalShortcuts.DEFAULTS }
        }.getOrDefault(TerminalShortcuts.DEFAULTS)
    }

    fun save(shortcuts: List<TerminalShortcut>) {
        val array =
            JSONArray(
                shortcuts.map { shortcut ->
                    JSONObject().apply {
                        put("id", shortcut.id)
                        put("label", shortcut.label)
                        put("payload", shortcut.payload)
                        put("kind", shortcut.kind.name)
                        put("enabled", shortcut.enabled)
                        put("custom", shortcut.custom)
                    }
                },
            )
        preferences.edit().putString(KEY, array.toString()).apply()
    }

    fun reset(): List<TerminalShortcut> {
        preferences.edit().remove(KEY).apply()
        return TerminalShortcuts.DEFAULTS
    }

    companion object {
        private const val FILE = "terminal_shortcuts"
        private const val KEY = "shortcuts"

        fun create(context: Context): TerminalShortcutStore =
            TerminalShortcutStore(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }
}

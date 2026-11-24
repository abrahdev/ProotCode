package dev.prootcode.app.data.environment

data class EnvironmentSettings(
    val icon: String = DEFAULT_ICON,
    val autostart: Boolean = false,
    val language: String = "English",
    val timezone: String = "UTC",
    val mountDownloads: Boolean = true,
    val downloadsPath: String? = null,
    val extraMounts: List<String> = emptyList(),
    val autoCleanup: Boolean = true,
    val codeServerPort: Int = 8080,
    val networkSharing: Boolean = true,
    val proxy: String = "",
    val adbShare: Boolean = false,
    val customUser: Boolean = true,
    val userName: String = "",
    val ssh: Boolean = false,
    val sshPort: Int = 2222,
    val backgroundExecution: Boolean = true,
    val priority: String = PRIORITY_NORMAL,
    val debugMode: Boolean = false,
) {
    companion object {
        const val DEFAULT_ICON = "default"
        const val PRIORITY_LOW = "low"
        const val PRIORITY_NORMAL = "normal"

        val PRIORITIES = listOf(PRIORITY_LOW, PRIORITY_NORMAL)
        val LANGUAGES = listOf("English", "Español", "Deutsch", "Français")
    }
}

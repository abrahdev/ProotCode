package dev.prootcode.app.data.environment

object PresetPackages {
    val essentials = listOf("git", "curl", "wget", "build-essential")
    val editors = listOf("neovim")
    val languages = listOf("python3", "nodejs", "npm", "golang", "rustc", "cargo")
    val utilities = listOf("htop", "ripgrep", "fd-find", "jq", "tmux")

    fun packagesFor(presets: Collection<String>): List<String> =
        presets
            .flatMap { preset ->
                when (preset) {
                    Presets.ESSENTIALS -> essentials
                    Presets.EDITORS -> editors
                    Presets.LANGUAGES -> languages
                    Presets.UTILITIES -> utilities
                    else -> emptyList()
                }
            }.distinct()
}

package dev.prootcode.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val DEFAULT_WIZARD_TOOLS: Map<String, Boolean> =
    mapOf(
        "git" to true,
        "build-essential" to true,
        "curl" to true,
        "ca-certificates" to true,
        "wget" to true,
        "unzip" to true,
        "xz-utils" to true,
        "bzip2" to true,
        "less" to true,
        "nano" to true,
        "code-server" to true,
        "opencode" to false,
        "openssh-client" to true,
        "rsync" to true,
        "iproute2" to true,
        "iputils-ping" to true,
        "bind9-dnsutils" to true,
        "htop" to true,
        "ripgrep" to true,
        "tmux" to true,
    )

data class WizardState(
    val step: Int = 0,
    val name: String = "",
    val distroIndex: Int = 0,
    val versionIndex: Int = 0,
    val tools: Map<String, Boolean> = DEFAULT_WIZARD_TOOLS,
    val mountDownloads: Boolean = false,
    val downloadsPath: String? = null,
    val extraMounts: List<String> = emptyList(),
    val icon: String = "default",
    val adbShare: Boolean = false,
    val customUser: Boolean = true,
    val userName: String = "",
)

class WizardViewModel : ViewModel() {
    private val state = MutableStateFlow(WizardState())
    val uiState: StateFlow<WizardState> = state.asStateFlow()

    fun next() {
        state.update { it.copy(step = (it.step + 1).coerceAtMost(LAST_STEP)) }
    }

    fun back() {
        state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    fun setName(name: String) {
        state.update { it.copy(name = name.take(40)) }
    }

    fun selectDistro(index: Int) {
        state.update { it.copy(distroIndex = index, versionIndex = 0) }
    }

    fun selectVersion(index: Int) {
        state.update { it.copy(versionIndex = index) }
    }

    fun toggleTool(id: String) {
        state.update { it.copy(tools = it.tools + (id to !(it.tools[id] ?: false))) }
    }

    fun setMountDownloads(
        enabled: Boolean,
        path: String? = null,
    ) {
        state.update {
            it.copy(
                mountDownloads = enabled,
                downloadsPath = if (enabled) path else null,
            )
        }
    }

    fun addMount(path: String) {
        state.update { it.copy(extraMounts = (it.extraMounts + path).distinct()) }
    }

    fun removeMount(path: String) {
        state.update { it.copy(extraMounts = it.extraMounts - path) }
    }

    fun setIcon(icon: String) {
        state.update { it.copy(icon = icon) }
    }

    fun setAdbShare(enabled: Boolean) {
        state.update { it.copy(adbShare = enabled) }
    }

    fun setCustomUser(enabled: Boolean) {
        state.update { it.copy(customUser = enabled) }
    }

    fun setUserName(name: String) {
        state.update { it.copy(userName = name.take(32)) }
    }

    fun reset() {
        state.value = WizardState()
    }

    companion object {
        const val LAST_STEP = 4
    }
}

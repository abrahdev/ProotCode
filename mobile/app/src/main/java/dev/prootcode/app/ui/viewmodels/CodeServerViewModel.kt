package dev.prootcode.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.runtime.codeserver.CodeServerManager
import dev.prootcode.app.runtime.codeserver.CodeServerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CodeServerViewModel(
    context: Context,
    private val environmentId: String,
) : ViewModel() {
    private val appContext = context.applicationContext
    private val registry = EnvironmentRegistry(File(appContext.filesDir, "environments.json"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val environmentState = MutableStateFlow<Environment?>(null)
    val environment: StateFlow<Environment?> = environmentState.asStateFlow()

    private val loadedState = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = loadedState.asStateFlow()

    val status: StateFlow<CodeServerStatus> = CodeServerManager.status(environmentId)

    private val reloadState = MutableStateFlow(0)
    val reloadKey: StateFlow<Int> = reloadState.asStateFlow()

    init {
        scope.launch {
            val environment =
                withContext(Dispatchers.IO) {
                    registry.load().firstOrNull { it.id == environmentId }
                }
            environmentState.value = environment
            loadedState.value = true
            if (environment != null) {
                CodeServerManager.start(appContext, environment)
            }
        }
    }

    fun start() {
        environmentState.value?.let { CodeServerManager.start(appContext, it) }
    }

    fun stop() {
        CodeServerManager.stop(environmentId)
    }

    fun reload() {
        reloadState.value += 1
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

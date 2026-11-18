package dev.prootcode.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.environment.EnvironmentRegistry
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

class EnvironmentDetailViewModel(
    context: Context,
    private val environmentId: String,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val registry = EnvironmentRegistry(File(context.filesDir, "environments.json"))

    private val state = MutableStateFlow<Environment?>(null)
    val environment: StateFlow<Environment?> = state.asStateFlow()

    private val loadedState = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = loadedState.asStateFlow()

    init {
        scope.launch {
            state.value =
                withContext(Dispatchers.IO) {
                    registry.load().firstOrNull { it.id == environmentId }
                }
            loadedState.value = true
        }
    }

    fun update(environment: Environment) {
        state.value = environment
        scope.launch { withContext(Dispatchers.IO) { registry.update(environment) } }
    }

    fun rename(name: String) {
        val current = state.value ?: return
        update(current.copy(name = name.take(40)))
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

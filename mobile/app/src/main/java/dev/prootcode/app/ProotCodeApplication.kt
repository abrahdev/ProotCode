package dev.prootcode.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.ui.terminal.TerminalSessionManager
import java.io.File
import java.util.concurrent.Executors

class ProotCodeApplication :
    Application(),
    Application.ActivityLifecycleCallbacks {
    private var startedActivities = 0
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities += 1
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) {
            executor.execute { closeBackgroundDisabledSessions() }
        }
    }

    private fun closeBackgroundDisabledSessions() {
        val environments =
            runCatching {
                EnvironmentRegistry(File(filesDir, "environments.json")).load()
            }.getOrDefault(emptyList())
        environments
            .filterNot { it.settings.backgroundExecution }
            .forEach { environment ->
                TerminalSessionManager.closeAll(this, environment.id)
            }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

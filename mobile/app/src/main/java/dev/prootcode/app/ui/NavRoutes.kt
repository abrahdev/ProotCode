package dev.prootcode.app.ui

object NavRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    const val ENVIRONMENT_ARG = "environmentId"
    const val ENVIRONMENT_PATTERN = "environment/{$ENVIRONMENT_ARG}"

    fun environment(id: String): String = "environment/$id"
}

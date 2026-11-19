package dev.prootcode.app.ui

object NavRoutes {
    const val HOME = "home"
    const val ABOUT = "about"
    const val CHANGELOG = "changelog"

    const val ENVIRONMENT_ARG = "environmentId"
    const val ENVIRONMENT_PATTERN = "environment/{$ENVIRONMENT_ARG}"

    const val TERMINAL_PATTERN = "terminal/{$ENVIRONMENT_ARG}"

    fun environment(id: String): String = "environment/$id"

    fun terminal(id: String): String = "terminal/$id"
}

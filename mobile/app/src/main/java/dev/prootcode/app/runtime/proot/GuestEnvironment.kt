package dev.prootcode.app.runtime.proot

import dev.prootcode.app.data.environment.EnvironmentSettings

object GuestEnvironment {
    fun of(settings: EnvironmentSettings): Map<String, String> =
        buildMap {
            put("TZ", settings.timezone.ifBlank { "UTC" })
            put("LANG", languageLocale(settings.language))
            if (settings.proxy.isNotBlank()) {
                put("http_proxy", settings.proxy)
                put("https_proxy", settings.proxy)
                put("HTTP_PROXY", settings.proxy)
                put("HTTPS_PROXY", settings.proxy)
            }
            if (settings.customUser && settings.userName.isNotBlank()) {
                put("USER", settings.userName)
            }
        }

    fun languageLocale(language: String): String =
        when (language) {
            "Español" -> "es_ES.UTF-8"
            "Deutsch" -> "de_DE.UTF-8"
            "Français" -> "fr_FR.UTF-8"
            else -> "C.UTF-8"
        }
}

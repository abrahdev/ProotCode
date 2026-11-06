pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProotCode"
include(":app")

// Termux terminal modules (vendored)
include(":terminal-emulator")
project(":terminal-emulator").projectDir = file("lib/terminal-emulator")

include(":terminal-view")
project(":terminal-view").projectDir = file("lib/terminal-view")

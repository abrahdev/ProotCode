package dev.prootcode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.ThemeMode
import dev.prootcode.app.ui.NavRoutes
import dev.prootcode.app.ui.codeserver.CodeServerActivity
import dev.prootcode.app.ui.screens.AboutScreen
import dev.prootcode.app.ui.screens.ChangelogScreen
import dev.prootcode.app.ui.screens.EnvironmentDetailScreen
import dev.prootcode.app.ui.screens.HomeScreen
import dev.prootcode.app.ui.terminal.TerminalScreen
import dev.prootcode.app.ui.terminal.TerminalSessionManager
import dev.prootcode.app.ui.theme.ProotCodeTheme
import dev.prootcode.app.ui.util.QuestWindow
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        QuestWindow.applyOrientation(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { AppPreferences.create(applicationContext) }
            val settings by preferences.theme.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme =
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            ProotCodeTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                customHue = settings.customHue,
                hardBackground = settings.hardBackground,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.HOME,
                    ) {
                        composable(NavRoutes.HOME) {
                            HomeScreen(
                                preferences = preferences,
                                onOpenEnvironmentDetail = { environment ->
                                    navController.navigate(NavRoutes.environment(environment.id))
                                },
                                onOpenTerminal = { environment ->
                                    navController.navigate(NavRoutes.terminal(environment.id))
                                },
                                onOpenNativeTerminal = {
                                    navController.navigate(
                                        NavRoutes.terminal(
                                            TerminalSessionManager.NATIVE_ENVIRONMENT_ID,
                                        ),
                                    )
                                },
                                onOpenCodeServer = { environment ->
                                    CodeServerActivity.start(
                                        this@MainActivity,
                                        environment.id,
                                    )
                                },
                            )
                        }
                        composable(NavRoutes.ABOUT) {
                            AboutScreen(
                                onBack = { navController.popBackStack() },
                                onChangelog = { navController.navigate(NavRoutes.CHANGELOG) },
                            )
                        }
                        composable(NavRoutes.CHANGELOG) {
                            ChangelogScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = NavRoutes.ENVIRONMENT_PATTERN,
                            deepLinks =
                                listOf(
                                    navDeepLink {
                                        uriPattern = "prootcode://environment/{${NavRoutes.ENVIRONMENT_ARG}}"
                                    },
                                ),
                            arguments =
                                listOf(
                                    navArgument(NavRoutes.ENVIRONMENT_ARG) {
                                        type = NavType.StringType
                                    },
                                ),
                        ) { entry ->
                            val environmentId =
                                entry.arguments?.getString(NavRoutes.ENVIRONMENT_ARG).orEmpty()
                            EnvironmentDetailScreen(
                                environmentId = environmentId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = NavRoutes.TERMINAL_PATTERN,
                            deepLinks =
                                listOf(
                                    navDeepLink {
                                        uriPattern = "prootcode://terminal/{${NavRoutes.ENVIRONMENT_ARG}}"
                                    },
                                ),
                            arguments =
                                listOf(
                                    navArgument(NavRoutes.ENVIRONMENT_ARG) {
                                        type = NavType.StringType
                                    },
                                ),
                        ) { entry ->
                            val environmentId =
                                entry.arguments?.getString(NavRoutes.ENVIRONMENT_ARG).orEmpty()
                            val native =
                                environmentId == TerminalSessionManager.NATIVE_ENVIRONMENT_ID
                            val environment =
                                remember(environmentId) {
                                    if (native) {
                                        null
                                    } else {
                                        EnvironmentRegistry(File(context.filesDir, "environments.json"))
                                            .load()
                                            .firstOrNull { it.id == environmentId }
                                    }
                                }
                            if (environment == null && !native) {
                                LaunchedEffect(environmentId) { navController.popBackStack() }
                            } else {
                                TerminalScreen(
                                    environment = environment,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

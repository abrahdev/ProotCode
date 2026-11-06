package dev.prootcode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.prootcode.app.ui.NavRoutes
import dev.prootcode.app.ui.screens.AboutScreen
import dev.prootcode.app.ui.screens.EnvironmentDetailScreen
import dev.prootcode.app.ui.screens.HomeScreen
import dev.prootcode.app.ui.screens.SettingsScreen
import dev.prootcode.app.ui.theme.ProotCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProotCodeTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.HOME,
                ) {
                    composable(NavRoutes.HOME) {
                        HomeScreen(
                            onOpenEnvironment = { environment ->
                                navController.navigate(NavRoutes.environment(environment.id))
                            },
                            onSettings = { navController.navigate(NavRoutes.SETTINGS) },
                        )
                    }
                    composable(NavRoutes.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onAbout = { navController.navigate(NavRoutes.ABOUT) },
                        )
                    }
                    composable(NavRoutes.ABOUT) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = NavRoutes.ENVIRONMENT_PATTERN,
                        arguments =
                            listOf(
                                navArgument(NavRoutes.ENVIRONMENT_ARG) { type = NavType.StringType },
                            ),
                    ) { entry ->
                        val environmentId =
                            entry.arguments?.getString(NavRoutes.ENVIRONMENT_ARG).orEmpty()
                        EnvironmentDetailScreen(
                            environmentId = environmentId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

package dev.haseeb.handi.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.haseeb.handi.ui.backup.BackupScreen
import dev.haseeb.handi.ui.detail.DetailScreen
import dev.haseeb.handi.ui.editor.EditorScreen
import dev.haseeb.handi.ui.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class DetailRoute(val id: Long)

/** id == 0 means "create a new recipe". */
@Serializable
data class EditorRoute(val id: Long = 0L)

@Serializable
data object BackupRoute

@Composable
fun HandiNavHost() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = HomeRoute,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(320)) + fadeIn(tween(320)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(280)) + fadeOut(tween(280)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onRecipeClick = { id -> nav.navigate(DetailRoute(id)) },
                onAddRecipe = { nav.navigate(EditorRoute()) },
                onBackup = { nav.navigate(BackupRoute) },
            )
        }
        composable<DetailRoute> {
            DetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(EditorRoute(id)) },
            )
        }
        composable<EditorRoute> {
            EditorScreen(
                onClose = { nav.popBackStack() },
                onSaved = { id, wasNew ->
                    if (wasNew) {
                        nav.navigate(DetailRoute(id)) {
                            popUpTo<EditorRoute> { inclusive = true }
                        }
                    } else {
                        nav.popBackStack()
                    }
                },
            )
        }
        composable<BackupRoute> {
            BackupScreen(onBack = { nav.popBackStack() })
        }
    }
}

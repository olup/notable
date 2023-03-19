package com.olup.notable

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun Router() {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = "library",

        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(route = "library") {
            /* Using composable function */
            Library(navController = navController)
        }
        composable(
            route = "books/{bookId}/pages/{pageId}",
            arguments = listOf(navArgument("bookId") {
                /* configuring arguments for navigation */
                type = NavType.StringType
            }, navArgument("pageId") {
                type = NavType.StringType
            })
        ) {
            BookUi(
                navController = navController,
                _bookId = it.arguments?.getString("bookId")!!,
                _pageId = it.arguments?.getString("pageId")!!
            )
        }
        composable(
            route = "pages/{pageId}",
            arguments = listOf(navArgument("pageId") {
                type = NavType.StringType
            })
        ) {
            BookUi(
                navController = navController,
                _bookId = null,
                _pageId = it.arguments?.getString("pageId")!!
            )
        }
        composable(
            route = "books/{bookId}/pages",
            arguments = listOf(navArgument("bookId") {
                /* configuring arguments for navigation */
                type = NavType.StringType
            })
        ) {
            PagesView(
                navController = navController,
                bookId = it.arguments?.getString("bookId")!!
            )
        }
        dialog(
            route = "books/{bookId}/modal-settings",
            arguments = listOf(navArgument("bookId") {
                type = NavType.StringType
            })
        ) {
            NotebookConfigDialog(
                navController = navController,
                bookId = it.arguments?.getString("bookId")!!
            )
        }
    }
}

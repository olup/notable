package com.example.inka

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun Router(restartCount: Int) {
    val navController = rememberAnimatedNavController()
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Black)
    )
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
            route = "book/{bookId}/pages/{pageId}",
            arguments = listOf(navArgument("bookId") {
                /* configuring arguments for navigation */
                type = NavType.StringType
            }, navArgument("pageId") {
                type = NavType.StringType
            })
        ) {
            BookUi(
                navController = navController,
                restartCount = restartCount,
                bookId = it.arguments?.getString("bookId")!!,
                pageId = it.arguments?.getString("pageId")!!
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
                restartCount = restartCount,
                bookId = null,
                pageId = it.arguments?.getString("pageId")!!
            )
        }
        composable(
            route = "book/{bookId}/pages",
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
            route = "book/{bookId}/modal-settings",
            arguments = listOf(navArgument("bookId") {
                type = NavType.StringType
            })
        ) {
            NotebookConfigDialog(
                navController = navController,
                bookId = it.arguments?.getString("bookId")!!
            )
        }
        dialog(
            route = "pages/{pageId}/modal-settings",
            arguments = listOf(navArgument("pageId") {
                type = NavType.StringType
            })
        ) {
            PageHomeConfigDialog(
                navController = navController,
                pageId = it.arguments?.getString("pageId")!!
            )
        }

    }
}

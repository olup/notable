package com.example.inka

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.inka.ui.theme.InkaTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.animation.navigation
import com.google.accompanist.navigation.animation.composable

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun Router(restartCount: Int){
    val navController = rememberAnimatedNavController()

        AnimatedNavHost(
            navController = navController,
            startDestination = "library",

            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None},
            popEnterTransition = {EnterTransition.None},
            popExitTransition = {ExitTransition.None},
        ) {
            composable(route = "library") {
                /* Using composable function */
                Library(navController = navController)
            }
            composable(
                route = "book/{bookId}/editor/{pageId}",
                arguments = listOf(navArgument("bookId") {
                    /* configuring arguments for navigation */
                    type = NavType.IntType
                }, navArgument("pageId") {
                    type = NavType.IntType
                })
            ) {
                BookUi(
                    navController = navController,
                    restartCount = restartCount,
                    bookId = it.arguments?.getInt("bookId")!!,
                    pageId = it.arguments?.getInt("pageId")!!
                )
            }
            composable(
                route = "book/{bookId}/pages",
                arguments = listOf(navArgument("bookId") {
                    /* configuring arguments for navigation */
                    type = NavType.IntType
                })
            ) {
                PagesView(
                    navController = navController,
                    bookId = it.arguments?.getInt("bookId")!!
                )
            }
            dialog(
                route = "book/edit/{bookId}",
                arguments = listOf(navArgument("bookId") {
                    type = NavType.IntType
                })
            ) {
                BookEditDialog(
                    navController = navController,
                    bookId = it.arguments?.getInt("bookId")!!
                )
            }

    }
}

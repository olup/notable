package com.olup.notable

import android.app.Activity
import android.widget.Space
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext


@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun Router(intentData: String?) {
    val navController = rememberAnimatedNavController()
    val context = LocalContext.current
    var isQuickNavOpen by remember {
        mutableStateOf(false)
    }
    var pageId: String? by remember { mutableStateOf(null) }
    var bookId: String? by remember { mutableStateOf(null) }
    var isFromIntent by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = isQuickNavOpen, block = {
        DrawCanvas.isDrawing.emit(!isQuickNavOpen)
    })

    // Handle navigation based on intent data
    LaunchedEffect(key1 = intentData) {
        if (intentData != null) {
            isFromIntent = true
            if (intentData.startsWith("page-")) {
                pageId = intentData.removePrefix("page-")
                navController.navigate("pages/$pageId")
            } else if (intentData.startsWith("book-")) {
                bookId = intentData.removePrefix("book-")
                navController.navigate("books/$bookId/pages")
            }
        }
    }

    if (isFromIntent) {
        // backhandler for intent
        BackHandler {
            if (pageId != null) {
                exportPageToPng(context, pageId!!)
            } else if (bookId != null) {
                exportBook(context, bookId!!)
            }
            // Exit the app
            (context as? Activity)?.finish()
        }
    }

    AnimatedNavHost(
        navController = navController,
        startDestination = "library?folderId={folderId}",

        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(
            route = "library?folderId={folderId}",
            arguments = listOf(navArgument("folderId") { nullable = true })
        ) {
            /* Using composable function */
            Library(navController = navController, folderId = it.arguments?.getString("folderId"))
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
            EditorView(
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
            EditorView(
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
    }

    if (isQuickNavOpen) QuickNav(navController = navController, { isQuickNavOpen = false })
    else Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .pointerInteropFilter {
                    if(it.size == 0f) return@pointerInteropFilter true
                    false
                }
                .pointerInput(Unit) {

                    detectTapGestures(
                        onDoubleTap = {
                            isQuickNavOpen = true
                        }
                    )
                })
    }

}

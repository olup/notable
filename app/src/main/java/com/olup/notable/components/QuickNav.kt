package com.olup.notable

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.icons.FeatherIcons
import compose.icons.feathericons.Home
import compose.icons.feathericons.Plus

@SuppressLint("SuspiciousIndentation")
@Composable
fun QuickNav(navController: NavController, onClose: () -> Unit) {
    val context = LocalContext.current
    val appRepository = AppRepository(context)

    val currentBackStackEntry = navController.currentBackStackEntry
    val pageId = currentBackStackEntry?.arguments?.getString("pageId")
    val kv = appRepository.kvProxy

    val settings by kv.observeKv("APP_SETTINGS", AppSettings.serializer(), AppSettings(version = 1))
        .observeAsState()

    fun setSettings(settings: AppSettings) {
        kv.setKv("APP_SETTINGS", settings, AppSettings.serializer())
    }

    val pages = settings?.quickNavPages ?: listOf()

    if (settings == null) return

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Spacer(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .noRippleClickable { onClose() })
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(10.dp)
        ) {

            Row {

                ToolbarButton(
                    imageVector = FeatherIcons.Home,
                    onSelect = {
                        val parentFolder =
                            if (pageId != null) appRepository.pageRepository.getById(pageId)?.parentFolderId else null
                        navController.navigate(
                            route = if (parentFolder != null) "library?folderId=${parentFolder}" else "library"
                        )
                        onClose()
                    }
                )


                if (pageId != null && !pages.contains(pageId)) {
                    Spacer(modifier = Modifier.width(5.dp))
                    ToolbarButton(
                        imageVector = FeatherIcons.Plus,
                        onSelect = {
                            if (settings == null) return@ToolbarButton
                            if (settings!!.quickNavPages.contains(pageId)) return@ToolbarButton
                            setSettings(
                                settings!!.copy(quickNavPages = pages + pageId)
                            )
                        }
                    )
                }
            }

            if (pages.isNotEmpty()) Spacer(modifier = Modifier.height(10.dp))

            Row {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = GridCells.Adaptive(minSize = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    content = {

                        items(pages.reversed()) { thisPageId ->
                            key(thisPageId) {
                                PagePreview(modifier = Modifier
                                    .border(1.dp, Color.Black)
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .noRippleClickable {
                                        val bookId =
                                            appRepository.pageRepository.getById(thisPageId)?.notebookId
                                        val url =
                                            if (bookId == null) "pages/${thisPageId}" else "books/${bookId}/pages/${thisPageId}"
                                        navController.navigate(url)
                                        onClose()
                                    }
                                    .draggable(
                                        orientation = Orientation.Vertical,
                                        onDragStopped = {
                                            if (settings == null) return@draggable
                                            setSettings(
                                                settings!!.copy(quickNavPages = pages.filterNot { it == thisPageId })
                                            )
                                        },
                                        state = rememberDraggableState(onDelta = {})
                                    ),
                                    pageId = thisPageId
                                )
                            }
                        }

                    })
            }
        }
    }

}


package com.olup.notable

import io.shipbook.shipbooksdk.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.olup.notable.AppRepository
import com.olup.notable.db.Folder
import com.olup.notable.db.Notebook
import com.olup.notable.db.Page
import compose.icons.FeatherIcons
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Settings
import java.net.URL
import kotlin.concurrent.thread

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun Library(navController: NavController, folderId: String? = null) {
    val context = LocalContext.current

    var isSettingsOpen by remember {
        mutableStateOf(false)
    }
    val appRepository = AppRepository(LocalContext.current)

    val books by appRepository.bookRepository.getAllInFolder(folderId).observeAsState()
    val singlePages by appRepository.pageRepository.getSinglePagesInFolder(folderId)
        .observeAsState()
    val folders by appRepository.folderRepository.getAllInFolder(folderId).observeAsState()

    var isLatestVersion by remember {
        mutableStateOf(true)
    }
    LaunchedEffect(key1 = Unit, block = {
        thread {
            isLatestVersion = isLatestVersion(context, true)
        }
    })

    Column(
        Modifier.fillMaxSize()
    ) {
        Topbar(
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                BadgedBox(
                    badge = { if(!isLatestVersion) Badge( backgroundColor = Color.Black, modifier = Modifier.offset(-12.dp, 10.dp) ) }
                ) {
                    Icon(
                        imageVector = FeatherIcons.Settings,
                        contentDescription = "",
                        Modifier
                            .padding(8.dp)
                            .noRippleClickable {
                                isSettingsOpen = true
                            })
                }

            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = "Add quick page",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .noRippleClickable {
                            val page = Page(
                                notebookId = null,
                                parentFolderId = folderId,
                                nativeTemplate = appRepository.kvProxy.get(
                                    "APP_SETTINGS", AppSettings.serializer()
                                )?.defaultNativeTemplate ?: "blank"
                            )
                            appRepository.pageRepository.create(page)
                            navController.navigate("pages/${page.id}")
                        }
                        .padding(10.dp))

                Text(text = "Add notebook",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .noRippleClickable {
                            appRepository.bookRepository.create(
                                Notebook(parentFolderId = folderId)
                            )
                        }
                        .padding(10.dp))

                Text(text = "Add Folder",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .noRippleClickable {
                            val folder = Folder(parentFolderId = folderId)
                            appRepository.folderRepository.create(folder)
                        }
                        .padding(10.dp))
            }
        }
        Row(
            Modifier
                .padding(10.dp)
        ) {
            BreadCrumb(folderId) { navController.navigate("library" + if (it == null) "" else "?folderId=${it}") }
        }
        Column(
            Modifier.padding(10.dp)
        ) {

            if (folders?.isEmpty()?.not() == true) {
                Text(text = "Folders")
                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(folders!!) { folder ->
                        var isFolderSettingsOpen by remember { mutableStateOf(false) }
                        if (isFolderSettingsOpen) FolderConfigDialog(
                            folderId = folder.id,
                            onClose = {
                                Log.i(TAG, "Closing Directory Dialog")
                                isFolderSettingsOpen = false
                            })

                        Row(
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("library?folderId=${folder.id}")
                                    },
                                    onLongClick = {
                                        isFolderSettingsOpen = !isFolderSettingsOpen
                                    },
                                )
                                .border(0.5.dp, Color.Black)
                                .padding(10.dp, 5.dp)
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Folder,
                                contentDescription = "folder icon",
                                Modifier.height(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(text = folder.title)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (singlePages?.isEmpty()?.not() == true) {
                Text(text = "Quick pages")
                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(singlePages!!.reversed()) { page ->
                        val pageId = page.id
                        var isPageSelected by remember { mutableStateOf(false) }
                        Box {
                            PagePreview(
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("pages/$pageId")
                                        },
                                        onLongClick = {
                                            isPageSelected = true
                                        },
                                    )
                                    .width(100.dp)
                                    .aspectRatio(3f / 4f)
                                    .border(1.dp, Color.Black, RectangleShape),
                                pageId = pageId
                            )
                            if (isPageSelected) PageMenu(
                                pageId = pageId,
                                canDelete = true,
                                onClose = { isPageSelected = false })
                        }
                    }

                }
                Spacer(Modifier.height(10.dp))
            }

            if (books?.isEmpty()?.not() == true) {
                Text(text = "Notebooks")
                Spacer(Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(books!!) { item ->
                        var isSettingsOpen by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .border(1.dp, Color.Black, RectangleShape)
                                .background(Color.White)
                                .clip(RoundedCornerShape(2))
                                .combinedClickable(
                                    onClick = {
                                        val bookId = item.id
                                        val pageId = item.openPageId ?: item.pageIds[0]
                                        navController.navigate("books/$bookId/pages/$pageId")
                                    },
                                    onLongClick = {
                                        isSettingsOpen = true
                                    },
                                )
                        ) {
                            Text(
                                text = item.pageIds.size.toString(),
                                modifier = Modifier
                                    .background(Color.Black)
                                    .padding(5.dp),
                                color = Color.White
                            )
                            Row(Modifier.fillMaxSize()) {
                                Text(
                                    text = item.title,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .align(CenterVertically)
                                        .fillMaxWidth()
                                )
                            }
                        }

                        if (isSettingsOpen) NotebookConfigDialog(
                            bookId = item.id,
                            onClose = { isSettingsOpen = false })
                    }
                }
            }
        }
    }

    if (isSettingsOpen) AppSettingsModal(onClose = { isSettingsOpen = false })
}




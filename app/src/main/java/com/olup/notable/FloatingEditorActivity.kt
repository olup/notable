package com.olup.notable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.olup.notable.db.Page
import com.olup.notable.ui.theme.InkaTheme
import com.olup.notable.views.FloatingEditorView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloatingEditorActivity : ComponentActivity() {
    private lateinit var appRepository: AppRepository
    private var pageId: String? = null
    private var bookId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.data?.lastPathSegment
        if (data == null) {
            finish()
            return
        }

        if (data.startsWith("page-")) {
            pageId = data.removePrefix("page-")
        } else if (data.startsWith("book-")) {
            bookId = data.removePrefix("book-")
        } else {
            pageId = data
            return
        }


        appRepository = AppRepository(this)

        setContent {
            InkaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    var showEditor by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        if (!Settings.canDrawOverlays(this@FloatingEditorActivity)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${packageName}")
                            )
                            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
                        } else {
                            showEditor = true
                        }
                    }

                    if (showEditor) {
                        FloatingEditorContent(navController, pageId, bookId)
                    }
                }
            }
        }
    }

    @Composable
    private fun FloatingEditorContent(
        navController: androidx.navigation.NavController,
        pageId: String? = null,
        bookId: String? = null
    ) {
        if (pageId != null) {
            var page = appRepository.pageRepository.getById(pageId)
            if (page == null) {
                page = Page(
                    id = pageId,
                    notebookId = null,
                    parentFolderId = null,
                    nativeTemplate = appRepository.kvProxy.get(
                        "APP_SETTINGS", AppSettings.serializer()
                    )?.defaultNativeTemplate ?: "blank"
                )
                appRepository.pageRepository.create(page)
            }

            FloatingEditorView(
                navController = navController,
                pageId = pageId,
                onDismissRequest = {
                    finish()
                }
            )
        } else if (bookId != null) {
            var book = appRepository.bookRepository.getById(bookId)
            FloatingEditorView(
                navController = navController,
                bookId = bookId,
                onDismissRequest = {
                    finish()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pageId?.let { id ->
            // Auto-export to PNG when the activity is destroyed
            Thread {
                exportPageToPng(this, id)
            }.start()
        }
        bookId?.let { id ->
            // Auto-export to PDF when the activity is destroyed
            lifecycleScope.launch(Dispatchers.IO) {
                exportBook(this@FloatingEditorActivity, id)
            }
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 1000
    }
}
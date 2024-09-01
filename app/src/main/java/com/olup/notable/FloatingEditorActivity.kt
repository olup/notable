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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.olup.notable.ui.theme.InkaTheme
import com.olup.notable.views.FloatingEditorView
import com.olup.notable.db.Page
import com.olup.notable.AppSettings

class FloatingEditorActivity : ComponentActivity() {
    private lateinit var appRepository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val pageId = intent.data?.lastPathSegment ?: return
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
                        FloatingEditorContent(pageId, navController)
                    }
                }
            }
        }
    }

    @Composable
    private fun FloatingEditorContent(pageId: String, navController: androidx.navigation.NavController) {
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
            onDismissRequest = { finish() }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) {
                recreate() // Recreate the activity to show the editor
            } else {
                // Permission denied, handle accordingly (e.g., show a message or close the activity)
                finish()
            }
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 1234
    }
}
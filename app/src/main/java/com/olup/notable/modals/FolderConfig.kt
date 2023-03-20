package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.olup.notable.db.FolderRepository


@ExperimentalComposeUiApi
@Composable
fun FolderConfigDialog(folderId: String, onClose : ()->Unit) {
    val folderRepository = FolderRepository(LocalContext.current)
    val folder = folderRepository.get(folderId) ?: return
    val context = LocalContext.current

    var folderTitle by remember {
        mutableStateOf(folder.title)
    }


    Dialog(
        onDismissRequest = { onClose() }
    ) {
        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
        ) {
            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {
                Text(text = "Folder Setting", fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Color.Black)
            )

            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {

                Row() {
                    Text(
                        text = "Folder Title",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = folderTitle,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light,
                            fontSize = 16.sp
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        onValueChange = { folderTitle = it },
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.background(Color(230,230,230,255)).padding(10.dp, 0.dp).onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                val updatedFolder = folder.copy(title = folderTitle)
                                folderRepository.update(updatedFolder)
                            }
                        }


                    )

                }
            }

            Box(
                Modifier
                    .padding(20.dp, 0.dp)
                    .height(0.5.dp)
                    .fillMaxWidth()
                    .background(Color.Black)
            )

            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {
                Text(text = "Delete Folder",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.noRippleClickable {
                        folderRepository.delete(folderId)
                        onClose()
                    })
            }
        }

    }
}
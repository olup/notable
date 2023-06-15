package com.olup.notable

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ScrollIndicator(context: Context, state: EditorState) {
    BoxWithConstraints(modifier = Modifier.width(5.dp).fillMaxHeight()) {
        val height = convertDpToPixel(this.maxHeight, LocalContext.current).toInt()
        val page = state.pageView
        println(page.scroll + height)
        println(page.height)
        val virtualHeight = max(page.height, page.scroll + height)
        if (virtualHeight <= height) return@BoxWithConstraints

        val indicatorSize = (height / virtualHeight.toFloat()) * height
        val indicatorPosition = (page.scroll / virtualHeight.toFloat()) * height


        if (!state.isToolbarOpen) return@BoxWithConstraints

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = convertPixelToDp(indicatorPosition, context)
                )
                .background(Color.Black)
                .height(
                    convertPixelToDp(indicatorSize, context)
                )

        )
    }
}
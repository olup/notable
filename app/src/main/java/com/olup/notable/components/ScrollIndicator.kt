package com.olup.notable

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ScrollIndicator(context: Context, state: EditorState) {
    val page = state.pageModel
    val virtualHeight = max(page.height, page.scroll + page.viewHeight)
    if(virtualHeight <= page.viewHeight) return

    val indicatorSize = (page.viewHeight / virtualHeight.toFloat()) * page.viewHeight
    val indicatorPosition = (page.scroll / virtualHeight.toFloat()) * page.viewHeight

    if(!state.isToolbarOpen) return

    Box(
        modifier = Modifier
            .offset(
                x = convertPixelToDp(SCREEN_WIDTH.toFloat(), context) - 5.dp,
                y = convertPixelToDp(indicatorPosition, context)
            )
            .width(5.dp)
            .background(Color.Black)
            .height(
                convertPixelToDp(indicatorSize, context)
            )

    )
}
package com.example.inka.utils

import android.graphics.Rect
import kotlinx.coroutines.flow.MutableSharedFlow

object AppBus {

    sealed class BusActions {
        data class RefreshArea(val area: Rect) : BusActions()
        data class PersistPage(val pageId: String) : BusActions()
        data class ExportPageToPdf(val pageId: String) : BusActions()
        data class ExportBookToPdf(val bookId: String) : BusActions()
    }

    val bus = MutableSharedFlow<BusActions>()

    fun emit(action : BusActions){

    }

}
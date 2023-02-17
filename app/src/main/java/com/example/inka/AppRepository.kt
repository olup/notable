package com.example.inka

import android.content.Context
import com.example.inka.db.*

class AppRepository(context: Context) {
    val bookRepository = BookRepository(context)
    val pageRepository = PageRepository(context)
    val strokeRepository = StrokeRepository(context)
    val pointRepository = PointRepository(context)

    companion object {
        var undoList = mutableListOf<List<StrokeWithPoints>>()
        fun clearUndo() {
            undoList.clear()
        }
    }

    fun getNextPageIdFromBookAndPage(
        notebookId: Int,
        pageId: Int
    ): Int {
        val book = bookRepository.getById(notebookId = notebookId)
        val pages = book.pageIds
        val index = pages.indexOf(pageId)
        if (index == pages.size - 1) {
            // creating a new page
            val pageId = pageRepository.create(Page(notebookId = notebookId))
            bookRepository.addPage(notebookId, pageId.toInt())
            return pageId.toInt()
        }
        return pages[index + 1]
    }

    fun getPreviousPageIdFromBookAndPage(
        notebookId: Int,
        pageId: Int
    ): Int? {
        val book = bookRepository.getById(notebookId = notebookId)
        val pages = book.pageIds
        val index = pages.indexOf(pageId)
        if (index == 0 || index == -1) {
            return null
        }
        return pages[index - 1]
    }


    fun undo(pageId: Int) {
        val page = pageRepository.getWithStrokeById(pageId = pageId)
        if (page.strokes.size == 0) return
        val lastStrokes = page.strokes.takeLast(10).reversed()
        val selectedStrokes = mutableListOf<StrokeWithPoints>()
        for ((index, strokeWithPoints) in lastStrokes.withIndex()) {
            if (index == 0) {
                selectedStrokes.add(strokeWithPoints)
                continue
            }
            if (lastStrokes[index - 1].points.first().timestamp > strokeWithPoints.points.last().timestamp + 200) break
            selectedStrokes.add(strokeWithPoints)
        }
        strokeRepository.deleteAll(selectedStrokes.map { it.stroke.id })
        undoList.add(selectedStrokes)
    }

    fun redo() {
        if(undoList.size == 0) return
        val lastUndo = undoList.removeLast()
        strokeRepository.create(lastUndo.map{it.stroke})
        pointRepository.create(lastUndo.flatMap { it.points })
    }
}

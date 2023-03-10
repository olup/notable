package com.olup.notable

import android.content.Context
import com.olup.notable.db.*
import java.util.*


class AppRepository(context: Context) {
    val context = context
    val bookRepository = BookRepository(context)
    val pageRepository = PageRepository(context)
    val strokeRepository = StrokeRepository(context)

    fun getNextPageIdFromBookAndPage(
        notebookId: String,
        pageId: String
    ): String {
        val book = bookRepository.getById(notebookId = notebookId)
        val pages = book!!.pageIds
        val index = pages.indexOf(pageId)
        if (index == pages.size - 1) {
            // creating a new page
            val page = Page(notebookId = notebookId)
            pageRepository.create(page)
            bookRepository.addPage(notebookId, page.id)
            return page.id
        }
        return pages[index + 1]
    }

    fun getPreviousPageIdFromBookAndPage(
        notebookId: String,
        pageId: String
    ): String? {
        val book = bookRepository.getById(notebookId = notebookId)
        val pages = book!!.pageIds
        val index = pages.indexOf(pageId)
        if (index == 0 || index == -1) {
            return null
        }
        return pages[index - 1]
    }

    fun duplicatePage(pageId: String) {
        val pageWithStrokes = pageRepository.getWithStrokeById(pageId) ?: return
        val duplicatedPage = pageWithStrokes.page.copy(
            id = UUID.randomUUID().toString(),
            scroll = 0,
            createdAt = Date(),
            updatedAt = Date()
        )
        pageRepository.create(duplicatedPage)
        strokeRepository.create(pageWithStrokes.strokes.map {
            it.copy(
                id = UUID.randomUUID().toString(),
                pageId = duplicatedPage.id,
                updatedAt = Date(),
                createdAt = Date()
            )
        })
        if(pageWithStrokes.page.notebookId != null) {
            val book = bookRepository.getById(pageWithStrokes.page.notebookId) ?: return
            val pageIndex = book.pageIds.indexOf(pageWithStrokes.page.id)
            if(pageIndex == -1) return
            val pageIds = book.pageIds.toMutableList()
            pageIds.add(pageIndex+1, duplicatedPage.id)
            bookRepository.update(book.copy(pageIds = pageIds))
        }
    }


}

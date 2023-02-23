package com.example.inka

import android.content.Context
import com.example.inka.db.*


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

}

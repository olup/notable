package com.olup.notable

import android.content.Context
import com.olup.notable.db.BookRepository
import com.olup.notable.db.FolderRepository
import com.olup.notable.db.ImageRepository
import com.olup.notable.db.KvProxy
import com.olup.notable.db.KvRepository
import com.olup.notable.db.Page
import com.olup.notable.db.PageRepository
import com.olup.notable.db.StrokeRepository
import java.util.Date
import java.util.UUID


class AppRepository(context: Context) {
    val context = context
    val bookRepository = BookRepository(context)
    val pageRepository = PageRepository(context)
    val strokeRepository = StrokeRepository(context)
    val imageRepository = ImageRepository(context)
    val folderRepository = FolderRepository(context)
    val kvRepository = KvRepository(context)
    val kvProxy = KvProxy(context)

    fun getNextPageIdFromBookAndPage(
        notebookId: String,
        pageId: String
    ): String {
        val book = bookRepository.getById(notebookId = notebookId)
        val pages = book!!.pageIds
        val index = pages.indexOf(pageId)
        if (index == pages.size - 1) {
            // creating a new page
            val page = Page(notebookId = notebookId, nativeTemplate = book.defaultNativeTemplate)
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
        val pageWithStrokes = pageRepository.getWithStrokeById(pageId)
        val pageWithImages = pageRepository.getWithImageById(pageId)
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
        imageRepository.create(pageWithImages.images.map {
            it.copy(
                id = UUID.randomUUID().toString(),
                pageId = duplicatedPage.id,
                updatedAt = Date(),
                createdAt = Date()
            )
        })
        if (pageWithStrokes.page.notebookId != null) {
            val book = bookRepository.getById(pageWithStrokes.page.notebookId) ?: return
            val pageIndex = book.pageIds.indexOf(pageWithStrokes.page.id)
            if (pageIndex == -1) return
            val pageIds = book.pageIds.toMutableList()
            pageIds.add(pageIndex + 1, duplicatedPage.id)
            bookRepository.update(book.copy(pageIds = pageIds))
        }
        if (pageWithImages.page.notebookId != null) {
            val book = bookRepository.getById(pageWithImages.page.notebookId) ?: return
            val pageIndex = book.pageIds.indexOf(pageWithImages.page.id)
            if (pageIndex == -1) return
            val pageIds = book.pageIds.toMutableList()
            pageIds.add(pageIndex + 1, duplicatedPage.id)
            bookRepository.update(book.copy(pageIds = pageIds))
        }
    }


}

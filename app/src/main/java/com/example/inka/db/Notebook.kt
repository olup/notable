package com.example.inka.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date
import java.util.UUID

@Entity
data class Notebook(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New notebook",
    val openPageId: String? = null,
    val pageIds: List<String> = listOf(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// DAO
@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebook")
    fun getAll(): LiveData<List<Notebook>>

    @Query("SELECT * FROM notebook WHERE id = (:notebookId)")
    fun getByIdLive(notebookId: String): LiveData<Notebook>

    @Query("SELECT * FROM notebook WHERE id = (:notebookId)")
    fun getById(notebookId: String): Notebook?

    @Query("UPDATE notebook SET openPageId=:pageId WHERE id=:notebookId")
    fun setOpenPageId(notebookId: String, pageId: String)

    @Query("UPDATE notebook SET pageIds=:pageIds WHERE id=:id")
    fun setPageIds(id: String, pageIds: List<String>)

    @Insert
    fun create(notebook: Notebook): Long

    @Update
    fun update(notebook: Notebook)

    @Query("DELETE FROM notebook WHERE id=:id")
    fun delete(id: String)
}

class BookRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.notebookDao()!!
    var pageDb = AppDatabase.getDatabase(context)?.pageDao()!!

    fun create(notebook: Notebook) {
        db.create(notebook)
        val page = Page(notebookId = notebook.id)
        pageDb.create(page)

        db.setPageIds(notebook.id, listOf(page.id))
        db.setOpenPageId(notebook.id, page.id)
    }

    fun update(notebook : Notebook) {
        db.update(notebook)
    }

    fun getAll(): LiveData<List<Notebook>> {
        return db.getAll()
    }

    fun getById(notebookId: String): Notebook? {
        return db.getById(notebookId)
    }

    fun getByIdLive(notebookId: String): LiveData<Notebook> {
        return db.getByIdLive(notebookId)
    }

    fun setOpenPageId(id: String, pageId: String) {
        db.setOpenPageId(id, pageId)
    }

    fun addPage(id: String, pageId: String) {
        var pageIds = (db.getById(id)?: return).pageIds
        db.setPageIds(id, pageIds + pageId)
    }

    fun delete(id: String) {
        db.delete(id)
    }

}
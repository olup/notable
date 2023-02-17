package com.example.inka.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.UUID

@Entity
data class Notebook(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "New notebook",
    val openPageId: Int? = null,
    val pageIds: List<Int> = listOf(),
)

// DAO
@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebook")
    fun getAll(): LiveData<List<Notebook>>

    @Query("SELECT * FROM notebook WHERE id = (:notebookId)")
    fun getById(notebookId: Int): Notebook

    @Query("UPDATE notebook SET openPageId=:pageId WHERE id=:notebookId")
    fun setOpenPageId(notebookId: Int, pageId: Int)

    @Query("UPDATE notebook SET pageIds=:pageIds WHERE id=:id")
    fun setPageIds(id: Int, pageIds: List<Int>)

    @Insert
    fun create(notebook: Notebook): Long

    @Update
    fun update(notebook: Notebook)

    @Query("DELETE FROM notebook WHERE id=:id")
    fun delete(id: Int)
}

class BookRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.notebookDao()!!
    var pageDb = AppDatabase.getDatabase(context)?.pageDao()!!

    fun create(notebook: Notebook) {
        val notebookId = db.create(notebook)
        val pageId = pageDb.create(Page(notebookId = notebookId.toInt()))

        db.setPageIds(notebookId.toInt(), listOf(pageId.toInt()))
        db.setOpenPageId(notebookId.toInt(), pageId.toInt())
    }

    fun update(notebook : Notebook) {
        db.update(notebook)
    }

    fun getAll(): LiveData<List<Notebook>> {
        return db.getAll()
    }

    fun getById(notebookId: Int): Notebook {
        return db.getById(notebookId)
    }

    fun setOpenPageId(id: Int, pageId: Int) {
        db.setOpenPageId(id, pageId)
    }

    fun addPage(id: Int, pageId: Int) {
        var pageIds = db.getById(id).pageIds
        db.setPageIds(id, pageIds + pageId)
    }

    fun delete(id: Int) {
        db.delete(id)
    }

}
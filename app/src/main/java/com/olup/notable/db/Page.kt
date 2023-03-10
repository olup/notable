package com.olup.notable.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Entity
data class Page(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scroll: Int = 0,

    @ColumnInfo(index = true)
    val notebookId: String?,

    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

data class PageWithStrokes(
    @Embedded val page: Page,
    @Relation(
        parentColumn = "id",
        entityColumn = "pageId",
        entity = Stroke::class
    )
    val strokes: List<Stroke>
)

// DAO
@Dao
interface PageDao {
    @Query("SELECT * FROM page WHERE id IN (:ids)")
    fun getMany(ids : List<String>): List<Page>

    @Query("SELECT * FROM page WHERE id = (:pageId)")
    fun getById(pageId: String): Page?

    @Transaction
    @Query("SELECT * FROM page WHERE id =:pageId")
    fun getPageWithStrokesById(pageId : String): PageWithStrokes

    @Query("UPDATE page SET scroll=:scroll WHERE id =:pageId")
    fun updateScroll(pageId: String, scroll:Int)

    @Query("SELECT * FROM page WHERE notebookId is null")
    fun getSinglePages() : LiveData<List<Page>>

    @Insert
    fun create(page: Page):Long

    @Query("DELETE FROM page WHERE id = :pageId")
    fun delete(pageId: String)
}

class PageRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.pageDao()!!

    fun create(page:Page):Long {
        return db.create(page)
    }

    fun updateScroll(id:String, scroll:Int) {
        return db.updateScroll(id, scroll)
    }

    fun getById(pageId: String):Page? {
        return db.getById(pageId)
    }

    fun getWithStrokeById(pageId: String):PageWithStrokes {
        return db.getPageWithStrokesById(pageId)
    }

    fun getSinglePages():LiveData<List<Page>> {
        return db.getSinglePages()
    }

    fun delete(pageId:String) {
        return db.delete(pageId)
    }
}
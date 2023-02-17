package com.example.inka.db

import android.content.Context
import androidx.room.*

@Entity
data class Page(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val scroll: Int = 0,

    @ColumnInfo(index = true)
    val notebookId: Int
)

data class StrokeWithPoints(
    @Embedded val stroke: Stroke,
    @Relation(
        parentColumn = "id",
        entityColumn = "strokeId"
    )
    val points: List<Point>
)

data class PageWithStrokes(
    @Embedded val page: Page,
    @Relation(
        parentColumn = "id",
        entityColumn = "pageId",
        entity = Stroke::class
    )
    val strokes: List<StrokeWithPoints>
)

// DAO
@Dao
interface PageDao {
    @Query("SELECT * FROM page WHERE id IN (:ids)")
    fun getMany(ids : List<Int>): List<Page>

    @Query("SELECT * FROM page WHERE id = (:pageId)")
    fun getById(pageId: Int): Page

    @Transaction
    @Query("SELECT * FROM page WHERE id =:pageId")
    fun getPageWithStrokesById(pageId : Int): PageWithStrokes

    @Query("UPDATE page SET scroll=:scroll WHERE id =:pageId")
    fun updateScroll(pageId: Int, scroll:Int)

    @Insert
    fun create(page: Page):Long

    @Delete
    fun delete(page: Page)
}

class PageRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.pageDao()!!

    fun create(page:Page):Long {
        return db.create(page)
    }

    fun updateScroll(id:Int, scroll:Int) {
        return db.updateScroll(id, scroll)
    }

    fun getById(pageId: Int):Page {
        return db.getById(pageId)
    }

    fun getWithStrokeById(pageId: Int):PageWithStrokes {
        return db.getPageWithStrokesById(pageId)
    }
}
package com.olup.notable.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Entity(
    foreignKeys = [ForeignKey(
        entity = Folder::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("parentFolderId"),
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Notebook::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("notebookId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Page(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), val scroll: Int = 0,
    @ColumnInfo(index = true) val notebookId: String? = null,
    @ColumnInfo(defaultValue = "blank") val nativeTemplate: String = "blank",
    @ColumnInfo(index = true) val parentFolderId: String? = null,
    val createdAt: Date = Date(), val updatedAt: Date = Date()
)

data class PageWithStrokes(
    @Embedded val page: Page, @Relation(
        parentColumn = "id", entityColumn = "pageId", entity = Stroke::class
    ) val strokes: List<Stroke>
)
data class PageWithImages(
    @Embedded val page: Page, @Relation(
        parentColumn = "id", entityColumn = "pageId", entity = Image::class
    ) val images: List<Image>
)

// DAO
@Dao
interface PageDao {
    @Query("SELECT * FROM page WHERE id IN (:ids)")
    fun getMany(ids: List<String>): List<Page>

    @Query("SELECT * FROM page WHERE id = (:pageId)")
    fun getById(pageId: String): Page?

    @Transaction
    @Query("SELECT * FROM page WHERE id =:pageId")
    fun getPageWithStrokesById(pageId: String): PageWithStrokes

    @Transaction
    @Query("SELECT * FROM page WHERE id =:pageId")
    fun getPageWithImagesById(pageId: String): PageWithImages

    @Query("UPDATE page SET scroll=:scroll WHERE id =:pageId")
    fun updateScroll(pageId: String, scroll: Int)

    @Query("SELECT * FROM page WHERE notebookId is null AND parentFolderId is :folderId")
    fun getSinglePagesInFolder(folderId: String? = null): LiveData<List<Page>>

    @Insert
    fun create(page: Page): Long

    @Update
    fun update(page: Page)

    @Query("DELETE FROM page WHERE id = :pageId")
    fun delete(pageId: String)
}

class PageRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.pageDao()!!

    fun create(page: Page): Long {
        return db.create(page)
    }

    fun updateScroll(id: String, scroll: Int) {
        return db.updateScroll(id, scroll)
    }

    fun getById(pageId: String): Page? {
        return db.getById(pageId)
    }

    fun getWithStrokeById(pageId: String): PageWithStrokes {
        return db.getPageWithStrokesById(pageId)
    }
    fun getWithImageById(pageId: String): PageWithImages {
        return db.getPageWithImagesById(pageId)
    }

    fun getSinglePagesInFolder(folderId: String? = null): LiveData<List<Page>> {
        return db.getSinglePagesInFolder(folderId)
    }

    fun update(page: Page) {
        return db.update(page)
    }

    fun delete(pageId: String) {
        return db.delete(pageId)
    }
}
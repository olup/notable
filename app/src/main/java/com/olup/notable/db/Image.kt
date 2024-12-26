package com.olup.notable.db

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.room.*
import com.olup.notable.Pen
import java.util.*



// Entity class for images
@Entity(
    foreignKeys = [ForeignKey(
        entity = Page::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("pageId"),
        onDelete = ForeignKey.CASCADE
    )])
data class Image(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    var x: Float,
    var y: Float,
    val height: Float,
    val width: Float,
    val bitmap: ByteArray,

    @ColumnInfo(index = true)
    val pageId: String,

    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// DAO for image operations
@Dao
interface ImageDao {
    @Insert
    fun create(image: Image): Long

    @Insert
    fun create(images: List<Image>)

    @Update
    fun update(image: Image)

    @Query("DELETE FROM Image WHERE id IN (:ids)")
    fun deleteAll(ids: List<String>)

    @Transaction
    @Query("SELECT * FROM Image WHERE id = :imageId")
    fun getById(imageId: String): Image
}

// Repository for stroke operations
class ImageRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)?.ImageDao()!!

    fun create(image: Image): Long {
        return db.create(image)
    }

    fun create(images: List<Image>) {
        db.create(images)
    }

    fun update(image: Image) {
        db.update(image)
    }

    fun deleteAll(ids: List<String>) {
        db.deleteAll(ids)
    }

    fun getImageWithPointsById(imageId: String): Image {
        return db.getById(imageId)
    }
}



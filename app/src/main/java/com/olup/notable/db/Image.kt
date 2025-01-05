package com.olup.notable.db

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.room.*
import com.olup.notable.Pen
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap // To convert ImageBitmap to Bitmap


// Entity class for images
@Entity(
    foreignKeys = [ForeignKey(
        entity = Page::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("pageId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Image(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    var x: Int = 0,
    var y: Int = 0,
    val height: Int,
    val width: Int,

    // use uri instead of bytearray
    //val bitmap: ByteArray,
    val uri: String? = null,

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

    @Query(
        """
    SELECT * FROM Image 
    WHERE :x >= x AND :x <= (x + width) 
      AND :y >= y AND :y <= (y + height)
      AND pageId= :pageId
    """
    )
    fun getImageAtPoint(x: Int, y: Int, pageId: String): Image?

}

// Repository for stroke operations
class ImageRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)?.ImageDao()!!

    fun create(image: Image): Long {
        return db.create(image)
    }

    fun create(
        imageUri: String,
        //position on canvas
        x: Int,
        y: Int,
        pageId: String,
        //size on canvas
        width: Int,
        height: Int
    ): Long {
        // Prepare the Image object with specified placement
        val imageToSave = Image(
            x = x,
            y = y,
            width = width,
            height = height,
            uri = imageUri,
            pageId = pageId
        )

        // Save the image to the database
        return db.create(imageToSave)
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

    fun getImageAtPoint(x: Int, y: Int, pageId: String): Image? {
        return db.getImageAtPoint(x, y,pageId)
    }
}



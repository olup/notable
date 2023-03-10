package com.olup.notable.db

import android.content.Context
import androidx.room.*
import com.olup.notable.Pen
import java.util.*

@kotlinx.serialization.Serializable
data class StrokePoint(
    val x: Float,
    var y: Float,
    val pressure: Float,
    val size: Float,
    val tiltX: Int,
    val tiltY: Int,
    val timestamp: Long,
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = Page::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("pageId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Stroke(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val size: Float,
    val pen: Pen,

    var top: Float,
    var bottom: Float,
    var left: Float,
    var right: Float,

    val points: List<StrokePoint>,

    @ColumnInfo(index = true)
    val pageId: String,

    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// DAO
@Dao
interface StrokeDao {
    @Insert
    fun create(stroke: Stroke): Long

    @Insert
    fun create(strokes: List<Stroke>)

    @Update
    fun update(stroke: Stroke)

    @Query("DELETE FROM stroke WHERE id IN (:ids)")
    fun deleteAll(ids: List<String>)

    @Transaction
    @Query("SELECT * FROM stroke WHERE id =:strokeId")
    fun getById(strokeId: String): Stroke
}

class StrokeRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.strokeDao()!!

    fun create(stroke: Stroke): Long {
        return db.create(stroke)
    }

    fun create(strokes: List<Stroke>) {
        return db.create(strokes)
    }

    fun update(stroke: Stroke) {
        return db.update(stroke)
    }

    fun deleteAll(ids: List<String>) {
        return db.deleteAll(ids)
    }

    fun getStrokeWithPointsById(strokeId: String): Stroke {
        return db.getById(strokeId)
    }

}
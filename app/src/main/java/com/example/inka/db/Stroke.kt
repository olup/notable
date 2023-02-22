package com.example.inka.db

import android.content.Context
import androidx.room.*
import com.example.inka.Pen

@Entity(
    foreignKeys = [ForeignKey(
        entity = Page::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("pageId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Stroke(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val size : Float,
    val pen : Pen,

    @ColumnInfo(index = true)
    val pageId: Int
)

// DAO
@Dao
interface StrokeDao {
    @Insert
    fun create(stroke: Stroke):Long

    @Insert
    fun create(strokes: List<Stroke>)

    @Query("DELETE FROM stroke WHERE id IN (:ids)")
    fun deleteAll(ids: List<Int>)

    @Transaction
    @Query("SELECT * FROM stroke WHERE id =:strokeId")
    fun getStrokeWithPointsById(strokeId : Int): StrokeWithPoints
}

class StrokeRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.strokeDao()!!

    fun create(stroke: Stroke):Long {
       return  db.create(stroke)
    }

    fun create(strokes: List<Stroke>) {
        return  db.create(strokes)
    }

    fun deleteAll(ids : List<Int>) {
        return  db.deleteAll(ids)
    }

    fun getStrokeWithPointsById(strokeId : Int) : StrokeWithPoints{
        return db.getStrokeWithPointsById(strokeId)
    }

}
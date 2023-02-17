package com.example.inka.db

import android.content.Context
import androidx.room.*

@Entity(
    foreignKeys = [ForeignKey(
        entity = Stroke::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("strokeId"),
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Page::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("pageId"),
        onDelete = ForeignKey.CASCADE
    )]
)

data class Point (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    var x: Float = 0f,
    var y: Float = 0f,
    var pressure: Float = 0f,
    var size: Float = 0f,
    var tiltX: Int = 0,
    var tiltY: Int = 0,
    var timestamp: Long = 0,

    @ColumnInfo(index = true)
    val strokeId: Int,
    @ColumnInfo(index = true)
    val pageId: Int
)

// DAO
@Dao
interface PointDao {
    @Insert
    fun create(points: List<Point>): List<Long>

    @Query("SELECT * FROM point WHERE pageId =:pageId")
    fun getByPageId(pageId : Int): List<Point>

    @Query("UPDATE point SET y=y+:offset WHERE pageId=:pageId AND y>:minY")
    fun offsetPoints(pageId : Int, minY : Float, offset: Float)
}

class PointRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.pointDao()!!

    fun getByPageId(pageId: Int):List<Point> {
        return db.getByPageId(pageId)
    }

    fun create(points: List<Point>) {
        db.create(points)
    }

    fun offsetPoints(pageId : Int, minY : Float, offset: Float) {
        db.offsetPoints(pageId, minY, offset)
    }
}
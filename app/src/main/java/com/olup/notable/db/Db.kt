package com.olup.notable.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

class Converters {
    @TypeConverter
    fun fromListString(value: List<String>) = Json.encodeToString(value)
    @TypeConverter
    fun toListString(value: String) = Json.decodeFromString<List<String>>(value)
    @TypeConverter
    fun fromListPoint(value: List<StrokePoint>) = Json.encodeToString(value)
    @TypeConverter
    fun toListPoint(value: String) = Json.decodeFromString<List<StrokePoint>>(value)

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}



@Database(
    entities = [Folder::class, Notebook::class, Page::class, Stroke::class],
    version = 24,
    autoMigrations = [
        AutoMigration(19,20),
        AutoMigration(20,21),
        AutoMigration(21,22),
        AutoMigration(23,24),
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun notebookDao(): NotebookDao
    abstract fun pageDao(): PageDao
    abstract fun strokeDao(): StrokeDao

    companion object {
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE =
                        Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                            .allowMainThreadQueries()
                            //.fallbackToDestructiveMigration()
                            .addMigrations(
                                MIGRATION_16_17,
                                MIGRATION_17_18,
                                        MIGRATION_22_23)
                            .build()
                }
            }
            return INSTANCE!!
        }
    }
}
package com.olup.notable.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import java.util.Date
import java.util.UUID

@Entity(
    foreignKeys = [ForeignKey(
        entity = Folder::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("parentFolderId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Folder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Folder",

    @ColumnInfo(index = true)
    val parentFolderId: String? = null,

    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// DAO
@Dao
interface FolderDao {
    @Query("SELECT * FROM folder WHERE parentFolderId IS :folderId")
    fun getChildrenFolders(folderId: String?): LiveData<List<Folder>>

    @Query("SELECT * FROM folder WHERE id IS :folderId")
    fun get(folderId: String): Folder


    @Insert
    fun create(folder: Folder): Long

    @Update
    fun update(folder: Folder)

    @Query("DELETE FROM folder WHERE id=:id")
    fun delete(id: String)
}

class FolderRepository(context: Context) {
    var db = AppDatabase.getDatabase(context).folderDao()

    fun create(folder: Folder) {
        db.create(folder)
    }

    fun update(folder: Folder) {
        db.update(folder)
    }

    fun getAllInFolder(folderId: String? = null): LiveData<List<Folder>> {
        return db.getChildrenFolders(folderId)
    }

    fun get(folderId: String): Folder {
        return db.get(folderId)
    }


    fun delete(id: String) {
        db.delete(id)
    }

}
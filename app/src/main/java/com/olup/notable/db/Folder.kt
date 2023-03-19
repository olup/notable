package com.olup.notable.db

import androidx.lifecycle.LiveData
import androidx.room.*
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
    val parentFolderId: String? = null,

    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// DAO
@Dao
interface FolderDao {
    @Query("SELECT * FROM folder WHERE parentFolderId=:folderId")
    fun getChildrenFolders(folderId:String?): LiveData<List<Folder>>

    @Insert
    fun create(folder: Folder): Long

    @Update
    fun update(folder: Folder)

    @Query("DELETE FROM folder WHERE id=:id")
    fun delete(id: String)
}

/*class FolderRepository(context: Context) {
    var db = AppDatabase.getDatabase(context)?.folderDao()!!

    fun create(folder: Folder) {
        db.create(folder)
    }

    fun update(folder : Folder) {
        db.update(folder)
    }

    fun getChildrenFolders(folderId: String): LiveData<List<Folder>> {
        return db.getChildrenFolders(folderId)
    }


    fun delete(id: String) {
        db.delete(id)
    }

}*/
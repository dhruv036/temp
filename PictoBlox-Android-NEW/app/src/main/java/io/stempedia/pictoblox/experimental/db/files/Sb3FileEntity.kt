package io.stempedia.pictoblox.experimental.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sb3_files")
data class Sb3FileEntity(
    @PrimaryKey val id: String,
    /**
     * Firebase uid
     */
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    /**
     * time stamp when the file is saved
     */
    @ColumnInfo(name = "created_date") val createdDate: Long,
    /**
     * This reflects the deletion from local record.
     */
    @ColumnInfo(name = "flag_for_deletion") val flagForDeletion: Int,
    /**
     * Sync index is a timestamp, when the files gets uploaded first.
     */
    @ColumnInfo(name = "sync_index") val syncIndex: Long,
    /**
     *  0 = remaining, 1 = uploaded, -1 = NA
     */
    //@ColumnInfo(name = "upload_status") val uploadStatus: Int,

    /**
     *  0 = remaining, 1 = downloaded, -1 = NA
     */
    //@ColumnInfo(name = "download_status") val downloadStatus: Int,
    /**
     * The file created at, usually hardware name.
     */
    @ColumnInfo(name = "origin") val origin: String,

    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "thumb_path") val thumbPath: String,


    /**
     *  0 = remaining, 1 = downloaded, -1 = NA
     */
    @ColumnInfo(name = "is_synced") val isSynced: Int,

    /**
     *
     */
    @ColumnInfo(name = "is_marked_to_sync") val isMarkedToSynced: Int
)

@Entity
data class Sb3FileUpdateEntity(
    val id: String,
    @ColumnInfo(name = "sync_index") val syncIndex: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Int
)


package io.stempedia.pictoblox.experimental.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_sync_index")
data class UserSyncIndexEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val id: String,
    @ColumnInfo(name = "sb3_sync_index") val sb3SyncIndex: Long
)
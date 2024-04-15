package io.stempedia.pictoblox.experimental.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "PopUpsCountTable")
data class PopupCountEntity(
    @PrimaryKey
    @ColumnInfo(name = "popup_id")
    val popupId: String,

    @ColumnInfo(name = "popup_current_count")
    val currentCount: Int = 0,
)
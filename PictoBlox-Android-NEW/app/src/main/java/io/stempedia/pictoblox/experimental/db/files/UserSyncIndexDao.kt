package io.stempedia.pictoblox.experimental.db.files

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Single


@Dao
interface UserSyncIndexDao {
    @Query("SELECT * FROM user_sync_index WHERE user_id = :userId")
    fun getSb3SyncIndex(userId: String): UserSyncIndexEntity

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(syncIndex: UserSyncIndexEntity)

    @Query("SELECT COUNT(*) FROM user_sync_index")
    fun getSyncIndexEntries(): Single<Int>
}
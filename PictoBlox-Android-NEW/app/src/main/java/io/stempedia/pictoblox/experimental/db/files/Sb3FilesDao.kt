package io.stempedia.pictoblox.experimental.db.files

import androidx.room.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
interface Sb3FilesDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(sb3File: Sb3FileEntity)

    /*@Query("DELETE FROM sb3_files")
    suspend fun deleteAll()*/

    /*
    * for all files by user
     */
   /* @Query("SELECT * FROM sb3_files WHERE user_id = :userId ORDER BY created_date DESC")
    fun getAllFilesByUser(userId: String): List<Sb3FileEntity>

    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND file_name LIKE '%' || :queryText || '%' ORDER BY created_date DESC")
    fun getQueriedFilesByUser(userId: String, queryText: String): List<Sb3FileEntity>*/

    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND is_marked_to_sync = 0 AND flag_for_deletion = 0 ORDER BY created_date DESC")
    fun getLocalFileByUser(userId: String): Observable<List<Sb3FileEntity>>

    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND is_marked_to_sync = 1 AND flag_for_deletion = 0 ORDER BY created_date DESC")
    fun getCloudFileByUser(userId: String): Observable<List<Sb3FileEntity>>

    /*
     * For 'recent' search
     */
    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND created_date > :timestamp ORDER BY created_date DESC")
    fun getRecentFilesByUser(userId: String, timestamp: Long): Observable<List<Sb3FileEntity>>

    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND created_date > :timestamp AND file_name LIKE '%' || :queryText || '%' ORDER BY created_date DESC")
    fun getQueriedRecentFilesByUser(userId: String, timestamp: Long, queryText: String): Single<List<Sb3FileEntity>>

    /*
    * For Syncing with cloud
     */
    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND is_synced = 0 AND flag_for_deletion = 0 ORDER BY created_date ASC")
    fun getLocalFilesToBeSync(userId: String): List<Sb3FileEntity>

    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND is_synced = 0 AND flag_for_deletion = 1 ORDER BY created_date ASC")
    fun getLocalFilesToBeDeleted(userId: String): List<Sb3FileEntity>

    @Query("SELECT * FROM sb3_files WHERE user_id = :userId AND is_synced = 1 AND flag_for_deletion = 1 ORDER BY created_date ASC")
    fun getSyncedToBeDeleted(userId: String): List<Sb3FileEntity>

    @Update(entity = Sb3FileEntity::class)
    fun update(obj: Sb3FileUpdateEntity)

    @Query("DELETE FROM sb3_files WHERE id = :id")
    fun deleteEntry(id: String)

    @Query("SELECT * FROM sb3_files WHERE id = :id")
    fun getEntry(id: String): Sb3FileEntity?


}
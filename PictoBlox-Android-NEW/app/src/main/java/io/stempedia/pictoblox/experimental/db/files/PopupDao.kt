package io.stempedia.pictoblox.experimental.db.files

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Observable

@Dao
interface PopupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePopup(popUp: PopupsEntity)

    @Query("select * from PopUpsTable where popup_id = :popUpId")
    fun fetchPopUpById(popUpId: String) : PopupsEntity

    @Query("select * from PopUpsTable")
    fun fetchAllPopUps(): List<PopupsEntity>


}
package io.stempedia.pictoblox.experimental.db.files

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PopupCountDao{


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun savePopupCount(popupCount: PopupCountEntity)

    @Query("select popup_current_count from PopUpsCountTable where popup_id = :popupId")
    fun fetchPopupCountById(popupId: String) : Int

    @Query("update popupscounttable set popup_current_count =popup_current_count+1 where popup_id = :popupId")
    fun updatePopUpCount(popupId: String)
}

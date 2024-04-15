package io.stempedia.pictoblox.experimental.db.files

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "PopUpsTable")
data class PopupsEntity(
    @PrimaryKey
    @ColumnInfo(name = "popup_id")
    val popUpId: String,
    @ColumnInfo(name = "body")
    @TypeConverters(MapTypeConverter::class)
    val body: String,
    @ColumnInfo(name = "button_text")
    @TypeConverters(MapTypeConverter::class)
    val buttonText: String,
    @ColumnInfo(name = "popup_delay_ms")
    val popUpDelayInms: Int,
    @ColumnInfo(name = "link")
    val link: String?,
    @ColumnInfo(name = "img_id")
    val img: String?,
    @ColumnInfo(name = "max_date")
    val maxDate: Long?,
    @ColumnInfo(name = "max_display_count")
    val maxDisplayCount: Int,
    @ColumnInfo(name = "title")
    @TypeConverters(MapTypeConverter::class)
    val title: String,
    @ColumnInfo(name = "target_audience")
    val targetAudience: TargetAudience,
)


enum class TargetAudience(name:String){
    TYPE_TEACHER("TYPE_TEACHER"),
    TYPE_STUDENT("TYPE_STUDENT"),
    TYPE_ALL("TYPE_ALL")
}
object DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}
object MapTypeConverter{

    @TypeConverter
    @JvmStatic
    fun stringToMap(value: String) : Map<String,String>{
        return Gson().fromJson(value,object : TypeToken<Map<String,String>?>(){}.type)
    }

    @TypeConverter
    @JvmStatic
    fun mapToString(value: Map<String,String>) : String{
        return if (value == null) "" else Gson().toJson(value)
    }
}
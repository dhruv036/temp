package io.stempedia.pictoblox.experimental.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.auth.FirebaseAuth
import io.stempedia.pictoblox.experimental.db.files.PopupCountDao
import io.stempedia.pictoblox.experimental.db.files.PopupCountEntity
import io.stempedia.pictoblox.experimental.db.files.PopupDao
import io.stempedia.pictoblox.experimental.db.files.PopupsEntity
import io.stempedia.pictoblox.experimental.db.files.Sb3FileEntity
import io.stempedia.pictoblox.experimental.db.files.Sb3FilesDao
import io.stempedia.pictoblox.experimental.db.files.UserSyncIndexDao
import io.stempedia.pictoblox.experimental.db.files.UserSyncIndexEntity
import kotlinx.coroutines.CoroutineScope

// Annotates class to be a Room Database with a table (entity) of the Word class
@Database(entities = [Sb3FileEntity::class, UserSyncIndexEntity::class, PopupsEntity::class, PopupCountEntity::class], version = 5, exportSchema = false)
public abstract class PictoBloxDatabase : RoomDatabase() {

    abstract fun sb3FilesDao(): Sb3FilesDao

    abstract fun popUpDao(): PopupDao

    abstract fun popUpCountDao(): PopupCountDao


    abstract fun userSyncIndexDao(): UserSyncIndexDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: PictoBloxDatabase? = null

        fun getDatabase(context: Context): PictoBloxDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PictoBloxDatabase::class.java,
                    "pictoblox_database"
                )
//                    .addCallback(callback)
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        val callback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.also { pictoBloxDB ->
                    FirebaseAuth.getInstance().currentUser?.also { user ->

                        val userSyncIndexEntity = UserSyncIndexEntity(user.uid, 0)
                        pictoBloxDB.userSyncIndexDao().insert(userSyncIndexEntity)
                    }
                }
            }
        }

    }
}
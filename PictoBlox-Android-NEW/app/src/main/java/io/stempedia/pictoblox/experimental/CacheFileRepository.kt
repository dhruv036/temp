package io.stempedia.pictoblox.experimental

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.stempedia.pictoblox.util.SPManager
import java.io.File
import java.io.FileOutputStream

class CacheFileRepository(val application: Context) {
    private val cachedFileName: String = "CachedSb3File.sb3"
    private val spManager: SPManager by lazy { SPManager(application) }
    private var isCachedFileRequested = false

    private val sb3CacheDir: File by lazy {
        File(application.filesDir, "cache")
            .apply {
                if (!exists())
                    mkdirs()
            }
    }

    fun saveFile(byteArray: ByteArray): Completable {
        return Completable.create { emitter ->

            val cacheFile = File(sb3CacheDir, cachedFileName)

            if (cacheFile.exists()) {
                cacheFile.delete()
            }

            try {

                FileOutputStream(cacheFile).use { os ->
                    run {
                        os.write(byteArray)
                    }
                }
                spManager.isCacheFileExists = true

                emitter.onComplete()

            } catch (e: Exception) {
                emitter.onError(e)
            }

        }
    }

    fun isCachedVersionExists(): Boolean {
        return spManager.isCacheFileExists || isCachedFileRequested
    }

    fun setCachedFileRequested() {
        isCachedFileRequested = true
    }
}
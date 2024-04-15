package io.stempedia.pictoblox.connectivity

import android.media.SoundPool
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.util.PictobloxLogger

class SoundHandler(val commManagerServiceImpl: CommManagerServiceImpl) {
    private val soundPool = SoundPool.Builder().setMaxStreams(2).build()
    private val soundKeys = mutableMapOf<Int, Int>()

    init {
       // soundKeys[1] = soundPool.load(commManagerServiceImpl, R.raw.sample_coin_sound, 0)
    }


    //TODO
    fun dispose() {
        soundPool.release()
    }


    fun playKey(key: Int) {
        soundKeys[key]?.also {
            soundPool.play(it, 1.0f, 1.0f, 0, 0, 1.0f)
        }?:run{
            PictobloxLogger.getInstance().logw("Key $key does not exists!!!")
        }
    }

}
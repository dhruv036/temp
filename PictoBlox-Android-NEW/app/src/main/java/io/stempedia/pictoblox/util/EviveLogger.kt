package io.stempedia.pictoblox.util

import android.util.Log
import io.stempedia.pictoblox.BuildConfig
import java.util.*
import java.util.concurrent.CountDownLatch


class PictobloxLogger : Observable() {

    companion object {

        @Volatile
        private var INSTANCE: PictobloxLogger? = null

        var payload : MutableMap<String,String>? = mutableMapOf()
        fun getInstance(): PictobloxLogger {
            if (INSTANCE == null){
                INSTANCE = PictobloxLogger()
            }

            return INSTANCE as PictobloxLogger
        }

        var map : MutableMap<String, CountDownLatch> = mutableMapOf()
    }


    private val tagDebug = "PictobloxDebug"


    fun isMapIsNull() = map == null

    fun initializeMap(key: String,latch: CountDownLatch) {
        map = mutableMapOf<String, CountDownLatch>().apply {
            this[key] = latch
        }
    }
    fun putLatch(key: String,latch: CountDownLatch){
        map.apply {
            this[key] =latch
        }
    }

    fun isLatchKeyPresent(key: String): Boolean{
        return map.containsKey(key = key)
    }

    fun getLoad(): MutableMap<String,String>?{
        if (payload == null){
            Log.d("method","empty")
            return null
        }else{
            return payload
        }
    }
    fun setPayload(load : MutableMap<String,String>?) {
//        Log.d("method", "2")
        payload = load
    }
    fun modifyPayload(key : String){
        payload?.remove(key)
    }

    fun logd(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tagDebug, msg)
        }
    }

    fun logw(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tagDebug, msg)
        }
    }

    fun logException(e: Throwable) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()

        } else {
            //log to server

        }
    }

    fun printUnsignedByte(b: Byte) {
        printUnsignedByte(b, "", "")
    }

    fun printUnsignedByte(b: Int) {
        printUnsignedByte(b, "", "")
    }

    fun printUnsignedByte(b: Byte, prefix: String, suffix: String) {
        printUnsignedByte(b.toInt().and(0xff), prefix, suffix)
    }

    fun printUnsignedByte(b: Int, prefix: String, suffix: String) {
        if (!BuildConfig.DEBUG) {
            return
        }
        logd("$prefix ${b.and(0xff)} $suffix")
    }

    fun printByteArrayUnsigned(b: ByteArray) {
        printByteArrayUnsigned(b, "")
    }

    fun printByteArrayUnsigned(b: ByteArray, prefix: String) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val printArray = IntArray(b.size)

        b.forEachIndexed { i: Int, byte: Byte -> printArray[i] = (byte.toInt().and(0xff)) }

        logd("$prefix [ ${b.size} ] ${Arrays.toString(printArray)}")

    }


}
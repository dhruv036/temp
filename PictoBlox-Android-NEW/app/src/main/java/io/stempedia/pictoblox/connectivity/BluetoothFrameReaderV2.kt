package io.stempedia.pictoblox.connectivity

import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.connectivity.EviveProtocolConstant.endByte1
import io.stempedia.pictoblox.connectivity.EviveProtocolConstant.endByte2
import io.stempedia.pictoblox.connectivity.EviveProtocolConstant.startByte1
import io.stempedia.pictoblox.connectivity.EviveProtocolConstant.startByte1Int
import io.stempedia.pictoblox.connectivity.EviveProtocolConstant.startByte2
import io.stempedia.pictoblox.util.PictobloxLogger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class BluetoothFrameReaderV2(private val listener: FrameReadObserver) : Runnable {
    private var queue: LinkedBlockingQueue<Byte> = LinkedBlockingQueue()
    //private var queue: ArrayBlockingQueue<Byte> = ArrayBlockingQueue()
    private val logger = PictobloxLogger()
    private lateinit var os: ByteArrayOutputStream
    private val verbose = BuildConfig.DEBUG
    private lateinit var thread: Thread
    private var isRunning: Boolean = false

    fun start() {
        if (!isRunning) {
            os = ByteArrayOutputStream()
            thread = Thread(this)
            isRunning = true
            thread.start()

        } else {
            logger.logw("Start called but bluetooth reader is already running")
        }
    }

    fun stop() {
        if (isRunning) {
            isRunning = false
            thread.interrupt()

        } else {
            logger.logw("stop called but bluetooth reader is not running")
        }
    }

    fun flushQueue(){
        queue.clear()
        repeat(100) {
            queue.put(0)
        }
    }

    fun putInQueue(b: ByteArray) {
        PictobloxLogger.getInstance().printByteArrayUnsigned(b, "raw")
        b.forEach {
            queue.put(it)
        }
    }

    override fun run() {
        try {
            while (isRunning) {
                poll(startByte1, " ", "[START_1]")?.apply {

                    poll(startByte2, " ", "[START_2]")?.apply {
                        os.reset()
                        os.write(startByte1Int)
                        os.write(this)

                        poll("   ", "[EXT_ID]")?.apply {
                            os.write(this)

                            poll("    ", "[TYPE]")?.apply {
                                os.write(this)

                                val loopCount: Int = when (this) {
                                    EviveProtocolResponseType.TYPE_BYTE -> 1

                                    EviveProtocolResponseType.TYPE_FLOAT -> 4

                                    EviveProtocolResponseType.TYPE_SHORT -> 2

                                    EviveProtocolResponseType.TYPE_STRING -> {
                                        val count = poll("     ", "[Byte]") ?: 0
                                        os.write(count)

                                        count
                                    }
                                    EviveProtocolResponseType.TYPE_DOUBLE -> 8

                                    EviveProtocolResponseType.TYPE_INT -> 4

                                    EviveProtocolResponseType.TYPE_SENSOR_OLD_FW -> 12

                                    EviveProtocolResponseType.TYPE_SENSOR_NEW_FW -> 16

                                    else -> 0

                                }

                                repeat(loopCount) {
                                    poll("     ", "[DATA]")?.apply {
                                        os.write(this)

                                    } ?: run {
                                        return
                                    }
                                }

                                poll(endByte1, " ", "[END_1]")?.apply {
                                    os.write(this)

                                    poll(endByte2, " ", "[END_2]")?.apply {
                                        os.write(this)
                                        listener.onFrameRead(os.toByteArray())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e2: InterruptedException) {
            logger.logException(e2)

        } catch (e3: Exception) {
            logger.logException(e3)

        } finally {
            clearResources()
        }
    }

    private fun poll(logPrefix: String, logSuffix: String): Int? {
        return poll(null, logPrefix, logSuffix)
    }

    private fun poll(expectedValue: Byte?, logPrefix: String, logSuffix: String): Int? {

        return queue.poll(2000, TimeUnit.MILLISECONDS)?.let {

            //IF expected value is passed then it has to match with polled value
            if (expectedValue == null || expectedValue == it) {
                if (verbose) {
                    logger.printUnsignedByte(it, logPrefix, logSuffix)

                }
                it.toInt().and(0xff)

            } else {
                if (verbose) {
                    logger.printUnsignedByte(it, "Expected $expectedValue was $it", logSuffix)
                }

                null
            }


        } ?: run {
            if (verbose) {
                logger.logd("$logPrefix Read time out : scraping frame at $logSuffix")
            }

            null
        }

    }

    private fun clearResources() {
        try {
            queue.clear() //This is not totally necessary, as queue will eventually get scrapped anyway but just for more clear approach.
            os.close()

        } catch (e: IOException) {

        } finally {
            logger.logd("bluetooth frame reader resources cleared...")
        }
    }
}


interface FrameReadObserver {
    fun onFrameRead(b: ByteArray)
}


object EviveProtocolConstant {
    const val startByte1 = 0xff.toByte()
    const val startByte1Int = 0xff
    const val startByte2 = 0x55.toByte()
    const val startByte2Int = 0x55
    const val endByte1 = 0x0D.toByte()
    const val endByte1Int = 0x0D
    const val endByte2 = 0x0A.toByte()
    const val endByte2Int = 0x0A
}

// 1 byte 2 float 3 short 4 len+string 5 double
object EviveProtocolResponseType {
    const val TYPE_BYTE = 0x01
    const val TYPE_FLOAT = 0x02
    const val TYPE_SHORT = 0x03
    const val TYPE_STRING = 0x04
    const val TYPE_DOUBLE = 0x05
    const val TYPE_INT = 0x06
    const val TYPE_SENSOR_OLD_FW = 0x07
    const val TYPE_SENSOR_NEW_FW = 0x08
    //const val TYPE_SENSOR = 0x07
}
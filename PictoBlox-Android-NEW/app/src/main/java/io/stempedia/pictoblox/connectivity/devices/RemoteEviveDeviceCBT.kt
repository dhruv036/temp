package io.stempedia.pictoblox.connectivity.devices

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.text.TextUtils
import io.stempedia.pictoblox.util.PictobloxLogger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/*
100 -> DISCONNECTED
101 -> CONNECTING
102 -> CONNECTED
103 -> ERROR
 */

class RemoteEviveDeviceCBT(private val btDevice: BluetoothDevice,
                           private val logger: PictobloxLogger,
                           /*private val observer: DeviceObserver, */override var observer2: DeviceObserver?= null) :
    RemoteEviveDevice {

    private var localStatus: Int = 100

    private lateinit var connectionThread: ConnectionThread
    private lateinit var readThread: ReaderThread
    private lateinit var writeThread: WriterThread
    private lateinit var mmOutStream: OutputStream
    private lateinit var mmInStream: InputStream
    private lateinit var btSocket: BluetoothSocket
    private val deviceName: String = if (TextUtils.isEmpty(btDevice.name)) {
        btDevice.address

    } else {
        btDevice.name
    }

    override val status: RemoteEviveDeviceStatus
        get() = when (localStatus) {
            100 -> RemoteEviveDeviceStatus.DISCONNECTED
            101 -> RemoteEviveDeviceStatus.CONNECTING
            102 -> RemoteEviveDeviceStatus.CONNECTED
            103 -> RemoteEviveDeviceStatus.ERROR

            else -> {
                RemoteEviveDeviceStatus.DISCONNECTED
            }
        }


    override fun getDeviceType(): Int {
        return btDevice.type
    }

    override fun tryWrite(b: ByteArray) {
        logger.printByteArrayUnsigned(b)
        if (localStatus != 102) {
            logger.logw("Write called, No active connection.")
            return
        }
        writeThread.putInQueue(b)
    }

    override fun tryConnect() {
        connectionThread = ConnectionThread()
        readThread = ReaderThread()
        writeThread = WriterThread(1)

        connectionThread.start()
        observer2?.connecting(deviceName)
        logger.logd("Trying to connect")
    }

    override fun tryDisconnect() {
        if (localStatus == 101) {
            connectionThread.abort()
        }

        if (localStatus == 102) {
            readThread.abort()
            writeThread.abort()
        }
        localStatus = 100
        observer2?.disconnected(deviceName)
    }

    inner class ConnectionThread : Thread() {
        override fun run() {
            localStatus = 101
            btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(HM05.UUID_GENERIC.value))

            try {
                btSocket.connect()
                mmInStream = btSocket.inputStream
                mmOutStream = btSocket.outputStream
                readThread.start()
                writeThread.start()
                localStatus = 102
                observer2?.connected(deviceName, btDevice.address)

            } catch (e: IOException) {
                logger.logException(e)
                //observer2?.error(e.message ?: "Connection error")
                observer2?.error("Error in establishing connection")
                localStatus = 103
            }
        }

        fun abort() {
            btSocket.close()
        }
    }

    inner class ReaderThread : Thread("CBT_reader") {
        private var bytes: Int = 0
        private var buffer = ByteArray(2048)
        private lateinit var trimmedBuffer: ByteArray

        override fun run() {
            try {

                while (true) {
                    bytes = mmInStream.read(buffer)

                    trimmedBuffer = ByteArray(bytes)
                    System.arraycopy(buffer, 0, trimmedBuffer, 0, bytes)
                    observer2?.notify(trimmedBuffer)
                }

            } catch (e: IOException) {
                logger.logException(e)
                logger.logw(e.message ?: "Read error")
                tryDisconnect()

            } catch (e2: InterruptedException) {
                logger.logd("Reader has been interrupted")

            }


        }

        fun abort() {
            interrupt()
            mmOutStream.close()
            mmInStream.close()
            btSocket.close()
        }
    }

    inner class WriterThread(val delay: Long) : Thread() {
        private val queue = LinkedBlockingQueue<ByteArray>(1000)

        fun putInQueue(b: ByteArray) {

            /*b.asList()
                    .chunked(60)
                    .forEach { queue.put(it.toByteArray()) }*/

            queue.put(b)

            /*b.forEach {
                queue.put(byteArrayOf(it))
            }*/
        }

        override fun run() {
            while (true) {
                try {

                    val t = queue.take()
                    //EviveLogger.getInstance().logd("Sending chunk..... ${t.size} ${queue.size}")
                    mmOutStream.write(t)

                    if (delay > 0) {
                        sleep(delay)
                    }

                } catch (e: IOException) {
                    //write failed
                    logger.logException(e)
                    logger.logw(e.message ?: "Write error")
                    tryDisconnect()
                    break

                } catch (e2: InterruptedException) {
                    logger.logd("Writer has been interrupted")
                    break
                }
            }
        }

        fun abort() {
            interrupt()

        }
    }
}
package io.stempedia.pictoblox.connectivity.devices

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.text.TextUtils
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import android.bluetooth.BluetoothGattDescriptor
import io.stempedia.pictoblox.util.PictobloxLogger


/*
100 -> DISCONNECTED
101 -> CONNECTING
102 -> CONNECTED
103 -> ERROR
 */


/**
 * Strictly an application context
 *
 *
 */

class RemoteEviveDeviceBLE(
    private val btDevice: BluetoothDevice,
    private val context: Context,
    private val logger: PictobloxLogger,
    override var observer2: DeviceObserver? = null
) :
    RemoteEviveDevice {

    private lateinit var mBluetoothGatt: BluetoothGatt
    private var localStatus: Int = 100
    private lateinit var mWriteCharacteristic: BluetoothGattCharacteristic
    private lateinit var mNotifyCharacteristic: BluetoothGattCharacteristic
    private val maxBufferSize: Int = 20
    private var isWriting: Boolean = false
    private var disconnectSilently = false

    /**
     * We need queue because only 20 byte can be written at time.
     */
    private val queue = LinkedBlockingQueue<ByteArray>(1000)

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

        b.asList()
            .chunked(maxBufferSize)
            .forEach { queue.put(it.toByteArray()) }

        if (!isWriting) {
            isWriting = true
            writeFromQueue()
        }
    }

    private fun writeFromQueue() {
        if (localStatus == 102) {//If we are connected
            mWriteCharacteristic.value = queue.take()
            logger.logd("writing from queue " + mWriteCharacteristic.value.size)
            if (!mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {
                logger.logw("Failed to write to characteristics")
            }
        }
    }

    override fun tryConnect() {
        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            logger.logd("try connect new")
            btDevice.connectGatt(context, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            logger.logd("try connect old")
            btDevice.connectGatt(context, false, mGattCallback)
        }
        localStatus = 101
        observer2?.connecting(deviceName)
        logger.logd("Status - connecting")
    }

    override fun tryDisconnect() {
        if (localStatus == 102) {
            mBluetoothGatt.disconnect()
            //mBluetoothGatt.close()
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {

            localStatus = 102
            logger.logd("connected.... legit.")

            var z = mBluetoothGatt.requestMtu(32)
            PictobloxLogger.getInstance().logd("MTU change request success? $z ")


            observer2?.connected(deviceName, btDevice.address)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            //BluetoothGatt.GATT_SUCCESS
            logger.logd("Mtu size changed to $mtu $status")

            //observer2?.notify(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            //logger.logw("Last write status $status")

            if (!queue.isEmpty()) {
                writeFromQueue()

            } else {
                isWriting = false
            }
            //observer2?.writeFinishBLECompat(status)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            logger.logd("onConnectionStateChange $status $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                logger.logd("Connecting in process - searching services")


                if (!mBluetoothGatt.discoverServices()) {
                    logger.logException(Exception("Service discovery failed"))
                    localStatus = 103
                    observer2?.error("Device not supported")
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logger.logd("disconnected")
                localStatus = 100
                if (disconnectSilently) {
                    disconnectSilently = false

                } else {
                    observer2?.disconnected(deviceName)
                }
                mBluetoothGatt.close()
                /*if(!queue.isEmpty()){
                  queue.clear()
                }*/

            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                logger.logd("STATE_CONNECTING")

            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                logger.logd("STATE_DISCONNECTING")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                var mCustomService: BluetoothGattService? =
                    gatt.getService(UUID.fromString(HM10.UUID_CUSTOM_SERVICE.value))

                if (mCustomService != null) {
                    mWriteCharacteristic =
                        mCustomService.getCharacteristic(UUID.fromString(HM10.UUID_CUSTOM_SERVICE_CHARACTERISTIC.value))
                    mNotifyCharacteristic = mWriteCharacteristic
                    //mNotifyCharacteristic = mCustomService.getCharacteristic(UUID.fromString(HM10.UUID_CUSTOM_SERVICE_CHARACTERISTIC.value))

                } else {

                    //ESP32?
                    val mCustomServiceOld = gatt.getService(UUID.fromString(ESP32OldFW.UUID_CUSTOM_SERVICE.value))
                    val mCustomServiceNew = gatt.getService(UUID.fromString(ESP32NewFW.UUID_CUSTOM_SERVICE.value))

                    if (mCustomServiceOld != null) {
                        mWriteCharacteristic =
                            mCustomServiceOld.getCharacteristic(UUID.fromString(ESP32OldFW.UUID_CUSTOM_SERVICE_WRITE_CHARACTERISTIC.value))
                        mNotifyCharacteristic =
                            mCustomServiceOld.getCharacteristic(UUID.fromString(ESP32OldFW.UUID_CUSTOM_SERVICE_NOTIFY_CHARACTERISTIC.value))

                        //mWriteCharacteristic.writeType
                        //Nothing that we can support
                    } else if (mCustomServiceNew != null) {
                        mWriteCharacteristic =
                            mCustomServiceNew.getCharacteristic(UUID.fromString(ESP32NewFW.UUID_CUSTOM_SERVICE_WRITE_CHARACTERISTIC.value))
                        mNotifyCharacteristic =
                            mCustomServiceNew.getCharacteristic(UUID.fromString(ESP32NewFW.UUID_CUSTOM_SERVICE_NOTIFY_CHARACTERISTIC.value))

                    } else {
                        localStatus = 103
                        logger.logw("Custom BLE Service not found")
                        observer2?.error("Device not supported")

                        return
                    }

                }

                //var z = mBluetoothGatt.requestMtu(32)
                //PictobloxLogger.getInstance().logd("MTU change request success? $z ")


                if ((mWriteCharacteristic.properties or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    PictobloxLogger.getInstance().logd("Write with no response possible")
                    mWriteCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

                } else {
                    mWriteCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }

                //This seem to be the default but just in case
                //mWriteCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                /*This characteristics is where r/w happens, we need to know when it changes*/
                gatt.setCharacteristicNotification(mNotifyCharacteristic, true)

                //var z = mBluetoothGatt.requestMtu(32)
                //PictobloxLogger.getInstance().logd("MTU change request success? $z ")

                val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                val descriptor = mNotifyCharacteristic.getDescriptor(uuid)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
/*
                localStatus = 102
                logger.logd("connected.... legit.")


                observer2?.connected(deviceName, btDevice.address)*/
                //val z = mBluetoothGatt.requestMtu(32)
                //PictobloxLogger.getInstance().logd("MTU change request success? $z ")


            } else {
                logger.logException(Exception("Service discovery failed"))
                localStatus = 103
                observer2?.error("Error in establishing connection")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                observer2?.notify(characteristic.value)

            } else {
                logger.logw("Read failed")
            }
        }

        /*        private var time: Long = 0L
                var frameCount = 0
                var byteCount = 0*/
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            /*val b = characteristic.value
            frameCount++
            byteCount = byteCount + b.size
            logger.logd("Frame " + frameCount+" bytes "+ byteCount+" time "+(System.currentTimeMillis() - time))
            time = System.currentTimeMillis()*/
            //logger.logd("characteristic.value ${String(characteristic.value)}")
            observer2?.notify(characteristic.value)

        }


    }
}
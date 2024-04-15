package io.stempedia.pictoblox.connectivity.devices

interface RemoteEviveDevice {
    val status: RemoteEviveDeviceStatus
    var observer2: DeviceObserver?

    fun tryWrite(b: ByteArray)

    fun tryConnect()

    fun tryDisconnect()

    /**
     * This is purely to make this new connectivity module compatible with older protocol execution
     */
    fun getDeviceType(): Int

}

interface DeviceObserver {
    fun notify(b: ByteArray)
    fun connecting(name: String)
    fun connected(name: String, address: String)
    fun disconnected(name: String)
    fun error(msg: String)
    //fun writeFinishBLECompat(status: Int)
}

enum class RemoteEviveDeviceStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}


enum class HM10(val value: String) {
    UUID_GENERIC("00001101-0000-1000-8000-00805F9B34FB"),
    UUID_CUSTOM_SERVICE("0000FFE0-0000-1000-8000-00805F9B34FB"),
    UUID_CUSTOM_SERVICE_CHARACTERISTIC("0000FFE1-0000-1000-8000-00805F9B34FB")
}


enum class ESP32OldFW(val value: String) {
    UUID_GENERIC("00001101-0000-1000-8000-00805F9B34FB"),
    UUID_CUSTOM_SERVICE("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
    UUID_CUSTOM_SERVICE_WRITE_CHARACTERISTIC("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
    UUID_CUSTOM_SERVICE_NOTIFY_CHARACTERISTIC("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
}

enum class ESP32NewFW(val value: String) {
    UUID_GENERIC("00001101-0000-1000-8000-00805F9B34FB"),
    UUID_CUSTOM_SERVICE("0000f005-0000-1000-8000-00805f9b34fb"),
    UUID_CUSTOM_SERVICE_WRITE_CHARACTERISTIC("5261da02-fa7e-42ab-850b-7c80220097cc"),
    UUID_CUSTOM_SERVICE_NOTIFY_CHARACTERISTIC("5261da01-fa7e-42ab-850b-7c80220097cc")
}

enum class HM05(val value: String) {
    UUID_GENERIC("00001101-0000-1000-8000-00805F9B34FB")
}
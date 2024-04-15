package io.stempedia.pictoblox.connectivity

import android.app.Service
import android.bluetooth.BluetoothDevice
import com.google.android.gms.common.api.ResolvableApiException

abstract class SearchDeviceService : Service() {
    abstract fun search(callback: SearchResultCallback)
    abstract fun search(c: Criteria, callback: SearchResultCallback)
    abstract fun stopSearch()
    abstract fun setSkipBLESearch(skip: Boolean)

}

interface SearchResultCallback {
    fun onBluetoothAdapterInitFailed()
    fun onDeviceFound(device: BluetoothDevice, type: DeviceType)
    fun onDeviceNameChanged(device: BluetoothDevice)
    fun onEnablingBluetooth()
    fun onBluetoothEnabled()
    //fun btNotEnabled()
    fun locationPermissionNotGranted()
    fun error(msg: String)
    fun gpsNotEnabledForV10(exception: ResolvableApiException)
    fun showUnresolvableErrorMessage()
    fun askBluetoothStart()
}



/**
 * Filter criteria
 */
interface Criteria

enum class DeviceType(val value: Int) {
    RECENTLY_PAIRED_WITH_EVIVE(1), RECENTLY_PAIRED_WITH_DEVICE(2), NEW_IN_RANGE(3)
}
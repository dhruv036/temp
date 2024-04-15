package io.stempedia.pictoblox.connectivity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import io.stempedia.pictoblox.connectivity.devices.ESP32NewFW
import io.stempedia.pictoblox.connectivity.devices.ESP32OldFW
import io.stempedia.pictoblox.connectivity.devices.HM10
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.util.*

/*
*
*
* REMEMBER :This is build to be used as a bound service, IF you start(with startService(Intent)) it
* you have to manually stop it or unintended performance penalty may occur.
 */

/**
 * Scans classic BT and low energy BT.
 *
 *
 */
class SearchDeviceServiceImpl : SearchDeviceService() {

    private val mBinder = LocalBinder()
    private lateinit var callback: SearchResultCallback
    private var btAdapter: BluetoothAdapter? = null
    private val logger = PictobloxLogger.getInstance()
    private var isSearchStarted: Boolean = false
    private lateinit var previouslyConnectedSet: Set<String>
    private var skipBLE = false
    private var isWaitingForBtToGetEnabled = false

    override fun onCreate() {
        super.onCreate()
        registerReceiver(btStateBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btStateBroadcastReceiver)
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@SearchDeviceServiceImpl
    }

    override fun search(callback: SearchResultCallback) {
        btAdapter = BluetoothAdapter.getDefaultAdapter()

        if (btAdapter == null) {
            callback.onBluetoothAdapterInitFailed()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!skipBLE) {
                callback.locationPermissionNotGranted()
                return
            }
        }


        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
            // Do something for lollipop and above versions
            val builder = LocationSettingsRequest.Builder().addLocationRequest(createLocationRequest())
            val task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                this.callback = callback
                previouslyConnectedSet = SPManager(this@SearchDeviceServiceImpl).recentlyConnectedDeviceSet
                enableAndStart()
            }

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    callback.gpsNotEnabledForV10(exception)

                } else {
                    callback.showUnresolvableErrorMessage()
                }

            }


        } else {
            this.callback = callback
            previouslyConnectedSet = SPManager(this@SearchDeviceServiceImpl).recentlyConnectedDeviceSet
            enableAndStart()
        }
    }

    override fun setSkipBLESearch(skip: Boolean) {
        skipBLE = skip
    }

    override fun search(c: Criteria, callback: SearchResultCallback) {
        //Nothing yet, but, we eventually want to set some criteria to deliver more pertinent list
    }

    //Manual stop
    override fun stopSearch() {
        if (isSearchStarted) {
            unregisterReceiver(mReceiver)

            btAdapter?.apply {
                cancelDiscovery()
                if (state == BluetoothAdapter.STATE_ON) {
                    bluetoothLeScanner.stopScan(leScanCallback)
                }
            }
            isSearchStarted = false
        }


    }

    private val btStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (isWaitingForBtToGetEnabled) {
                val state = intent!!.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        callback.onBluetoothEnabled()
                        startSearch()
                        isWaitingForBtToGetEnabled = false

                    }
                    /*BluetoothAdapter.STATE_OFF -> {
                        logger.logd("BluetoothAdapter.STATE_OFF ")
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        logger.logd("BluetoothAdapter.STATE_TURNING_ON ")

                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        logger.logd("BluetoothAdapter.STATE_TURNING_OFF ")

                    }*/
                }
            }

        }

    }


    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    private fun enableAndStart() {

        btAdapter?.apply {
            if (!isEnabled) {
                //callback.btNotEnabled()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S  && ActivityCompat.checkSelfPermission(
                        this@SearchDeviceServiceImpl,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                if (enable()) {
                    isWaitingForBtToGetEnabled = true
                    callback.onEnablingBluetooth()

                } else {
                    callback.askBluetoothStart()
                    callback.error("Please turn on your Bluetooth")
                }

                //return

            } else {
                callback.onBluetoothEnabled()
                startSearch()
            }
        }
    }

    private fun startSearch() {
        //getPairedDevices()
        findCBTDevices()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            findBLEDevices()
        }
        isSearchStarted = true
        logger.logd("Search started for paired, CBT and BLE")
    }

    private fun getPairedDevices() {
        btAdapter?.bondedDevices?.forEach {

            val type = if (previouslyConnectedSet.contains(it.address)) {
                DeviceType.RECENTLY_PAIRED_WITH_EVIVE
            } else {
                DeviceType.RECENTLY_PAIRED_WITH_DEVICE
            }

            callback.onDeviceFound(it, type)
        }
    }

    private fun findCBTDevices() {
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)

        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mReceiver, filter)

        filter = IntentFilter(BluetoothDevice.ACTION_NAME_CHANGED)
        registerReceiver(mReceiver, filter)

        //We are certain that btadapter here wont be null as this method is called after the check
        if (btAdapter!!.isDiscovering) {
            btAdapter!!.cancelDiscovery()
        }
        btAdapter!!.startDiscovery()
    }

    private fun findBLEDevices() {
        val filterHM10 = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(HM10.UUID_CUSTOM_SERVICE.value)))
            .build()

        val filterESP32OldFW = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(ESP32OldFW.UUID_CUSTOM_SERVICE.value)))
            .build()

        val filterESP32NewFW = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(ESP32NewFW.UUID_CUSTOM_SERVICE.value)))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        //btAdapter?.bluetoothLeScanner?.startScan(leScanCallback)
        btAdapter?.bluetoothLeScanner?.startScan(mutableListOf(filterHM10, filterESP32OldFW, filterESP32NewFW), scanSettings, leScanCallback)
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.apply {
                val type = if (previouslyConnectedSet.contains(device.address)) {
                    DeviceType.RECENTLY_PAIRED_WITH_EVIVE
                } else {
                    DeviceType.NEW_IN_RANGE
                }

                callback.onDeviceFound(device, type)
            }
        }
    }

    //Note :: when scan finish we probably should start again? IDK, it should run long enough to scan a device if it is around, we'll see.
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {

                BluetoothDevice.ACTION_FOUND -> intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.apply {
                    //if (bondState != BluetoothDevice.BOND_BONDED) {
                    val type = if (previouslyConnectedSet.contains(address)) {
                        DeviceType.RECENTLY_PAIRED_WITH_EVIVE
                    } else {
                        DeviceType.NEW_IN_RANGE
                    }


                    callback.onDeviceFound(this, type)
                    //}
                }


                BluetoothDevice.ACTION_NAME_CHANGED -> intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.apply {
                    if (bondState != BluetoothDevice.BOND_BONDED) {
                        callback.onDeviceNameChanged(this)
                    }


                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> logger.logd("CBT scanning completed")
            }
        }
    }

    //Creating least impactful GPS Request
    private fun createLocationRequest() = LocationRequest().apply {
        interval = 60_000
        fastestInterval = 60_000
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }
}
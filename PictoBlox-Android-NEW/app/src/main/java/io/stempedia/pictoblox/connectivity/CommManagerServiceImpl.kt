package io.stempedia.pictoblox.connectivity

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.devices.*
import io.stempedia.pictoblox.home.Home2Activity
import io.stempedia.pictoblox.learn.CourseManager
import io.stempedia.pictoblox.util.DabbleNotificationType
import io.stempedia.pictoblox.util.NotificationHelper
import io.stempedia.pictoblox.util.PictoBloxAnalyticsEventLogger
import io.stempedia.pictoblox.util.SPManager
import java.util.Locale


class CommManagerServiceImpl : CommManagerService(), DeviceObserver, FrameReadObserver {

    private val mBinder = LocalBinder()

    private var remoteDevice: RemoteEviveDevice? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var notificationHelper: NotificationHelper

    //private lateinit var notificationIntent: PendingIntent
    private val autoConnectHelper = AutoConnectHelper(handler)
    private var deviceType = -1
    private val bluetoothBufferReaderV2 = BluetoothFrameReaderV2(this)
    lateinit var communicationHandler: CommunicationHandlerWithPictoBloxWeb
    lateinit var courseManager: CourseManager


    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@CommManagerServiceImpl
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        communicationHandler = CommunicationHandlerWithPictoBloxWeb(handler, this)
        notificationHelper = NotificationHelper(this)
        courseManager = CourseManager(this)
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = manager.adapter

        if (mBluetoothAdapter == null) {
            Toast.makeText(this@CommManagerServiceImpl, R.string.bt_init_failed_msg, Toast.LENGTH_LONG).show()
        }

        autoConnectHelper.init()
        autoConnectHelper.start()


        registerReceiver(autoConnectHelper, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onDestroy() {
        super.onDestroy()
        autoConnectHelper.dispose()
        communicationHandler.onDestroy()
    }


    override fun setAutoConnectFlag(flag: Boolean) {
        autoConnectHelper.changeAutoConnectFlag(flag)
    }

    override fun connect(address: String) {
        PictoBloxAnalyticsEventLogger.getInstance().setBluetoothConnectAttempt()

        mBluetoothAdapter?.apply {
            if (isEnabled) {
                val device = createDevice2(address, this)
                deviceType = device.first

                device.second?.apply {
                    observer2 = this@CommManagerServiceImpl
                    remoteDevice = this
                    tryConnect()

                } ?: run {
                    logger.logw("Selected device not supported")
                    error("Selected device not supported")
                    //registerConnectStack(ConnectStack.ERROR_LEVEL_1.value)
                    PictoBloxAnalyticsEventLogger.getInstance().setBluetoothError("Device type ${getDeviceTypeString(deviceType)} is not supported")
                }

            } else {
                logger.logw("Bluetooth not enabled")
                error("Bluetooth not enabled")
                //registerConnectStack(ConnectStack.DEVICE_ITEM_CLICK.value)
            }
        }
    }

    override fun disconnect() {
        autoConnectHelper.isDisconnectUserInitiated = true
        if (isConnected()) {
            remoteDevice?.tryDisconnect()
        }
    }

    override fun isConnected() = remoteDevice?.let { it.status == RemoteEviveDeviceStatus.CONNECTED }
        ?: run { false }

    override fun write(b: ByteArray) {
        remoteDevice?.tryWrite(b)
    }

    override fun onUserInitiatedConnectionProcedure() {
        autoConnectHelper.stop()
    }

    override fun notify(b: ByteArray) {
        //dataListener?.onDataReceived(b)
        bluetoothBufferReaderV2.putInQueue(b)
    }

    override fun connecting(name: String) {
        connectionListeners.forEach { handler.post { it.onDeviceConnecting(name) } }
    }

    override fun connected(name: String, address: String) {

        autoConnectHelper.onConnected(name, address)
        //Once user initiated connect is successful we reset this flag if previously set so that auto-connect loop can re-instantiated
        autoConnectHelper.isDisconnectUserInitiated = false

        //Add this device to recently connected device
        sharedPreferencesManager.recentlyConnectedDeviceSet =
            sharedPreferencesManager.recentlyConnectedDeviceSet.plus(address)

        //Start buffer reader
        bluetoothBufferReaderV2.start()

        //Foreground service status
        setForegroundStatus()

        connectionListeners.forEach {
            handler.post { it.onDeviceConnected(name, address) }
        }

        PictoBloxAnalyticsEventLogger.getInstance().setBluetoothConnected(getDeviceTypeString(deviceType))
        //registerConnectStack(ConnectStack.CONNECT.value)
    }

    override fun disconnected(name: String) {
        PictoBloxAnalyticsEventLogger.getInstance().setBluetoothDisconnected()
        autoConnectHelper.onDisconnected()
        bluetoothBufferReaderV2.stop()
        removeForegroundStatus()

        connectionListeners.forEach {
            handler.post {
                it.onDeviceDisconnected(name)
            }
        }
    }

    override fun error(msg: String) {
        autoConnectHelper.onConnectionFailed()
        PictoBloxAnalyticsEventLogger.getInstance().setBluetoothError("<${getDeviceTypeString(deviceType)}> : $msg")
        //registerConnectStack(ConnectStack.ERROR_LEVEL_2.value)
        connectionListeners.forEach { handler.post { it.error(msg) } }
    }

    override fun onFrameRead(b: ByteArray) {
        communicationHandler.processFrame(b)
    }

    /*
    *
    *
    *
    *
    * Helper method, Local stuff.
    *
    *
    *
    * ************

     */


    private fun setForegroundStatus() {
        val builder = notificationHelper.createBuilder(
            DabbleNotificationType.TYPE_SERVICE_FOREGROUND,
            getString(R.string.dabble_connected),
            ""
        )
        builder.setContentIntent(PendingIntent.getActivity(
            this,
            0,
            Intent(this, Home2Activity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        ))
        notificationHelper.startForeground(this@CommManagerServiceImpl)


        //startForeground(/*ONGOING_NOTIFICATION_ID*/103, createNotification())
    }


    private fun removeForegroundStatus() {
        stopForeground(true)
        //notificationHelper.startForeground(this@CommManagerServiceImpl)
    }


    private fun createDevice2(address: String, mBluetoothAdapter: BluetoothAdapter): Pair<Int, RemoteEviveDevice?> {
        val device = mBluetoothAdapter.getRemoteDevice(address)

        logger.logd("Creating device " + device.type)
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> Pair(
                device.type,
                RemoteEviveDeviceBLE(device, applicationContext, logger)
            )
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> Pair(device.type, RemoteEviveDeviceCBT(device, logger))

            else -> {
                Pair(device.type, null)
            }
        }
    }

    /*private fun registerConnectStack(level: String) {
        val bundle = Bundle()
        bundle.putString(getString(R.string.analytics_connect_stack_level), level)
        FirebaseAnalytics
            .getInstance(this@CommManagerServiceImpl)
            .logEvent(getString(R.string.analytics_connect_stack), bundle)
    }*/

    /*private fun registerConnectAttemptAnalytics() {
        FirebaseAnalytics
            .getInstance(this)
            .logEvent(getString(R.string.analytics_connect_attempt), null)
    }*/

    /*private fun registerUnsupportedDeviceAnalytics(deviceType: String) {
        val bundle = Bundle()
        bundle.putString(getString(R.string.analytics_connect_device_type), deviceType)
        FirebaseAnalytics
            .getInstance(this)
            .logEvent(getString(R.string.analytics_connect_unsupported_device), bundle)
    }*/
/*
    private fun registerConnectErrorAnalytics(deviceType: String, error: String) {
        val bundle = Bundle()
        bundle.putString(getString(R.string.analytics_connect_device_type), deviceType)
        bundle.putString(getString(R.string.analytics_connect_fail_reason), error)
        FirebaseAnalytics
            .getInstance(this)
            .logEvent(getString(R.string.analytics_connect_fail), bundle)
    }*/

    /*private fun registerConnectSuccessAnalytics(deviceType: String) {
        val bundle = Bundle()
        bundle.putString(getString(R.string.analytics_connect_device_type), deviceType)
        FirebaseAnalytics
            .getInstance(this)
            .logEvent(getString(R.string.analytics_connect_success), bundle)
    }*/


    //connection_error
    //device_type
    //board version
    //evive lib version
    /*private fun registerContentViewEventForAnalytics(event: String, value: String) {
        val bundle = Bundle()
        bundle.putString("device_type", value)
        FirebaseAnalytics.getInstance(this).logEvent(event, bundle)
    }*/


    private fun getDeviceTypeString(type: Int): String {
        return when (type) {
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "CLASSIC"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
            else -> "UNKNOWN"

        }
    }

    inner class AutoConnectHelper(val handler: Handler) : Runnable, DeviceObserver, BroadcastReceiver() {

        private var isAutoConnectTimerSet = false
        private var isDeviceConnectProcessInitiated = false
        var isDisconnectUserInitiated = false
        private var exponentialDelay = 3000
        private val tag = "AUTO_CONNECT"
        private var isConnected = false
        private var exponentialDelayMultiplier = 1
        private var autoConnectAttemptCount = 0


        fun init() {
            registerReceiver(this@AutoConnectHelper, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        }

        fun dispose() {
            stop()
            unregisterReceiver(this@AutoConnectHelper)
        }

        override fun run() {
            isAutoConnectTimerSet = false
            mBluetoothAdapter?.apply {
                if (isEnabled) {
                    isDeviceConnectProcessInitiated = true

                    val device = createDevice2(sharedPreferencesManager.lastConnectedDeviceAddress, this)
                    deviceType = device.first

                    device.second?.apply {
                        autoConnectAttemptCount++
                        observer2 = this@AutoConnectHelper
                        remoteDevice = this
                        tryConnect()

                    } ?: run {
                        logger.logw("$tag Selected device not supported")
                        error("$tag Selected device not supported")
                    }

                }
            }
        }

        fun changeAutoConnectFlag(autoConnect: Boolean) {
            sharedPreferencesManager.isAutoConnectToLastDevice = autoConnect

            if (autoConnect) {
                start()

            } else {
                stop()
            }

            registerAutoConnectedEvent(autoConnect)

        }

        fun start() {

            if (sharedPreferencesManager.isAutoConnectToLastDevice
                && !isDisconnectUserInitiated
                && !isDeviceConnectProcessInitiated
                && !isAutoConnectTimerSet
                && !isConnected
            ) {

                if (!TextUtils.isEmpty(sharedPreferencesManager.lastConnectedDeviceAddress)) {
                    val delay = calculateExponentialDelay()
                    handler.postDelayed(this@AutoConnectHelper, delay)
                    isAutoConnectTimerSet = true
                }
            }
        }

        fun stop() {
            if (isAutoConnectTimerSet) {
                handler.removeCallbacks(this)
            }

            if (isDeviceConnectProcessInitiated) {
                remoteDevice?.tryDisconnect()
            }
        }

        fun onConnected(name: String, address: String) {
            isConnected = true
            autoConnectAttemptCount = 0
            sharedPreferencesManager.lastConnectedDeviceAddress = address
            sharedPreferencesManager.lastConnectedDeviceName = name
            resetExponentialDelay()
            if (isDeviceConnectProcessInitiated) {
                //handing over listener to service
                remoteDevice?.observer2 = this@CommManagerServiceImpl
                isDeviceConnectProcessInitiated = false
            }


        }

        fun onDisconnected() {
            autoConnectAttemptCount = 0
            isConnected = false
            start()

            //TODO???
            isDeviceConnectProcessInitiated = false
        }

        fun onConnectionFailed() {
            isConnected = false
            isDeviceConnectProcessInitiated = false
            start()
        }

        private fun calculateExponentialDelay(): Long {
            val delay = (exponentialDelay * exponentialDelayMultiplier).toLong()

            if (exponentialDelayMultiplier < 4) {
                exponentialDelayMultiplier++
            }

            return delay
        }

        private fun resetExponentialDelay() {
            exponentialDelayMultiplier = 1
            exponentialDelay = 3000
        }

        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent!!.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    start()
                }

                BluetoothAdapter.STATE_OFF -> {
                    //TODO test this well...
                    stop()
                }

                /*  BluetoothAdapter.STATE_TURNING_ON -> {

                }

                BluetoothAdapter.STATE_TURNING_OFF -> {

                }*/
            }

        }

        //Just delegate just in case we still haven't switched listeners
        override fun notify(b: ByteArray) {
            this@CommManagerServiceImpl.notify(b)
        }

        override fun connecting(name: String) {
            //We wont be bugging user with multiple connecting toasts
        }

        override fun connected(name: String, address: String) {
            this@CommManagerServiceImpl.connected(name, address)
            onConnected(name, address)
            registerAutoConnectedSession(autoConnectAttemptCount)
        }

        override fun disconnected(name: String) {
            onDisconnected()
//            removeForegroundStatus()
        }

        override fun error(msg: String) {
            onConnectionFailed()
        }

        private fun registerAutoConnectedEvent(enabled: Boolean) {
            val bundle = Bundle()
            bundle.putString(
                getString(R.string.analytics_auto_connect_event_flag_changed_from),
                getString(R.string.analytics_auto_connect_event_flag_changed_from_dialog)
            )
            bundle.putString(getString(R.string.analytics_auto_connect_event_flag_value), enabled.toString())
            /*FirebaseAnalytics
                .getInstance(this@CommManagerServiceImpl)
                .logEvent(getString(R.string.analytics_auto_connect_event_flag), bundle)*/
        }

        private fun registerAutoConnectedSession(count: Int) {
            val bundle = Bundle()
            bundle.putInt(getString(R.string.analytics_success_after), count)
            /*FirebaseAnalytics
                .getInstance(this@CommManagerServiceImpl)
                .logEvent(getString(R.string.analytics_auto_connect_session), bundle)*/
        }
    }
}

enum class ConnectStack(val value: String) {
    ICON_CLICK("bt_icon_click"),
    DEVICE_LISTING("bt_device_listing"),
    DEVICE_ITEM_CLICK("bt_device_clicked"),
    ERROR_LEVEL_1("bt_connect_error_level_1"),
    ERROR_LEVEL_2("bt_connect_error_level_2"),
    CONNECT("bt_connect_success")
}




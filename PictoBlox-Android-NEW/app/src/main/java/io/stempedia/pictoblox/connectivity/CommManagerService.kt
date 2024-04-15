package io.stempedia.pictoblox.connectivity

import android.app.Service
import android.os.Handler
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager

interface ConnectionStatusListener {
    fun onDeviceConnecting(name: String)
    fun onDeviceConnected(name: String, address: String)
    fun onDeviceDisconnected(name: String)
    fun error(msg: String)
}

abstract class CommManagerService : Service() {
    protected val handler = Handler()
    protected val logger = PictobloxLogger.getInstance()


    protected lateinit var sharedPreferencesManager: SPManager

    protected val connectionListeners = mutableListOf<ConnectionStatusListener>()

    abstract fun connect(address: String)
    abstract fun disconnect()
    abstract fun isConnected(): Boolean
    abstract fun write(b: ByteArray)
    abstract fun onUserInitiatedConnectionProcedure()
    abstract fun setAutoConnectFlag(flag: Boolean)


    fun addConnectionListener(listener: ConnectionStatusListener) {
        connectionListeners.add(listener)
    }

    fun removeConnectionListener(listener: ConnectionStatusListener) {
        connectionListeners.remove(listener)
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferencesManager = SPManager(this)
    }
}

interface DataListener {
    fun onDataReceived(b: ByteArray)
}



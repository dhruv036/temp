package io.stempedia.pictoblox.uiUtils

import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.MultiCameraActivity
import com.jiangdg.ausbc.camera.CameraUVC
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.*
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager

abstract class AbsActivity :  MultiCameraActivity(), ConnectionStatusListener {
    protected var commManagerService: CommManagerServiceImpl? = null
    private val compositeDisposable = CompositeDisposable()
    private val requestDeviceDiscovery = 100
    protected lateinit var sharedPreferencesManager: SPManager

    protected abstract fun onPBServiceConnected(commManagerService: CommManagerServiceImpl)
    protected abstract fun onBeforeServiceGetsDisconnected(commManagerService: CommManagerServiceImpl)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferencesManager = SPManager(this@AbsActivity)
        /*bindService(
            Intent(this, CommManagerServiceImpl::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )*/
    }

    override fun onCameraAttached(camera: MultiCameraClient.ICamera) {
    }

    override fun onCameraConnected(camera: MultiCameraClient.ICamera) {
    }

    override fun onCameraDetached(camera: MultiCameraClient.ICamera) {
    }

    override fun onCameraDisConnected(camera: MultiCameraClient.ICamera) {
    }

    override fun getRootView(layoutInflater: LayoutInflater): View? {
        return View(this)
    }

    override fun generateCamera(ctx: Context, device: UsbDevice): MultiCameraClient.ICamera {
        return CameraUVC(ctx,device)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        commManagerService?.apply {
            onBeforeServiceGetsDisconnected(this)
            removeConnectionListener(this@AbsActivity)
            this@AbsActivity.unbindService(serviceConnection)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, CommManagerServiceImpl::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        PictobloxLogger.getInstance().logd("")
        Log.d("DEBUG", "onStop: ")
//        commManagerService?.apply {
//            onBeforeServiceGetsDisconnected(this)
//            removeConnectionListener(this@AbsActivity)
//            this@AbsActivity.unbindService(serviceConnection)
//        }
    }

    fun add(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

/*    override fun onStart() {
        super.onStart()
        bindService(Intent(this, CommManagerServiceImpl::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        commManagerService?.apply {
            onBeforeServiceGetsDisconnected(this)
            removeConnectionListener(this@AbsActivity)
            this@AbsActivity.unbindService(serviceConnection)
        }
    }*/

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            (service as CommManagerServiceImpl.LocalBinder).getService().apply {

                addConnectionListener(this@AbsActivity)
                onPBServiceConnected(this)
                commManagerService = this
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            commManagerService = null
        }
    }


    internal fun handleDeviceClick() {
        commManagerService?.apply {

            if (isConnected()) {


                val dialog = InteractiveDialog()
                dialog.arguments = Bundle().apply {
                    putInt(INTERACTIVE_DIALOG_ICON, R.drawable.ic_dialog_disconnect)
                    putInt(INTERACTIVE_DIALOG_MESSAGE, R.string.disconnect_prompt_message)
                    putInt(INTERACTIVE_DIALOG_BUTTON_LEFT, R.string.no)
                    putInt(INTERACTIVE_DIALOG_BUTTON_RIGHT, R.string.yes)
                }


                dialog.setButtonClickListener(object : InteractiveDialogClickListener {
                    override fun onRightButtonClicked(dialog: Dialog) {
                        disconnect()
                        dialog.dismiss()
                    }

                    override fun onLeftButtonClicked(dialog: Dialog) {
                        dialog.dismiss()
                    }

                })
                dialog.show(supportFragmentManager, InteractiveDialog::class.java.name)


            } else {
                onUserInitiatedConnectionProcedure()
                /*FirebaseAnalytics
                    .getInstance(this)
                    .logEvent(getString(R.string.analytics_connect_device_list), null)*/
                startActivityForResult(
                    Intent(this, DeviceDiscoveryActivity::class.java),
                    requestDeviceDiscovery
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestDeviceDiscovery) {
            if (resultCode == Activity.RESULT_OK) {
                commManagerService?.connect(data!!.getStringExtra(EXTRA_DEVICE_ADDRESS)!!)

            } else {
                Toast.makeText(this, getString(R.string.search_canceled), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun autoConnectDialog(connectedDeviceName: String) {
        if (!sharedPreferencesManager.autoConnectPromptAsked && !sharedPreferencesManager.isAutoConnectToLastDevice) {

            val message = SpannableStringBuilder()
            message.append(getString(R.string.auto_connect_step1))
            message.append(
                SpannedString(connectedDeviceName),
                StyleSpan(Typeface.BOLD),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            message.append(getString(R.string.auto_connect_step2))

            val dialog = InteractiveDialog()

            dialog.arguments = Bundle().apply {
                putInt(INTERACTIVE_DIALOG_ICON, R.drawable.ic_dialog_autoconnect)
                putCharSequence(INTERACTIVE_DIALOG_MESSAGE_CHAR_SEQ, message)
                putInt(INTERACTIVE_DIALOG_BUTTON_LEFT, R.string.no)
                putInt(INTERACTIVE_DIALOG_BUTTON_RIGHT, R.string.yes)
            }

            dialog.setButtonClickListener(object : InteractiveDialogClickListener {
                override fun onRightButtonClicked(dialog: Dialog) {
                    commManagerService?.setAutoConnectFlag(true)
                    Toast.makeText(
                        this@AbsActivity,
                        getString(R.string.preference_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }

                override fun onLeftButtonClicked(dialog: Dialog) {
                    dialog.dismiss()
                }

            })
            /*if (!isFinishing) {
                dialog.show(supportFragmentManager, InteractiveDialog::class.java.name)
            }*/


            sharedPreferencesManager.autoConnectPromptAsked = true
        }
    }
}
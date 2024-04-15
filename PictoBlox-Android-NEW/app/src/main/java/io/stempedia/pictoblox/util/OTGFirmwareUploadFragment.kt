package io.stempedia.pictoblox.util

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.fragment.app.Fragment
import com.physicaloid.lib.Boards
import com.physicaloid.lib.Physicaloid
import com.physicaloid.lib.programmer.avr.UploadErrors
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragOtgUploadFirmwareBinding
import java.io.InputStream

const val OTG_BOARD_NAME = "board_name"

class OTGFirmwareUploadFragment : Fragment() {
    private val vm = OTGFirmwareUploadFragmentVM(this)
    private lateinit var mBinding: FragOtgUploadFirmwareBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.frag_otg_upload_firmware, container, false)
        mBinding.data = vm
        vm.onCreateView(arguments)
        return mBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val filter = IntentFilter("USB_PERMISSION")
        context.registerReceiver(vm.receiver, filter)
    }

    override fun onDetach() {
        super.onDetach()
        context?.unregisterReceiver(vm.receiver)
    }

    fun askActivityToRemoveThisFragment() {
        activity?.also {
            it.supportFragmentManager.popBackStack()
        }
    }

}

class OTGFirmwareUploadFragmentVM(val fragment: OTGFirmwareUploadFragment) : Physicaloid.UploadCallBack {
    //val buttonText = ObservableField<String>()
    val isUploading = ObservableBoolean()
    //val isError = ObservableBoolean()
    val infoText = ObservableField<String>()
    val boardSelectedText = ObservableField<String>()
    val buttonText = ObservableField<String>()
    val uploadingPercentage = ObservableInt(0)
    private var selectedBoard: Int = -1
    private var physicaloid: Physicaloid? = null
    private val handler = Handler()
    private var assetStream: InputStream? = null

    private val boardMap = mapOf(
        "evive" to Boards.ARDUINO_MEGA_2560_ADK,
        "Arduino Mega" to Boards.ARDUINO_MEGA_2560_ADK,
        "Arduino Uno (M328P only)" to Boards.ARDUINO_UNO,
        "Arduino Nano" to Boards.ARDUINO_NANO_328
        /*"Esp32" to Boards.ARDUINO_MEGA_2560_ADK,
        "T-watch" to Boards.ARDUINO_MEGA_2560_ADK,
        "Tecbits" to Boards.ARDUINO_NANO_328*/
    )

    //This is just convenience
    private val boardNameArray = boardMap.keys.toTypedArray()

    fun onCreateView(arguments: Bundle?) {

        //If board name is already given from webview then we directly start uploading
        arguments?.also {
            val index = boardNameArray.indexOf(it.getString(OTG_BOARD_NAME))
            if (index != -1) {
                selectedBoard = index
                boardSelectedText.set(boardNameArray[selectedBoard])
                onUploadClicked()

            } else {
                boardSelectedText.set("Select Board")
                buttonText.set("Upload")
            }

        } ?: kotlin.run {
            boardSelectedText.set("Select Board")
            buttonText.set("Upload")
        }
    }


    fun onSelectBoardClicked() {

        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Select Board")
            .setSingleChoiceItems(boardNameArray, selectedBoard) { dialog, which ->
                selectedBoard = which
                boardSelectedText.set(boardNameArray[selectedBoard])
                dialog?.dismiss()
            }
            .create()
            .show()

    }

    fun onUploadClicked() {
        fragment.activity?.also {

            if (isUploading.get()) {
                physicaloid?.cancelUpload()

            } else {
                if (selectedBoard != -1) {
                    uploadFirmware(it, boardMap.getValue(boardNameArray[selectedBoard]))

                } else {
                    Toast.makeText(fragment.requireContext(), "Please Select Board", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onExternalPlaneClicked() {
        if (!isUploading.get()) {
            fragment.askActivityToRemoveThisFragment()

        }
    }

    fun onIgnoreClick() {

    }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isUSBAccessApproved = intent?.getBooleanExtra("permission", false) ?: false

            if (isUSBAccessApproved) {
                if (selectedBoard != -1) {
                    uploadFirmware(fragment.requireContext(), boardMap.getValue(boardNameArray[selectedBoard]))

                } else {
                    Toast.makeText(fragment.requireContext(), "Please Select Board", Toast.LENGTH_LONG).show()
                }

            } else {

                infoText.set("Permission to access USB device is\nrequired to upload firmware")
            }

        }

    }

    private fun uploadFirmware(context: Context, board: Boards) {

        if (isUploading.get()) {
            Toast.makeText(fragment.requireContext(), "Uploading already in process", Toast.LENGTH_LONG).show()
            return
        }

        buttonText.set("Stop")

        assetStream = context.assets.open("essential_files/eviveFirmware.hex")

        physicaloid = Physicaloid(context)

        uploadingPercentage.set(0)
        //isError.set(false)
        isUploading.set(true)

        physicaloid?.upload(board, assetStream, this)


    }

    override fun onPostUpload(success: Boolean) {
        assetStream?.close()
        fragment.activity?.also {
            handler.post {
                if (success) {
                    Toast.makeText(it, "Firmware Successfully Uploaded", Toast.LENGTH_LONG).show()
                    fragment.askActivityToRemoveThisFragment()

                } else {
                    infoText.set("upload failed")
                }
            }
        }
    }

    override fun onCancel() {
        handler.post {
            isUploading.set(false)
            assetStream?.close()
            infoText.set("upload canceled")
        }
    }

    override fun onUploading(value: Int) {
        handler.post {
            uploadingPercentage.set(value)
            infoText.set("uploading")
        }
    }

    override fun onError(err: UploadErrors?) {
        handler.postDelayed({

            isUploading.set(false)
            assetStream?.close()
            infoText.set("Error: ${err?.description}")
            buttonText.set("Retry")

        }, 500)
    }

    override fun onPreUpload() {
        handler.post {
            infoText.set("Upload starting")
        }
    }
}
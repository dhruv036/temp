package io.stempedia.pictoblox.userInputArgument


import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.databinding.FragmentKeyboardArgumentBinding


const val ARGUMENT_TYPE = "type"
const val ARGUMENT_PLACE_HOLDER = "placeHolder"
const val ARGUMENT_CURRENT_VALUE = "CurrentValue"
//const val ARGUMENT_PARAM = "argParam"
const val HANDLER_FUNCTION = "handlerFun"
const val ARGUMENT_TYPE_NUMBER = "number"
const val ARGUMENT_TYPE_STRING = "string"
const val ARGUMENT_TYPE_FLOAT = "float"



class KeyboardArgumentFragment : DialogFragment() {
    private lateinit var mBinding: FragmentKeyboardArgumentBinding
    private val vm = KeyboardArgumentVM(this)
    private var commManagerService: CommManagerServiceImpl? = null
    //private var argumentParam: String = ""
    private var argumentType: String = ""
    private var argumentCurrentValue: String = ""
    private var argumentPlaceHolder: String = ""
    private var handlerFunction: String = ""


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setStyle(STYLE_NO_FRAME, android.R.style.Theme)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_keyboard_argument, container, false)
        mBinding.data = vm

        mBinding.editText2.setOnEditorActionListener { _, actionId, event -> vm.onEditorAction(actionId, event) }

        mBinding.editText2.requestFocus()

        validateParameters()

        when (argumentType) {
            ARGUMENT_TYPE_STRING -> {
                mBinding.editText2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }

            ARGUMENT_TYPE_NUMBER -> {
                //mBinding.editText2.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                mBinding.editText2.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            }

            ARGUMENT_TYPE_FLOAT -> {
                mBinding.editText2.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            }
        }

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return mBinding.root
    }

    fun showCantPostEmptyArgumentResponse() {
        Toast.makeText(activity, "String input cannot be empty", Toast.LENGTH_LONG).show()
    }

    fun showCantPostInvalidIntegerArgumentResponse() {
        Toast.makeText(activity, "Only Integer input is valid", Toast.LENGTH_LONG).show()
    }

    fun showCantPostInvalidFloatArgumentResponse() {
        Toast.makeText(activity, "Only float input is valid", Toast.LENGTH_LONG).show()
    }

    private fun validateParameters() {
        arguments?.apply {
            //argumentParam = getString(ARGUMENT_PARAM, "")
            argumentType = getString(ARGUMENT_TYPE, "")
            argumentPlaceHolder = getString(ARGUMENT_PLACE_HOLDER, "")
            argumentCurrentValue = getString(ARGUMENT_CURRENT_VALUE, "")
            handlerFunction = getString(HANDLER_FUNCTION, "")

            if (argumentType != ARGUMENT_TYPE_NUMBER && argumentType != ARGUMENT_TYPE_STRING && argumentType != ARGUMENT_TYPE_FLOAT) {
                dismiss()
            }


        } ?: run {
            dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.apply {
            bindService(
                Intent(this, CommManagerServiceImpl::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        commManagerService?.apply {
            //onBeforeServiceGetsDisconnected(this)
            activity?.unbindService(serviceConnection)
        }
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            (service as CommManagerServiceImpl.LocalBinder).getService().apply {

                //onPBServiceConnected(this)
                commManagerService = this
                vm.onServiceConnected(argumentType, argumentPlaceHolder, argumentCurrentValue, handlerFunction,this)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            vm.onBeforeServiceDisconnect()
            commManagerService = null
        }
    }

}

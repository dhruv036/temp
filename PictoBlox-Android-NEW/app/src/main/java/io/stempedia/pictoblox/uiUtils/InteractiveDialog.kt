package io.stempedia.pictoblox.uiUtils

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.DialogInteractiveBinding

const val INTERACTIVE_DIALOG_ICON = "icon"
const val INTERACTIVE_DIALOG_MESSAGE = "msg"
const val INTERACTIVE_DIALOG_MESSAGE_CHAR_SEQ = "msg2"
const val INTERACTIVE_DIALOG_BUTTON_LEFT = "btn1"
const val INTERACTIVE_DIALOG_BUTTON_RIGHT = "btn2"
const val INTERACTIVE_CANCELABLE = "cancelable"
const val INTERACTIVE_LEFT_BUTTON_COLOR = "left_color"
const val INTERACTIVE_RIGHT_BUTTON_COLOR = "right_color"

interface InteractiveDialogClickListener {
    fun onLeftButtonClicked(dialog: Dialog)
    fun onRightButtonClicked(dialog: Dialog)
}

class InteractiveDialog : DialogFragment() {

    lateinit var mBinding: DialogInteractiveBinding
    private var listener: InteractiveDialogClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        mBinding = DataBindingUtil.inflate(activity!!.layoutInflater, R.layout.dialog_interactive, null, false)

        arguments?.getInt(INTERACTIVE_DIALOG_ICON)?.apply {
            if (this != 0) {
                mBinding.ivInteractiveIcon.setImageResource(this)
            }
        }
        arguments?.getInt(INTERACTIVE_DIALOG_MESSAGE)?.apply {
            if (this != 0) {
                mBinding.tvInteractiveMessage.text = getString(this)
            }
        }

        arguments?.getCharSequence(INTERACTIVE_DIALOG_MESSAGE_CHAR_SEQ)?.apply {
            mBinding.tvInteractiveMessage.text = this
        }

        arguments?.getInt(INTERACTIVE_DIALOG_BUTTON_LEFT)?.apply {
            if (this != 0) {
                mBinding.btnInteractiveLeft.visibility = View.VISIBLE
                mBinding.btnInteractiveLeft.text = getString(this)
            }
        }
        arguments?.getInt(INTERACTIVE_DIALOG_BUTTON_RIGHT)?.apply {
            if (this != 0) {
                mBinding.btnInteractiveRight.visibility = View.VISIBLE
                mBinding.btnInteractiveRight.text = getString(this)
            }
        }
        arguments?.getInt(INTERACTIVE_LEFT_BUTTON_COLOR)?.apply {
            if (this != 0) {
                mBinding.btnInteractiveLeft.setTextColor(ContextCompat.getColor(activity!!, this))
            }
        }
        arguments?.getInt(INTERACTIVE_RIGHT_BUTTON_COLOR)?.apply {
            if (this != 0) {
                mBinding.btnInteractiveRight.setTextColor(ContextCompat.getColor(activity!!, this))
            }
        }


        mBinding.btnInteractiveLeft.setOnClickListener { listener?.onLeftButtonClicked(dialog!!) }

        mBinding.btnInteractiveRight.setOnClickListener { listener?.onRightButtonClicked(dialog!!) }


        val alertBuilder = AlertDialog.Builder(context!!)
                .setView(mBinding.root)

        arguments?.getBoolean(INTERACTIVE_CANCELABLE, true)?.apply {
            alertBuilder.setCancelable(this)
        }

        val d = alertBuilder.create()
        d?.window?.setBackgroundDrawableResource(android.R.color.transparent);

        return d

    }

    fun setButtonClickListener(listener: InteractiveDialogClickListener) {
        this.listener = listener
    }
}
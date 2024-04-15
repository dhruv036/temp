package io.stempedia.pictoblox.userInputArgument

import android.text.TextUtils
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.databinding.ObservableField
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl

class KeyboardArgumentVM(val fragment: KeyboardArgumentFragment) {
    private var commManagerServiceImpl: CommManagerServiceImpl? = null

    //private var argumentParam: String = ""
    private var argumentType: String = ""
    private var argumentPlaceholder: String = ""
    private var argumentCurrentValue: String = ""
    private var handlerFun: String = ""
    val text = ObservableField<String>()

    //val hint = ObservableInt(R.string.argument_dialog_enter_text)
    val hint = ObservableField<String>()

    fun onServiceConnected(
        argumentType: String,
        argPlaceHolder: String,
        argCurrentValue: String,
        handlerFun: String,
        commManagerServiceImpl: CommManagerServiceImpl
    ) {
        this.argumentType = argumentType
        this.argumentPlaceholder = argPlaceHolder
        this.argumentCurrentValue = argCurrentValue
        this.handlerFun = handlerFun
        this.commManagerServiceImpl = commManagerServiceImpl

        //hint.set(commManagerServiceImpl.resources.getString(R.string.argument_dialog_hint, argumentParam))
        hint.set(argPlaceHolder)

        if (!TextUtils.isEmpty(argumentCurrentValue)) {
            text.set(argumentCurrentValue)
        }
    }

    fun onBeforeServiceDisconnect() {
        commManagerServiceImpl = null
    }

    fun onEditorAction(actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            commManagerServiceImpl?.communicationHandler?.also {

                when (argumentType) {
                    ARGUMENT_TYPE_STRING -> {

                        if (!TextUtils.isEmpty(text.get())) {
                            it.apiFromPictobloxWeb.editSpriteParamters(handlerFun, text.get()!!)
                            fragment.dismiss()

                        } else {
                            fragment.showCantPostEmptyArgumentResponse()

                        }
                    }

                    ARGUMENT_TYPE_NUMBER -> {

                        try {
                            val intArg = (text.get() ?: "").toInt()
                            it.apiFromPictobloxWeb.editSpriteParamters(
                                handlerFun,
                                intArg.toString()
                            ) //We always send in string the cast is just for validation
                            fragment.dismiss()

                        } catch (e: NumberFormatException) {
                            fragment.showCantPostInvalidIntegerArgumentResponse()
                        }

                    }

                    ARGUMENT_TYPE_FLOAT -> {

                        try {
                            val intArg = (text.get() ?: "").toFloat()
                            it.apiFromPictobloxWeb.editSpriteParamters(
                                handlerFun,
                                intArg.toString()
                            ) //We always send in string the cast is just for validation
                            fragment.dismiss()

                        } catch (e: NumberFormatException) {
                            fragment.showCantPostInvalidFloatArgumentResponse()
                        }

                    }
                }


            }
            return true
        }
        return false
    }


}
package io.stempedia.pictoblox.util

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.GsonBuilder

class PictoBloxAnalyticsEventLogger(val application: Application) {
    private val analytics = FirebaseAnalytics.getInstance(application)
    private val shouldLogEvents = true

    companion object {

        @Volatile
        private var INSTANCE: PictoBloxAnalyticsEventLogger? = null

        fun getInstance(): PictoBloxAnalyticsEventLogger {
            return INSTANCE!!
        }

        fun createInstance(application: Application) {
            INSTANCE = PictoBloxAnalyticsEventLogger(application)
        }

    }


    fun setLanguageSelected(languageCode: String) {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("language_selected",
            Bundle().apply {
                putString(FirebaseAnalytics.Param.VALUE, languageCode)
            }
        )
    }

    fun setExampleOpened(exampleName: String) {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("examples_opened",
            Bundle().apply {
                putString(FirebaseAnalytics.Param.VALUE, exampleName)
            }
        )
    }

    fun setBluetoothConnected(type: String) {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("bluetooth_connect",
            Bundle().apply {
                putString("Bluetooth_type", type)
            }
        )
    }

    fun setBluetoothDisconnected() {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("bluetooth_disconnect", null)
    }


    fun setBluetoothError(reason: String) {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("bluetooth_error",
            Bundle().apply {
                putString("reason", reason)
            }
        )
    }

    fun setBluetoothConnectAttempt() {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("bluetooth_connect_attempt", null)
    }

    fun setFileSaved(fileVersion: String) {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("file_save",
            Bundle().apply {
                putString("file_version", fileVersion)
            }
        )
    }


    fun setTourStarted() {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("tour_started",
            Bundle().apply {
                putString("location", "first_time")
            }
        )
    }


    fun setTourSkipped() {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("tour_skipped",
            Bundle().apply {
                putString("tour_type", "modal")
            }
        )
    }


    fun setTourEndingLinkClicked(captionValue: String) {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("tour_ending_link_clicked",
            Bundle().apply {
                putString("caption_value", captionValue)
            }
        )
    }

    fun setSignInCompleted() {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("sign_in_completed", null)
    }

    fun setSignUpCompleted() {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("sign_up_completed", null)
    }

    fun setPwdResetLinkSent() {
        if (!shouldLogEvents) {
            return
        }
        analytics.logEvent("password_reset_link_sent", null)
    }

    fun setWebPictoBloxEvent(jsonString: String) {
        PictobloxLogger.getInstance().logd("Web log event : $jsonString")
        if (!shouldLogEvents) {
            return
        }

        val webEvent = GsonBuilder().create().fromJson<WebPictoBloxAnalyticsEvent>(jsonString, WebPictoBloxAnalyticsEvent::class.java)

        val bundle = Bundle().apply {
            webEvent.eventParams.forEach { param -> putString(param.key, param.value) }
        }
        analytics.logEvent(webEvent.eventName, bundle)

    }


    data class WebPictoBloxAnalyticsEvent(var eventName: String, var eventParams: List<EventParam>)
    data class EventParam(var key: String, var value: String)
}

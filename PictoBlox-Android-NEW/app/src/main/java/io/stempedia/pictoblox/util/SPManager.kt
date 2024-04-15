package io.stempedia.pictoblox.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.connectivity.PictoBloxWebLocale

class SPManager(context: Context) {

    private val sharedPreferenceName = "PictobloxApp"
    private var sharedPreference: SharedPreferences =
        context.getSharedPreferences(sharedPreferenceName, Activity.MODE_PRIVATE)

    fun clearAll() = sharedPreference.edit().clear().apply()

    fun checkCourseAssetEntryExist(id: String): Boolean {
        return sharedPreference.getBoolean("CA_${id}", false)
    }

    fun setCourseAssetEntry(id: String) {
        sharedPreference.edit().putBoolean("CA_${id}", true).apply()
    }

    fun getVersionOfExampleFile(id: String): Int {
        return sharedPreference.getInt("versionOf$id", 0)
    }

    fun setVersionOfExampleFile(id: String, version: Int) {
        sharedPreference.edit().putInt("versionOf$id", version).apply()
    }

    fun storeLastLoginAccountType(type: String){
        sharedPreference.edit().putString("accountType", type).apply()
    }

    fun getLastLoginAccountType() = sharedPreference.getString("accountType","TYPE_ALL")

    fun getVersionOfAIModel(aiModel: String): Long {
        return sharedPreference.getLong("versionOf${aiModel}", 0)
    }

    fun setVersionOfAIModel(aiModel: String, version: Long) {
        sharedPreference.edit().putLong("versionOf${aiModel}", version).apply()
    }

    var firebaseUserDeviceId: String
        set(value) = sharedPreference.edit().putString("firebaseUserDeviceId", value).apply()
        get() = sharedPreference.getString("firebaseUserDeviceId", "") ?: ""


    var pictobloxWebviewPreferenceJson: String
        set(value) = sharedPreference.edit().putString("pictobloxWebviewPreferenceJson", value).apply()
        get() = sharedPreference.getString("pictobloxWebviewPreferenceJson", "{}") ?: "{}"

    var pictobloxLocale: String
        set(value) = sharedPreference.edit().putString("pictobloxLocale", value).apply().also {
            Log.e("lange","5 $pictobloxLocale")
        }
        get() = sharedPreference.getString("pictobloxLocale", PictoBloxWebLocale.ENGLISH.code) ?: ""

    var saveFileCounter: Int
        set(value) = sharedPreference.edit().putInt("saveFileCounter", value).apply()
        get() = sharedPreference.getInt("saveFileCounter", 1)

    var isFeedbackFormShownForThisVersion: Boolean
        set(value) = sharedPreference.edit().putBoolean("feedback${BuildConfig.VERSION_CODE}", value).apply()
        get() = sharedPreference.getBoolean("feedback${BuildConfig.VERSION_CODE}", false)

    var shouldShowAndroid10IncompatibilityInfo: Boolean
        set(value) = sharedPreference.edit().putBoolean("shouldShowAndroid10IncompatibilityInfo", value).apply()
        get() = sharedPreference.getBoolean("shouldShowAndroid10IncompatibilityInfo", true)

    var isSubscribedToDabbleNews: Boolean
        set(value) = sharedPreference.edit().putBoolean("isSubscribedToDabbleNews", value).apply()
        get() = sharedPreference.getBoolean("isSubscribedToDabbleNews", true)

    var isGettingStartedShown: Boolean
        set(value) = sharedPreference.edit().putBoolean("isGettingStartedShown", value).apply()
        get() = sharedPreference.getBoolean("isGettingStartedShown", true)

    var isFirstTimeInstall: Boolean
        set(value) = sharedPreference.edit().putBoolean("isFirstTimeInstall", value).apply()
        get() = sharedPreference.getBoolean("isFirstTimeInstall", true)

    var isSignUpIncomplete: Boolean
        set(value) = sharedPreference.edit().putBoolean("isSignUpIncomplete", value).apply()
        get() = sharedPreference.getBoolean("isSignUpIncomplete", false)

    var isLessonMusicMuted: Boolean
        set(value) = sharedPreference.edit().putBoolean("isLessonMusicMuted", value).apply()
        get() = sharedPreference.getBoolean("isLessonMusicMuted", false)

    var resendTimeStamp: Long
        set(value) = sharedPreference.edit().putLong("resendTimeStamp", value).apply()
        get() = sharedPreference.getLong("resendTimeStamp", 0)

    var emailToBeVerified: String
        set(value) = sharedPreference.edit().putString("guardianEmail", value).apply()
        get() = sharedPreference.getString("guardianEmail", "") ?: ""

    var hasUserSeenTour: Boolean
        set(value) = sharedPreference.edit().putBoolean("hasUserSeenTour", value).apply()
        get() = sharedPreference.getBoolean("hasUserSeenTour", false)

    var isAutoConnectToLastDevice: Boolean
        set(value) = sharedPreference.edit().putBoolean("autoConnect", value).apply()
        get() = sharedPreference.getBoolean("autoConnect", false)

    var lastConnectedDeviceAddress: String
        set(value) = sharedPreference.edit().putString("lastDeviceAddress", value).apply()
        get() = sharedPreference.getString("lastDeviceAddress", "") ?: ""

    var lastConnectedDeviceName: String
        set(value) = sharedPreference.edit().putString("lastConnectedDeviceName", value).apply()
        get() = sharedPreference.getString("lastConnectedDeviceName", "") ?: ""

    var recentlyConnectedDeviceSet: Set<String>
        set(value) = sharedPreference.edit().putStringSet("pastDeviceList", value).apply()
        get() = sharedPreference.getStringSet("pastDeviceList", mutableSetOf())!!

    var autoConnectPromptAsked: Boolean
        set(value) = sharedPreference.edit().putBoolean("autoConnectPromptAsked", value).apply()
        get() = sharedPreference.getBoolean("autoConnectPromptAsked", false)

    /*var isCachedVersionAvailable: Boolean
        set(value) = sharedPreference.edit().putBoolean("isCachedVersionAvailable", value).apply()
        get() = sharedPreference.getBoolean("isCachedVersionAvailable", false)
*/
    var selectedBoard: String
        set(value) = sharedPreference.edit().putString("selectedBoard", value).apply()
        get() = sharedPreference.getString("selectedBoard", "") ?: ""

    var shouldContinueFromWhereUserLeft: Boolean
        set(value) = sharedPreference.edit().putBoolean("shouldContinueFromWhereUserLeft", value).apply()
        get() = sharedPreference.getBoolean("shouldContinueFromWhereUserLeft", false)

    var sampleProjectCopied: Boolean
        set(value) = sharedPreference.edit().putBoolean("sampleProjectCopied", value).apply()
        get() = sharedPreference.getBoolean("sampleProjectCopied", false)

    var cachedProjectName: String
        set(value) = sharedPreference.edit().putString("cachedProjectName", value).apply()
        get() = sharedPreference.getString("cachedProjectName", "") ?: ""

    var isExternalPictoBloxEnabled: Boolean
        set(value) = sharedPreference.edit().putBoolean("isExternalPictoBloxEnabled", value).apply()
        get() = sharedPreference.getBoolean("isExternalPictoBloxEnabled", false)

    var externalPictoBloxDetail: String
        set(value) = sharedPreference.edit().putString("externalPictoBloxDetail", value).apply()
        get() = sharedPreference.getString("externalPictoBloxDetail", "") ?: ""

    var guardianVerificationId: String
        set(value) = sharedPreference.edit().putString("guardianVerificationId", value).apply()
        get() = sharedPreference.getString("guardianVerificationId", "") ?: ""

    var isEssentialFileCopied: Boolean
        set(value) = sharedPreference.edit().putBoolean("isEssentialFileCopied", value).apply()
        get() = sharedPreference.getBoolean("isEssentialFileCopied", false)

    var isMinorForLogin: Boolean
        set(value) = sharedPreference.edit().putBoolean("isMinorForLogin", value).apply()
        get() = sharedPreference.getBoolean("isMinorForLogin", false)

    var isSb3FileSyncEnabled: Boolean
        set(value) = sharedPreference.edit().putBoolean("isSb3FileSyncEnabled", value).apply()
        get() = sharedPreference.getBoolean("isSb3FileSyncEnabled", false)

    var isCacheFileExists: Boolean
        set(value) = sharedPreference.edit().putBoolean("isCacheFileExists", value).apply()
        get() = sharedPreference.getBoolean("isCacheFileExists", false)


    var isGlobalLinkInfoShown: Boolean
        set(value) = sharedPreference.edit().putBoolean("isGlobalLinkFirstInfoShown", value).apply()
        get() = sharedPreference.getBoolean("isGlobalLinkFirstInfoShown", false)

    var versionName: String
        set(value) = sharedPreference.edit().putString("versionName", value).apply()
        get() = sharedPreference.getString("versionName", "")?:""

}
package io.stempedia.pictoblox.experimental

import io.stempedia.pictoblox.connectivity.Board

class APIsByWebViewForNative {

    fun openNewProject() = "javascript:openNewProject()"

    internal fun openProject(fileName: String, base64String: String) = "javascript:openProject(\"$fileName\",\"$base64String\")"

    internal fun saveProject(name: String) = "javascript:saveProject(\"$name\")"

    internal fun responseFromHardwareForStringValue(response: String, isVM: Boolean) = "javascript:responseFromHardware(\"$response\", ${isVM})"

    internal fun responseFromHardware(response: String, isVM: Boolean) = "javascript:responseFromHardware(${response},${isVM})"

    internal fun responseForFirmware(response: String) = "javascript:responseForFirmware(\"$response\")"

    internal fun boardSelected(board: Board, replaceProject: Boolean) = "javascript:boardSelected(\"${board.stringValue}\",$replaceProject)"

    fun updateConnectionState(isConnected: Boolean) = "javascript:updateConnectionState($isConnected)"

    fun openCourse(z: String) = "javascript:onOpenLesson($z)"

    fun startTourSession() = "javascript:startTourSession()"

    fun setUserId(userId: String?) = "javascript:registerUserToken(\"$userId\")"

    fun setStart() = "javascript:onPictoBloxStart()"

    fun setStop() = "javascript:onPictoBloxStop()"

    fun onModelFilesLocated(path: String) = "javascript:onModelFilesLocated(\"$path\")"

    fun sendAiModelLoadingCanceled() = "javascript:onCancelModelLoading()"

    fun setWebviewSharedPreferenceJson(json: String) = "javascript:setPersistentDatabase($json)"

//    fun setLocale(locale: String) = "javascript:onLanguageSelected(\"$locale\")"

}
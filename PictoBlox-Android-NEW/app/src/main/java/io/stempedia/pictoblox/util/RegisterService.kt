package io.stempedia.pictoblox.util

import android.app.ActivityManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Process


class RegisterService : IntentService("Restart Worker") {

    override fun onHandleIntent(intent: Intent?) {
        //restart after 3 seconds

        //restart after 3 seconds
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        launchApp(this, "io.stempedia.pictoblox")
        stopSelf()
//        Process.killProcess(Process.myPid())
    }

    fun launchApp(context: Context, packageName: String?) {
        val manager = context.packageManager
        val i = manager.getLaunchIntentForPackage(packageName!!)
        i!!.addCategory(Intent.CATEGORY_LAUNCHER)
        context.startActivity(i)
    }

}
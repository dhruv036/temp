package io.stempedia.pictoblox.help

import android.content.Intent
import android.net.Uri
import io.stempedia.pictoblox.R


class HelpVM(val activity: HelpActivity) {
    val help1 = HelpItemVM(
        "Getting Started",
        "Learn about PictoBlox App, its interface, and how to make animations in the app.",
        R.drawable.thumb_getting_started_with_pictoblox
    )
    val help2 = HelpItemVM(
        "PictoBlox App with evive",
        "Learn how to interact with evive using the app and make a script to control a robot.",
        R.drawable.thumb_two_wheel_drive_robot
    )
    val help3 = HelpItemVM(
        "PictoBlox App with Arduino",
        "Learn how to interact with Arduino using the app and make a script to control a robot.",
        R.drawable.thumb_arduino_two_wheel_drive_pictoblox
    )

    fun help1Clicked() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/GettingStarterwithPictoBlox_App")))
    }

    fun help2Clicked() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/eviverobotwithPictoBloxApp")))
    }

    fun help3Clicked() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/ArduinoRobotwithPictoBloxApp")))
    }

}

class HelpItemVM(val title: String, val description: String, val imageResource: Int)
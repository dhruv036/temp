package io.stempedia.pictoblox.home

import android.content.Intent
import io.stempedia.pictoblox.account.AccountHelper
import io.stempedia.pictoblox.learn.CourseListActivity
import io.stempedia.pictoblox.util.PictobloxLogger

class HomeItemVM(val item:Int,val activityVm: HomeActivityVM, val activity: Home2Activity, val icn: Int?, val bg: Int, val title: String, val enabled: Boolean) {

    fun onItemClick() {
        when (item) {
            1 -> {
                if (enabled) {
                    activity.startProjectListActivity()

                } else {
                    activity.showComingSoon()
                }
            }

            5 -> {
                if (enabled) {
                    activity.goToExternalPage("https://thestempedia.com/shop")
                } else {
                activity.showComingSoon()
                }
            }
           4 -> {
                if (enabled) {
                    activity.goToExternalPage("https://codeavour.org/?utm_source=PictoBloxApp&utm_medium=Android&utm_campaign=awareness&utm_content=register&utm_term=Codeavour")

                } else {
                    activity.showComingSoon()
                }
            }

            3 -> {
                if (enabled) {
                    activity.goToExternalPage("https://thestempedia.com/project")

                } else {
                    activity.showComingSoon()
                }
            }

            2 -> {
                if (enabled) {
                    activity.startExampleActivity()

                } else {
                    activity.showComingSoon()
                }
            }
        }
    }
}
package io.stempedia.pictoblox.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import io.stempedia.pictoblox.R

class NotificationHelper(val context: Context) {
    private var builder: NotificationCompat.Builder? = null
    private var type: DabbleNotificationType? = null
    private val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createBuilder(type: DabbleNotificationType, title: String, message: String): NotificationCompat.Builder {
        return createBuilder(type, R.drawable.ic_notification, title, message)
    }

    fun createBuilder(type: DabbleNotificationType, icon: Int, title: String, message: String): NotificationCompat.Builder {
        this.type = type
        builder = NotificationCompat.Builder(context, createNotificationChannel(type)).apply {
            setContentTitle(title)
            setContentText(message)
            setSmallIcon(icon)
            setAutoCancel(true)
        }

        return builder!!
    }

    fun showNotification() {
        builder?.apply {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(type!!.id, build())
        }
    }

    fun startForeground(service: Service) {
        builder?.apply {
            service.startForeground(type!!.id, build())
        }
    }

    fun clearNotification(type: DabbleNotificationType) {
        mNotificationManager.cancel(type.id)
    }

    private fun createNotificationChannel(type: DabbleNotificationType): String {
        val ni = context.getString(type.chanelId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(ni,
                    ni, NotificationManager.IMPORTANCE_HIGH)

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.setShowBadge(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            mNotificationManager.createNotificationChannel(notificationChannel)
        }

        return ni

    }
}


enum class DabbleNotificationType(val id: Int, val chanelId: Int) {
    TYPE_SERVICE_FOREGROUND(103, R.string.function_notification_channel_id),


}
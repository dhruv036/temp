package io.stempedia.pictoblox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PictoBloxFirebaseInstanceIdService : FirebaseMessagingService() {


    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = remoteMessage.data["title"]
        val imageUrl = remoteMessage.data["image_url"]
//        val contentUrl = remoteMessage.data["content_url"]
        val timeStamp = remoteMessage.data["time_stamp"]
        val shortDescription = remoteMessage.data["short_description"] ?: ""


        val builder = NotificationCompat.Builder(
            applicationContext,
            createNotificationChannel(mNotificationManager, PictoBloxNotificationTypes.TYPE_NOTIFICATION_PROMOTION)
        ).apply {
            setContentTitle(title)
            setContentText(shortDescription)
            setSmallIcon(R.drawable.ic_notification_white)
            setAutoCancel(true)
        }

        val intent = Intent(Intent.ACTION_VIEW)


//        intent.data = Uri.parse(contentUrl)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, Intent.createChooser(intent, "Open Link With"), PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)
        mNotificationManager.notify(PictoBloxNotificationTypes.TYPE_NOTIFICATION_PROMOTION.id, builder.build())

//        Glide.with(applicationContext)
//            .asBitmap()
//            .apply(RequestOptions().circleCrop())
//            .load(imageUrl)
//            .placeholder(R.drawable.ic_account3)
//            .into(object : CustomTarget<Bitmap>() {
//                override fun onLoadCleared(placeholder: Drawable?) {
//
//                }
//
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(resource))
//                    mNotificationManager.notify(PictoBloxNotificationTypes.TYPE_NOTIFICATION_PROMOTION.id, builder.build())
//                }
//            })
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }


    private fun createNotificationChannel(mNotificationManager: NotificationManager, type: PictoBloxNotificationTypes): String {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                type.chanelId,
                type.chanelId, NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.MAGENTA
            notificationChannel.setShowBadge(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            mNotificationManager.createNotificationChannel(notificationChannel)
        }

        return type.chanelId

    }
}

enum class PictoBloxNotificationTypes(val id: Int, val chanelId: String) {
    TYPE_NOTIFICATION_SERVICE(103, "Service"),
    TYPE_NOTIFICATION_PROMOTION(104, "News and Content")

}
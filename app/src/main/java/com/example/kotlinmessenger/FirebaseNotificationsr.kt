package com.example.kotlinmessenger

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import com.example.kotlinmessenger.Constants.AppConstants
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*
import kotlin.collections.HashMap
import com.example.kotlinmessenger.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage


class FirebaseNotificationsr : FirebaseMessagingService() {

    private val TAG = "FB NOTIFICATION SERVICE"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "IL TOKEN GENERATO è $token")
        updateToken(token)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (remoteMessage.data.isNotEmpty()) {
            val map: Map<String, String> = remoteMessage.data

            val title = map["title"]
            val message = map["message"]
            val hisId = map["hisId"]
            val hisImage = map["hisImage"]
            val chatId = map["chatId"]
            val myLanguage = map["hisLanguage"]
            val hisLanguage = map["myLanguage"]

            val sharedPreferences = getSharedPreferences("preference", MODE_PRIVATE)
            val conv = sharedPreferences.getString("conv", "")
            if (conv != hisId) {

                Log.i(TAG, "Notify from $hisId title: $title message: $message chatId: $chatId")

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                    createOreonotification(
                        title!!,
                        message!!,
                        hisId!!,
                        chatId!!,
                        hisImage!!,
                        myLanguage!!,
                        hisLanguage!!
                    )
                else createnormalnotification(
                    title!!,
                    message!!,
                    hisId!!,
                    chatId!!,
                    hisImage!!,
                    myLanguage!!,
                    hisLanguage!!
                )
            }else{
                Log.i(TAG,"Notification suppressed")
            }
        }
    }

    fun updateToken(token: String) {
        val databaseReference =
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users").child(
                FirebaseAuth.getInstance().uid!!
            )
        val map: MutableMap<String, Any> = HashMap()
        map["token"] = token
        databaseReference.updateChildren(map)
    }

    fun createnormalnotification(
        title: String,
        message: String,
        hisId: String,
        chatId: String,
        hisImage: String,
        myLanguage: String,
        hisLanguage: String
    ) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, AppConstants.CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setColor(ResourcesCompat.getColor(resources, R.color.colorPrimary, null))
            .setSound(uri)
        val intent = Intent(this, MessageActivity::class.java)
        intent.putExtra("hisId", hisId)
        intent.putExtra("chatId", chatId)
        intent.putExtra("hisImage", hisImage)
        intent.putExtra("myLanguage", myLanguage)
        intent.putExtra("hisLanguage", hisLanguage)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        builder.setContentIntent(pendingIntent)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Random().nextInt(85 - 65), builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createOreonotification(
        title: String,
        message: String,
        hisId: String,
        chatId: String,
        hisImage: String,
        myLanguage: String,
        hisLanguage: String
    ) {
        val channel = NotificationChannel(
            AppConstants.CHANNEL_ID,
            "Message",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setShowBadge(true)
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MessageActivity::class.java)
        intent.putExtra("hisId", hisId)
        intent.putExtra("chatId", chatId)
        intent.putExtra("hisImage", hisImage)
        intent.putExtra("hisLanguage", hisLanguage)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val notification = Notification.Builder(this, AppConstants.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setColor(ResourcesCompat.getColor(resources, R.color.colorPrimary, null))
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(100, notification)
    }

}
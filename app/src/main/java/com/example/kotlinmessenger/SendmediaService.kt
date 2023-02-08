package com.example.kotlinmessenger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.util.Util
import com.example.kotlinmessenger.Constants.AppConstants
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import id.zelory.compressor.Compressor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class SendmediaService : Service() {

    private lateinit var builder: NotificationCompat.Builder
    private var MAX_PROGRESS = 0
    private lateinit var manager: NotificationManager
    private var chatID: String? = null
    private var hisID: String? = null
    private var myID: String? = null
    private var hisImage: String? = null
    private var myName: String? = null
    private var images: ArrayList<String>? = null
    private val TAG = "SEND MEDIA SERVICE"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            hisID = intent.getStringExtra("hisID")
            chatID = intent.getStringExtra("chatID")
            images = intent.getStringArrayListExtra("media")
            //myID = intent.getStringExtra("myID")
            myName = intent.getStringExtra("myName")
            hisImage = intent.getStringExtra("hisImage")
        }

        MAX_PROGRESS = images!!.size

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            createChannel()

        startForeground(100, getNotification().build())

        for (a in images!!.indices) {
            GlobalScope.launch {
                compressFinal(images!![a].toUri())
                builder.setProgress(MAX_PROGRESS, a + 1, false)
                manager.notify(600, builder.build())
            }
        }

        if(chatID==null)

        builder.setContentTitle("Sending Complete")
            .setProgress(MAX_PROGRESS, MAX_PROGRESS, false)
        manager.notify(600, builder.build())
        stopSelf()

        if(MAX_PROGRESS > 1) gettokenForNotification("sent some photos")
        else gettokenForNotification("sent one photo")

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotification(): NotificationCompat.Builder {
        builder = NotificationCompat.Builder(this, "android")
            .setContentText("Sending Media")
            .setProgress(MAX_PROGRESS, 0, false)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)

        manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(600, builder.build())

        return builder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel("android", "Message", NotificationManager.IMPORTANCE_HIGH)
        channel.setShowBadge(true)
        channel.lightColor = R.color.colorPrimary
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        channel.description = "Sending Media"

        manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private suspend fun compressFinal(intentData: Uri) {
        Log.d(TAG, "Tryin to compress the image")
        val actualImage: File? = FileUtil.from(this, intentData)
        var compressedImage: File? = null
        actualImage?.let { imageFile ->
            GlobalScope.launch {
                // Default compression
                compressedImage = Compressor.compress(this@SendmediaService, imageFile)
                Log.d(TAG, "compressed image: $compressedImage")
                uploadImage(compressedImage!!)
            }
        } ?: Log.d(TAG, "Please choose an image!")
    }

    private fun uploadImage(fileName: File) {
        Log.d(TAG, "uploading image")
        val pathDB = "/$chatID/"
        val ref = FirebaseStorage.getInstance().reference.child(pathDB + System.currentTimeMillis())
        Log.d(TAG, "ref: ${Uri.fromFile(fileName)}")
        ref.putFile(Uri.fromFile(fileName)).addOnFailureListener { Log.d(TAG, "error: $it") }
            .addOnSuccessListener { taskSnapshot ->
                val task = taskSnapshot.storage.downloadUrl
                task.addOnCompleteListener { uri: Task<Uri> ->
                    if (uri.isSuccessful) {
                        val path = uri.result.toString()
                        val messageModel = MessageModel(
                            FirebaseAuth.getInstance().uid!!,
                            hisID!!,
                            path,
                            System.currentTimeMillis().toString(),
                            "image"
                        )
                        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("chat").child(chatID!!).push().setValue(messageModel)
                    }
                }
            }
    }

    private fun gettokenForNotification(message: String) {
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app").getReference("users")
            .child(hisID!!).child("token").get()
            .addOnSuccessListener {
                val myID = FirebaseAuth.getInstance().uid
                val token = it.value.toString()
                val to = JSONObject()
                val data = JSONObject()
                data.put("hisId", myID)
                data.put("title", myName)
                data.put("message", message)
                data.put("chatId", chatID)
                data.put("hisImage",hisImage)
                to.put("to", token)
                to.put("data", data)

                sendNotification(to)
            }
    }

    private fun sendNotification(to: JSONObject) {
        Log.d(TAG, "token notifica: $to")
        val request: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            AppConstants.NOTIFICATION_URL,
            to,
            Response.Listener { response: JSONObject ->
                Log.d(TAG, "onResponse: $response")
            },
            Response.ErrorListener {
                Log.d(TAG, "onError: $it")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val map: MutableMap<String, String> = HashMap()

                map["Authorization"] = "key=" + AppConstants.SERVER_KEY
                map["Content-type"] = "application/json"
                return map
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        val requestQueue = Volley.newRequestQueue(this)
        request.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }


}

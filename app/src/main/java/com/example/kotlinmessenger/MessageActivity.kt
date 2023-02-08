package com.example.kotlinmessenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.kotlinmessenger.Constants.AppConstants
import com.example.kotlinmessenger.adapter.MessageAdapter
import com.example.kotlinmessenger.databinding.ActivityMessageBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.*
import com.google.mlkit.nl.translate.TranslateLanguage
import org.json.JSONObject

class MessageActivity : AppCompatActivity() {

    private lateinit var activityMessageBinding: ActivityMessageBinding
    private var hisId: String? = null
    private var hisUsername: String? = null
    private var hisImageUrl: String? = null
    private var myImageUrl: String? = null
    private var chatId: String? = null
    private lateinit var myId: String
    private lateinit var appUtil: AppUtil
    private var firebaseRecyclerAdapter: FirebaseRecyclerAdapter<MessageModel, ViewHolder>? = null
    private var myName: String? = null
    private val TAG = "MESSAGE ACTIVITY"
    private var messageList = ArrayList<MessageModel>()
    private var messageAdapter: MessageAdapter? = null
    private var isTyping = false
    private var t: Long = 0
    private var deadLine: Long = 500
    private var debugTime: Long = 0
    private var isWriting = false
    private var isOnline = false
    private var READ_EXTERNAL_STORAGE_REQUEST_CODE = 1001
    private var imageUris = ArrayList<String>()
    private var isWritingMessage: MessageModel? = null

    //Language parameter
    private var translator: LanguageManager? = null
    private var hisLanguage : String = ""
    private var myLanguage : String = ""

    var sharedPreferences : SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        activityMessageBinding = ActivityMessageBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(activityMessageBinding.root)

        sharedPreferences = getSharedPreferences("preference", Context.MODE_PRIVATE)
        myLanguage = sharedPreferences!!.getString("myLanguage","")!!

        appUtil = AppUtil(this)
        myId = appUtil.getUID()!!
        activityMessageBinding.activity = this

        hisId = intent.getStringExtra("hisId")
        hisImageUrl = intent.getStringExtra("hisImage")
        hisUsername = intent.getStringExtra("hisUsername")
        hisLanguage = intent.getStringExtra("hisLanguage")!!
        chatId = computeChatId()

        isWritingMessage = MessageModel(hisId!!, myId, "...", System.currentTimeMillis().toString(), "IS_WRITING")

        checkHisImage()
        setConv()

        translator = LanguageManager(myLanguage,hisLanguage)
        activityMessageBinding.messageToolbar.username = hisUsername

        activityMessageBinding.btnSend.setOnClickListener {
            val message: String = activityMessageBinding.msgText.text.toString()
            if (message.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_message), Toast.LENGTH_SHORT).show()
            } else {
                setNoWriting()
                sendMessage(message)
                gettokenForNotification(message)
            }
        }

        activityMessageBinding.btnDataSend.setOnClickListener {
            pickImageIntent()
        }

        activityMessageBinding.messageToolbar.msgBack.setOnClickListener {
            startActivity(Intent(this, DashBoard::class.java))
            finish()
        }

        activityMessageBinding.msgText.doOnTextChanged { text, start, before, count ->
            val TIME_FACTOR: Long = 350
            if (!isTyping) {
                Log.d(TAG, "Is typing")
                t = System.currentTimeMillis()
                debugTime = t
                isTyping = true
                setWriting()
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        // This method will be executed once the timer is over
                        if (System.currentTimeMillis() > t) {
                            Log.d(TAG, "No more typing (2)")
                            setNoWriting()
                            isTyping = false
                        } else {
                            Log.d(TAG, "difference from stop: ${t - System.currentTimeMillis()}")
                        }
                    },
                    TIME_FACTOR // value in milliseconds
                )
            } else {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        // This method will be executed once the timer is over
                        if (System.currentTimeMillis() > t) {
                            if (isTyping) {
                                Log.d(
                                    TAG,
                                    "No more typing (1) difference ${t - debugTime - deadLine}"
                                )
                                setNoWriting()
                                isTyping = false
                                deadLine = 500
                            }
                        } else {
                            //Log.d(TAG,"difference: ${t-System.currentTimeMillis()}")
                        }
                    },
                    deadLine // value in milliseconds
                )
            }
            t += TIME_FACTOR
            deadLine += (TIME_FACTOR)
            Log.d(TAG, "difference: ${t - System.currentTimeMillis()}")
        }

        activityMessageBinding.msgText.addOnLayoutChangeListener(object :
            View.OnLayoutChangeListener {
            override fun onLayoutChange(
                p0: View?,
                p1: Int,
                p2: Int,
                p3: Int,
                p4: Int,
                p5: Int,
                p6: Int,
                p7: Int,
                p8: Int
            ) {
                activityMessageBinding.messageRecyclerView.adapter?.itemCount?.let {
                    activityMessageBinding.messageRecyclerView.smoothScrollToPosition(
                        it
                    )
                }
            }
        })

        Log.d(TAG, "hisID: $hisId hisImage: $hisImageUrl hisUsername: $hisUsername")

        translator!!.onReady {
            Log.d(TAG,"Translator (onReady call) -> ${translator!!.modelAvailable}")
            sperimentalReadMessages(chatId!!)
        }

        checkOnlineStatusAndUsername()
        getMynameAnImage()

        Log.e(TAG, "Chat id computed: ${computeChatId()}")
    }

    private fun getMynameAnImage() {
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users").child(myId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        myName = snapshot.getValue().toString()
                        Log.d(TAG, "nome caricato: $myName")
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users").child(myId).child("image")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        myImageUrl = snapshot.getValue().toString()
                        Log.d(TAG, "Mia immagine caricata: $myImageUrl")
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun checkHisImage() {
        if (hisImageUrl == null) {
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/users").child(hisId!!).child("image").get()
                .addOnSuccessListener {
                    hisImageUrl = it.getValue().toString()
                    activityMessageBinding.hisImage = hisImageUrl
                }
        } else activityMessageBinding.hisImage = hisImageUrl
    }

    private fun setConv(){
        sharedPreferences!!.edit().putString("conv",hisId)
            .apply()
    }

    private fun clearConv(){
        sharedPreferences!!.edit().putString("conv","")
            .apply()
    }

    private fun checkHisLanguage(callback: (String) -> Unit){
        if(hisLanguage == ""){
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/users").child(hisId!!).child("language").get()
                .addOnSuccessListener {
                    hisLanguage = it.getValue().toString()
                    callback(hisLanguage)
                }
                .addOnFailureListener {
                    Log.e(TAG,"His language error query -> $it")
                }
        }
    }

    private fun checkMyLanguage(callback: () -> Unit){
        if(myLanguage == ""){
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/users").child(myId).child("language").get()
                .addOnSuccessListener {
                    myLanguage = it.getValue().toString()
                    callback()
                }
                .addOnFailureListener {
                    Log.e(TAG,"My language error query -> $it")
                }
        }
    }

    private fun CreateChat(message: String) {
        Log.d(TAG, "Chat created: $message")
        var databaseReference =
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/chatlist").child(myId)
        //chatId = databaseReference.push().key [OLD]
        val chatListMod =
            ChatListModel(chatId!!, message, System.currentTimeMillis().toString(), hisId!!)
        databaseReference.child(chatId!!).setValue(chatListMod)
        databaseReference =
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/chatlist").child(hisId!!)
        val chatList =
            ChatListModel(chatId!!, message, System.currentTimeMillis().toString(), myId!!)
        databaseReference.child(chatId!!).setValue(chatList)
        //sperimentalReadMessages(chatId!!)
        sendMessage(message)
    }

    private fun sendMessage(message: String) {
        activityMessageBinding.msgText.text = null
        Log.d(TAG, "Send : $message")
        if (chatId == null) {
            Log.d(TAG, "ID CHAT NULL")
            CreateChat(message)
        } else {
            Log.d(TAG, "SEND OK")
            val databaseChatReference =
                FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("/chat").child(chatId!!)
            val messageModel = MessageModel(
                myId,
                hisId!!,
                message,
                type = "text",
                date = System.currentTimeMillis().toString()
            )
            translator!!.translate(message){
                messageModel.translatedMessage = it!!
                databaseChatReference.push().setValue(messageModel)
            }
            //30chars
            val Map: MutableMap<String, Any> = HashMap()
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("chatlist").child(hisId!!).child(chatId!!).child("chatid").get()
                .addOnSuccessListener {
                    if (it.exists()) {
                        if(message.length > 29) Map["lastMessage"] = message.substring(0,25)+"..."
                        else Map["lastMessage"] = message
                        Map["date"] = System.currentTimeMillis()
                        var databaseReference =
                            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("/chatlist").child(myId).child(chatId!!)
                        databaseReference.updateChildren(Map)
                        databaseReference =
                            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("/chatlist").child(hisId!!).child(chatId!!)
                        databaseReference.updateChildren(Map)
                    } else {
                        if(message.length > 29) Map["lastMessage"] = message.substring(0,25)+"..."
                        else Map["lastMessage"] = message
                        Map["date"] = System.currentTimeMillis()
                        Map["chatId"] = chatId!!
                        Map["member"] = hisId!!
                        var databaseReference =
                            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("/chatlist").child(myId).child(chatId!!)
                        databaseReference.updateChildren(Map)
                        Map["member"] = myId
                        databaseReference =
                            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("/chatlist").child(hisId!!).child(chatId!!)
                        databaseReference.updateChildren(Map)
                    }
                }
                .addOnFailureListener {
                    if(message.length > 29) Map["lastMessage"] = message.substring(0,25)+"..."
                    else Map["lastMessage"] = message
                    Map["date"] = System.currentTimeMillis()
                    var databaseReference =
                        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("/chatlist").child(myId).child(chatId!!)
                    databaseReference.updateChildren(Map)
                    databaseReference =
                        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("/chatlist").child(hisId!!).child(chatId!!)
                    databaseReference.updateChildren(Map)
                }
        } //end else
    }

    private fun sperimentalReadMessages(chatId: String) {
        //Prima fase caricamento chats
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("chat").child(chatId).get()
            .addOnSuccessListener {
                it.children.forEach {
                    val senderId = it.child("senderId").value.toString()
                    val reciverId = it.child("reciverId").value.toString()
                    val message = it.child("message").value.toString()
                    val date = it.child("date").value.toString()
                    val type = it.child("type").value.toString()
                    val tranlastedMessage = it.child("translatedMessage").value.toString()
                    var model = MessageModel(senderId, reciverId, message, date, type)
                    model.translatedMessage = tranlastedMessage
                    messageList.add(model)
                }
                activityMessageBinding.messageRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    setHasFixedSize(true)
                    Log.d(TAG,"Translator: ${translator!!.modelAvailable}")
                    messageAdapter = MessageAdapter(messageList,translator!!,context)
                    adapter = messageAdapter
                    activityMessageBinding.messageRecyclerView.smoothScrollToPosition(messageList.size)
                }
                syncNewMessages(chatId)
                writingListener()
            }
            .addOnFailureListener {
                Log.d(TAG, "Error loading existing messages -> $it")
            }
    }

    private fun syncNewMessages(chatId: String) {
        val query =
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("chat").child(chatId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                removeIsWritingBox()
                if (snapshot.childrenCount > messageList.size) {
                    val lastMessageChildren = snapshot.children.last()
                    val senderId = lastMessageChildren.child("senderId").value.toString()
                    val reciverId = lastMessageChildren.child("reciverId").value.toString()
                    val message = lastMessageChildren.child("message").value.toString()
                    val date = lastMessageChildren.child("date").value.toString()
                    val type = lastMessageChildren.child("type").value.toString()
                    val tranlastedMessage = lastMessageChildren.child("translatedMessage").value.toString()
                    var model = MessageModel(senderId, reciverId, message, date, type)
                    model.translatedMessage = tranlastedMessage
                    messageList.add(model)
                    activityMessageBinding.messageRecyclerView.adapter!!.notifyDataSetChanged()
                    activityMessageBinding.messageRecyclerView.smoothScrollToPosition(messageList.size)
                } else {
                    Log.d(TAG,"No new message found children: ${snapshot.childrenCount} array: ${messageList.size}")
                    activityMessageBinding.messageRecyclerView.adapter!!.notifyDataSetChanged()
                    activityMessageBinding.messageRecyclerView.smoothScrollToPosition(messageList.size)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    class ViewHolder(var viewDataBinding: ViewDataBinding) :
        RecyclerView.ViewHolder(viewDataBinding.root)

    override fun onPause() {
        if (firebaseRecyclerAdapter != null) {
            firebaseRecyclerAdapter!!.stopListening()
        }
        clearConv()
        appUtil.updateOnlineStatus("offline")
        super.onPause()
    }

    override fun onResume() {
        appUtil.updateOnlineStatus("online")
        setConv()
        super.onResume()
    }

    private fun checkOnlineStatusAndUsername() {
        val databaseReference =
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users").child(hisId!!)
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (!isOnline && snapshot.child("online").value == "online") {
                        val userModel = snapshot.getValue(UserModel::class.java)
                        activityMessageBinding.online = userModel!!.online
                        Log.d(TAG, "stato interlocutore: ${activityMessageBinding.online}")
                        isOnline = true
                    }
                    if (isOnline && snapshot.child("online").value == "offline") {
                        val userModel = snapshot.getValue(UserModel::class.java)
                        activityMessageBinding.online = userModel!!.online
                        Log.d(TAG, "stato interlocutore: ${activityMessageBinding.online}")
                        isOnline = false
                    }
                    if (snapshot.child("name").value != hisUsername) {
                        hisUsername = snapshot.child("name").value.toString()
                        activityMessageBinding.messageToolbar.username = hisUsername
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun writingListener() {
        val databaseReference =
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users").child(hisId!!)
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Him typing: ${snapshot.child("typing").value}")
                if (snapshot.child("typing").value == "true" && !isWriting) {
                    messageList.add(isWritingMessage!!)
                    isWriting = true
                    activityMessageBinding.messageRecyclerView.adapter!!.notifyDataSetChanged()
                    activityMessageBinding.messageRecyclerView.smoothScrollToPosition(messageList.size)
                }
                if (snapshot.child("typing").value == "false" && isWriting) {
                    messageList.remove(isWritingMessage)
                    isWriting = false
                    activityMessageBinding.messageRecyclerView.adapter!!.notifyDataSetChanged()
                    activityMessageBinding.messageRecyclerView.smoothScrollToPosition(messageList.size)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun removeIsWritingBox() {
        if (isWriting) {
            messageList.remove(isWritingMessage)
            isWriting = false
            activityMessageBinding.messageRecyclerView.adapter!!.notifyDataSetChanged()
            activityMessageBinding.messageRecyclerView.smoothScrollToPosition(messageList.size)
        }
    }

    private fun setWriting() {
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users").child(myId).child("typing").setValue("true")
    }

    private fun setNoWriting() {
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users").child(myId).child("typing").setValue("false")
    }

    private fun gettokenForNotification(message: String) {
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users")
            .child(hisId!!).child("token").get()
            .addOnSuccessListener {
                val token = it.value.toString()
                val to = JSONObject()
                val data = JSONObject()
                data.put("hisId", myId)
                data.put("title", myName)
                data.put("hisImage", myImageUrl)
                data.put("message", message)
                data.put("chatId", chatId)
                data.put("myLanguage",myLanguage)
                data.put("hisLanguage",hisLanguage)
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

    private val getAction =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                Log.d(TAG, "Photo selected: ${result.data}")
                if (result.data!!.clipData != null) {
                    val count = result.data!!.clipData!!.itemCount - 1
                    Log.d(
                        TAG,
                        "Selezionata più di un immagine: ${result.data!!.clipData!!.itemCount} immagini -> ${result.data!!.clipData} "
                    )
                    for (i in 0..count) {
                        imageUris.add(result.data!!.clipData!!.getItemAt(i).uri.toString())
                    }
                } else {
                    Log.d(TAG, "Selezionata una sola immagine: ${result.data!!.data}")
                    imageUris.add(result.data!!.data!!.toString())
                }

                val intent = Intent(this, SendmediaService::class.java)
                intent.putExtra("hisID", hisId)
                intent.putExtra("chatID", chatId)
                intent.putExtra("myName", myName)
                intent.putExtra("hisImage", myImageUrl)
                intent.putStringArrayListExtra("media", imageUris)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                    startForegroundService(intent)
                else
                    startService(intent)

                imageUris.clear()
            }
        }

    private fun pickImageIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.setAction(Intent.ACTION_GET_CONTENT)
        getAction.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            READ_EXTERNAL_STORAGE_REQUEST_CODE -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageIntent()
                } else {
                    Toast.makeText(
                        this@MessageActivity,
                        getString(R.string.text_toast0),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }

    private fun computeChatId(): String {
        var newId = ""
        for (i in myId.indices) {
            if (hisId!![i].code >= myId[i].code) newId += hisId!![i]
            else newId += myId!![i]
        }
        return newId
    }

    override fun onDestroy() {
        setNoWriting()
        appUtil.updateOnlineStatus("offline")
        clearConv()
        super.onDestroy()
    }
}









package com.example.kotlinmessenger

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.example.kotlinmessenger.Constants.AppConstants
import com.google.android.gms.auth.GoogleAuthUtil.getToken
import com.google.android.gms.auth.zzd.getToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

class SplashScreen : AppCompatActivity() {

    private var firebaseAuth : FirebaseAuth? = null
    private lateinit var appUtil: AppUtil
    private val TAG = "SLASHSCREEN ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        firebaseAuth = FirebaseAuth.getInstance()
        appUtil = AppUtil(this)
        Log.d(TAG, "${firebaseAuth!!.currentUser}")

        Handler().postDelayed({
            isAlreadyLogged()
        }, 1000)

    }


    private fun isAlreadyLogged(){
        val currentUSer = FirebaseAuth.getInstance().currentUser
        if(currentUSer != null){
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users").child(currentUSer.uid).child("token").get()
                .addOnFailureListener {

                }
                .addOnSuccessListener { savedToken ->
                    FirebaseMessaging.getInstance().token.addOnSuccessListener{
                        if (!savedToken.exists()) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }else if((savedToken.value != it)) {
                            val builder = AlertDialog.Builder(this)
                            builder.setMessage("Already logged on a different device! Do you wanna switch on this device?")
                            builder.setTitle("Warning")
                            builder.setCancelable(false)
                            builder.setPositiveButton("Yes",
                                DialogInterface.OnClickListener { dialogInterface, i ->
                                        updateToken(it,currentUSer.uid)
                                        startActivity(Intent(this, DashBoard::class.java))
                                        finish()
                                     })
                            builder.setNegativeButton("No",
                                DialogInterface.OnClickListener { dialogInterface, i ->
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish() })
                            builder.create().show()
                        }else{
                            startActivity(Intent(this, DashBoard::class.java))
                            finish()
                        }
                    }
                }
        }else{
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun updateToken(token:String,uid: String) {
        val databaseReference = FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app").getReference("users").child(uid)
        val map:MutableMap<String, Any> = HashMap()
        map["token"]=token
        databaseReference.updateChildren(map)
    }
}

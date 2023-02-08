package com.example.kotlinmessenger.Fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kotlinmessenger.Constants.AppConstants
import com.example.kotlinmessenger.DashBoard
import com.example.kotlinmessenger.R
import com.example.kotlinmessenger.databinding.FLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage


class f_login : Fragment(R.layout.f_login) {

    lateinit var binding: FLoginBinding
    private val TAG = "LOGIN FRAGMENT"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FLoginBinding.inflate(inflater, container, false)
        binding.loginButton.setOnClickListener {
            isAlreadyLogged()
        }

        binding.signUpButton.setOnClickListener {
            val fragment = f_register()
            val transaction = fragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainer, fragment)?.commit()
        }

        return binding.root

    }

    private fun isAlreadyLogged() {
        val email = binding.mailET.text.toString()
        val password = binding.passwordET.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity, getString(R.string.login_error), Toast.LENGTH_LONG)
                .show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    Toast.makeText(activity, getString(R.string.no_user), Toast.LENGTH_LONG)
                        .show()
                } else {
                    val currentUSer = FirebaseAuth.getInstance().currentUser
                    if (currentUSer != null) {
                        val uid = FirebaseAuth.getInstance().uid
                        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("users").child(uid!!).child("token").get()
                            .addOnSuccessListener { savedToken ->
                                Log.d(TAG, "Saved token: ${savedToken.value}")
                                FirebaseMessaging.getInstance().token.addOnSuccessListener {
                                    if (!savedToken.exists()) {
                                        Log.d(TAG, "Saved token is null")
                                        performLogin()
                                    } else if ((savedToken.value != it)) {
                                        Log.d(TAG, "Saved token id different!")
                                        val builder = AlertDialog.Builder(activity)
                                        builder.setMessage("Already logged on a different device! Are you sure to make login here?")
                                        builder.setTitle("Warning")
                                        builder.setCancelable(false)
                                        builder.setPositiveButton(
                                            "Yes",
                                            DialogInterface.OnClickListener { dialogInterface, i -> performLogin() })
                                        builder.setNegativeButton(
                                            "No",
                                            DialogInterface.OnClickListener { dialogInterface, i ->
                                                binding.mailET.setText("")
                                                binding.passwordET.setText("")
                                            })
                                        builder.create().show()
                                    } else {
                                        startActivity(Intent(activity, DashBoard::class.java))
                                    }
                                }
                            }
                    }
                }
            }
    }

    private fun performLogin() {
        val email = binding.mailET.text.toString()
        val password = binding.passwordET.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity, getString(R.string.login_error), Toast.LENGTH_LONG)
                .show()
            return
        }
        Log.d(TAG, "Login with email $email and password $password")
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { result ->
                    if (result != null) {
                        val uid = FirebaseAuth.getInstance().uid
                        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("users").child(uid!!).child("token")
                            .setValue(result)
                            .addOnSuccessListener {
                                Log.d(TAG, "Token successfully assigned -> $result")
                                startActivity(Intent(activity, DashBoard::class.java))
                            }
                            .addOnFailureListener {
                                Log.d(TAG, "Error token not assigned -> $it")
                            }
                    }
                }
                bindUrlToUSer(FirebaseAuth.getInstance().uid!!)
                startActivity(Intent(activity, DashBoard::class.java))
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Failed to Login: ${it.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun bindUrlToUSer(uid: String) {
        FirebaseStorage.getInstance().reference.child(AppConstants.PATH + uid).downloadUrl
            .addOnSuccessListener {
                FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("/users").child(uid).child("image").setValue(it.toString())
                    .addOnSuccessListener {
                        Log.d(TAG, "Url image correctly bind to userDB")
                    }
                    .addOnFailureListener {
                        Log.d(TAG, "Error to bind url image -> $it")
                    }
            }
            .addOnFailureListener {
                Log.d(TAG, "Error to bind url image -> $it")
            }
    }
}
package com.example.kotlinmessenger.Fragment

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.kotlinmessenger.*
import com.example.kotlinmessenger.Constants.AppConstants
import com.example.kotlinmessenger.databinding.FRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*


class f_register : Fragment(R.layout.f_register) {

    lateinit var binding: FRegisterBinding
    private lateinit var appUtil: AppUtil
    private val TAG = "REGISTRATION FRAGMENT"
    private var lan = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FRegisterBinding.inflate(inflater, container, false)
        val language = languageCompanion()

        appUtil = AppUtil(requireActivity())
        binding.loginreturnButton.setOnClickListener {
            val fragment = f_login()
            val transaction = fragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainer, fragment)?.commit()
        }
        binding.confirmButton.setOnClickListener {
            performRegistration()
        }

        val ad: ArrayAdapter<*> = ArrayAdapter<String?>(
            requireActivity(),
            R.layout.spinner_item,
            language.supported_languages
        )

        ad.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = ad

        val systemLanguage = Locale.getDefault().getLanguage()
        binding.languageSpinner.setSelection(
            language.getPosition(systemLanguage)
        )
        lan = language.getBCP47CodeFromIntCode(language.getPosition(systemLanguage))
        binding.languageSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    lan = language.getBCP47CodeFromIntCode(p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        return binding.root
    }

    private fun performRegistration() {
        val email = binding.mailET.text.toString()
        val password = binding.passwordET.text.toString()
        val confirm_password = binding.confirmPasswordET.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity, getString(R.string.text_toast1), Toast.LENGTH_LONG).show()
            return
        }
        Log.d("Main Activity", "$password :check $confirm_password")
        if (password != confirm_password) {
            Toast.makeText(activity, getString(R.string.text_toast2), Toast.LENGTH_LONG).show()
            return
        }

        //info visibili su logcat
        Log.d("Main Activity", "Email is $email")
        Log.d("Main Activity", "Password is $password")

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    return@addOnCompleteListener
                }
                Log.d("Main Activity", "Utente creato con successo ${it.result?.user?.uid}")
                saveUserToFirebaseDatabase()
            }
            .addOnFailureListener {
                //info visibili su logcat
            }
    }//fine performRegistration

    private fun saveUserToFirebaseDatabase() {
        Log.d("Register Activity", "Chiamata salvataggio dati utente")
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val userRef =
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/users")
        Log.d("Register Activity", "Reference : $userRef")
        //val user = User(uid, binding.username.text.toString(), "Hello")
        val user = UserModel(binding.username.text.toString(), "Hello", "", uid, "offline", "false")
        user.language = lan
        userRef.child(uid).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(activity, "Successfully registration", Toast.LENGTH_LONG).show()
                setDefaultAvatar(uid)
                //Token Handler
                FirebaseMessaging.getInstance().token.addOnSuccessListener { result ->
                    if (result != null) {
                        val token = result
                        val databaseReference =
                            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                                .getReference("users").child(appUtil.getUID()!!).child("token")
                                .setValue(token)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Token successfully assigned -> $token")
                                    startActivity(Intent(activity, DashBoard::class.java))
                                }
                                .addOnFailureListener {
                                    Log.d(TAG, "Error token not assigned -> $it")
                                }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(activity, getString(R.string.text_toast3), Toast.LENGTH_LONG).show()
                Log.d(TAG, "Utente NON salvato nel db")
            }
    }

    private fun setDefaultAvatar(uid: String) {
        val storageReference = FirebaseStorage.getInstance().reference
        val storageRef =
            FirebaseStorage.getInstance().reference.child(AppConstants.PATH + "default-avatar")
        val localFile = File.createTempFile("tempImage", "jpeg")
        storageRef.getFile(localFile)
            .addOnSuccessListener {
                val url = localFile.toUri()
                storageReference!!.child(AppConstants.PATH + uid).putFile(url!!)
                    .addOnSuccessListener {
                        bindUrlToUSer(uid)
                    }
            }
            .addOnFailureListener {

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
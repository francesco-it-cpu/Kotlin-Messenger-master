package com.example.kotlinmessenger.Fragment

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kotlinmessenger.ProfileActivity
import com.example.kotlinmessenger.R
import com.example.kotlinmessenger.databinding.FSettingsBinding
import com.example.kotlinmessenger.languageCompanion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

class f_settings : Fragment(R.layout.f_settings) {

    lateinit var binding: FSettingsBinding
    val language = languageCompanion()
    var userSnapshot: DataSnapshot? = null
    private var TAG="f_settings"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FSettingsBinding.inflate(inflater, container, false)

        val myId = FirebaseAuth.getInstance().uid
        val sharedPreferences = activity!!.getSharedPreferences("preference", Context.MODE_PRIVATE)
        val autoTranslation = sharedPreferences.getBoolean("autoTranslation", false)
        val edit = sharedPreferences.edit()
        getActivity()?.getWindow()
            ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        val ad: ArrayAdapter<*> = ArrayAdapter<String?>(
            requireActivity(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            language.supported_languages
        )

        ad.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = ad

        if (autoTranslation) binding.autoTranslationCheck.isChecked = true

        val lan = sharedPreferences.getString(
            "myLanguage",
            ""
        )!!
        Toast.makeText(activity,"Language sp: $lan ",Toast.LENGTH_SHORT).show()
        if(lan == ""){
            FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("/users").child(myId!!).child("language").get()
                .addOnSuccessListener {
                    binding.languageSpinner.setSelection(
                        language.getPosition(it.value.toString()
                        )
                    )
                }
                .addOnFailureListener {
                    Log.e(TAG,"My language error query -> $it")
                }
        }else{
            binding.languageSpinner.setSelection(
                language.getPosition(
                    lan
                )
            )
        }



        binding.languageSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    edit.putString("myLanguage", language.getBCP47CodeFromIntCode(p2))
                    edit.apply()
                    FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("/users").child(myId!!).child("language")
                        .setValue(language.getBCP47CodeFromIntCode(p2))
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        binding.autoTranslationCheck.setOnCheckedChangeListener { compoundButton, b ->
            edit.putBoolean("autoTranslation", b)
            edit.apply()
        }

        binding.userBox.setOnClickListener {
            val intent = Intent(activity,ProfileActivity::class.java)
            startActivity(intent)
        }

        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("/users").child(myId!!).get()
            .addOnSuccessListener {
                binding.userImage = it.child("image").value.toString()
                binding.statusTV.text = it.child("status").value.toString()
                binding.usernameTV.text = it.child("name").value.toString()
                userSnapshot = it
            }

        return binding.root
    }
}

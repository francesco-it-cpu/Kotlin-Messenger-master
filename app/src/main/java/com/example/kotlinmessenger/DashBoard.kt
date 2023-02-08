package com.example.kotlinmessenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.kotlinmessenger.Fragment.f_contacts
import com.example.kotlinmessenger.Fragment.f_conversations
import com.example.kotlinmessenger.Fragment.f_settings
import com.example.kotlinmessenger.databinding.ActivityDashBoardBinding

class DashBoard : AppCompatActivity() {

    lateinit var binding: ActivityDashBoardBinding
    private var appUtil=AppUtil(this)
    private var TAG="DashboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val conversationsF = f_conversations()
        val contactsF = f_contacts()
        val settings = f_settings()

        

        val intent = Intent(this, ProfileActivity::class.java)
        if ( savedInstanceState==null) {
            replaceFragment(conversationsF)
            Log.e(TAG, "instance è null")
        }
        Log.e(TAG, "fragment_code: ${savedInstanceState?.getInt("Fragment_Code")}")

        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId){
                R.id.chat -> replaceFragment(conversationsF)
                R.id.contacts -> replaceFragment(contactsF)
                R.id.settings -> replaceFragment(settings)
                //R.id.settings -> startActivity(intent)
            }
            return@setOnItemSelectedListener true
        }

    }




    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayoutWrapper, fragment).commit()
    }


    override fun onPause() {
        super.onPause()
        appUtil.updateOnlineStatus("offline")
    }

    override fun onResume() {
        super.onResume()
        appUtil.updateOnlineStatus("online")
    }



}
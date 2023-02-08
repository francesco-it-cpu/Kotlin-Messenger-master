package com.example.kotlinmessenger.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinmessenger.MessageActivity
import com.example.kotlinmessenger.Constants.AppConstants
import com.example.kotlinmessenger.UserModel
import com.example.kotlinmessenger.databinding.ContactItemLayoutBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlin.collections.ArrayList

class ContactAdapter(private var appContacts: ArrayList<UserModel>) :
    RecyclerView.Adapter<ContactAdapter.ViewHolder>(), Filterable {

    private var allContact: ArrayList<UserModel> = appContacts
    private val TAG = "CONTACT ADAPTER"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val contactItemLayoutBinding =
            ContactItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(contactItemLayoutBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userModel = allContact[position]
        if (userModel.image.isEmpty()) {
            FirebaseStorage.getInstance().reference.child(AppConstants.PATH + userModel.uid).downloadUrl
                .addOnSuccessListener {
                    userModel.image = it.toString()
                    Log.d(TAG, "Url image loaded successfully")
                    holder.contactItemLayoutBinding.userModel = userModel
                }
                .addOnFailureListener {
                    Log.d(TAG, "Failed to load url image -> $it")
                    holder.contactItemLayoutBinding.userModel = userModel
                }
        }else holder.contactItemLayoutBinding.userModel = userModel

        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users").child(userModel.uid).child("language").get()
            .addOnSuccessListener { hisLanguage ->
                holder.itemView.setOnClickListener {
                    val intent = Intent(it.context, MessageActivity::class.java)
                    intent.putExtra("hisId", userModel.uid)
                    intent.putExtra("hisImage", userModel.image)
                    intent.putExtra("hisUsername",userModel.name)
                    intent.putExtra("hisLanguage",hisLanguage.value.toString())
                    it.context.startActivity(intent)
                }
            }
    }

    override fun getItemCount(): Int {
        return allContact.size
    }

    class ViewHolder(val contactItemLayoutBinding: ContactItemLayoutBinding) :
        RecyclerView.ViewHolder(contactItemLayoutBinding.root) {
    }

    override fun getFilter(): Filter? {
        return null
    }

}
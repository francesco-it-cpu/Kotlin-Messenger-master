package com.example.kotlinmessenger.Fragment

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinmessenger.*
import com.example.kotlinmessenger.databinding.FConversationsBinding
import com.google.firebase.database.FirebaseDatabase
import com.example.kotlinmessenger.R
import com.example.kotlinmessenger.adapter.ChatAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class f_conversations : Fragment(R.layout.f_conversations) {

    private lateinit var binding: FConversationsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var chatAdapter: ChatAdapter? = null
    private var activeChats = ArrayList<ChatModel>()

    private val TAG = "CONVERSATION FRAGMENT"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activeChats = ArrayList<ChatModel>()

        binding = FConversationsBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        getAllActiveChat()

        getActivity()?.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        return binding.root
    }

    private fun getAllActiveChat() {
        val uid = FirebaseAuth.getInstance().uid ?: ""

        val ref = FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("/chatlist")
        ref.child(uid).get()
            .addOnSuccessListener {
                it.children.forEach {
                    val lastMessage = it.child("lastMessage").value.toString()
                    val date = it.child("date").value.toString()
                    val chatID = it.child("chatId").value.toString()
                    val hisId = it.child("member").value.toString()
                    activeChats.add(ChatModel(chatID,"",lastMessage,"",date,hisId,""))
                }
                updateRecycleView(activeChats)
                syncNewChats()
            }
            .addOnFailureListener {
                Toast.makeText(activity,"Error query for active chats failed!",Toast.LENGTH_SHORT).show()
            }
    }

    private fun syncNewChats(){
        val uid = FirebaseAuth.getInstance().uid ?: ""
        FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("/chatlist").child(uid).addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(activeChats.size < snapshot.childrenCount){
                        //New chat created
                            if(activeChats.size == 0) binding.noChatTV.visibility = View.INVISIBLE
                        snapshot.children.last().let {
                            val lastMessage = it.child("lastMessage").value.toString()
                            val date = it.child("date").value.toString()
                            val chatID = it.child("chatId").value.toString()
                            val hisId = it.child("member").value.toString()
                            activeChats.add(ChatModel(chatID,"",lastMessage,"",date,hisId,""))
                            binding.recyclerViewChat.adapter!!.notifyItemInserted(activeChats.size-1)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun updateRecycleView(collection: ArrayList<ChatModel>) {
        if(collection.size == 0){
            binding.noChatTV.visibility = View.VISIBLE
        }else{
            binding.noChatTV.visibility = View.INVISIBLE
        }
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            chatAdapter = ChatAdapter(collection,binding.noChatTV,requireActivity())
            adapter = chatAdapter
        }

    }
}
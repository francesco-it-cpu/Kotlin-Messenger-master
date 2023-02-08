package com.example.kotlinmessenger


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kotlinmessenger.Constants.AppConstants
import com.example.kotlinmessenger.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var username: String
    private lateinit var status: String
    private lateinit var imageUrl: String

    private var selectedPhotoUri: Uri? = null
    private var databaseReference: DatabaseReference? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var storageReference: StorageReference? = null
    private lateinit var myId: String
    private lateinit var appUtil: AppUtil
    private lateinit var binding: ActivityProfileBinding
    private var myToken = ""
    private val TAG = "PROFILE ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityProfileBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appUtil= AppUtil(this)
        myId = appUtil.getUID()!!
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app").getReference("Users")
        storageReference = FirebaseStorage.getInstance().reference

        binding.btnDataDone.setOnClickListener {
            if(checkData()){
                if(selectedPhotoUri == null){
                    uploadData(username,status)
                }else{
                    uploadData(username,status,selectedPhotoUri!!)
                }
            }
        }
        binding.buttonlogout.setOnClickListener{
            val databaseReference =
                FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("users").child(myId).child("token").removeValue()
            Log.d(TAG, "token removed")
            firebaseAuth!!.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }



        binding.imgPickImage.setOnClickListener {
            Log.d("RegisterActivity", "Try to show photo selector")
            val intent=Intent(Intent.ACTION_PICK)
            intent.type="image/*"
            getAction.launch(intent)
        }

        binding.Back.setOnClickListener {
            startActivity(Intent(this, DashBoard::class.java))
        }

        loadData()
    }

    private val getAction= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        if(result.resultCode==Activity.RESULT_OK && result.data!=null){
            Log.d(TAG,"Photo selected")
            selectedPhotoUri= result.data?.data//location di dove l immagine è stata memorizzata
            //caricamento immagine sulla pagina di login
            val bitmap= MediaStore.Images.Media.getBitmap(contentResolver,selectedPhotoUri)
            binding.imgUser.setImageBitmap(bitmap)
        }
    }

    private fun checkData(): Boolean {
        username = binding.usernameET.text.toString().trim()
        status = binding.userStatusET.text.toString().trim()
        Log.d("Profile","Check data")
        if (username.isEmpty()) {
            binding.usernameET.error = "Filed is required"
            return false
        } else if (status.isEmpty()) {
            binding.userStatusET.error = "Filed is required"
            return false
        } else return true
    }

    //Funzione che carica i dati dell'utente nell'interfaccia
    private fun loadData(){
        val uid = FirebaseAuth.getInstance().uid ?: ""
        if(uid != ""){
            val ref = FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app").getReference("/users/$uid")
            val data = ref.get()
                .addOnFailureListener {
                    Toast.makeText(this,"Fetching error: $it",Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener {
                    myToken = it.child("token").value.toString()
                    val status = it.child("status").value
                    val username = it.child("name").value
                    binding.usernameET.setText(username.toString())
                    binding.userStatusET.setText(status.toString())
                    //Caricamento e visualizzazione dell'immagine da Firebase
                    val storageRef = FirebaseStorage.getInstance().reference.child(AppConstants.PATH + uid)
                    val localFile = File.createTempFile("tempImage","jpeg")
                    storageRef.getFile(localFile)
                        .addOnSuccessListener {
                            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                            binding.imgUser.setImageBitmap(bitmap)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this,"Image loading failed", Toast.LENGTH_SHORT).show()
                        }
                }
        }else{
            Toast.makeText(this,"You are not logged",Toast.LENGTH_LONG).show()
        }


    }

    //Aggiorna solo stato e username
    private fun uploadData(name: String, status: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app").getReference("/users/$uid")
        val user = UserModel(name,status,"",uid,"online","false")
        ref.setValue(user)
            .addOnSuccessListener {
                bindUrlToUSer(uid)
                Toast.makeText(this,"Successfully updated",Toast.LENGTH_SHORT).show()
                Log.d(TAG, "User successfully updated")
                startActivity(Intent(this, DashBoard::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "impossibile salvare nel db", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Save Error -> $it")
            }
        ref.child("token").setValue(myToken)
            .addOnFailureListener {
                Log.d(TAG,"Token manteined")
            }
            .addOnSuccessListener {
                Log.d(TAG,"Token lost")
            }
    }

    //Modifica nome, stato e foto
    private fun uploadData(name: String, status: String, image: Uri) = kotlin.run {
        Log.d(TAG,"URI: $image")
        Log.d(TAG,"PAth: ${storageReference!!.child(AppConstants.PATH + firebaseAuth!!.uid)}")
        storageReference!!.child(AppConstants.PATH + firebaseAuth!!.uid).putFile(image)
            .addOnSuccessListener {
                Log.d(TAG,"Updateing user...")
                val task = it.storage.downloadUrl
                task.addOnCompleteListener { uri ->
                    imageUrl = uri.result.toString()
                    val uid = FirebaseAuth.getInstance().uid ?: ""
                    val ref = FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app").getReference("/users/$uid")
                    val user = UserModel(name,status,imageUrl,uid,"online","false")
                    ref.setValue(user)
                        .addOnSuccessListener {
                            Log.d(TAG, "User sevaed correctly")
                            Toast.makeText(this,"Successfully updated",Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error! Save not possible", Toast.LENGTH_LONG).show()
                            Log.d(TAG, "Utente NON salvato nel db")
                        }
                    ref.child("token").setValue(myToken)
                        .addOnFailureListener {
                              Log.d(TAG,"Token manteined")
                        }
                        .addOnSuccessListener {
                            Log.d(TAG,"Token lost")
                        }
                    startActivity(Intent(this, DashBoard::class.java))
                    finish()
                }
            }
    }

    private fun uploadImageToFireDataBase( image: Uri){
        storageReference!!.child(AppConstants.PATH + "default-avatar").putFile(image)
            .addOnSuccessListener {
                Toast.makeText(this,"File uploaded",Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    //Verifica se è concesso il permesso per accedere alla memoria
    private fun storageRequestPermission() = ActivityCompat.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), 1000
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindUrlToUSer(uid: String) {
        FirebaseStorage.getInstance().reference.child(AppConstants.PATH + uid).downloadUrl
            .addOnSuccessListener {
                FirebaseDatabase.getInstance("https://kotlin-messenger-288bc-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("/users").child(uid).child("image").setValue(it.toString())
                    .addOnSuccessListener {
                        Log.d(TAG,"Url image correctly bind to userDB")
                    }
                    .addOnFailureListener {
                        Log.d(TAG,"Error to bind url image -> $it")
                    }
            }
            .addOnFailureListener {
                Log.d(TAG,"Error to bind url image -> $it")
            }
    }

}
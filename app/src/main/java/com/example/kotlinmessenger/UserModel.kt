package com.example.kotlinmessenger

import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

data class UserModel(
    var name: String = "",
    val status: String = "",
    var image: String = "",
    val uid: String = "",
    val online: String = "offline",
    val typing: String = "false"
    ) {

    var language: String = ""

    companion object {

        @JvmStatic
        @BindingAdapter("imageUrl")
        fun loadImage(view: CircleImageView, imageUrl: String?) {
            imageUrl?.let {
                Glide.with(view.context).load(imageUrl).into(view)
            }
        }
    }

}


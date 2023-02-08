package com.example.kotlinmessenger

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

class MessageModel constructor(
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var date: String = "",
    var type: String = ""
) {

    var translatedMessage = ""

    companion object {

        @JvmStatic
        @BindingAdapter("imageMessage")
        fun loadImage(imageView: ImageView, image: String?) {
            imageView.maxHeight = 500
            imageView.maxWidth = 300
            if (image != null) {
                Glide.with(imageView.context).load(image).into(imageView)
            }
        }
    }
}
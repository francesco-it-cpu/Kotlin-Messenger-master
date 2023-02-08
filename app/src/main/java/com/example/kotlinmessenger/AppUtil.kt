package com.example.kotlinmessenger

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import kotlin.coroutines.coroutineContext

class AppUtil(val context: Context) {

    companion object {

        private const val SECOND_MILLIS: Int = 1000
        private const val MINUTE_MILLIS: Int = 60 * SECOND_MILLIS
        private const val HOUR_MILLIS: Int = 60 * MINUTE_MILLIS
        private const val DAY_MILLIS: Int = 24 * HOUR_MILLIS
    }

    fun getUID(): String? {
        val firebaseAuth = FirebaseAuth.getInstance()
        return firebaseAuth.uid
    }

    fun getTimeAgo(time: Long): String? {

        var time = time

        if (time < 1000000000000L) {
            time *= 1000
        }
        val now = System.currentTimeMillis()
        if (time > now || time <= 0) {
            return null
        }

        val diff = now - time
        Log.d("DATE","Ora: ${now} time: $time diff: $diff")

        return when {
            diff < MINUTE_MILLIS -> {
                " "+context.getString(R.string.just_now)
            }
            diff < 2 * MINUTE_MILLIS -> {
                " "+context.getString(R.string.a_minute_ago)
            }
            diff < 50 * MINUTE_MILLIS -> {
                (diff / MINUTE_MILLIS).toString() + " "+context.getString(R.string.minutes_ago)
            }
            diff < 90 * MINUTE_MILLIS -> {
                " "+context.getString(R.string.an_hour_ago)
            }
            diff < 24 * HOUR_MILLIS -> {
                (diff / HOUR_MILLIS).toString() + " "+context.getString(R.string.hours_ago)
            }
            diff < 48 * HOUR_MILLIS -> {
                " "+context.getString(R.string.yesterday)
            }
            else -> {
                (diff / DAY_MILLIS).toString() + " "+context.getString(R.string.days_ago)
            }
        }
    }

    fun updateOnlineStatus(status: String) {
        val databaseReference =
            FirebaseDatabase.getInstance().getReference("users").child(getUID()!!)
        val map = HashMap<String, Any>()
        map["online"] = status
        databaseReference.updateChildren(map)
    }

}
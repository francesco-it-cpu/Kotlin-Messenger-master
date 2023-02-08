package com.example.kotlinmessenger

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.*

class LanguageManager(sourceLanguage: String, targetLanguage: String) {

    var foreignLanguage: String? = null
    var nativeLanguage: String? = null
    var modelAvailable: Boolean = false
    var translator: Translator? = null
    val TAG = "LANGUAGE MANAGER"

    init {
        setSourceLanguage(sourceLanguage)
        setTarghetLanguage(targetLanguage)
        createTranslator()
    }

    fun identifyLanguage(message: String, callback: (String) -> Unit) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(message)
            .addOnSuccessListener {
                callback(it)
                Log.d(
                    TAG,
                    "from language: ${TranslateLanguage.fromLanguageTag(it)} invece: ${TranslateLanguage.ENGLISH}"
                )
                foreignLanguage = TranslateLanguage.fromLanguageTag(it)!!
            }
            .addOnFailureListener {
                callback("und")
            }
        /* [Come si usa]
        *  lManager.identifyLanguage("ciao sono Mario") {
            Toast.makeText(this,"Lingua rilevata: $it",Toast.LENGTH_SHORT).show()
        }*/
    }

    fun setSourceLanguage(language: String) {
        foreignLanguage = language
    }

    fun setTarghetLanguage(language: String){
        nativeLanguage = language
    }

    fun createTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(foreignLanguage!!)
            .setTargetLanguage(nativeLanguage!!)
            .build()
        translator = Translation.getClient(options)
        checkConditions()
    }

    fun checkConditions() {
        val conditions = DownloadConditions.Builder()
            .build()
        translator!!.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.i(TAG, "Model correctly downloaded")
                modelAvailable = true
                Log.d(TAG, "Translator: $modelAvailable")
            }
            .addOnFailureListener {
                Log.e(TAG, "Error : model not correctly downloaded -> $it")
                modelAvailable = false
                Log.d(TAG, "Translator: $modelAvailable")
            }
    }

    fun onReady(callback: () -> Unit){
        val modelManager = RemoteModelManager.getInstance()
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener {
                it.forEach {
                    if(it.language == foreignLanguage) {
                        modelAvailable = true
                        callback()
                    }
                }
            }
    }

    fun translate(message: String, callback: (String?) -> Unit) {
        if (!modelAvailable) {
            checkConditions()
            callback(null)
        }
        translator!!.translate(message)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully translated! original: $message to: $it")
                callback(it)
            }
            .addOnFailureListener {
                Log.e(TAG, "Error during translation -> $it")
            }
    }


}
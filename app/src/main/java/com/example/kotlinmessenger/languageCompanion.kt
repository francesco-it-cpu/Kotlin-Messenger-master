package com.example.kotlinmessenger

import com.google.mlkit.nl.translate.TranslateLanguage

class languageCompanion {

    companion object {

        const val Afrikaans = 0
        const val Arabic = 1
        const val Belarusian = 2
        const val Bulgarian = 3
        const val Bengali = 4
        const val Catalan = 5
        const val Czech = 6
        const val Welsh = 7
        const val Danish = 8
        const val German = 9
        const val Greek = 10
        const val English = 11
        const val Esperanto = 12
        const val Spanish = 13
        const val Estonian = 14
        const val Persian = 15
        const val Finnish = 16
        const val French = 17
        const val Irish = 18
        const val Galician = 19
        const val Gujarati = 20
        const val Hebrew = 21
        const val Hindi = 22
        const val Croatian = 23
        const val Haitian = 24
        const val Hungarian = 25
        const val Indonesian = 26
        const val Icelandic = 27
        const val Italian = 28
        const val Japanese = 29
        const val Georgian = 30
        const val Kannada = 31
        const val Korean = 32
        const val Lithuanian = 33
        const val Latvian = 34
        const val Macedonian = 35
        const val Marathi = 36
        const val Malay = 37
        const val Maltese = 38
        const val Dutch = 39
        const val Norwegian = 40
        const val Polish = 41
        const val Portuguese = 42
        const val Romanian = 43
        const val Russian = 44
        const val Slovak = 45
        const val Slovenian = 46
        const val Albanian = 47
        const val Swedish = 48
        const val Swahili = 49
        const val Tamil = 50
        const val Telugu = 51
        const val Thai = 52
        const val Tagalog = 53
        const val Turkish = 54
        const val Ukrainian = 55
        const val Urdu = 56
        const val Vietnamese = 57
        const val Chinese = 58

    }

    val supported_languages_code = arrayOf(
        "af",
        "ar",
        "be",
        "bg",
        "bn",
        "ca",
        "cs",
        "cy",
        "da",
        "de",
        "dl",
        "en",
        "eo",
        "es",
        "et",
        "fa",
        "fi",
        "fr",
        "ga",
        "gl",
        "gu",
        "he",
        "hi",
        "hr",
        "ht",
        "hu",
        "id",
        "is",
        "it",
        "ja",
        "ka",
        "kn",
        "ko",
        "lt",
        "lv",
        "mk",
        "nr",
        "ms",
        "mt",
        "nl",
        "no",
        "pl",
        "pt",
        "ro",
        "ru",
        "sk",
        "sl",
        "sq",
        "sv",
        "sw",
        "ta",
        "te",
        "th",
        "tl",
        "tr",
        "uk",
        "ur",
        "vi",
        "zh"
    )

    val supported_languages = arrayOf(
        "Afrikaans",
        "Arabic",
        "Belarusian",
        "Bulgarian",
        "Bengali",
        "Catalan",
        "Czech",
        "Welsh",
        "Danish",
        "German",
        "Greek",
        "English",
        "Esperanto",
        "Spanish",
        "Estonian",
        "Persian",
        "Finnish",
        "French",
        "Irish",
        "Galician",
        "Gujarati",
        "Hebrew",
        "Hindi",
        "Croatian",
        "Haitian",
        "Hungarian",
        "Indonesian",
        "Icelandic",
        "Italian",
        "Japanese",
        "Georgian",
        "Kannada",
        "Korean",
        "Lithuanian",
        "Latvian",
        "Macedonian",
        "Marathi",
        "Malay",
        "Maltese",
        "Dutch",
        "Norwegian",
        "Polish",
        "Portuguese",
        "Romanian",
        "Russian",
        "Slovak",
        "Slovenian",
        "Albanian",
        "Swedish",
        "Swahili",
        "Tamil",
        "Telugu",
        "Thai",
        "Tagalog",
        "Turkish",
        "Ukrainian",
        "Urdu",
        "Vietnamese",
        "Chinese"
    )


    fun getLanguageFromIntCode(code: Int): String {
        return supported_languages[code]
    }

    fun getLanguageToBCP47Code(language: String): String {
        val position = supported_languages.indexOf(language)
        return supported_languages_code[position]
    }

    fun getBCP47CodeFromIntCode(code: Int): String {
        return supported_languages_code[code]
    }

    fun getPosition(code: String): Int{
        if(code == "") return 0
        return supported_languages_code.indexOf(code)
    }
}
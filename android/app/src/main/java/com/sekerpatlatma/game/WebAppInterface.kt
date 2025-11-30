package com.sekerpatlatma.game

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun vibrate() {
        // Titreşim için
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    @JavascriptInterface
    fun getStorageData(key: String): String {
        val sharedPref = context.getSharedPreferences("SekerPatlatma", Context.MODE_PRIVATE)
        return sharedPref.getString(key, "") ?: ""
    }

    @JavascriptInterface
    fun setStorageData(key: String, value: String) {
        val sharedPref = context.getSharedPreferences("SekerPatlatma", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }
}

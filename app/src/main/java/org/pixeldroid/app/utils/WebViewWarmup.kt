package org.pixeldroid.app.utils

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import java.util.concurrent.atomic.AtomicBoolean

object WebViewWarmup {

    private val warmedUp = AtomicBoolean(false)
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    fun scheduleWarmup(context: Context, delayMs: Long = 1500L) {
        if (warmedUp.get()) return
        handler.postDelayed({
            if (warmedUp.get()) return@postDelayed
            try {
                WebView(context.applicationContext).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = false
                    loadUrl("about:blank")
                    destroy()
                }
                warmedUp.set(true)
            } catch (exception: Exception) {
                Log.w("WebViewWarmup", "Unable to warm up WebView", exception)
            }
        }, delayMs)
    }
}


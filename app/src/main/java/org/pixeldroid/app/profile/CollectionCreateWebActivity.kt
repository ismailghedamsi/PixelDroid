package org.pixeldroid.app.profile

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.activity.enableEdgeToEdge
import com.google.android.material.snackbar.Snackbar
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityCollectionCreateWebBinding
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.WebViewWarmup
import org.pixeldroid.app.utils.openUrl

class CollectionCreateWebActivity : BaseActivity() {

    private lateinit var binding: ActivityCollectionCreateWebBinding
    private lateinit var primaryUrl: String
    private lateinit var fallbackUrl: String
    private var fallbackTried = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WebViewWarmup.scheduleWarmup(this, delayMs = 0L)
        binding = ActivityCollectionCreateWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.topBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val user = db.userDao().getActiveUser()
        if (user == null) {
            Snackbar.make(binding.root, getString(R.string.something_went_wrong), Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        val instanceUrl = user.instance_uri.trimEnd('/')
        primaryUrl = "$instanceUrl/i/collections/create"
        fallbackUrl = "$instanceUrl/collections/create"

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progress.isVisible = newProgress < 100
            }
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progress.isVisible = true
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val targetUrl = request?.url?.toString() ?: return false
                return if (targetUrl.startsWith(instanceUrl)) {
                    false
                } else {
                    openExternalUrl(targetUrl)
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.isVisible = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    handleLoadFailure()
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true && errorResponse?.statusCode == 404) {
                    handleLoadFailure()
                }
            }
        }

        binding.webView.loadUrl(primaryUrl)
    }

    private fun openExternalUrl(url: String) {
        if (!openUrl(url)) {
            Snackbar.make(binding.root, getString(R.string.something_went_wrong), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun handleLoadFailure() {
        binding.progress.isVisible = false
        if (!fallbackTried) {
            fallbackTried = true
            binding.webView.loadUrl(fallbackUrl)
        } else {
            Snackbar.make(binding.root, getString(R.string.something_went_wrong), Snackbar.LENGTH_LONG)
                .setAction(R.string.open_in_browser) {
                    openExternalUrl(primaryUrl)
                }.show()
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            settings.javaScriptEnabled = false
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}


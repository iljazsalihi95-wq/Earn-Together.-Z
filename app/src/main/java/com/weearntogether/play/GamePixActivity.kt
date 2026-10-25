package com.weearntogether.play

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class GamePixActivity : Activity() {
    companion object {
        private const val LOCAL_CATALOG_URL = "file:///android_asset/index.html"
        private const val NETWORK_FALLBACK_URL = "https://cdn.jsdelivr.net/gh/iljazsalihi95-wq/Earn-Together.-Z@main/docs/gamepix/index.html"
    }

    private lateinit var webView: WebView
    private var fallbackAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        CookieManager.getInstance().setAcceptCookie(true)
        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    super.onReceivedError(view, request, error)
                    // The catalogue itself is bundled in the APK. Only fall back to the
                    // network copy if that main local document somehow cannot be opened.
                    if (request.isForMainFrame && !fallbackAttempted) {
                        fallbackAttempted = true
                        view.loadUrl(NETWORK_FALLBACK_URL)
                    }
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(LOCAL_CATALOG_URL)
        }
        setContentView(webView)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }
}

package com.weearntogether.play

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class GamePixActivity : Activity() {
    companion object {
        // Serve the existing generated GamePix catalogue directly from the repository CDN.
        // This avoids depending on GitHub Pages being enabled for the repository.
        const val CATALOG_URL = "https://cdn.jsdelivr.net/gh/iljazsalihi95-wq/Earn-Together.-Z@main/docs/gamepix/index.html"
        private const val PAGES_FALLBACK_URL = "https://iljazsalihi95-wq.github.io/Earn-Together.-Z/gamepix/"
    }

    private lateinit var webView: WebView
    private var fallbackAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame && !fallbackAttempted) {
                        fallbackAttempted = true
                        view.loadUrl(PAGES_FALLBACK_URL)
                    }
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(CATALOG_URL)
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
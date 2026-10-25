package com.weearntogether.play

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView

class GamePixPlayerActivity : Activity() {
    private lateinit var web:WebView
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=Color.BLACK;window.navigationBarColor=Color.BLACK
        val url=intent.getStringExtra("url")?:run{finish();return};val title=intent.getStringExtra("title")?:"GamePix"
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.BLACK)}
        root.addView(TextView(this).apply{text="‹  $title";textSize=17f;setTextColor(Color.WHITE);setPadding(24,20,16,20);setOnClickListener{finish()}},LinearLayout.LayoutParams(-1,-2))
        CookieManager.getInstance().setAcceptCookie(true)
        web=WebView(this).apply{setBackgroundColor(Color.BLACK);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.databaseEnabled=true;settings.mediaPlaybackRequiresUserGesture=false;settings.cacheMode=WebSettings.LOAD_DEFAULT;settings.mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE;settings.javaScriptCanOpenWindowsAutomatically=true;CookieManager.getInstance().setAcceptThirdPartyCookies(this,true);webViewClient=WebViewClient();webChromeClient=WebChromeClient();loadUrl(url)}
        root.addView(web,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)}
    override fun onResume(){super.onResume();web.onResume()};override fun onPause(){web.onPause();super.onPause()}
    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(web.canGoBack())web.goBack() else super.onBackPressed()}
    override fun onDestroy(){web.stopLoading();web.loadUrl("about:blank");web.clearHistory();web.removeAllViews();web.destroy();super.onDestroy()}
}

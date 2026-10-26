package com.weearntogether.play

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Bridge is exposed only to bundled, allowlisted HTTPS-origin game assets. No wallet authority. */
class Html5GamesActivity : Activity() {
    private lateinit var web: WebView
    private var ads: LevelPlayManager? = null
    private var adRequest: String? = null
    private var playerId: String? = null
    private var closed = false
    private var currentGame = "word"
    private var videoRewards: Html5VideoRewards? = null
    private val gameKeys = listOf("word","memory","garden","bubble","merge","falling","block","arrows","treasure","water","park","lines","stack")
    private val origin = "games.weearntogether.invalid"
    private val handlers = android.os.Handler(android.os.Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("wet_html5_gameplay", MODE_PRIVATE) }
    private fun accountUnchanged() = AccountStore(this).let { it.isSignedIn() && it.currentUserId() == playerId }
    private fun fullscreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val account = AccountStore(this)
        if (!account.isSignedIn()) { finish(); return }
        playerId = account.currentUserId()
        videoRewards = Html5VideoRewards(this, playerId!!)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        fullscreen()
        web = WebView(this)
        web.setBackgroundColor(0xff10162e.toInt())
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        web.addJavascriptInterface(Bridge(), "GameNative")
        val allowed = mapOf("index.html" to "text/html", "style.css" to "text/css", "engine.js" to "application/javascript", "app.js" to "application/javascript", "arcade.js" to "application/javascript", "arcade-ui.js" to "application/javascript")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val uri = request.url
                val file = uri.path?.removePrefix("/html5/")
                if (uri.scheme == "https" && uri.host == origin && uri.path == "/html5/$file" && allowed.containsKey(file)) {
                    return try { WebResourceResponse(allowed[file], "UTF-8", assets.open("html5/$file")) }
                    catch (_: Exception) { denied() }
                }
                return denied()
            }
            override fun onPageFinished(view: WebView, url: String) {
                val uri = Uri.parse(url)
                if (uri.host != origin || uri.path != "/html5/index.html") return
                val saved = prefs.getString(playerId + ":studio", "{}") ?: "{}"
                view.evaluateJavascript("window.GameApp&&GameApp.bootstrap(" + JSONObject.quote(saved) + ")", null)
            }
        }
        setContentView(web)
        handlers.postDelayed(rewardSync,15000)
        ads = LevelPlayManager(this, playerId!!).also { it.initialize(false) }
        val game = intent.getStringExtra("game")?.takeIf { it in gameKeys }
        web.loadUrl("https://$origin/html5/index.html" + (game?.let { "?game=$it" } ?: ""))
    }
    private fun denied() = WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
    private fun result(id: String, granted: Boolean) {
        if (closed || isFinishing || isDestroyed || adRequest != id) return
        adRequest = null
        web.evaluateJavascript("window.GameApp&&GameApp.adResult(" + JSONObject.quote(id) + "," + granted + ")", null)
        fullscreen()
    }
    private fun pushCoinSummary() {
        if(closed || !::web.isInitialized || !accountUnchanged()) return
        val summary=videoRewards?.summary() ?: return
        web.evaluateJavascript("window.GameApp&&GameApp.coinSummary("+summary.toString()+")",null)
    }
    private val rewardSync = object: Runnable {
        override fun run() { if(!closed){videoRewards?.sync();pushCoinSummary();handlers.postDelayed(this,15000)} }
    }
    inner class Bridge {
        @JavascriptInterface fun gameStarted(key:String) { runOnUiThread { if(key in gameKeys && accountUnchanged()){currentGame=key;videoRewards?.prepare(key)} } }
        @JavascriptInterface fun coinSummary() { runOnUiThread { pushCoinSummary() } }
        @JavascriptInterface fun openWallet() { runOnUiThread { if(accountUnchanged())startActivity(android.content.Intent(this@Html5GamesActivity,WalletActivity::class.java)) } }
        @JavascriptInterface fun close() { runOnUiThread { finish() } }
        @JavascriptInterface fun saveState(raw: String) {
            if (raw.length > 24000 || closed || !accountUnchanged()) return
            try {
                val data = JSONObject(raw); val clean = JSONObject()
                for (key in gameKeys) {
                    val s = data.optJSONObject(key) ?: JSONObject().put("stars",JSONArray()).put("unlocked",1).put("tokens",0)
                    val stars = s.getJSONArray("stars"); val values = JSONArray()
                    for (i in 0 until 40) values.put(stars.optInt(i, 0).coerceIn(0, 3))
                    clean.put(key, JSONObject().put("unlocked", s.getInt("unlocked").coerceIn(1, 40))
                        .put("stars", values).put("tokens", s.getInt("tokens").coerceIn(0, 100000)))
                }
                // These tokens are gameplay-only and never sent to any wallet/reward endpoint.
                prefs.edit().putString(playerId + ":studio", clean.toString()).apply()
            } catch (_: Exception) {}
        }
        @JavascriptInterface fun rewarded(id: String) { runOnUiThread {
            if (closed || !accountUnchanged() || adRequest != null || id.length !in 8..100) return@runOnUiThread
            if(videoRewards?.canWatch()!=true){adRequest=id;result(id,false);return@runOnUiThread}
            adRequest = id
            val rewardGame = currentGame
            var earned = false
            val shown = try { ads?.showRewardedForGameplayWithCompletion({ if(!earned && accountUnchanged()){earned = true;videoRewards?.completed(rewardGame)} }, { result(id, earned);pushCoinSummary() }) == true } catch (_: Exception) { false }
            if (!shown) result(id, false)
            else handlers.postDelayed({ result(id, false) }, 65000)
        } }
        @JavascriptInterface fun levelBreak(id: String) { runOnUiThread {
            if (closed || !accountUnchanged() || adRequest != null || id.length !in 8..100) return@runOnUiThread
            adRequest = id
            val shown = try { ads?.showInterstitialAtNaturalBreak { result(id, false) } == true } catch (_: Exception) { false }
            if (!shown) result(id, false)
            else handlers.postDelayed({ result(id, false) }, 65000)
        } }
    }
    @Deprecated("Deprecated in Java") override fun onBackPressed() {
        if (::web.isInitialized && adRequest == null) web.evaluateJavascript("window.GameApp&&GameApp.back()", null)
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if(hasFocus) fullscreen() }
    override fun onPause() { if(::web.isInitialized)web.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); if(::web.isInitialized){ if(!accountUnchanged()){finish();return};web.onResume();fullscreen()} }
    override fun onDestroy() {
        closed = true; handlers.removeCallbacksAndMessages(null); ads?.dispose()
        if(::web.isInitialized){web.removeJavascriptInterface("GameNative");web.stopLoading();web.destroy()}
        super.onDestroy()
    }
}

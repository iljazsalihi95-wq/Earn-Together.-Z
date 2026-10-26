package com.weearntogether.play

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** SDK completion receipts, never wallet authority. The server decides coins and verification. */
class Html5VideoRewards(context: Context, private val user: String) {
    companion object { private val working = AtomicBoolean(false); private val guard = Any() }
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("wet_html5_video_receipts", Context.MODE_PRIVATE)
    private val account get() = AccountStore(app)
    private fun sameAccount() = account.isSignedIn() && account.currentUserId() == user
    private fun receipts(): JSONArray = try { JSONArray(prefs.getString("$user:receipts", "[]")) } catch (_: Exception) { JSONArray() }
    /** Only called from the native LevelPlay onAdRewarded callback, never from JS. */
    fun completed(game: String) {
        if (!sameAccount()) return
        synchronized(guard) {
            var rows = receipts()
            // Bound offline data without silently overwriting an unsubmitted receipt.
            if (rows.length() >= 1000) {
                val trim=JSONArray();var removed=false
                for(i in 0 until rows.length()){val r=rows.getJSONObject(i);if(!removed && r.optString("state")!="WAITING"){removed=true}else trim.put(r)}
                if(!removed)return
                rows=trim
            }
            rows.put(JSONObject().put("id", UUID.randomUUID().toString()).put("game", "html5_$game")
                .put("completed_at", System.currentTimeMillis()).put("state", "WAITING").put("coins", 0))
            prefs.edit().putString("$user:receipts", rows.toString()).commit()
        }
        sync()
    }
    fun summary(): JSONObject = synchronized(guard) {
        val rows = receipts(); var pending = 0L; var waiting = 0; var verified = 0L
        for (i in 0 until rows.length()) { val r = rows.getJSONObject(i)
            when (r.optString("state")) { "WAITING" -> waiting++; "PENDING" -> pending += r.optLong("coins"); "VERIFIED" -> verified += r.optLong("coins") }
        }
        val server=try{JSONObject(prefs.getString("$user:balance","{}"))}catch(_:Exception){JSONObject()}
        JSONObject().put("pending_coins",server.optLong("pending_coins",pending)).put("verified_coins",server.optLong("verified_coins",verified)).put("waiting_videos", waiting)
    }
    private fun session(game: String, token: String): JSONObject {
        val key = "$user:session:$game"
        val old = try { JSONObject(prefs.getString(key, "{}")) } catch (_: Exception) { JSONObject() }
        if (old.optLong("valid_until") > System.currentTimeMillis() && (old.optInt("ad_reward_coins") > 0 || System.currentTimeMillis()-old.optLong("native_started")<120_000)) return old
        val value = BackendClient().request("startRewardSession", JSONObject().put("session_token", token).put("game_id", game))
        value.put("native_started", System.currentTimeMillis()).put("valid_until", System.currentTimeMillis() + 7_000_000)
        prefs.edit().putString(key, value.toString()).commit()
        return value
    }
    fun prepare(game: String) {
        if (!sameAccount()) return
        CloudWorker.executor.execute { try { if (sameAccount()) session("html5_$game", account.requireToken()) } catch (_: Exception) {} }
        sync()
    }
    fun sync() {
        if (!sameAccount() || !working.compareAndSet(false, true)) return
        CloudWorker.executor.execute {
            try {
                val token = account.requireToken()
                val snapshots = synchronized(guard) { receipts().toString() }
                val rows = JSONArray(snapshots)
                for (i in 0 until rows.length()) {
                    if (!sameAccount()) break
                    val r = rows.getJSONObject(i); if (r.optString("state") != "WAITING") continue
                    val s = session(r.getString("game"), token)
                    // Older deployments default AD_REWARD to zero. Keep the completed video for retry.
                    if (s.optInt("ad_reward_coins", 0) <= 0 || System.currentTimeMillis() - s.optLong("native_started") < 11_000) continue
                    val answer = BackendClient().request("recordRewardEvent", JSONObject().put("session_token", token)
                        .put("session_id", s.getString("session_id")).put("game_id", r.getString("game"))
                        .put("source", "AD_REWARD").put("event_id", r.getString("id")))
                    answer.optJSONObject("balance")?.let{prefs.edit().putString("$user:balance",it.toString()).putLong("$user:balance_at",System.currentTimeMillis()).apply()}
                    val state = answer.optString("status")
                    if (state !in listOf("PENDING", "VERIFIED", "REJECTED")) continue
                    synchronized(guard) {
                        val current = receipts()
                        for (j in 0 until current.length()) { val row = current.getJSONObject(j)
                            if (row.optString("id") == r.getString("id")) row.put("state", state)
                                .put("coins", answer.optInt("coins", 0).coerceAtLeast(0)).put("transaction_id", answer.optString("transaction_id"))
                        }
                        prefs.edit().putString("$user:receipts", current.toString()).commit()
                    }
                    // Respect the existing server event interval; retry remaining receipts later.
                    break
                }
                if(sameAccount() && System.currentTimeMillis()-prefs.getLong("$user:balance_at",0)>60_000){
                    val balance=BackendClient().request("getBalance",JSONObject().put("session_token",token))
                    prefs.edit().putString("$user:balance",balance.toString()).putLong("$user:balance_at",System.currentTimeMillis()).apply()
                }
            } catch (_: Exception) { /* Offline, old backend or expired auth: durable receipt remains queued. */ }
            finally { working.set(false) }
        }
    }
    fun canWatch(): Boolean = synchronized(guard) { val rows=receipts();var waiting=0;for(i in 0 until rows.length())if(rows.getJSONObject(i).optString("state")=="WAITING")waiting++;waiting<1000 }
}

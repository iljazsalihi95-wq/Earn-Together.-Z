package com.weearntogether.play

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Optimistic version checks; divergent device/cloud boards always require a player choice. */
class CloudProgress(context: Context) {
    private val context = context.applicationContext
    private val api = BackendClient()
    private val accounts = AccountStore(this.context)
    private val meta = this.context.getSharedPreferences("wet_cloud_progress_meta", Context.MODE_PRIVATE)
    data class Conflict(val user: String, val game: String, val local: String, val cloud: String, val cloudVersion: String, val deviceLevel: Int, val cloudLevel: Int)
    data class SyncResult(val message: String, val conflicts: List<Conflict> = emptyList())
    private fun key(user: String, game: String) = "$user:$game"
    private fun canonical(v: Any?): String = when (v) {
        is JSONObject -> v.keys().asSequence().toList().sorted().joinToString(prefix="{", postfix="}") { JSONObject.quote(it)+":"+canonical(v.get(it)) }
        is JSONArray -> (0 until v.length()).joinToString(prefix="[",postfix="]") { canonical(v.get(it)) }
        is String -> JSONObject.quote(v)
        else -> v.toString()
    }
    private fun signature(state: JSONObject) = MessageDigest.getInstance("SHA-256").digest(canonical(state).toByteArray()).joinToString("") { "%02x".format(it) }
    private fun hasProgress(state: JSONObject) = state.keys().asSequence().any { (state.optJSONObject(it)?.length() ?: 0) > 0 }
    private fun level(game: String, state: JSONObject): Int {
        val data=state.optJSONObject(ProgressPreferences.gameFiles.getValue(game).first()) ?: JSONObject()
        return data.optInt(if(game=="LazyGarden") "unlocked_level" else "level", 1)
    }
    private fun payload(game: String, state: JSONObject, version: String): JSONObject {
        val data=state.optJSONObject(ProgressPreferences.gameFiles.getValue(game).first()) ?: JSONObject()
        val score=data.optInt(if(game=="Bubble Shooter") "points" else "score",0).coerceIn(0,1_000_000_000)
        return JSONObject().put("game",game).put("level",level(game,state).coerceIn(0,1_000_000_000))
            .put("score",score).put("best_score",data.optInt("best",score).coerceIn(0,1_000_000_000))
            .put("progress_json",state.toString()).put("base_updated_at",version)
    }
    private fun remember(user: String, game: String, state: JSONObject, version: String) {
        meta.edit().putString(key(user,game)+":signature",signature(state)).putString(key(user,game)+":version",version).apply()
    }
    private fun backup(user: String, game: String, source: String, state: JSONObject) {
        // Keep the latest losing save from both sources; never discard it during conflict resolution.
        meta.edit().putString(key(user,game)+":backup:"+source,state.toString()).apply()
    }
    fun sync(allowRestore: Boolean = true): SyncResult {
        val user=accounts.currentUserId() ?: return SyncResult("Sign in to sync")
        val token=accounts.requireToken()
        val remote=api.request("getProgress",JSONObject().put("session_token",token)).getJSONArray("progress")
        val cloud=(0 until remote.length()).map { remote.getJSONObject(it) }.associateBy { it.getString("game") }
        val conflicts=mutableListOf<Conflict>();val uploads=JSONArray();val sent=mutableMapOf<String,JSONObject>()
        for(game in ProgressPreferences.gameFiles.keys){
            val local=ProgressPreferences.state(context,user,game);val row=cloud[game]
            if(row==null){if(hasProgress(local)){uploads.put(payload(game,local,""));sent[game]=local};continue}
            val state=JSONObject(row.getString("progress_json"));val version=row.getString("updated_at")
            // Refuse unknown schema without altering either source.
            if(!ProgressPreferences.gameFiles.getValue(game).all { state.optJSONObject(it)!=null }) return SyncResult("Cloud save format needs review. Device saves are safe.")
            val localSig=signature(local);val cloudSig=signature(state);val baseSig=meta.getString(key(user,game)+":signature",null);val baseVersion=meta.getString(key(user,game)+":version",null)
            when (ProgressSyncPolicy.decide(hasProgress(local),localSig,cloudSig,baseSig,version,baseVersion)) {
                ProgressSyncPolicy.Decision.SAME -> remember(user,game,local,version)
                ProgressSyncPolicy.Decision.DOWNLOAD -> if(allowRestore){backup(user,game,"device",local);ProgressPreferences.apply(context,user,game,state);remember(user,game,state,version)}
                ProgressSyncPolicy.Decision.UPLOAD -> {uploads.put(payload(game,local,version));sent[game]=local}
                ProgressSyncPolicy.Decision.CONFLICT -> {backup(user,game,"device",local);backup(user,game,"cloud",state);conflicts+=Conflict(user,game,local.toString(),state.toString(),version,level(game,local),level(game,state))}
            }
        }
        if(uploads.length()>0){
            val saved=api.request("syncProgress",JSONObject().put("session_token",token).put("progress",uploads)).getJSONArray("progress")
            for(i in 0 until saved.length()){
                val row=saved.getJSONObject(i);val game=row.getString("game");val state=sent.getValue(game)
                if(!row.optBoolean("conflict")) remember(user,game,state,row.getString("updated_at"))
                else {val remoteState=JSONObject(row.getString("progress_json"));backup(user,game,"device",state);backup(user,game,"cloud",remoteState);conflicts+=Conflict(user,game,state.toString(),remoteState.toString(),row.getString("updated_at"),level(game,state),row.getInt("level"))}
            }
        }
        return SyncResult(if(conflicts.isEmpty()) "Cloud progress is up to date" else "Choose which save to continue",conflicts)
    }
    fun resolve(conflict: Conflict, useDevice: Boolean) {
        check(accounts.currentUserId()==conflict.user) { "Account changed. Please sync again." }
        val current=ProgressPreferences.state(context,conflict.user,conflict.game)
        check(signature(current)==signature(JSONObject(conflict.local))) { "Device save changed. Please sync again." }
        if(useDevice){
            val result=api.request("saveProgress",payload(conflict.game,current,conflict.cloudVersion).put("session_token",accounts.requireToken())).getJSONArray("progress").getJSONObject(0)
            check(!result.optBoolean("conflict")) { "Cloud save changed. Please sync again before choosing." }
            remember(conflict.user,conflict.game,current,result.getString("updated_at"))
        }else{
            // Check remote version once more before replacing the active cache.
            val rows=api.request("getProgress",JSONObject().put("session_token",accounts.requireToken()).put("game",conflict.game)).getJSONArray("progress")
            check(rows.length()==1 && rows.getJSONObject(0).getString("updated_at")==conflict.cloudVersion) { "Cloud save changed. Please sync again." }
            val state=JSONObject(conflict.cloud);ProgressPreferences.apply(context,conflict.user,conflict.game,state);remember(conflict.user,conflict.game,state,conflict.cloudVersion)
        }
    }
}

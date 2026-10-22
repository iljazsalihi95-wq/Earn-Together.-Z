package com.weearntogether.play

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Locale

/** Real online accounts. Local legacy password hashes are left untouched, never used as cloud sessions. */
class AccountStore(context: Context) {
    private val context=context.applicationContext
    private val prefs=this.context.getSharedPreferences("wet_cloud_session",Context.MODE_PRIVATE)
    private val legacy=this.context.getSharedPreferences("wet_guest_profile",Context.MODE_PRIVATE)
    private val secure=SecureSession(this.context)
    private val api=BackendClient()
    data class Result(val ok:Boolean,val message:String)
    fun currentEmail():String?=prefs.getString("email",null)
    fun currentName():String?=prefs.getString("name",null)
    fun currentUserId():String?=prefs.getString("user_id",null)
    fun accountStatus():String=prefs.getString("status","UNKNOWN") ?: "UNKNOWN"
    fun isSignedIn():Boolean=currentUserId()!=null && accountStatus()=="ACTIVE" && prefs.getLong("expires_at_ms",0)>System.currentTimeMillis() && secure.token()!=null
    fun requireToken():String { if(!isSignedIn()) throw BackendException("SESSION_EXPIRED","Please sign in again");return secure.token() ?: throw BackendException("SESSION_EXPIRED","Please sign in again") }
    fun register(name:String,email:String,password:String,confirm:String):Result {
        val cleanName=name.trim();val cleanEmail=email.trim().lowercase(Locale.ROOT)
        if(cleanName.length !in 2..100)return Result(false,"Enter your full name (2–100 characters)")
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches())return Result(false,"Enter a valid email")
        if(password.length !in 8..256)return Result(false,"Password must be 8–256 characters")
        if(password!=confirm)return Result(false,"Passwords do not match")
        return authenticate("register",JSONObject().put("name",cleanName).put("email",cleanEmail).put("password",password))
    }
    fun login(email:String,password:String):Result {
        val cleanEmail=email.trim().lowercase(Locale.ROOT)
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches())return Result(false,"Enter a valid email")
        if(password.isEmpty())return Result(false,"Enter your password")
        return authenticate("login",JSONObject().put("email",cleanEmail).put("password",password))
    }
    private fun authenticate(action:String,data:JSONObject):Result=try {
        val r=api.request(action,data)
        secure.save(r.getString("session_token"))
        saveProfile(r)
        ProgressPreferences.migrateLegacy(context,r.getString("user_id"))
        Result(true,"Signed in")
    }catch(e:Exception){Result(false,message(e))}
    fun googleChallenge():String=api.request("googleChallenge").getString("nonce")
    fun googleSignIn(idToken:String,link:Boolean=false):Result {
        val data=JSONObject().put("id_token",idToken)
        if(link)data.put("session_token",requireToken())
        return authenticate(if(link)"linkGoogle" else "googleSignIn",data)
    }
    private fun saveProfile(r:JSONObject){
        val expires=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).apply { timeZone=TimeZone.getTimeZone("UTC") }.parse(r.getString("expires_at"))!!.time
        check(prefs.edit().putString("user_id",r.getString("user_id")).putString("name",r.getString("name"))
            .putString("email",r.getString("email")).putString("status",r.getString("status")).putLong("expires_at_ms",expires).commit())
        // The ad SDK receives the real server user ID. No token/password is exposed to it.
        legacy.edit().putString("guest_id",r.getString("user_id")).remove("signed_in").remove("account_email").apply()
    }
    fun restore():Result {
        if(!isSignedIn()){clear();return Result(false,"Please sign in")}
        return try{saveProfile(api.request("validateSession",JSONObject().put("session_token",requireToken())));Result(true,"Session verified")}
        catch(e:BackendException){if(e.code=="SESSION_EXPIRED"){clear();Result(false,e.message?:"Please sign in again")}else Result(isSignedIn(),"Offline — using device saves")}
        catch(_:Exception){Result(isSignedIn(),"Offline — using device saves")}
    }
    fun refreshProfile():Result=try{saveProfile(api.request("getProfile",JSONObject().put("session_token",requireToken())));Result(true,"Profile updated")}
        catch(e:Exception){if(e is BackendException&&e.code=="SESSION_EXPIRED")clear();Result(false,message(e))}
    fun updateName(name:String):Result=try{
        val r=api.request("updateProfile",JSONObject().put("session_token",requireToken()).put("name",name.trim()))
        prefs.edit().putString("name",r.getString("name")).apply();Result(true,"Profile updated")
    }catch(e:Exception){Result(false,message(e))}
    fun logout():Result {
        return try{val token=secure.token();if(token!=null)api.request("logout",JSONObject().put("session_token",token));clear();Result(true,"Signed out")}
        catch(e:BackendException){if(e.code=="SESSION_EXPIRED"){clear();Result(true,"Signed out")}else Result(false,"Could not revoke your session. Reconnect and try again.")}
        catch(_:Exception){Result(false,"Could not revoke your session. Reconnect and try again.")}
    }
    fun clear(){secure.clear();legacy.edit().remove("guest_id").remove("signed_in").remove("account_email").apply()}
    private fun message(e:Exception)=if(e is BackendException)e.message ?: "Request failed" else "Connection failed. Check your connection and try again."
}

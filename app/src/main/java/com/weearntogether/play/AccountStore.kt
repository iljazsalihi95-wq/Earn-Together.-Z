package com.weearntogether.play

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local account store used until a remote account backend is configured.
 * Passwords are never stored in plain text. This does not pretend to be cloud sync.
 */
class AccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("wet_accounts_v1", Context.MODE_PRIVATE)
    private val session = context.getSharedPreferences("wet_guest_profile", Context.MODE_PRIVATE)

    data class Result(val ok:Boolean, val message:String)

    fun isSignedIn():Boolean = session.getBoolean("signed_in", false) && session.contains("account_email")
    fun currentEmail():String? = session.getString("account_email", null)
    fun currentName():String? = session.getString("display_name", null)

    fun register(name:String,email:String,password:String,confirm:String):Result {
        val cleanName=name.trim()
        val cleanEmail=email.trim().lowercase()
        if(cleanName.length < 2) return Result(false,"Enter your full name")
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) return Result(false,"Enter a valid email")
        if(password.length < 8) return Result(false,"Password must have at least 8 characters")
        if(password != confirm) return Result(false,"Passwords do not match")
        val key=accountKey(cleanEmail)
        if(prefs.contains("${key}_hash")) return Result(false,"An account with this email already exists on this device")
        val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
        val hash=hash(password,salt)
        prefs.edit()
            .putString("${key}_email",cleanEmail)
            .putString("${key}_name",cleanName)
            .putString("${key}_salt",Base64.encodeToString(salt,Base64.NO_WRAP))
            .putString("${key}_hash",Base64.encodeToString(hash,Base64.NO_WRAP))
            .putLong("${key}_created",System.currentTimeMillis())
            .apply()
        signInSession(cleanName,cleanEmail)
        return Result(true,"Account created")
    }

    fun login(email:String,password:String):Result {
        val cleanEmail=email.trim().lowercase()
        val key=accountKey(cleanEmail)
        val salt64=prefs.getString("${key}_salt",null) ?: return Result(false,"Account not found on this device")
        val expected64=prefs.getString("${key}_hash",null) ?: return Result(false,"Account not found on this device")
        val salt=Base64.decode(salt64,Base64.NO_WRAP)
        val expected=Base64.decode(expected64,Base64.NO_WRAP)
        if(!MessageDigest.isEqual(expected,hash(password,salt))) return Result(false,"Incorrect password")
        val name=prefs.getString("${key}_name","Player") ?: "Player"
        signInSession(name,cleanEmail)
        return Result(true,"Signed in")
    }

    fun logout(){
        session.edit().remove("signed_in").remove("account_email").remove("guest_id").apply()
    }

    private fun signInSession(name:String,email:String){
        val stableId="account_${accountKey(email)}"
        session.edit()
            .putBoolean("signed_in",true)
            .putString("account_email",email)
            .putString("display_name",name)
            .putString("guest_id",stableId)
            .putLong("last_login",System.currentTimeMillis())
            .apply()
    }

    private fun accountKey(email:String):String {
        val bytes=MessageDigest.getInstance("SHA-256").digest(email.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(""){"%02x".format(it)}.take(32)
    }

    private fun hash(password:String,salt:ByteArray):ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(),salt,120_000,256)).encoded
}

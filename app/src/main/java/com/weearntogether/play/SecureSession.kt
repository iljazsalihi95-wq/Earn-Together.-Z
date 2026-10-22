package com.weearntogether.play

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Session token encrypted with a device-bound Android Keystore AES-GCM key. */
class SecureSession(context: Context) {
    private val prefs = context.getSharedPreferences("wet_cloud_session", Context.MODE_PRIVATE)
    private val alias = "wet_cloud_session_v1"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("token_cipher", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("token_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).commit())
    }
    fun token(): String? = try {
        val encrypted = prefs.getString("token_cipher", null)
        val iv = prefs.getString("token_iv", null)
        if (encrypted == null || iv == null) null else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))) }
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
        }
    } catch (_: Exception) { null }
    fun clear() { prefs.edit().clear().commit() }
}

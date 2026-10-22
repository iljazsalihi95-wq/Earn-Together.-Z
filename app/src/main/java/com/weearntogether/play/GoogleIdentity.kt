package com.weearntogether.play

import android.app.Activity
import android.os.CancellationSignal
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.concurrent.Executor

/** Google ID tokens stay in memory; only our backend's encrypted session is persisted. */
class GoogleIdentity(private val activity:Activity) {
    private val manager=CredentialManager.create(activity)
    private val main=Executor { action -> activity.runOnUiThread(action) }
    private var cancellation:CancellationSignal?=null
    companion object {
        const val WEB_CLIENT_ID="726413248892-hvdca11ahhbb1lm6in5sdviglep69494.apps.googleusercontent.com"
    }
    fun signIn(nonce:String,complete:(String?,String?)->Unit){
        val option=GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).setNonce(nonce).build()
        val request=GetCredentialRequest.Builder().addCredentialOption(option).build()
        val signal=CancellationSignal();cancellation=signal
        manager.getCredentialAsync(activity,request,signal,main,object:CredentialManagerCallback<GetCredentialResponse,GetCredentialException>{
            override fun onResult(result:GetCredentialResponse){
                cancellation=null
                try{
                    val credential=result.credential
                    if(credential !is CustomCredential||credential.type!=GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)throw IllegalStateException()
                    complete(GoogleIdTokenCredential.createFrom(credential.data).idToken,null)
                }catch(_:Exception){complete(null,"Google did not return a valid sign-in credential. Try again.")}
            }
            override fun onError(e:GetCredentialException){
                cancellation=null
                complete(null,if(e is GetCredentialCancellationException)"Google sign-in cancelled" else "Google sign-in unavailable. Check Google Play services, your connection, and the app's OAuth configuration.")
            }
        })
    }
    fun clear(complete:()->Unit){
        manager.clearCredentialStateAsync(ClearCredentialStateRequest(),null,main,object:CredentialManagerCallback<Void?,ClearCredentialException>{
            override fun onResult(result:Void?){complete()}
            override fun onError(e:ClearCredentialException){complete()}
        })
    }
    fun cancel(){cancellation?.cancel();cancellation=null}
}

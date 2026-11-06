package com.weearntogether.play

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech

/** Announce only a loaded ad; continue once from dismissal/failure, never from reward claims. */
internal class ArenaCommercialIntro(private val activity:Activity,private val ready:()->Unit,private val cancelled:()->Unit){
    private val handler=Handler(Looper.getMainLooper())
    private var dialog:AlertDialog?=null
    private var speech:TextToSpeech?=null
    private var finished=false
    private fun finish(showAd:Boolean){
        if(finished)return
        finished=true;handler.removeCallbacksAndMessages(null);speech?.stop();speech?.shutdown();speech=null
        dialog?.setOnCancelListener(null);dialog?.dismiss();dialog=null
        if(showAd)ready() else cancelled()
    }
    fun show(){
        dialog=AlertDialog.Builder(activity).setTitle("PUSHIM I SHKURTËR")
            .setMessage("Tani është koha për një publicitet.\nVazhdojmë pas publicitetit.")
            .create().apply{setOnCancelListener{finish(false)};show()}
        // A bound prevents a missing device voice from blocking the next round.
        handler.postDelayed({finish(true)},7000)
        speech=TextToSpeech(activity){result->
            if(!finished){
                val language=java.util.Locale.forLanguageTag("sq-AL")
                val engine=speech
                if(result==TextToSpeech.SUCCESS&&engine!=null&&engine.isLanguageAvailable(language)>=0){
                    engine.language=language
                    engine.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){
                        override fun onStart(id:String?){}
                        override fun onDone(id:String?){handler.post{finish(true)}}
                        @Deprecated("TTS callback") override fun onError(id:String?){handler.post{finish(true)}}
                    })
                    engine.speak("Tani është koha për një publicitet. Vazhdojmë pas publicitetit.",TextToSpeech.QUEUE_FLUSH,null,"arena-commercial")
                }else handler.postDelayed({finish(true)},2500)
            }
        }
    }
    fun cancel(){finish(false)}
}

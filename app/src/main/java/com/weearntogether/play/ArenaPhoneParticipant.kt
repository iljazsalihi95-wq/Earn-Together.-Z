package com.weearntogether.play

import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.abs
import kotlin.math.sin

/** Broadcast caller inset. The virtual character speaks only from playback audio energy. */
internal class ArenaPhoneParticipant {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint=TextPaint(Paint.ANTI_ALIAS_FLAG)
    fun draw(canvas:Canvas,status:String,speaking:Boolean,energy:Float,time:Long) {
        val e=if(speaking)energy.coerceIn(0f,1f) else 0f
        val left=448f;val top=291f;val right=695f;val bottom=512f
        paint.shader=LinearGradient(left,top,right,bottom,intArrayOf(0xff142a50.toInt(),0xff2e1b4b.toInt()),null,Shader.TileMode.CLAMP)
        canvas.drawRoundRect(left,top,right,bottom,16f,16f,paint);paint.shader=null
        paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;paint.color=0xffd8b76a.toInt()
        canvas.drawRoundRect(left,top,right,bottom,16f,16f,paint);paint.style=Paint.Style.FILL
        textPaint.typeface=Typeface.create("sans-serif-medium",Typeface.BOLD);textPaint.textAlign=Paint.Align.CENTER
        textPaint.textSize=14f;textPaint.color=0xffffd782.toInt();canvas.drawText("SHOKU VIRTUAL",571f,314f,textPaint)
        paint.color=0xff314764.toInt();canvas.drawCircle(571f,393f,60f,paint)
        // A deliberately illustrated avatar; no claim that a real person has joined the call.
        paint.color=0xff324478.toInt();canvas.drawOval(515f,421f,627f,472f,paint)
        paint.color=0xffbf886c.toInt();canvas.drawRoundRect(559f,417f,584f,444f,9f,9f,paint)
        paint.color=0xffe0ae8c.toInt();canvas.drawOval(535f,344f,608f,431f,paint)
        paint.color=0xff211f30.toInt()
        val hair=Path().apply{moveTo(534f,382f);lineTo(532f,357f);quadTo(540f,328f,573f,334f);quadTo(613f,330f,610f,378f);lineTo(601f,365f);quadTo(574f,368f,548f,355f);close()}
        canvas.drawPath(hair,paint)
        paint.color=0xff292631.toInt();canvas.drawCircle(555f,386f,3f,paint);canvas.drawCircle(589f,386f,3f,paint)
        // Jaw opening follows decoded speech; silence has a closed mouth even during playback.
        paint.color=0xff603e3e.toInt();canvas.drawRoundRect(559f,407f-e*3f,585f,410f+e*9f,5f,5f,paint)
        paint.style=Paint.Style.STROKE;paint.color=0xff8cc9ea.toInt();paint.strokeWidth=5f
        canvas.drawArc(529f,338f,613f,423f,180f,180f,false,paint)
        paint.style=Paint.Style.FILL;paint.color=0xff87b4d5.toInt();canvas.drawRoundRect(527f,379f,537f,406f,4f,4f,paint);canvas.drawRoundRect(605f,379f,615f,406f,4f,4f,paint)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=3f;canvas.drawLine(610f,403f,598f,418f,paint);canvas.drawLine(598f,418f,588f,418f,paint);paint.style=Paint.Style.FILL
        // Pulses indicate a call attempt; waveform and mouth remain audio-driven.
        val dialing=!speaking&&(status.contains("TELEFONOHET")||status.contains("LIDH"))
        if(dialing){paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;paint.color=0xff8cc9ea.toInt();paint.alpha=(100+70*abs(sin(time/450.0))).toInt();canvas.drawCircle(571f,393f,64f,paint);paint.alpha=255;paint.style=Paint.Style.FILL}
        for(i in 0..10){val h=2f+e*13f*(1f-abs(i-5)/7f);paint.color=0xff72e0e9.toInt();canvas.drawRoundRect(531f+i*8,477f-h,535f+i*8,477f,2f,2f,paint)}
        textPaint.textSize=11f;textPaint.color=Color.WHITE
        val caption=if(speaking)"SHOKU PO FLET" else status
        canvas.drawText(TextUtils.ellipsize(caption,textPaint,219f,TextUtils.TruncateAt.END).toString(),571f,499f,textPaint)
    }
}

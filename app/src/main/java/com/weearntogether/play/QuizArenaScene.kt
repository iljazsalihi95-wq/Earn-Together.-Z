package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

/** The actual round drives the studio display, clock and presenter gestures. */
class QuizArenaScene(context:Context):View(context){
    var reaction=0
        set(value){field=value;reactionStarted=android.os.SystemClock.uptimeMillis();invalidate()}
    var speaking=false
        set(value){if(value&&!field)speechStarted=android.os.SystemClock.uptimeMillis();field=value;invalidate()}
    var speechEnergy:(()->Float)?=null
    var suitColor=0xff273b68.toInt();var shirtColor=Color.WHITE;var tieColor=0xffc99b4e.toInt()
    var phoneState="";var friendSpeaking=false
    var playerName="LOJTARI";var portrait:Bitmap?=null
    var playerSpeaking=false
    var playerSpeechEnergy:(()->Float)?=null
    var questionPrompt="";var questionCategory="";var questionNumber=""
    var timerText="";var stageStatus="";var remainingFraction=1f
    private var speechStarted=0L;private var reactionStarted=0L
    private val presenter=ArenaPresenter(context)
    private val playerActor=ArenaSeatedActor(context)
    private val phoneParticipant=ArenaPhoneParticipant()
    private val p=Paint(3);private val textPaint=TextPaint(3);private val namePaint=TextPaint(3);private var attached=false
    private var layoutText="";private var questionLayout:StaticLayout?=null
    private val audience=StudioAssets.get(context,"audience");private val studio=StudioAssets.get(context,"arena")
    override fun onAttachedToWindow(){super.onAttachedToWindow();attached=true}
    override fun onDetachedFromWindow(){attached=false;super.onDetachedFromWindow()}
    private fun promptLayout():StaticLayout{
        if(questionLayout==null||layoutText!=questionPrompt){
            layoutText=questionPrompt;textPaint.color=Color.WHITE;textPaint.typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
            var size=34f;var layout:StaticLayout
            do{textPaint.textSize=size;layout=StaticLayout.Builder.obtain(questionPrompt,0,questionPrompt.length,textPaint,610).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(2f,1f).build();size-=2f}while(layout.height>172&&size>=24f)
            questionLayout=layout
        }
        return questionLayout!!
    }
    var audienceVotes:IntArray?=null
    private var camera=0f
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        val save=c.save();val scale=minOf(width/720f,height/560f)
        c.translate((width-720f*scale)/2,(height-560f*scale)/2);c.scale(scale,scale)
        val time=android.os.SystemClock.uptimeMillis()
        val energy=if(speaking)(speechEnergy?.invoke()?:0f).coerceIn(0f,1f) else 0f
        camera+=((if(audienceVotes!=null)1f else 0f)-camera)*.12f
        // One continuous stage: rear LED display, floor, two chairs, front audience.
        c.drawBitmap(studio,null,RectF(0f,0f,720f,560f),p)
        p.shader=LinearGradient(0f,0f,0f,560f,0x66071025,0xe6071025.toInt(),Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,720f,560f,p);p.shader=null
        drawStudioLights(c,time)
        c.save();c.translate(0f,-camera*90f);c.scale(1f+camera*.12f,1f+camera*.12f,360f,500f)
        p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=10f;p.color=0xffffd782.toInt()
        c.drawText("WE EARN TOGETHER  •  QUIZ ARENA PREMIUM",360f,15f,p)
        p.color=0xff080e20.toInt();c.drawRoundRect(51f,25f,669f,283f,12f,12f,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=4f;p.color=0xff8abed5.toInt();c.drawRoundRect(51f,25f,669f,283f,12f,12f,p);p.style=Paint.Style.FILL
        p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=17f;p.color=0xffe3be7b.toInt()
        c.drawText(questionCategory.uppercase().take(48),360f,55f,p)
        val prompt=promptLayout();c.save();c.translate(55f,67f+(172-prompt.height).coerceAtLeast(0)/2);prompt.draw(c);c.restore()
        p.color=0xff233656.toInt();c.drawRoundRect(72f,258f,648f,265f,4f,4f,p)
        p.color=if(remainingFraction<.2f)0xffffa37e.toInt() else 0xff63d9ef.toInt()
        c.drawRoundRect(72f,258f,72f+576f*remainingFraction.coerceIn(0f,1f),265f,4f,4f,p)
        // Perspective floor and practical strip lighting.
        p.color=0xff102841.toInt();c.drawOval(72f,374f,648f,491f,p)
        p.style=Paint.Style.STROKE;p.color=0xff417c9b.toInt();p.strokeWidth=2f;c.drawOval(72f,374f,648f,491f,p);p.style=Paint.Style.FILL
        c.save();c.translate(62f,205f);c.scale(.43f,.43f);drawPlayer(c,time);c.restore()
        c.save();c.translate(438f,205f);c.scale(.43f,.43f);presenter.draw(c,if(friendSpeaking)0f else energy,speaking&&!friendSpeaking,reaction,time);c.restore()
        p.color=0xff0b162b.toInt();c.drawRoundRect(290f,326f,430f,435f,12f,12f,p)
        p.style=Paint.Style.STROKE;p.color=0xffbb995e.toInt();p.strokeWidth=2f;c.drawRoundRect(290f,326f,430f,435f,12f,12f,p);p.style=Paint.Style.FILL
        p.textSize=15f;p.color=0xff8ed8ed.toInt();c.drawText(questionNumber,360f,350f,p)
        p.textSize=if(timerText.length>6)22f else 35f;p.color=if(reaction<0)0xffffa5ad.toInt() else Color.WHITE;c.drawText(timerText,360f,389f,p)
        p.textSize=11f;p.color=0xffe3be7b.toInt();c.drawText(stageStatus.take(29),360f,416f,p)
        nameplate(c,playerName,172f,if(playerSpeaking)0xff72e0e9.toInt() else 0xffffd782.toInt())
        nameplate(c,"PREZANTUESI",548f,if(speaking&&!friendSpeaking)0xff72e0e9.toInt() else 0xffffd782.toInt())
        if(speaking)for(i in 0..8){val h=3f+14f*energy*(1f-abs(i-4)/6f);p.color=0xff67d8ed.toInt();c.drawRoundRect(312f+i*11,444f-h,317f+i*11,444f,2f,2f,p)}
        val audienceFrame=if(reaction>0&&(time/320)%2==1L)1 else 0
        c.drawBitmap(audience,Rect(0,audienceFrame*audience.height/2,audience.width,(audienceFrame+1)*audience.height/2),RectF(0f,535f,720f,560f),p)
        c.restore()
        audienceVotes?.let { votes ->
            p.color=0xf00a152b.toInt();c.drawRoundRect(80f,302f,640f,485f,14f,14f,p)
            p.textSize=18f;p.color=0xffe3be7b.toInt();c.drawText("KAMERA E PUBLIKUT • SIMULIM",360f,331f,p)
            val rowHeight=minOf(29f,123f/votes.size.coerceAtLeast(1))
            votes.forEachIndexed { i,v ->
                val y=351f+i*rowHeight;p.color=0xff304563.toInt();c.drawRoundRect(160f,y,565f,y+18,4f,4f,p)
                p.color=0xff37acc5.toInt();c.drawRoundRect(160f,y,160f+405f*v/100,y+18,4f,4f,p)
                p.color=Color.WHITE;p.textSize=16f;c.drawText("${'A'+i}",133f,y+15,p);c.drawText("$v%",605f,y+15,p)
            }
            // Boom microphone in the audience camera shot.
            p.color=0xffa7b5c5.toInt();p.strokeWidth=5f;c.drawLine(37f,560f,77f,450f,p);c.drawLine(77f,450f,127f,426f,p)
            p.color=0xff202735.toInt();c.drawRoundRect(115f,412f,160f,435f,10f,10f,p)
        }
        if(phoneState.isNotEmpty()){
            phoneParticipant.draw(c,phoneState,friendSpeaking&&speaking,energy,time)
        }
        if(reaction>0&&(time-reactionStarted)<4000)drawFireworks(c,time-reactionStarted)
        c.restoreToCount(save)
        if(attached&&windowVisibility==VISIBLE)postInvalidateDelayed(if(speaking||playerSpeaking||audienceVotes!=null)40 else 80)
    }
    private fun nameplate(c:Canvas,name:String,center:Float,color:Int){
        p.color=0xe6111d39.toInt();c.drawRoundRect(center-113f,510f,center+113f,534f,7f,7f,p)
        namePaint.textSize=12f;namePaint.color=color;namePaint.typeface=Typeface.DEFAULT_BOLD;namePaint.textAlign=Paint.Align.CENTER
        c.drawText(android.text.TextUtils.ellipsize(name,namePaint,208f,android.text.TextUtils.TruncateAt.END).toString(),center,527f,namePaint)
    }
    private fun drawStudioLights(c:Canvas,time:Long){
        val sway=sin(time/3100.0).toFloat()*26f
        for(i in 0..3){
            val x=35f+i*216f;val tint=if(i%2==0)0x346dafff else 0x349e6dff
            p.shader=LinearGradient(x,0f,x+sway,490f,tint,Color.TRANSPARENT,Shader.TileMode.CLAMP)
            val beam=Path().apply{moveTo(x-7f,0f);lineTo(x+7f,0f);lineTo(x+95f+sway,490f);lineTo(x-95f+sway,490f);close()}
            c.drawPath(beam,p);p.shader=null
        }
    }
    private fun drawFireworks(c:Canvas,elapsed:Long){
        val colors=intArrayOf(0xffffd979.toInt(),0xff65e2ef.toInt(),0xffd9a6ff.toInt(),Color.WHITE)
        for(burst in 0..5){
            val age=elapsed-burst*380L
            if(age !in 0..1750)continue
            val t=age/1750f;val radius=12f+110f*t
            val cx=70f+(burst*137%590);val cy=50f+(burst*79%320)
            p.alpha=((1f-t)*255).toInt();p.strokeWidth=2.5f
            for(ray in 0..23){
                val angle=ray*Math.PI/12+burst
                p.color=colors[(burst+ray)%colors.size];p.alpha=((1f-t)*255).toInt()
                val dx=kotlin.math.cos(angle).toFloat();val dy=kotlin.math.sin(angle).toFloat()
                val fall=70f*t*t
                c.drawLine(cx+dx*radius*.72f,cy+dy*radius*.72f+fall,cx+dx*radius,cy+dy*radius+fall,p)
                c.drawCircle(cx+dx*radius,cy+dy*radius+fall,2.5f*(1f-t)+.5f,p)
            }
        }
        p.alpha=255
    }
    private fun drawPlayer(c:Canvas,time:Long){
        val amplitude=if(playerSpeaking)(playerSpeechEnergy?.invoke()?:0f).coerceIn(0f,1f) else 0f
        playerActor.draw(c,portrait,playerName,amplitude,time,suitColor,shirtColor,tieColor)
    }
}

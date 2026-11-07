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
    private val p=Paint(3);private val textPaint=TextPaint(3);private var attached=false
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
        c.save();c.translate(0f,-camera*90f);c.scale(1f+camera*.12f,1f+camera*.12f,360f,500f)
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
        c.save();c.translate(105f,288f);c.scale(.68f,.68f);chair(c,129f);drawPlayer(c,time);c.restore()
        c.save();c.translate(423f,288f);c.scale(.68f,.68f);chair(c,100f);presenter.draw(c,if(friendSpeaking)0f else energy,speaking&&!friendSpeaking,reaction,time);c.restore()
        p.color=0xff0b162b.toInt();c.drawRoundRect(264f,326f,456f,435f,12f,12f,p)
        p.style=Paint.Style.STROKE;p.color=0xffbb995e.toInt();p.strokeWidth=2f;c.drawRoundRect(264f,326f,456f,435f,12f,12f,p);p.style=Paint.Style.FILL
        p.textSize=15f;p.color=0xff8ed8ed.toInt();c.drawText(questionNumber,360f,350f,p)
        p.textSize=if(timerText.length>6)22f else 35f;p.color=if(reaction<0)0xffffa5ad.toInt() else Color.WHITE;c.drawText(timerText,360f,389f,p)
        p.textSize=11f;p.color=0xffe3be7b.toInt();c.drawText(stageStatus.take(29),360f,416f,p)
        p.textSize=13f;c.drawText(playerName.take(23),193f,466f,p);c.drawText("PREZANTUESI",491f,466f,p)
        if(speaking)for(i in 0..8){val h=3f+14f*energy*(1f-abs(i-4)/6f);p.color=0xff67d8ed.toInt();c.drawRoundRect(312f+i*11,444f-h,317f+i*11,444f,2f,2f,p)}
        val audienceFrame=if(reaction>0&&(time/320)%2==1L)1 else 0
        c.drawBitmap(audience,Rect(0,audienceFrame*audience.height/2,audience.width,(audienceFrame+1)*audience.height/2),RectF(0f,483f,720f,560f),p)
        c.restore()
        audienceVotes?.let { votes ->
            p.color=0xf00a152b.toInt();c.drawRoundRect(80f,302f,640f,485f,14f,14f,p)
            p.textSize=18f;p.color=0xffe3be7b.toInt();c.drawText("KAMERA E PUBLIKUT • SIMULIM",360f,331f,p)
            votes.forEachIndexed { i,v ->
                val y=351f+i*29f;p.color=0xff304563.toInt();c.drawRoundRect(160f,y,565f,y+18,4f,4f,p)
                p.color=0xff37acc5.toInt();c.drawRoundRect(160f,y,160f+405f*v/100,y+18,4f,4f,p)
                p.color=Color.WHITE;p.textSize=16f;c.drawText("${'A'+i}",133f,y+15,p);c.drawText("$v%",605f,y+15,p)
            }
            // Boom microphone in the audience camera shot.
            p.color=0xffa7b5c5.toInt();p.strokeWidth=5f;c.drawLine(37f,560f,77f,450f,p);c.drawLine(77f,450f,127f,426f,p)
            p.color=0xff202735.toInt();c.drawRoundRect(115f,412f,160f,435f,10f,10f,p)
        }
        if(phoneState.isNotEmpty()){
            p.color=0xf00a152b.toInt();c.drawRoundRect(170f,288f,550f,339f,12f,12f,p)
            p.color=0xff8ee3e9.toInt();p.textSize=18f;c.drawText("☎  "+phoneState.take(28),360f,319f,p)
        }
        if(reaction>0&&(time-reactionStarted)<4000)drawFireworks(c,time-reactionStarted)
        c.restoreToCount(save)
        if(attached&&windowVisibility==VISIBLE)postInvalidateDelayed(if(speaking||playerSpeaking||audienceVotes!=null)40 else 80)
    }
    private fun chair(c:Canvas,x:Float){
        p.color=0xff050914.toInt();c.drawOval(x-105,255f,x+110,283f,p)
        p.shader=LinearGradient(x-85,90f,x+85,210f,0xff263b56.toInt(),0xff080f20.toInt(),Shader.TileMode.CLAMP)
        c.drawRoundRect(x-91,65f,x+91,226f,32f,32f,p);c.drawRoundRect(x-105,179f,x+105,230f,16f,16f,p);p.shader=null
        p.color=0xff1b263a.toInt();c.drawRoundRect(x-46,200f,x-7,270f,12f,12f,p);c.drawRoundRect(x+7,200f,x+46,270f,12f,12f,p)
        p.color=0xff080c16.toInt();c.drawRoundRect(x-53,258f,x-4,275f,7f,7f,p);c.drawRoundRect(x+4,258f,x+53,275f,7f,7f,p)
        p.color=0xff9ba6ad.toInt();c.drawRect(x-4,229f,x+4,278f,p);c.drawRect(x-70,276f,x+70,282f,p)
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
        p.shader=LinearGradient(72f,111f,186f,161f,suitColor,0xff142039.toInt(),Shader.TileMode.CLAMP)
        c.drawRoundRect(48f,107f,208f,184f,24f,24f,p);p.shader=null
        p.color=shirtColor;val shirt=Path().apply{moveTo(106f,108f);lineTo(129f,157f);lineTo(153f,108f);close()};c.drawPath(shirt,p)
        p.color=tieColor;val tie=Path().apply{moveTo(125f,113f);lineTo(133f,113f);lineTo(138f,143f);lineTo(129f,154f);lineTo(120f,143f);close()};c.drawPath(tie,p)
        val face=RectF(68f,8f,187f,131f);val clip=Path().apply{addOval(face,Path.Direction.CW)}
        c.save();c.clipPath(clip)
        val amplitude=if(playerSpeaking)(playerSpeechEnergy?.invoke()?:.35f).coerceIn(0f,1f) else 0f
        val nod=if(playerSpeaking)kotlin.math.sin(time/145.0).toFloat()*amplitude*2f else 0f
        c.translate(0f,nod)
        val photo=portrait
        if(photo!=null){val size=minOf(photo.width,photo.height);c.drawBitmap(photo,Rect((photo.width-size)/2,(photo.height-size)/2,(photo.width+size)/2,(photo.height+size)/2),face,p)}
        else{p.color=0xff376999.toInt();c.drawRect(face,p);p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=42f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(playerName.take(1).uppercase(),129f,93f,p)}
        c.restore();p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.color=0xffe3bc74.toInt();c.drawOval(face,p);p.style=Paint.Style.FILL
    }

}

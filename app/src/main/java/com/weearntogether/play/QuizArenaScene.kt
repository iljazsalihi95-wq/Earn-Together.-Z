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
    var playerName="LOJTARI";var portrait:Bitmap?=null
    var questionPrompt="";var questionCategory="";var questionNumber=""
    var timerText="";var stageStatus="";var remainingFraction=1f
    private var speechStarted=0L;private var reactionStarted=0L
    private val p=Paint(3);private val textPaint=TextPaint(3);private var attached=false
    private var layoutText="";private var questionLayout:StaticLayout?=null
    private val audience=StudioAssets.get(context,"audience");private val studio=StudioAssets.get(context,"arena");private val host=StudioAssets.get(context,"host")
    override fun onAttachedToWindow(){super.onAttachedToWindow();attached=true}
    override fun onDetachedFromWindow(){attached=false;super.onDetachedFromWindow()}
    private fun promptLayout():StaticLayout{
        if(questionLayout==null||layoutText!=questionPrompt){
            layoutText=questionPrompt;textPaint.color=Color.WHITE;textPaint.typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
            var size=40f;var layout:StaticLayout
            do{textPaint.textSize=size;layout=StaticLayout.Builder.obtain(questionPrompt,0,questionPrompt.length,textPaint,610).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(2f,1f).build();size-=2f}while(layout.height>190&&size>=26f)
            questionLayout=layout
        }
        return questionLayout!!
    }
    override fun onDraw(c:Canvas){super.onDraw(c);val save=c.save();c.scale(width/720f,height/560f);val time=android.os.SystemClock.uptimeMillis();val phase=time/650.0
        c.drawBitmap(studio,Rect(0,(studio.height*.068f).toInt(),studio.width,(studio.height*.776f).toInt()),RectF(0f,0f,720f,480f),p)
        p.shader=RadialGradient((360+140*sin(phase)).toFloat(),80f,480f,intArrayOf(if(reaction<0)0x446f1636 else if(reaction>0)0x446f5f16 else 0x4446bfff,Color.TRANSPARENT),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,720f,480f,p);p.shader=null
        val speechTime=(time-speechStarted).coerceAtLeast(0);val reactionTime=(time-reactionStarted).coerceAtLeast(0)
        val talkSequence=intArrayOf(0,1,6,1,0,7,1,6)
        val frame=when{speaking->talkSequence[((speechTime/310)%talkSequence.size).toInt()];reaction>0->if((reactionTime/260)%2==0L)3 else 2;reaction<0->if((reactionTime/430)%2==0L)5 else 7;else->if((time/2200)%2==0L)2 else 7}
        val src=Rect((frame%4)*host.width/4,(frame/4)*host.height/2,(frame%4+1)*host.width/4,(frame/4+1)*host.height/2)
        val hostBounce=when{speaking->sin(speechTime/180.0)*4;reaction>0->-abs(sin(reactionTime/170.0))*7;else->sin(phase*2)}.toFloat()
        val hostTilt=when{speaking->sin(speechTime/260.0)*1.8;reaction>0->sin(reactionTime/190.0)*2.4;reaction<0->sin(reactionTime/520.0)*-.8;else->0.0}.toFloat()
        c.save();c.clipRect(496f,0f,692f,186f);c.rotate(hostTilt,594f,168f);c.translate(0f,hostBounce);c.drawBitmap(host,src,RectF(501f,2f,686f,236f),p);c.restore()
        val photo=portrait
        if(photo!=null){val r=RectF(75f,25f,183f,133f);p.color=Color.WHITE;c.drawRoundRect(70f,20f,188f,138f,20f,20f,p);c.save();val clip=Path().apply{addRoundRect(r,15f,15f,Path.Direction.CW)};c.clipPath(clip);val size=minOf(photo.width,photo.height);c.drawBitmap(photo,Rect((photo.width-size)/2,(photo.height-size)/2,(photo.width+size)/2,(photo.height+size)/2),r,p);c.restore()}
        else{p.color=0xff4a91df.toInt();c.drawCircle(129f,78f,43f,p);p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=44f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(playerName.take(1).uppercase(),129f,94f,p)}
        p.color=0xe60a1b35.toInt();c.drawRoundRect(40f,147f,218f,180f,10f,10f,p);c.drawRoundRect(500f,147f,688f,180f,10f,10f,p);p.textAlign=Paint.Align.CENTER;p.textSize=17f;p.typeface=Typeface.DEFAULT_BOLD;p.color=0xffffd77e.toInt();c.drawText(playerName.take(21),129f,170f,p);c.drawText(if(speaking)"PREZANTUESI • LIVE" else if(reaction>0)"MASHALLAH • SAKTË" else if(reaction<0)"PROVO PËRSËRI" else "PREZANTUESI",594f,170f,p)
        p.shader=LinearGradient(210f,10f,510f,180f,0xff142958.toInt(),0xff191039.toInt(),Shader.TileMode.CLAMP);c.drawRoundRect(210f,8f,510f,182f,28f,28f,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=if(reaction>0)0xffffe59a.toInt() else 0xffffd77e.toInt();c.drawRoundRect(210f,8f,510f,182f,28f,28f,p);p.style=Paint.Style.FILL
        p.textSize=22f;p.color=0xff80ddff.toInt();c.drawText(questionNumber,360f,44f,p);p.textSize=if(timerText.length>6)29f else 58f;p.color=if(reaction<0)0xffffb0b5.toInt() else if(reaction>0)0xffffe59a.toInt() else Color.WHITE;c.drawText(timerText,360f,112f,p)
        p.textSize=18f;p.color=0xffffd77e.toInt();c.drawText(stageStatus.take(28),360f,148f,p)
        if(speaking)for(i in 0..8){p.color=if(i%2==0)0xff73dbff.toInt() else 0xffffd77e.toInt();val h=(7+18*abs(sin(speechTime/120.0+i*.72))).toFloat();c.drawRoundRect(294f+i*15,171f-h,301f+i*15,171f,3f,3f,p)}
        p.shader=LinearGradient(0f,190f,720f,480f,0xf5162854.toInt(),0xfa18112e.toInt(),Shader.TileMode.CLAMP);c.drawRoundRect(24f,194f,696f,475f,28f,28f,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=2.5f;p.color=if(reaction>0)0xffffd77e.toInt() else 0xff67c7ff.toInt();c.drawRoundRect(24f,194f,696f,475f,28f,28f,p);p.style=Paint.Style.FILL
        p.textSize=21f;p.color=0xffffd77e.toInt();c.drawText(questionCategory.uppercase().take(43),360f,228f,p)
        val prompt=promptLayout();c.save();c.translate(55f,251f+(190-prompt.height).coerceAtLeast(0)/2);prompt.draw(c);c.restore()
        p.color=0xff304669.toInt();c.drawRoundRect(48f,453f,672f,462f,5f,5f,p);p.color=if(remainingFraction<.2f)0xffffac6f.toInt() else 0xff77d7ff.toInt();c.drawRoundRect(48f,453f,48f+624f*remainingFraction.coerceIn(0f,1f),462f,5f,5f,p)
        if(reaction>0)for(i in 0..52){val x=((i*83)%700+10).toFloat();val y=((phase*92+i*31)%550).toFloat();p.color=when(i%3){0->0xffffd979.toInt();1->0xff65e2ef.toInt();else->0xffd9a6ff.toInt()};c.drawCircle(x,y,if(i%4==0)4.5f else 2.8f,p)}
        val audienceFrame=if(reaction>0&&(time/220)%2==1L)1 else 0;c.drawBitmap(audience,Rect(0,audienceFrame*audience.height/2,audience.width,(audienceFrame+1)*audience.height/2),RectF(0f,480f,720f,560f),p)
        c.restoreToCount(save);if(attached&&windowVisibility==VISIBLE)postInvalidateDelayed(if(speaking||reaction!=0)32 else 55)
    }
}

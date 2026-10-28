package com.weearntogether.play

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/** Studio compositing uses HD rendered assets, expression frames and live scene effects. */
class QuizArenaScene(context:Context):View(context){
    var reaction=0;var speaking=false;var playerName="LOJTARI";var portrait:Bitmap?=null
    private val p=Paint(3);private var attached=false
    private val audience=StudioAssets.get(context,"audience");private val studio=StudioAssets.get(context,"arena");private val host=StudioAssets.get(context,"host")
    override fun onAttachedToWindow(){super.onAttachedToWindow();attached=true}
    override fun onDetachedFromWindow(){attached=false;super.onDetachedFromWindow()}
    override fun onDraw(c:Canvas){super.onDraw(c);val save=c.save();c.scale(width/720f,height/340f);val time=android.os.SystemClock.uptimeMillis();val phase=time/650.0
        c.drawBitmap(studio,Rect(0,(studio.height*.068f).toInt(),studio.width,(studio.height*.776f).toInt()),RectF(0f,0f,720f,340f),p)
        val frame=when{reaction>0->3;reaction<0->5;speaking->if((time/220)%2==0L)1 else 6;else->if((time/2600)%2==0L)2 else 7}
        val src=Rect((frame%4)*host.width/4,(frame/4)*host.height/2,(frame%4+1)*host.width/4,(frame/4+1)*host.height/2)
        c.save();c.clipRect(495f,4f,645f,142f);c.drawBitmap(host,src,RectF(495f,4f,645f,213f),p);c.restore()
        val photo=portrait
        if(photo!=null){val r=RectF(75f,35f,183f,146f);p.color=Color.WHITE;c.drawRoundRect(70f,30f,188f,151f,20f,20f,p);c.save();val clip=Path().apply{addRoundRect(r,15f,15f,Path.Direction.CW)};c.clipPath(clip);val size=minOf(photo.width,photo.height);c.drawBitmap(photo,Rect((photo.width-size)/2,(photo.height-size)/2,(photo.width+size)/2,(photo.height+size)/2),r,p);c.restore()}
        else {p.color=0xff4a91df.toInt();c.drawCircle(129f,93f,43f,p);p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=44f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(playerName.take(1).uppercase(),129f,109f,p)}
        p.color=0xe60a1b35.toInt();c.drawRoundRect(43f,216f,225f,250f,10f,10f,p);c.drawRoundRect(492f,216f,674f,250f,10f,10f,p);p.textAlign=Paint.Align.CENTER;p.textSize=16f;p.typeface=Typeface.DEFAULT_BOLD;p.color=0xffffd77e.toInt();c.drawText(playerName.take(21),134f,239f,p);c.drawText("PREZANTUESI",583f,239f,p)
        if(reaction>0)for(i in 0..38){val x=((i*83)%700+10).toFloat();val y=((phase*75+i*31)%410).toFloat();p.color=if(i%2==0)0xffffd979.toInt() else 0xff65e2ef.toInt();c.drawRect(x,y,x+4,y+9,p)}
        val audienceFrame=if(reaction>0&&(time/280)%2==1L)1 else 0
        c.drawBitmap(audience,Rect(0,audienceFrame*audience.height/2,audience.width,(audienceFrame+1)*audience.height/2),RectF(0f,260f,720f,340f),p)
        c.restoreToCount(save);if(attached&&windowVisibility==VISIBLE)postInvalidateDelayed(40)
    }
}

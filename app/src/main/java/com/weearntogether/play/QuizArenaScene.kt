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
        set(value){field=value;invalidate()}
    var speechEnergy:(()->Float)?=null
    var suitColor=0xff273b68.toInt();var shirtColor=Color.WHITE;var tieColor=0xffc99b4e.toInt()
    var phoneState="";var friendSpeaking=false
    var playerName="LOJTARI";var portrait:Bitmap?=null
    var playerSpeaking=false
    var playerSpeechEnergy:(()->Float)?=null
    var questionPrompt="";var questionCategory="";var questionNumber=""
    var alternatives: List<String> = emptyList()
    var selectedChoice = -1
        set(value) { field = value; invalidate() }
    var correctChoice = -1
    var timerText="";var stageStatus="";var remainingFraction=1f
private var reactionStarted=0L
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
            var size=30f;var layout:StaticLayout
            do{textPaint.textSize=size;layout=StaticLayout.Builder.obtain(questionPrompt,0,questionPrompt.length,textPaint,610).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(2f,1f).build();size-=2f}while(layout.height>106&&size>=18f)
            questionLayout=layout
        }
        return questionLayout!!
    }
    var audienceVotes:IntArray?=null
    private var camera=0f
    private var framing=0f
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        val save=c.save();val scale=minOf(width/720f,height/560f)
        c.translate((width-720f*scale)/2,(height-560f*scale)/2);c.scale(scale,scale)
        val time=android.os.SystemClock.uptimeMillis()
        val energy=if(speaking)(speechEnergy?.invoke()?:0f).coerceIn(0f,1f) else 0f
        val target = when {
            audienceVotes != null -> -.7f
            phoneState.isNotEmpty() -> -.3f
            speaking -> 1f
            reaction != 0 -> -.45f
            else -> 0f
        }
        camera += (target - camera) * .09f
        framing += ((if (target == 0f) 0f else 1f) - framing) * .09f
        // One continuous stage: rear LED display, floor, two chairs, front audience.
        c.drawBitmap(studio,null,RectF(0f,0f,720f,560f),p)
        p.shader=LinearGradient(0f,0f,0f,560f,0x66071025,0xe6071025.toInt(),Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,720f,560f,p);p.shader=null
        drawStudioLights(c,time)
        c.save();c.translate(-camera * 18f, 0f);c.scale(1f+framing*.035f,1f+framing*.035f,360f,360f)
        p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=10f;p.color=0xffffd782.toInt()
        c.drawText("WE EARN TOGETHER  •  QUIZ ARENA PREMIUM",360f,15f,p)
        p.color=0xff080e20.toInt();c.drawRoundRect(51f,25f,669f,283f,12f,12f,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=4f;p.color=0xff8abed5.toInt();c.drawRoundRect(51f,25f,669f,283f,12f,12f,p);p.style=Paint.Style.FILL
        p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=17f;p.color=0xffe3be7b.toInt()
        c.drawText(questionCategory.uppercase().take(48),360f,55f,p)
        val prompt = promptLayout()
        c.save()
        c.clipRect(55f, 65f, 665f, 171f)
        c.translate(55f, 65f + (106 - prompt.height).coerceAtLeast(0) / 2)
        prompt.draw(c)
        c.restore()
        drawAlternatives(c, time)
        p.color=0xff233656.toInt();c.drawRoundRect(72f,258f,648f,265f,4f,4f,p)
        p.color=if(remainingFraction<.2f)0xffffa37e.toInt() else 0xff63d9ef.toInt()
        c.drawRoundRect(72f,258f,72f+576f*remainingFraction.coerceIn(0f,1f),265f,4f,4f,p)
        // Perspective floor and practical strip lighting.
        p.color=0xff102841.toInt();c.drawOval(72f,374f,648f,491f,p)
        p.style=Paint.Style.STROKE;p.color=0xff417c9b.toInt();p.strokeWidth=2f;c.drawOval(72f,374f,648f,491f,p);p.style=Paint.Style.FILL
        c.save();c.translate(73f, 303f);c.scale(.31f,.31f);drawPlayer(c,time);c.restore()
        c.save()
        c.translate(267f, 277f)
        c.scale(.48f, .48f)
        presenter.draw(c, if (friendSpeaking) 0f else energy, time)
        c.restore()
        drawPodium(c)
        nameplate(c,playerName,154f,if(playerSpeaking)0xff72e0e9.toInt() else 0xffffd782.toInt())
        nameplate(c,"PREZANTUESI",360f,if(speaking&&!friendSpeaking)0xff72e0e9.toInt() else 0xffffd782.toInt())
        if(speaking && !friendSpeaking)for(i in 0..8){val h=3f+14f*energy*(1f-abs(i-4)/6f);p.color=0xff67d8ed.toInt();c.drawRoundRect(312f+i*11,498f-h,317f+i*11,498f,2f,2f,p)}
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
    private fun drawAlternatives(canvas: Canvas, time: Long) {
        textPaint.textSize = 17f
        for ((index, value) in alternatives.withIndex()) {
            val x = 72f + index % 2 * 292f
            val y = 175f + index / 2 * 40f
            val pulse = ((1f + sin(time / 135.0).toFloat()) * 20f).toInt()
            p.color = when (index) {
                correctChoice -> Color.rgb(12, 110 + pulse, 91)
                selectedChoice -> if (correctChoice < 0) 0xff80542d.toInt() else 0xff843650.toInt()
                else -> 0xff222849.toInt()
            }
            canvas.drawRoundRect(x, y, x + 284f, y + 33f, 7f, 7f, p)
            p.textAlign = Paint.Align.LEFT
            p.textSize = 17f
            p.color = 0xffffd782.toInt()
            canvas.drawText("${'A'+index}", x + 11f, y + 22f, p)
            p.color = Color.WHITE
            canvas.drawText(android.text.TextUtils.ellipsize(value, textPaint, 242f,
                android.text.TextUtils.TruncateAt.END).toString(), x + 36f, y + 22f, p)
        }
        p.textAlign = Paint.Align.CENTER
    }
    private fun drawPodium(canvas: Canvas) {
        p.color = 0xffd6b56d.toInt()
        p.strokeWidth = 4f
        canvas.drawLine(409f, 410f, 387f, 365f, p)
        p.color = 0xff142135.toInt()
        canvas.drawRoundRect(373f, 358f, 394f, 369f, 5f, 5f, p)
        p.shader = LinearGradient(280f, 415f, 446f, 521f,
            intArrayOf(0xff284d87.toInt(), 0xff141b42.toInt(), 0xff41256e.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(282f, 411f, 438f, 510f, 10f, 10f, p)
        p.shader = null
        p.color = 0xffd6b56d.toInt()
        canvas.drawRoundRect(278f, 409f, 442f, 414f, 2f, 2f, p)
        p.textSize = 11f
        canvas.drawText(questionNumber, 360f, 433f, p)
        p.color = Color.WHITE
        p.textSize = if (timerText.length > 6) 17f else 27f
        canvas.drawText(timerText, 360f, 461f, p)
        p.textSize = 9f
        p.color = 0xffe3be7b.toInt()
        canvas.drawText(stageStatus.take(29), 360f, 478f, p)
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

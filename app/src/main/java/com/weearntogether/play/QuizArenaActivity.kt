package com.weearntogether.play

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.graphics.*
import android.view.*
import android.media.ToneGenerator
import android.media.AudioManager
import java.util.Locale
import kotlin.math.min

/** Native, resolution-independent Quiz Arena gameplay. No WebView. */
class QuizArenaActivity : Activity(), TextToSpeech.OnInitListener {
    data class Question(val category:String,val prompt:String,val choices:List<String>,val correct:Int,val why:String)
    private val bank=listOf(
        Question("KURANI","Sa sure ka Kurani?",listOf("112","114","116","110"),1,"Kurani përmban 114 sure."),
        Question("HADITHI","Cili dijetar përmblodhi Sahih el-Buhariun?",listOf("Ibn Sina","El-Buhariu","El-Idrisi","El-Havarizmi"),1,"Imam el-Buhariu është përmbledhësi i Sahih el-Buhariut."),
        Question("FIKHU","Sa rekate farz ka namazi i sabahut?",listOf("2","3","4","5"),0,"Sabahu ka dy rekate farz."),
        Question("TEFSIRI","Çfarë do të thotë El-Felek në suren 113?",listOf("Agimi","Mali","Deti","Nata"),0,"El-Felek nënkupton agimin."),
        Question("HISTORI ISLAME","Ku emigroi Profeti Muhamed ﷺ nga Meka?",listOf("Taif","Medinë","Kufe","Basra"),1,"Hixhreti u bë nga Meka në Medinë."),
        Question("GJEOGRAFI","Cili është kryeqyteti i Kanadasë?",listOf("Toronto","Ottawa","Montreal","Vankuver"),1,"Ottawa është kryeqyteti i Kanadasë."),
        Question("SHKENCË","Cili planet është më afër Diellit?",listOf("Toka","Venusi","Mërkuri","Marsi"),2,"Mërkuri është më afër Diellit."),
        Question("MATEMATIKË","Sa është 12 × 8?",listOf("86","96","108","88"),1,"12 × 8 = 96."),
        Question("HISTORI","Në cilin vit ra Muri i Berlinit?",listOf("1987","1989","1991","1993"),1,"Muri i Berlinit u hap më 9 nëntor 1989."),
        Question("GJUHË","Si thuhet libër në gjermanisht?",listOf("Haus","Buch","Baum","Stuhl"),1,"Buch do të thotë libër.")
    )
    private var index=0; private var score=0; private var seconds=60
    private var choosing=true; private var categoryMenu=false; private var chosenCategory="TË GJITHA"; private var chosenLanguage="Shqip"
    private val languages=listOf("Shqip","Deutsch","English","Italiano","Türkçe","Français")
    private val categories=listOf("TË GJITHA","KURANI","HADITHI","FIKHU","TEFSIRI","HISTORI ISLAME","GJEOGRAFI","SHKENCË","MATEMATIKË","HISTORI","GJUHË")
    private var session=bank
    private var selected=-1; private var finished=false; private var removed=setOf<Int>(); private var helpUsed=false
    private var timer:CountDownTimer?=null; private var tts:TextToSpeech?=null
    private val sound=ToneGenerator(AudioManager.STREAM_MUSIC,75)
    private lateinit var stage:Stage
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;stage=Stage();setContentView(stage);tts=TextToSpeech(this,this);stage.invalidate()}
    override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS){tts?.language=Locale("sq");speakCurrent()}}
    private fun speakCurrent(){if(!choosing&&index<session.size)tts?.speak(session[index].prompt,TextToSpeech.QUEUE_FLUSH,null,"quiz-question")}
    private fun startSession(){session=if(chosenCategory=="TË GJITHA")bank.shuffled() else bank.filter{it.category==chosenCategory}.shuffled();if(session.isEmpty())return;choosing=false;index=0;score=0;finished=false;showQuestion()}
    private fun showQuestion(){timer?.cancel();selected=-1;removed=emptySet();helpUsed=false;seconds=60;stage.invalidate();speakCurrent();timer=object:CountDownTimer(60000,1000){override fun onTick(ms:Long){seconds=((ms+999)/1000).toInt();if(seconds<=10&&seconds>0)sound.startTone(ToneGenerator.TONE_PROP_BEEP,65);stage.invalidate()};override fun onFinish(){seconds=0;answer(-1)}}.start()}
    private fun answer(n:Int){if(selected!=-1||finished)return;timer?.cancel();selected=if(n<0)4 else n;if(n==session[index].correct){score+=100+seconds;sound.startTone(ToneGenerator.TONE_PROP_ACK,260)}else sound.startTone(ToneGenerator.TONE_PROP_NACK,350);stage.invalidate()}
    private fun advance(){if(selected==-1)return;if(index==session.lastIndex){finished=true;timer?.cancel()}else{index++;showQuestion()};stage.invalidate()}
    override fun onDestroy(){timer?.cancel();tts?.stop();tts?.shutdown();sound.release();super.onDestroy()}
    inner class Stage:View(this@QuizArenaActivity){
        private val p=Paint(3); private val gold=Color.rgb(255,207,99);private val white=Color.WHITE
        private fun box(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int,rad:Float=22f){p.color=color;p.style=Paint.Style.FILL;c.drawRoundRect(l,t,r,b,rad,rad,p)}
        private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=white,center:Boolean=true){p.color=color;p.textSize=size;p.typeface=Typeface.create("sans-serif",Typeface.BOLD);p.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p)}
        private fun lines(c:Canvas,s:String,x:Float,y:Float,width:Float,size:Float,color:Int=white):Float{val words=s.split(" ");var line="";var yy=y;p.textSize=size;for(w in words){val next=if(line.isEmpty())w else "$line $w";if(p.measureText(next)>width&&line.isNotEmpty()){text(c,line,x,yy,size,color);yy+=size*1.4f;line=w}else line=next};if(line.isNotEmpty())text(c,line,x,yy,size,color);return yy}
        override fun onDraw(raw:Canvas){super.onDraw(raw);val c=raw;val save=c.save();val scale=min(width/420f,height/840f);c.translate((width-420*scale)/2,(height-840*scale)/2);c.scale(scale,scale)
            p.shader=LinearGradient(0f,0f,420f,840f,0xff070f2b.toInt(),0xff30215e.toInt(),Shader.TileMode.CLAMP);c.drawRect(0f,0f,420f,840f,p);p.shader=null
            for(i in 0..7){p.color=0x18ffd782;p.strokeWidth=2f;c.drawLine(i*65f,0f,210f,430f,p)}
            text(c,"✦ QUIZ ARENA ✦",210f,49f,29f,gold)
            if(choosing){text(c,if(categoryMenu)"ZGJIDH FUSHËN" else "ZGJIDH GJUHËN",210f,122f,24f,gold);val opts=if(categoryMenu)categories else languages;opts.forEachIndexed{i,v->val top=145f+i*if(categoryMenu)57f else 90f;box(c,25f,top,395f,top+49f,0xff253d7a.toInt(),16f);text(c,v,210f,top+32f,18f)};if(categoryMenu)text(c,"Prezantuesi: Në cilën fushë do të pyetesh?",210f,810f,14f);c.restoreToCount(save);return}
            box(c,18f,69f,402f,119f,0xff172953.toInt());text(c,"NIVELI ${index+1} / ${session.size}",112f,100f,16f);text(c,"★ $score",321f,100f,19f,gold)
            box(c,26f,138f,394f,322f,0xff101d45.toInt(),32f)
            box(c,43f,160f,145f,270f,0xff304b93.toInt());box(c,275f,160f,377f,270f,0xff5b337f.toInt())
            text(c,"◉",94f,225f,58f,gold);text(c,"◉",326f,225f,58f,gold)
            text(c,"LOJTARI",94f,292f,14f);text(c,"PREZANTUESI",326f,292f,14f)
            box(c,155f,188f,265f,250f,if(seconds<=10)0xffae314c.toInt() else 0xff176b8e.toInt());text(c,"${seconds}s",210f,230f,31f)
            val q=session[index];text(c,q.category,210f,360f,16f,gold)
            box(c,19f,380f,401f,486f,0xff213366.toInt(),25f);lines(c,q.prompt,210f,421f,350f,21f)
            q.choices.forEachIndexed{i,s->val top=504f+i*65f;val color=when{selected!=-1&&i==q.correct->0xff16815d.toInt();selected==i->0xff9d354a.toInt();i in removed->0xff242a43.toInt();else->0xff253d7a.toInt()};box(c,25f,top,395f,top+55,color,17f);text(c,"${'A'+i}. $s",210f,top+35f,18f,if(i in removed)0xff777d94.toInt() else white)}
            if(finished){box(c,20f,382f,400f,807f,0xff132954.toInt());text(c,"SFIDA U PËRFUNDUA!",210f,465f,25f,gold);text(c,"★ $score PIKË",210f,545f,30f);text(c,"Prek për të zgjedhur fushën",210f,645f,17f)}
            else if(selected!=-1){box(c,25f,765f,395f,832f,0xffb38b39.toInt());lines(c,q.why,210f,790f,340f,15f,0xff10162b.toInt());text(c,"PREK PËR PYETJEN TJETËR ›",210f,830f,13f,white)}
            else{box(c,25f,777f,190f,828f,0xffa07835.toInt());text(c,if(helpUsed)"50/50 U PËRDOR" else "✂ 50 / 50",108f,810f,16f);box(c,205f,777f,395f,828f,0xff315b91.toInt());text(c,"🔊 LEXO PYETJEN",300f,810f,15f)}
            c.restoreToCount(save)}
        override fun onTouchEvent(e:android.view.MotionEvent):Boolean{if(e.action!=MotionEvent.ACTION_UP)return true;val scale=min(width/420f,height/840f);val x=(e.x-(width-420*scale)/2)/scale;val y=(e.y-(height-840*scale)/2)/scale
            if(choosing){val opts=if(categoryMenu)categories else languages;val step=if(categoryMenu)57f else 90f;val n=((y-145f)/step).toInt();if(n in opts.indices&&y>=145f+n*step&&y<=194f+n*step){if(categoryMenu){chosenCategory=opts[n];startSession()}else{chosenLanguage=opts[n];categoryMenu=true;tts?.language=when(chosenLanguage){"Deutsch"->Locale.GERMAN;"English"->Locale.ENGLISH;"Italiano"->Locale.ITALIAN;"Türkçe"->Locale("tr");"Français"->Locale.FRENCH;else->Locale("sq")};invalidate()}};return true}
            if(finished){choosing=true;categoryMenu=true;finished=false;invalidate();return true}
            if(selected!=-1){advance();return true}
            if(y in 504f..754f&&x in 25f..395f){val n=((y-504f)/65f).toInt();if(n in 0..3&&n !in removed)answer(n)}
            else if(y in 777f..833f){if(x<195f&&!helpUsed){helpUsed=true;val wrong=(0..3).filter{it!=session[index].correct}.shuffled().take(2);removed=wrong.toSet();invalidate()}else if(x>=195f)speakCurrent()}
            return true}
    }
}

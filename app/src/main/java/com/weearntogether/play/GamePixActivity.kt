package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class GamePixActivity : Activity() {
    data class Game(val title:String,val category:String,val image:String,val url:String)
    private val bg=0xff080e22.toInt(); private val card=0xff141f42.toInt(); private val gold=0xffffce70.toInt()
    private val worker=Executors.newFixedThreadPool(5); private val ui=Handler(Looper.getMainLooper())
    private lateinit var grid:GridLayout; private lateinit var status:TextView; private lateinit var more:Button
    private val games=mutableListOf<Game>(); private var page=1; private var loading=false; private var category="ALL"; private var query=""
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int,r:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(18),dp(16),dp(30));setBackgroundColor(bg)}
        root.addView(TextView(this).apply{text="GAMES";textSize=30f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD)})
        root.addView(TextView(this).apply{text="GamePix • choose a category and play instantly";textSize=13f;setTextColor(0xffaebce0.toInt());setPadding(0,0,0,dp(14))})
        val search=EditText(this).apply{hint="Search games…";setSingleLine();setTextColor(Color.WHITE);setHintTextColor(0xff8290b7.toInt());background=rounded(card,18);setPadding(dp(18),0,dp(18),0)}
        root.addView(search,LinearLayout.LayoutParams(-1,dp(52)).apply{bottomMargin=dp(12)})
        val chips=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf("ALL","PUZZLE","MATCH-3","ARCADE","ACTION","RACING","SPORTS","ADVENTURE","STRATEGY","CASUAL","FARMING","BUILDING","PLATFORM","SHOOTER").forEach{c->chips.addView(Button(this).apply{text=c;isAllCaps=false;textSize=11f;setTextColor(if(c=="ALL")bg else Color.WHITE);background=rounded(if(c=="ALL")gold else card,16);setOnClickListener{category=c;render()}},LinearLayout.LayoutParams(-2,dp(42)).apply{rightMargin=dp(7)})}
        root.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(chips)},LinearLayout.LayoutParams(-1,dp(52)))
        status=TextView(this).apply{text="Loading GamePix catalogue…";textSize=13f;setTextColor(gold);setPadding(0,dp(8),0,dp(10))};root.addView(status)
        grid=GridLayout(this).apply{columnCount=2;alignmentMode=GridLayout.ALIGN_BOUNDS};root.addView(grid)
        more=Button(this).apply{text="LOAD MORE GAMES";isAllCaps=false;setTypeface(null,Typeface.BOLD);setTextColor(bg);background=rounded(gold,18);setOnClickListener{loadPage()}};root.addView(more,LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(14)})
        search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){query=s?.toString()?.trim()?.lowercase()?:"";render()};override fun afterTextChanged(e:Editable?){}})
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)});loadPage() }
    private fun loadPage(){if(loading)return;loading=true;more.isEnabled=false;status.text="Loading GamePix page $page…";val p=page
        worker.execute{try{val conn=(URL("https://feeds.gamepix.com/v2/json?sid=71376&pagination=96&page=$p").openConnection() as HttpURLConnection).apply{connectTimeout=15000;readTimeout=20000};val text=conn.inputStream.bufferedReader().use{it.readText()};val root=JSONObject(text);val arr=when{root.has("items")->root.getJSONArray("items");root.has("games")->root.getJSONArray("games");root.has("data")->root.getJSONArray("data");else->throw IllegalStateException("GamePix feed format changed")};val batch=ArrayList<Game>();for(i in 0 until arr.length()){val o=arr.getJSONObject(i);val play=o.optString("url");if(play.isNotBlank())batch+=Game(o.optString("title","Game"),o.optString("category","other"),o.optString("banner_image",o.optString("image")),play)};ui.post{games+=batch;page++;loading=false;more.isEnabled=batch.isNotEmpty();status.text="${games.size} GamePix games loaded";render()}}catch(e:Exception){ui.post{loading=false;more.isEnabled=true;status.text="Could not load GamePix. Tap LOAD MORE to retry."}}}}
    private fun render(){grid.removeAllViews();val filtered=games.filter{g->(category=="ALL"||g.category.equals(category,true))&&(query.isBlank()||g.title.lowercase().contains(query)||g.category.lowercase().contains(query))};status.text="${filtered.size} shown • ${games.size} loaded";filtered.forEach{addCard(it)}}
    private fun addCard(g:Game){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(10));background=rounded(card,20);isClickable=true;setOnClickListener{play(g)}}
        val image=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;setBackgroundColor(0xff202b50.toInt())};box.addView(image,LinearLayout.LayoutParams(-1,dp(145)))
        box.addView(TextView(this).apply{text=g.title;textSize=15f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);maxLines=2;setPadding(dp(4),dp(9),dp(4),dp(3))})
        box.addView(TextView(this).apply{text=g.category.uppercase();textSize=10f;setTextColor(gold);setPadding(dp(4),0,dp(4),dp(7))})
        box.addView(TextView(this).apply{text="PLAY  ›";textSize=12f;setTextColor(gold);setTypeface(null,Typeface.BOLD);setPadding(dp(4),0,dp(4),0)})
        val width=(resources.displayMetrics.widthPixels-dp(44))/2;grid.addView(box,GridLayout.LayoutParams().apply{this.width=width;height=GridLayout.LayoutParams.WRAP_CONTENT;setMargins(dp(4),dp(5),dp(4),dp(5))});loadImage(g.image,image)}
    private fun loadImage(src:String,target:ImageView){if(src.isBlank())return;worker.execute{try{val b=URL(src).openStream().use{BitmapFactory.decodeStream(it)};ui.post{if(!isFinishing&&!isDestroyed)target.setImageBitmap(b)}}catch(_:Exception){}}}
    private fun play(g:Game){startActivity(Intent(this,GamePixPlayerActivity::class.java).putExtra("url",g.url).putExtra("title",g.title))}
    override fun onDestroy(){worker.shutdownNow();super.onDestroy()}
}

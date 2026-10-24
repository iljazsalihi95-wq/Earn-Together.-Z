package com.weearntogether.play

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import com.weearntogether.lazygarden.LazyGardenActivity

class MainActivity : Activity() {
    private val navy=Color.rgb(6,12,31); private val panel=Color.rgb(17,27,58); private val panel2=Color.rgb(27,39,78); private val purple=Color.rgb(112,67,232); private val blue=Color.rgb(25,126,242); private val gold=Color.rgb(255,198,65)
    private lateinit var root:LinearLayout
    private val loyalty by lazy{LoyaltyProgress(this)}; private val playHistory by lazy{PlayHistory(this)}; private val profile by lazy{getSharedPreferences("wet_guest_profile",MODE_PRIVATE)}; private val accounts by lazy{AccountStore(this)}
    private var levelPlay:LevelPlayManager?=null; private var atHome=false
    private val googleIdentity by lazy { GoogleIdentity(this) }; private var googleInFlight=false
    private var busy=false; private var firstResume=true
    private var cloudMessage=""; private var pendingConflicts=listOf<CloudProgress.Conflict>()
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun enabled(view:View,enable:Boolean){view.isEnabled=enable;if(view is android.view.ViewGroup)for(i in 0 until view.childCount)enabled(view.getChildAt(i),enable)}
    private fun request(status:TextView?=null,work:()->AccountStore.Result,success:()->Unit){
        if(busy)return;busy=true;enabled(root,false);status?.text="Connecting securely…"
        CloudWorker.executor.execute {
            val result=try{work()}catch(e:Exception){AccountStore.Result(false,e.message?:"Request failed. Try again.")}
            runOnUiThread {if(!isFinishing&&!isDestroyed){busy=false;enabled(root,true);if(result.ok)success()else{status?.text=result.message;Toast.makeText(this,result.message,Toast.LENGTH_LONG).show()}}}
        }
    }
    private fun restoreAccount(){screen("PLAYER ACCOUNT");label("Restoring your secure session…");request(work={val result=accounts.restore();cloudMessage=result.message;AccountStore.Result(true,result.message)}){if(accounts.isSignedIn())syncAndHome()else welcome()}}
    private fun syncAndHome(){
        if(busy)return
        if(!accounts.isSignedIn()){welcome();return}
        screen("CLOUD PROGRESS");label("Checking your saves. Your device progress stays safe.");busy=true
        CloudWorker.executor.execute {
            var expired=false
            val result=try{CloudProgress(applicationContext).sync()}catch(e:Exception){expired=e is BackendException&&e.code=="SESSION_EXPIRED";CloudProgress.SyncResult(if(expired)"Please sign in again" else "Offline — device progress saved; cloud sync will retry")}
            runOnUiThread {if(!isFinishing&&!isDestroyed){busy=false;if(expired){accounts.clear();welcome()}else{cloudMessage=result.message;pendingConflicts=result.conflicts;initAds();home();showNextConflict()}}}
        }
    }
    private fun showNextConflict(){
        val conflict=pendingConflicts.firstOrNull()?:return
        AlertDialog.Builder(this).setTitle("${conflict.game}: choose your save")
            .setMessage("Device: level ${conflict.deviceLevel}\nCloud: level ${conflict.cloudLevel}\nBoth copies are backed up. Choose which one to continue.")
            .setPositiveButton("Use device"){_,_->resolveSave(conflict,true)}
            .setNegativeButton("Use cloud"){_,_->resolveSave(conflict,false)}
            .setNeutralButton("Later"){_,_->}.show()
    }
    private fun resolveSave(conflict:CloudProgress.Conflict,useDevice:Boolean){request(work={CloudProgress(applicationContext).resolve(conflict,useDevice);AccountStore.Result(true,"Save selected")}){pendingConflicts=pendingConflicts.drop(1);cloudMessage="Save selected and synced";home();showNextConflict()}}
    override fun onResume(){super.onResume();if(firstResume){firstResume=false}else if(atHome&&!busy&&accounts.isSignedIn())syncAndHome()}
    override fun onStop(){super.onStop();if(!busy&&accounts.isSignedIn()){val app=applicationContext;CloudWorker.executor.execute{try{CloudProgress(app).sync(false)}catch(_:Exception){}}}}

    private fun tr(s:String)=AppLocale.t(s)
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=navy;window.navigationBarColor=navy;window.decorView.systemUiVisibility=0;if(accounts.isSignedIn())restoreAccount()else welcome()}
    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(busy)return;if(accounts.isSignedIn()&&!atHome)syncAndHome() else super.onBackPressed()}
    private fun initAds(){if(levelPlay!=null)return;val id=profile.getString("guest_id",null)?:return;levelPlay=LevelPlayManager(this,id);levelPlay?.initialize()}
    private fun rounded(color:Int,r:Float)=GradientDrawable().apply{setColor(color);cornerRadius=r}
    private fun gradient()=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(purple,blue)).apply{cornerRadius=30f}
    private fun screen(title:String=""){
        atHome=false
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(20),dp(20),dp(24));setBackgroundColor(navy)}
        val scroll=ScrollView(this).apply{isFillViewport=true;setBackgroundColor(navy);addView(root)}
        scroll.setOnApplyWindowInsetsListener{_,insets->root.setPadding(dp(20)+insets.systemWindowInsetLeft,dp(20)+insets.systemWindowInsetTop,dp(20)+insets.systemWindowInsetRight,dp(24)+insets.systemWindowInsetBottom);insets}
        setContentView(scroll);scroll.requestApplyInsets();if(title.isNotEmpty())heading(title,30)
    }
    private fun heading(s:String,size:Int=22,color:Int=Color.WHITE){root.addView(TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);setTypeface(null,Typeface.BOLD);setPadding(3,10,3,10)})}
    private fun label(s:String,size:Int=15,color:Int=0xffbdc8e7.toInt()){root.addView(TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);setPadding(3,5,3,9)})}
    private fun button(label:String,action:()->Unit){root.addView(Button(this).apply{text=label;textSize=16f;isAllCaps=false;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE);background=gradient();minHeight=0;minimumHeight=0;setOnClickListener{action()}},LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(12)})}
    private fun field(hint:String,password:Boolean=false)=EditText(this).apply{this.hint=hint;setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(0xff8794b8.toInt());setPadding(28,18,28,18);background=rounded(panel,28f);if(password)inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD}
    private fun addField(v:EditText){root.addView(v,LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(12)})}
    private fun section(title:String){heading(title,15,gold)}
    private fun authHeader(subtitle:String){heading("WE EARN TOGETHER",30,gold);label(subtitle,17);label("Your account. Your games. Your progress, synced.",14,0xff9cadd8.toInt())}
    private fun welcome(){screen();authHeader("PLAY • PROGRESS • TOGETHER");val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(26,28,26,28);background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff25104f.toInt(),0xff102d65.toInt())).apply{cornerRadius=38f}};card.addView(TextView(this).apply{text="PLAYER ACCOUNT";textSize=14f;setTextColor(gold);setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER});card.addView(TextView(this).apply{text="Create your player account and keep your game saves in the cloud.";textSize=18f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(0,12,0,12)});root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=24});button("CREATE ACCOUNT"){registerScreen()};button("SIGN IN"){loginScreen()}}
    private fun registerScreen(){screen();authHeader("CREATE ACCOUNT");val name=field("Full name");val email=field("Email").apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS};val pass=field("Password (8+ characters)",true);val confirm=field("Confirm password",true);addField(name);addField(email);addField(pass);addField(confirm);val status=TextView(this).apply{setTextColor(0xffff9c9c.toInt());textSize=14f;gravity=Gravity.CENTER;setPadding(8,0,8,14)};root.addView(status);button("CREATE ACCOUNT"){val n=name.text.toString();val e=email.text.toString();val p=pass.text.toString();val c=confirm.text.toString();request(status,{accounts.register(n,e,p,c)}){pass.text.clear();confirm.text.clear();syncAndHome()}};googleButton(status);button("I ALREADY HAVE AN ACCOUNT"){loginScreen()}}
    private fun loginScreen(){screen();authHeader("SIGN IN");val email=field("Email").apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS};val pass=field("Password",true);addField(email);addField(pass);val status=TextView(this).apply{setTextColor(0xffff9c9c.toInt());textSize=14f;gravity=Gravity.CENTER;setPadding(8,0,8,14)};root.addView(status);button("SIGN IN"){val e=email.text.toString();val p=pass.text.toString();request(status,{accounts.login(e,p)}){pass.text.clear();syncAndHome()}};googleButton(status);button("CREATE NEW ACCOUNT"){registerScreen()}}
    private fun googleButton(status:TextView){root.addView(Button(this).apply{text="CONTINUE WITH GOOGLE";isAllCaps=false;textSize=15f;setTypeface(null,Typeface.BOLD);setTextColor(navy);background=rounded(Color.WHITE,30f);setOnClickListener{googleLogin(status)}},LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(16)})}
    private fun googleLogin(status:TextView?=null,link:Boolean=false){
        if(busy)return
        busy=true;googleInFlight=true;enabled(root,false);status?.text="Connecting to Google…"
        fun failed(message:String){busy=false;googleInFlight=false;enabled(root,true);status?.text=message;Toast.makeText(this,message,Toast.LENGTH_LONG).show()}
        CloudWorker.executor.execute {
            val challenge=try{accounts.googleChallenge()}catch(_:Exception){null}
            runOnUiThread {
                if(isFinishing||isDestroyed)return@runOnUiThread
                if(challenge==null){failed("Could not connect. Check your connection and try again.");return@runOnUiThread}
                googleIdentity.signIn(challenge){token,error->
                    if(isFinishing||isDestroyed)return@signIn
                    if(token==null){failed(error?:"Google sign-in failed")}else{
                        CloudWorker.executor.execute {
                            val result=try{accounts.googleSignIn(token,link)}catch(_:Exception){AccountStore.Result(false,"Google sign-in could not be completed")}
                            runOnUiThread {if(!isFinishing&&!isDestroyed){busy=false;googleInFlight=false;enabled(root,true);if(result.ok)syncAndHome()else failed(result.message)}}
                        }
                    }
                }
            }
        }
    }
    override fun onDestroy(){if(googleInFlight)googleIdentity.cancel();super.onDestroy()}
    private fun hero(){val h=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(28,28,28,28);background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff4b2398.toInt(),0xff0d7bd8.toInt(),0xff071b42.toInt())).apply{cornerRadius=42f}};h.addView(TextView(this).apply{text="LAZYGARDEN";textSize=15f;setTextColor(gold);setTypeface(null,Typeface.BOLD)});h.addView(GameArtwork(this,"garden"),LinearLayout.LayoutParams(dp(120),dp(120)).apply{topMargin=dp(12)});h.addView(TextView(this).apply{text="RESTORE. MATCH. BUILD.";textSize=26f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER;setPadding(0,10,0,8)});h.addView(TextView(this).apply{text="250 progressive garden levels, restoration missions and challenges.";textSize=15f;setTextColor(0xffd7e5ff.toInt());gravity=Gravity.CENTER});h.addView(Button(this).apply{text="PLAY LAZYGARDEN";isAllCaps=false;setTypeface(null,Typeface.BOLD);setTextColor(navy);background=rounded(gold,26f);setOnClickListener{lazyGarden()}},LinearLayout.LayoutParams(-1,104).apply{topMargin=20});root.addView(h,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=20})}
    private fun stats(){val box=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=rounded(panel,32f);setPadding(10,17,10,17)};fun add(v:String,l:String,c:Int){box.addView(TextView(this).apply{text="$v\n${tr(l)}";gravity=Gravity.CENTER;textSize=15f;setTextColor(c);setTypeface(null,Typeface.BOLD)},LinearLayout.LayoutParams(0,86,1f))};add(loyalty.points.toString(),"LOYALTY",gold);add(loyalty.currentStreak.toString(),"STREAK",Color.WHITE);add("${loyalty.todayGames}/${loyalty.dailyGoal}","TODAY",Color.WHITE);root.addView(box,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=20})}
    private fun gameKey(icon:String)=when(icon){"🌿"->"garden";"🫧"->"bubble";"🧩"->"block";"🔵"->"lines";"⚡"->"stack";"🔢"->"2048";else->"block"}
    private fun missionCard(icon:String,title:String,desc:String,category:String,progress:Int,target:Int,action:()->Unit){
        val q=progress.coerceIn(0,target)
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(12),dp(12),dp(14));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(panel2,panel,0xff101b3f.toInt())).apply{cornerRadius=dp(22).toFloat()};isClickable=true;isFocusable=true;setOnClickListener{action()}}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        top.addView(GameArtwork(this,gameKey(icon)),LinearLayout.LayoutParams(dp(104),dp(104)).apply{rightMargin=dp(14)})
        val words=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        words.addView(TextView(this).apply{text=category;textSize=11f;setTextColor(gold);setTypeface(null,Typeface.BOLD)})
        words.addView(TextView(this).apply{text=title;textSize=21f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);setPadding(0,dp(3),0,dp(3))})
        words.addView(TextView(this).apply{text=tr(desc);textSize=13f;setTextColor(0xffc5d1ef.toInt());maxLines=2})
        words.addView(TextView(this).apply{text="${tr("PLAY")}  ›";textSize=14f;setTextColor(gold);setTypeface(null,Typeface.BOLD);setPadding(0,dp(8),0,0)})
        top.addView(words,LinearLayout.LayoutParams(0,-2,1f));box.addView(top)
        box.addView(ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=target;this.progress=q},LinearLayout.LayoutParams(-1,dp(5)).apply{topMargin=dp(10)})
        box.addView(TextView(this).apply{text="$q/$target";gravity=Gravity.END;textSize=11f;setTextColor(0xffaab8dc.toInt())})
        root.addView(box,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
    }
    private fun resumeLast(){when(playHistory.lastGame){"garden"->lazyGarden();"bubble"->bubbleShooter();"block"->blockPuzzle();"stack"->neonStack();"lines"->colorLines();"2048"->merge2048();else->gamesHub()}}
    private fun nav(){val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=rounded(0xff0e1834.toInt(),30f);setPadding(5,5,5,5)};fun item(label:String,action:()->Unit){row.addView(Button(this).apply{text=tr(label);isAllCaps=false;textSize=11f;setTextColor(Color.WHITE);setTypeface(null,Typeface.BOLD);minHeight=0;minimumHeight=0;background=rounded(panel2,20f);setOnClickListener{action()}},LinearLayout.LayoutParams(0,92,1f).apply{setMargins(3,3,3,3)})};item("HOME"){home()};item("GAMES"){gamesHub()};item("QUESTS"){progressDashboard()};item("PROFILE"){account()};root.addView(row,LinearLayout.LayoutParams(-1,112).apply{topMargin=10;bottomMargin=70})}
    private fun progress(game:String)=playHistory.plays(game).coerceAtMost(10)
    private fun home(){screen();atHome=true;val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL};top.addView(TextView(this).apply{text="WE EARN TOGETHER";textSize=17f;setTextColor(gold);setTypeface(null,Typeface.BOLD)},LinearLayout.LayoutParams(0,70,1f));top.addView(TextView(this).apply{text=accounts.currentName()?:tr("PLAYER");gravity=Gravity.CENTER;textSize=13f;setTextColor(Color.WHITE);background=rounded(panel,22f);setOnClickListener{account()}},LinearLayout.LayoutParams(210,70));root.addView(top);if(cloudMessage.isNotEmpty())label(cloudMessage,12);hero();stats();if(playHistory.lastGame!=null){section(tr("CONTINUE PLAYING"));button(tr("RESUME LAST GAME")){resumeLast()}};section("POPULAR GAMES");missionCard("🫧","Bubble Shooter","Clear colorful boards and master level challenges.","ARCADE",progress("bubble"),10){bubbleShooter()};missionCard("🧩","Block Puzzle","Build combos, clear lines and chase high scores.","PUZZLE",progress("block"),10){blockPuzzle()};nav()}
    private fun gamesHub(){screen("GAMES");label("Choose a game visually. Your progress is saved offline and synced to your account.");section("GAMEPIX ONLINE GAMES");button("OPEN GAMEPIX CATALOG • 96+ GAMES"){startActivity(Intent(this,GamePixActivity::class.java))};label("Official GamePix catalogue • Publisher SID 71376 • GamePix ads/tracking preserved",12,gold);missionCard("🌿","LazyGarden","Match, restore and progress through a 250-level garden campaign.","FEATURED",progress("garden"),10){lazyGarden()};missionCard("🫧","Bubble Shooter","Clear colorful boards and master level challenges.","ARCADE",progress("bubble"),10){bubbleShooter()};missionCard("🧩","Block Puzzle","Build combos, clear lines and chase high scores.","PUZZLE",progress("block"),10){blockPuzzle()};missionCard("🔵","Color Lines","Build lines of five or more on the 9×9 board.","STRATEGY",progress("lines"),10){colorLines()};missionCard("⚡","Neon Stack","Time every drop and chase perfect stacks.","ARCADE",progress("stack"),10){neonStack()};missionCard("🔢","2048","Merge tiles and build your best board.","LOGIC",progress("2048"),10){merge2048()};nav()}
    private fun account(){
        screen(tr("PROFILE"));heading(accounts.currentName()?:tr("PLAYER"),25,gold)
        label(accounts.currentEmail()?:"",15);section("PLAYER ID");label(accounts.currentUserId()?:"",13)
        section("ACCOUNT STATUS");label(accounts.accountStatus(),15,gold);label(cloudMessage,13)
        button("SYNC CLOUD PROGRESS"){syncAndHome()}
        if(pendingConflicts.isNotEmpty())button("CHOOSE SAVED PROGRESS"){showNextConflict()}
        val name=field("Full name").apply{setText(accounts.currentName())};addField(name)
        button("UPDATE NAME"){val value=name.text.toString();request(work={accounts.updateName(value)}){account()}}
        button("REFRESH PROFILE"){request(work={accounts.refreshProfile()}){account()}}
        button("LINK GOOGLE ACCOUNT"){googleLogin(link=true)}
        button("LOG OUT"){request(work={accounts.logout()}){googleIdentity.clear{levelPlay=null;pendingConflicts=emptyList();welcome()}}}
        nav()
    }
    private fun progressDashboard(){screen(tr("QUESTS"));heading("${loyalty.points} ${tr("LOYALTY")}",30,gold);label("${tr("GAMES")}: ${loyalty.gamesPlayed}   •   ${tr("STREAK")}: ${loyalty.currentStreak}",16);section(tr("GAME MISSIONS"));missionCard("🌿","LazyGarden","Complete garden play sessions.","FEATURED",progress("garden"),10){lazyGarden()};missionCard("🫧","Bubble Shooter","Complete 10 play sessions.","ARCADE",progress("bubble"),10){bubbleShooter()};missionCard("🧩","Block Puzzle","Complete 10 play sessions.","PUZZLE",progress("block"),10){blockPuzzle()};nav()}
    private fun gameShell(title:String,subtitle:String,view:View,restart:()->Unit){screen(title);label(tr(subtitle),15,gold);root.addView(view,LinearLayout.LayoutParams(-1,1250));button(tr("RESTART")){restart()};button(tr("PLAY HUB")){levelPlay?.showInterstitialAtNaturalBreak();syncAndHome()}}
    private fun lazyGarden(){playHistory.mark("garden");startActivity(Intent(this,LazyGardenActivity::class.java))}
    private fun bubbleShooter(){playHistory.mark("bubble");val game=BubbleShooterView(this){levelPlay?.showInterstitialAtNaturalBreak()};gameShell("Bubble Shooter","Aim, shoot and clear the board.",game){game.restart()}}
    private fun colorLines(){playHistory.mark("lines");val game=ColorLinesView(this);gameShell("Color Lines","9×9 strategy. Create lines of five or more.",game){game.restart()}}
    private fun neonStack(){playHistory.mark("stack");val game=NeonStackView(this);gameShell("Neon Stack","Precision timing. Chase perfect drops.",game){game.restart()}}
    private fun merge2048(){playHistory.mark("2048");val game=Merge2048View(this);gameShell("2048","Merge tiles and beat your best score.",game){game.restart()}}
    private fun blockPuzzle(){playHistory.mark("block");val game=BlockPuzzleView(this){levelPlay?.showInterstitialAtNaturalBreak()};gameShell("Block Puzzle","Place pieces, clear lines and build combos.",game){game.restart()}}
}
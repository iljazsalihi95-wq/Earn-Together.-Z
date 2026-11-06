package com.weearntogether.play

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import kotlin.math.*

/** Fantasy arcade renderer: stylised targets/blades only, not a real-world throwing simulation. */
class KnifeHitActivity:Activity(){
    private lateinit var game:KnifeHitView
    private lateinit var recovery:LinearLayout
    private lateinit var watch:Button
    private var unityAds:LevelPlayManager?=null
    private var adMob:NativeGameAds?=null
    private var showingVideo=false
    override fun onCreate(state:Bundle?){super.onCreate(state);window.statusBarColor=0xff07152f.toInt();window.navigationBarColor=0xff07152f.toInt();window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xff071020.toInt())}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xff101b43.toInt(),0xff43217d.toInt(),0xff225cc8.toInt()))}
        top.addView(Button(this).apply{text="‹";textSize=24f;setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(54),dp(48)))
        top.addView(TextView(this).apply{text="BLADE QUEST • HD 2D";textSize=18f;setTextColor(0xffffc857.toInt());setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,dp(48),1f))
        top.addView(Button(this).apply{text="↻";textSize=20f;setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{game.restartLevel()}},LinearLayout.LayoutParams(dp(54),dp(48)))
        root.addView(top)
        game=KnifeHitView(this)
        root.addView(Button(this).apply {
            text="✦  ARSENALI I THIKAVE  ›"
            isAllCaps=false;setTextColor(0xffffdc88.toInt());textSize=14f
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xff183653.toInt(),0xff34215e.toInt())).apply{cornerRadius=dp(16).toFloat();setStroke(dp(1),0x6682cfff)}
            setOnClickListener { game.openShop(this@KnifeHitActivity) }
        },LinearLayout.LayoutParams(-1,dp(48)).apply{setMargins(dp(12),dp(4),dp(12),dp(4))})
        root.addView(game,LinearLayout.LayoutParams(-1,0,1f))
        recovery=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        watch=Button(this).apply {
            text="▶ VIDEO • VAZHDO";isAllCaps=false;setTextColor(Color.WHITE);background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xff225eac.toInt(),0xff6531b1.toInt())).apply{cornerRadius=dp(16).toFloat()}
            setOnClickListener { showRecoveryVideo() }
        }
        recovery.addView(watch,LinearLayout.LayoutParams(0,dp(52),2f))
        recovery.addView(Button(this).apply {
            text="↻ RIFILLO";isAllCaps=false;setTextColor(0xffffdb85.toInt());background=GradientDrawable().apply{setColor(0xff232043.toInt());cornerRadius=dp(16).toFloat();setStroke(dp(1),0x88ffce69.toInt())}
            setOnClickListener { game.restartLevel() }
        },LinearLayout.LayoutParams(0,dp(52),1f))
        root.addView(recovery)
        if(!(BuildConfig.DEBUG && intent.getBooleanExtra("native_qa",false))) {
            adMob=NativeGameAds(this,"knife_hit",gameplayOnly=true)
            val id=AccountStore(this).currentUserId()
                ?:getSharedPreferences("wet_guest_profile",MODE_PRIVATE).getString("guest_id",null)
            unityAds=LevelPlayManager(this,id).also {
                it.initialize(makeActive=false,rewardedOnly=true)
            }
        }
        game.onRecoveryChanged=::refreshRecovery
        refreshRecovery()
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(12),dp(6),dp(12),dp(10));background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xff09152f.toInt(),0xff241050.toInt(),0xff0b3562.toInt())).apply{setStroke(dp(1),0x6658c8ff)}}
        fun navButton(label:String,action:()->Unit)=TextView(this).apply{text=label;textSize=12f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD);isClickable=true;isFocusable=true;background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff153b7a.toInt(),0xff6b2ea6.toInt(),0xffb9872d.toInt())).apply{cornerRadius=dp(20).toFloat();setStroke(dp(1),0x99ffd66b.toInt())};setOnClickListener{performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);action()}}
        nav.addView(navButton("BLOCK PUZZLE ›"){startActivity(Intent(this,BlockPuzzleActivity::class.java))},LinearLayout.LayoutParams(0,dp(42),1f).apply{rightMargin=dp(6)})
        nav.addView(navButton("BUBBLE SHOOTER ›"){startActivity(Intent(this,BubbleShooterActivity::class.java))},LinearLayout.LayoutParams(0,dp(42),1f).apply{leftMargin=dp(6)})
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(58)));root.setOnApplyWindowInsetsListener{v,i->v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom);i};setContentView(root);root.requestApplyInsets()
    }
    private fun refreshRecovery() {
        recovery.visibility=if(game.hasLostRound) View.VISIBLE else View.GONE
        watch.visibility=if(game.rescueToken()!=null && adMob!=null) View.VISIBLE else View.GONE
        watch.isEnabled=!showingVideo
    }

    private fun showRecoveryVideo() {
        val token=game.rescueToken()?:return
        if(showingVideo)return
        showingVideo=true
        refreshRecovery()
        val grant={
            if(!isFinishing&&!isDestroyed)game.continueAfterVerifiedVideo(token)
            Unit
        }
        val finish={
            showingVideo=false
            if(!isFinishing&&!isDestroyed)refreshRecovery()
        }
        fun useAdMob() {
            if(isFinishing||isDestroyed||game.rescueToken()!=token) {
                finish()
                return
            }
            if(adMob?.rewardedForGameplay(grant,finish)!=true) {
                finish()
                Toast.makeText(this,"Videoja nuk është gati. Mund ta rifillosh nivelin.",Toast.LENGTH_SHORT).show()
            }
        }
        if(unityAds?.showRewardedForGameplayWithFallback(grant,finish,::useAdMob)!=true)useAdMob()
    }

    override fun onDestroy() {
        unityAds?.dispose()
        adMob?.destroy()
        super.onDestroy()
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onResume(){super.onResume();if(::game.isInitialized)game.setRunning(true)};override fun onPause(){if(::game.isInitialized)game.setRunning(false);super.onPause()}
}

class KnifeHitView(context:android.content.Context):View(context){
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val prefs=ProgressPreferences.open(context,"knife_hit")
    private var level=prefs.getInt("level",1).coerceIn(1,200);private var best=prefs.getInt("best",1).coerceAtLeast(1);private var score=prefs.getInt("score",0).coerceAtLeast(0)
    private data class Blade(val name: String, val price: Int)
    private val blades = listOf(Blade("Çelik", 0), Blade("Akull", 30), Blade("Ar", 60), Blade("Ametist", 100))
    private var coins = prefs.getInt("blade_coins", 0).coerceAtLeast(0)
    private var owned = prefs.getInt("blade_owned", 1) or 1
    private var selected = prefs.getInt("blade_selected", 0).takeIf { it in blades.indices && owns(it) } ?: 0
    private var remaining = 0
    private var totalBlades = 0
    private var lost = false
    private var rescueUsed = false
    private var attempt = 0
    var onRecoveryChanged: (() -> Unit)? = null
    val hasLostRound get() = lost

    fun rescueToken(): Int? = attempt.takeIf { lost && !rescueUsed }

    fun continueAfterVerifiedVideo(token: Int) {
        if (rescueToken() != token) return
        rescueUsed = true
        lost = false
        missAge = 1f
        save()
        onRecoveryChanged?.invoke()
        invalidate()
    }
    private var cleared = false
    private var completed = prefs.getBoolean("campaign_complete", false) && level == 200
    private var running = true
    private var shopOpen = false
    private var ambience = 0f
    private var angle = 0f
    private var speed = 1f
    private var direction = 1f
    private var motionTime = 0f
    private var nextTurn = 0f
    private var last = 0L
    private var flight = -1f
    private var hitAge = 1f
    private var missAge = 1f
    private var clearAge = 0f
    private var boss = false
    private var targetStyle = 0
    private val stuck = mutableListOf<Float>()
    private val obstacles = mutableListOf<Float>()

    init { isFocusable = true; isClickable = true; setupLevel(restore = true) }

    private fun setupLevel(restore: Boolean = false) {
        attempt++
        rescueUsed = false
        lost = false
        cleared = false
        flight = -1f
        hitAge = 1f
        missAge = 1f
        angle = 0f
        motionTime = 0f
        nextTurn = 2.8f
        totalBlades = (5 + (level - 1) / 4).coerceAtMost(12)
        remaining = totalBlades
        boss = level % 10 == 0
        targetStyle = ((level - 1) / 5) % 5
        speed = (.85f + level * .017f).coerceAtMost(3.2f)
        direction = if ((level / 3) % 2 == 0) 1f else -1f
        stuck.clear()
        obstacles.clear()
        val count = when { boss -> 4; level >= 35 -> 3; level >= 15 -> 2; level >= 6 -> 1; else -> 0 }
        repeat(count) { obstacles.add((it * (360f / count) + level * 17f) % 360f) }
        if (restore && prefs.getInt("board_level", -1) == level) {
            val raw = prefs.getString("stuck_angles", "").orEmpty()
            val angles = if (raw.isBlank()) emptyList() else raw.split(',').mapNotNull { it.toFloatOrNull() }
            if (angles.size < totalBlades && angles.all { it in 0f..<360f } &&
                angles.indices.all { i -> angles.take(i).none { diff(it, angles[i]) < collisionAngle } &&
                    obstacles.none { diff(it, angles[i]) < collisionAngle } }) {
                stuck.addAll(angles)
                remaining -= angles.size
                lost = prefs.getBoolean("round_lost", false)
                rescueUsed = prefs.getBoolean("round_rescue_used", false)
            }
        }
        save()
        onRecoveryChanged?.invoke()
        invalidate()
    }

    fun setRunning(active: Boolean) {
        running = active
        last = 0L
        if (active) invalidate()
    }

    fun restartLevel() {
        if (completed) {
            completed = false
            level = 1
            score = 0
        } else if (cleared) {
            level++
        }
        setupLevel()
    }

    private fun save() {
        val nextLevel = if (cleared && !completed) level + 1 else level
        best = max(best, nextLevel)
        prefs.edit().putInt("level", nextLevel).putInt("best", best).putInt("score", score)
            .putInt("board_level", nextLevel).putString("stuck_angles", if (cleared) "" else stuck.joinToString(","))
            .putBoolean("round_rescue_used", !cleared && rescueUsed).putBoolean("round_lost", lost).putBoolean("campaign_complete", completed)
            .putInt("blade_coins", coins).putInt("blade_owned", owned).putInt("blade_selected", selected).apply()
    }

    private fun owns(index: Int) = (owned and (1 shl index)) != 0

    fun openShop(activity: Activity) {
        shopOpen = true
        val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(12), 0) }
        val dialog = android.app.AlertDialog.Builder(activity).setView(list).setPositiveButton("MBYLL", null).create()
        fun refresh() {
            dialog.setTitle("Thikat • $coins monedha")
            list.removeAllViews()
            blades.forEachIndexed { index, blade ->
                val row = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
                row.addView(object : View(activity) {
                    override fun onDraw(canvas: Canvas) {
                        renderBlade(canvas, width / 2f, height * .52f, 28f, width * .12f, height * .38f, index)
                    }
                }, LinearLayout.LayoutParams(dp(44), dp(54)))
                row.addView(Button(activity).apply {
                    text = when {
                        selected == index -> "${blade.name} • E zgjedhur"
                        owns(index) -> "${blade.name} • Zgjidh"
                        else -> "${blade.name} • ${blade.price} monedha"
                    }
                    isEnabled = selected != index && (owns(index) || coins >= blade.price)
                    setOnClickListener {
                        if (!owns(index)) {
                            coins -= blade.price
                            owned = owned or (1 shl index)
                        }
                        selected = index
                        save()
                        refresh()
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                list.addView(row)
            }
        }
        dialog.setOnDismissListener { shopOpen = false; last = 0L; invalidate() }
        refresh()
        dialog.show()
    }

    private val collisionAngle get() = if (totalBlades + obstacles.size >= 14) 11f else 13f

    private fun diff(a: Float, b: Float) = abs((a - b + 540f) % 360f - 180f)

    private fun impact() {
        flight = -1f
        val targetImpact = (90f - angle + 360f) % 360f
        if (stuck.any { diff(it, targetImpact) < collisionAngle } || obstacles.any { diff(it, targetImpact) < collisionAngle }) {
            lost = true
            missAge = 0f
            score = (score - 15).coerceAtLeast(0)
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } else {
            stuck.add(targetImpact)
            remaining--
            score += 20 + level * 3
            hitAge = 0f
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (remaining == 0) {
                cleared = true
                clearAge = 0f
                completed = level == 200
                coins += if (boss) 25 else 10
                score += 150 * level
            }
        }
        save()
        if(lost)onRecoveryChanged?.invoke()
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (completed) restartLevel()
        else if(cleared && clearAge >= .6f){level++;setupLevel()}
        else if (!lost && !shopOpen && !cleared && flight < 0f) { flight = 0f; invalidate() }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    private fun label(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=Color.WHITE,center:Boolean=false){p.shader=null;p.style=Paint.Style.FILL;p.color=color;p.textSize=size;p.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);p.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p);p.textAlign=Paint.Align.LEFT}
    private fun drawBlade(c:Canvas,x:Float,y:Float,rotation:Float,scale:Float=1f,variant:Int=selected) {
        renderBlade(c, x, y, rotation, width * .026f * scale, height * .09f * scale, variant)
    }
    private fun renderBlade(c:Canvas,x:Float,y:Float,rotation:Float,w:Float,h:Float,variant:Int){
        c.save();c.rotate(rotation,x,y);p.style=Paint.Style.FILL;val metal=when(variant){1->intArrayOf(0xffffffff.toInt(),0xff8ee8ff.toInt(),0xff5276a8.toInt());2->intArrayOf(0xfffff2b0.toInt(),0xffffc857.toInt(),0xff8d5b20.toInt());3->intArrayOf(0xfff6e9ff.toInt(),0xffba7cff.toInt(),0xff56328c.toInt());else->intArrayOf(0xffffffff.toInt(),0xffc7d5f5.toInt(),0xff66779f.toInt())};p.shader=LinearGradient(x-w,y-h,x+w,y,metal,null,Shader.TileMode.CLAMP);val blade=Path().apply{
            moveTo(x + if (variant == 3) w * .55f else 0f,y-h)
            when (variant) {
                1 -> { lineTo(x+w,y-h*.48f); lineTo(x+w*.55f,y+h*.18f) }
                2 -> { quadTo(x+w*1.65f,y-h*.43f,x+w*.66f,y+h*.18f) }
                3 -> { quadTo(x+w*1.45f,y-h*.48f,x+w*.48f,y+h*.18f) }
                else -> { lineTo(x+w,y-h*.15f); lineTo(x+w*.58f,y+h*.18f) }
            }
            lineTo(x-w*.58f,y+h*.18f)
            lineTo(x-w,y-h*.15f)
            close()
        };c.drawPath(blade,p);p.shader=null;p.color=0xffffc857.toInt();c.drawRoundRect(x-w*1.45f,y+h*.12f,x+w*1.45f,y+h*.25f,w*.35f,w*.35f,p);p.color=when(variant){1->0xff167fb4.toInt();2->0xff7b451f.toInt();3->0xff7a35c9.toInt();else->0xff4d287f.toInt()};c.drawRoundRect(x-w*.72f,y+h*.23f,x+w*.72f,y+h*.62f,w*.32f,w*.32f,p);c.restore()}
    private val palettes = arrayOf(
        intArrayOf(0xffecad65.toInt(), 0xff9b552d.toInt(), 0xff391e26.toInt()),
        intArrayOf(0xffe5f4ff.toInt(), 0xff5e7c9e.toInt(), 0xff182541.toInt()),
        intArrayOf(0xffb3fff1.toInt(), 0xff1da485.toInt(), 0xff083544.toInt()),
        intArrayOf(0xffe7fcff.toInt(), 0xff50b5eb.toInt(), 0xff1e427c.toInt()),
        intArrayOf(0xfffaa9ff.toInt(), 0xff8448d5.toInt(), 0xff291546.toInt())
    )
    private val stageNames = listOf("ROJA E PYLLIT", "FORGJA E RRUFESË", "TEMPULLI I KRISTALIT", "KALAJA E AKULLIT", "PORTA E YJEVE")
    private val worldNames = listOf("PYLLI I DRITAVE", "KËSHTJELLA E ÇELIKUT", "LUGINA E KRISTALIT", "MBRETËRIA E AKULLIT", "KOZMOSI MAGJIK")
    private val worldTints = intArrayOf(0xff52edb0.toInt(),0xffffa34f.toInt(),0xffb795ff.toInt(),0xff73dbff.toInt(),0xffeb7dff.toInt())
    private fun fill(color:Int){p.shader=null;p.style=Paint.Style.FILL;p.color=color}
    private fun stroke(color:Int,w:Float){p.shader=null;p.style=Paint.Style.STROKE;p.color=color;p.strokeWidth=w}
    private fun panel(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int){fill(color);c.drawRoundRect(l,t,r,b,width*.025f,width*.025f,p)}

    /** Animated layered scenery shares the live game clock and pauses with the Activity. */
    private fun drawWorld(c:Canvas){
        val w=width.toFloat();val h=height.toFloat();val tint=worldTints[targetStyle]
        val sky=when(targetStyle){0->intArrayOf(0xff041b29.toInt(),0xff10413f.toInt(),0xff071421.toInt());1->intArrayOf(0xff100e2c.toInt(),0xff442943.toInt(),0xff111528.toInt());2->intArrayOf(0xff100d31.toInt(),0xff302467.toInt(),0xff101b37.toInt());3->intArrayOf(0xff061d38.toInt(),0xff184367.toInt(),0xff101d3c.toInt());else->intArrayOf(0xff0e082b.toInt(),0xff35205a.toInt(),0xff071a3d.toInt())}
        p.style=Paint.Style.FILL;p.shader=LinearGradient(0f,0f,w,h,sky,null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
        p.shader=RadialGradient(w*.5f,h*.37f,w*.66f,intArrayOf((tint and 0x00ffffff) or 0x33000000,Color.TRANSPARENT),null,Shader.TileMode.CLAMP);c.drawCircle(w*.5f,h*.37f,w*.66f,p);p.shader=null
        // Distant stars, drifting embers and fireflies. Deterministic, no allocations of sprites.
        repeat(45){i->val x=((i*137.2f+w*.04f*sin(ambience*.12f+i))%w+w)%w;val y=((i*71.9f-ambience*(4+i%4))%h+h)%h;fill(Color.argb((45+65*(.5f+.5f*sin(ambience+i))).toInt(),Color.red(tint),Color.green(tint),Color.blue(tint)));c.drawCircle(x,y,w*(.0015f+(i%3)*.001f),p)}
        // Three depth layers; silhouette shapes differ in every unlocked world.
        repeat(3){layer->val base=h*(.74f+layer*.07f);fill(intArrayOf(0x33102d4e,0x7730224c,0xcc081526.toInt())[layer]);val land=Path();land.moveTo(0f,h)
            repeat(17){i->val x=w*i/16f;val y=base-h*(.05f+.06f*sin(i*1.7f+layer));if(i==0)land.lineTo(x,y)else land.lineTo(x,y)};land.lineTo(w,h);land.close();c.drawPath(land,p)
        }
        repeat(9){i->val x=w*(i/8f);val base=h*(.85f+(i%3)*.03f);val tall=h*(.15f+(i%4)*.06f);fill(0xaa061723.toInt())
            when(targetStyle){
                0->{c.drawRoundRect(x-w*.009f,base-tall,x+w*.009f,base,w*.008f,w*.008f,p);repeat(3){j->c.drawOval(x-w*(.06f+j*.012f),base-tall+h*j*.045f,x+w*(.06f+j*.012f),base-tall+h*(.1f+j*.045f),p)}}
                1->{c.drawRect(x-w*.035f,base-tall,x+w*.035f,base,p);repeat(3){j->c.drawRect(x-w*.045f+j*w*.03f,base-tall-h*.015f,x-w*.03f+j*w*.03f,base-tall+h*.01f,p)}}
                else->{val peak=Path().apply{moveTo(x-w*.08f,base);lineTo(x-w*.035f,base-tall*.75f);lineTo(x,base-tall);lineTo(x+w*.06f,base-tall*.3f);lineTo(x+w*.085f,base);close()};c.drawPath(peak,p);stroke((tint and 0x00ffffff) or 0x33000000,w*.002f);c.drawLine(x,base-tall,x+w*.025f,base,p)}
            }
        }
        // Perspective platform beneath the launcher.
        p.style=Paint.Style.FILL;p.shader=RadialGradient(w*.5f,h*.87f,w*.37f,intArrayOf((tint and 0x00ffffff) or 0x55000000,Color.TRANSPARENT),null,Shader.TileMode.CLAMP);c.drawOval(w*.08f,h*.82f,w*.92f,h*.94f,p);p.shader=null
        stroke((tint and 0x00ffffff) or 0x77000000,w*.003f);c.drawOval(w*.19f,h*.854f,w*.81f,h*.91f,p);p.style=Paint.Style.FILL
    }

    private fun drawGuardian(c:Canvas,cx:Float,cy:Float,r:Float){
        // Eyes and mouth remain upright while the physical target and attached blades rotate.
        val blink=if(ambience%5.8f>5.62f).12f else 1f
        val hurt=(1f-hitAge/.28f).coerceIn(0f,1f)
        val eyeY=cy-r*.13f;val eyeW=r*.23f;val eyeH=r*(.17f-.07f*hurt)*blink
        for(side in listOf(-1f,1f)){
            val x=cx+side*r*.35f
            fill(0xcc071326.toInt());c.drawOval(x-eyeW,eyeY-eyeH,x+eyeW,eyeY+eyeH,p)
            p.shader=RadialGradient(x,eyeY,r*.16f,intArrayOf(Color.WHITE,if(boss)0xffff6f68.toInt() else worldTints[targetStyle],Color.TRANSPARENT),null,Shader.TileMode.CLAMP);c.drawCircle(x,eyeY,r*.16f*blink,p);p.shader=null
            fill(0xff071122.toInt());c.drawOval(x-r*.04f,eyeY-r*.085f*blink,x+r*.04f,eyeY+r*.085f*blink,p)
            stroke(palettes[targetStyle][2],r*.065f);c.drawLine(x-eyeW,eyeY-r*.23f-side*r*.06f,x+eyeW,eyeY-r*.23f+side*r*.06f,p)
        }
        val mouth=RectF(cx-r*.26f,cy+r*.2f,cx+r*.26f,cy+r*(if(hurt>0).56f else .43f))
        fill(0xbb031125.toInt());c.drawOval(mouth,p)
        stroke(0x99ffffff.toInt(),r*.018f);c.drawArc(mouth,12f,156f,false,p);p.style=Paint.Style.FILL
    }

    private fun drawVictory(c:Canvas){
        val w=width.toFloat();val h=height.toFloat();val t=(clearAge/.45f).coerceIn(0f,1f);val tint=worldTints[targetStyle]
        fill(Color.argb((210*t).toInt(),3,8,24));c.drawRect(0f,0f,w,h,p)
        repeat(38){i->val a=i*2.39996f;val d=w*(.08f+clearAge*(.1f+i%4*.035f));fill(Color.argb((220*(1-clearAge/4f)).toInt().coerceIn(0,255),if(i%2==0)255 else Color.red(tint),if(i%2==0)213 else Color.green(tint),if(i%2==0)98 else Color.blue(tint)));c.drawCircle(w*.5f+cos(a)*d,h*.39f+sin(a)*d-h*clearAge*.04f,w*.006f,p)}
        c.save();c.translate(w*.5f,h*.39f);val scale=.7f+.3f*t;c.scale(scale,scale)
        stroke(tint,w*.006f);c.drawCircle(0f,0f,w*.17f,p);stroke(0xffffd475.toInt(),w*.003f);c.drawCircle(0f,0f,w*.19f,p)
        label(c,"✦",0f,w*.045f,w*.16f,0xffffdb85.toInt(),true);c.restore()
        label(c,if(completed)"LEGJENDA U PËRFUNDUA" else "NIVELI $level U KALUA",w*.5f,h*.57f,w*.055f,0xffffdc88.toInt(),true)
        label(c,"+${if(boss)25 else 10} MONEDHA  •  +${150*level} PIKË",w*.5f,h*.62f,w*.033f,Color.WHITE,true)
        if(level%5==0&&!completed){label(c,"PAMJE E RE U HAP",w*.5f,h*.69f,w*.043f,tint,true);label(c,worldNames[(level/5)%5],w*.5f,h*.735f,w*.037f,Color.WHITE,true)}
        else label(c,if(boss)"BOSS I MUNDUR" else "SFIDA TJETËR PO AFRON",w*.5f,h*.71f,w*.037f,tint,true)
        if(!completed){panel(c,w*.18f,h*.79f,w*.82f,h*.86f,0xff283b68.toInt());label(c,"VAZHDO  ›",w*.5f,h*.836f,w*.04f,Color.WHITE,true)}
    }

    private fun drawTarget(c: Canvas, cx: Float, cy: Float, r: Float) {
        c.save()
        c.translate(cx, cy)
        c.rotate(angle)
        val colors = if (boss) intArrayOf(0xffffdf86.toInt(), 0xffbb3a69.toInt(), 0xff381540.toInt()) else palettes[targetStyle]
        p.shader = RadialGradient(-r*.24f,-r*.29f,r*1.25f,colors,null,Shader.TileMode.CLAMP)
        c.drawCircle(0f,0f,r,p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = r*.055f
        p.color = colors[0]
        c.drawCircle(0f,0f,r*.95f,p)
        p.strokeWidth = r*.023f
        p.color = 0x77ffffff
        val pattern = if (boss) 5 else targetStyle
        when (pattern) {
            0 -> repeat(6) { i ->
                val ring = r * (.16f + i*.12f)
                c.drawOval(-ring*.86f-r*.09f,-ring,ring*.86f-r*.09f,ring,p)
            }
            1 -> repeat(8) {
                c.drawCircle(0f,-r*.78f,r*.066f,p)
                c.drawLine(0f,-r*.64f,0f,-r*.27f,p)
                c.rotate(45f)
            }
            2, 3 -> repeat(if (pattern == 2) 5 else 6) {
                val facet = Path().apply {
                    moveTo(0f,0f); lineTo(-r*.22f,-r*.43f); lineTo(0f,-r*.85f)
                    lineTo(r*.22f,-r*.43f); close()
                }
                p.style = Paint.Style.FILL
                p.color = if (pattern == 2) 0x776bffd1 else 0x888fdbff.toInt()
                c.drawPath(facet,p)
                p.style = Paint.Style.STROKE
                p.color = 0xbbffffff.toInt()
                c.drawPath(facet,p)
                c.rotate(if (pattern == 2) 72f else 60f)
            }
            else -> {
                val points = if (boss) 8 else 5
                val star = Path()
                repeat(points * 2) { i ->
                    val a = i * PI / points
                    val radius = r * if (i % 2 == 0) .88f else .47f
                    val x = cos(a).toFloat()*radius
                    val y = sin(a).toFloat()*radius
                    if (i == 0) star.moveTo(x,y) else star.lineTo(x,y)
                }
                star.close()
                c.drawPath(star,p)
                c.drawCircle(0f,0f,r*.36f,p)
            }
        }
        p.style = Paint.Style.FILL
        c.restore()
    }

    private fun advance(dt: Float) {
        if (cleared) {
            clearAge += dt
            if (clearAge >= 3.2f && !completed) { level++; setupLevel() }
            return
        }
        if (completed || lost) return
        motionTime += dt
        if (level >= 8 && motionTime >= nextTurn) {
            direction = -direction
            nextTurn += 2.3f + (level % 5) * .24f
        }
        val variation = if (level < 4) 1f else 1f + .30f * sin(motionTime * 1.9f)
        val turnRamp = if (level < 8) 1f else ((nextTurn - motionTime) * 3f).coerceIn(.26f, 1f)
        angle = (angle + speed * direction * 92f * dt * variation * turnRamp + 360f) % 360f
        if (flight >= 0f) {
            flight += dt / .15f
            if (flight >= 1f) impact()
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (width <= 0 || height <= 0) return
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else ((now-last)/1e9f).coerceIn(0f,.04f)
        last = now
        if (running && !shopOpen) {
            hitAge += dt
            missAge += dt
            advance(dt)
        }
        if(running&&!shopOpen)ambience+=dt
        drawWorld(c)
        val cx = width / 2f
        val cy = height * .39f
        val r = min(width*.245f,height*.205f)
        val impactY = cy + r + height*.09f*.82f
        val bounce = if (hitAge < .14f) -r*.055f*sin(hitAge/.14f*PI).toFloat() else 0f
        val targetY = cy + bounce
        panel(c,width*.03f,height*.015f,width*.97f,height*.12f,0xbb101b36.toInt())
        label(c,"NIVELI $level/200",width*.055f,height*.055f,width*.038f,0xffffc857.toInt())
        label(c,"REKORD $best",width*.39f,height*.055f,width*.033f,0xffd9ccff.toInt())
        label(c,"PIKË $score",width*.72f,height*.055f,width*.032f)
        label(c,if(boss) "BOSS • ${stageNames[targetStyle]}" else stageNames[targetStyle],cx,height*.105f,width*.036f,0xff78dfff.toInt(),true)
        // Orbital halo creates depth behind the live target.
        c.save();c.rotate(-angle*.25f,cx,targetY);stroke((worldTints[targetStyle] and 0x00ffffff) or 0x55000000,r*.018f);c.drawOval(cx-r*1.28f,targetY-r*1.12f,cx+r*1.28f,targetY+r*1.12f,p);c.restore();p.style=Paint.Style.FILL
        c.save()
        val shake=if(missAge<.3f)sin(missAge*95f)*width*.014f*(1-missAge/.3f) else 0f
        c.translate(shake,0f)
        if(cleared){val shrink=(1-clearAge/.6f).coerceIn(.05f,1f);c.scale(shrink,shrink,cx,targetY)}
        drawTarget(c,cx,targetY,r)
        drawGuardian(c,cx,targetY,r)
        for (a0 in obstacles) {
            val a = Math.toRadians((a0 + angle).toDouble())
            val x = cx + cos(a).toFloat()*r*.79f
            val y = targetY + sin(a).toFloat()*r*.79f
            p.color = 0xffff618d.toInt()
            c.drawCircle(x,y,r*.11f,p)
            p.color = 0xffffe278.toInt()
            c.drawCircle(x,y,r*.046f,p)
        }
        for (a0 in stuck) {
            val a = Math.toRadians((a0 + angle).toDouble())
            val distance = r + height*.09f*.52f
            drawBlade(c,cx+cos(a).toFloat()*distance,targetY+sin(a).toFloat()*distance,a0+angle-90f,.69f)
        }
        c.restore()
        if (hitAge < .32f) {
            repeat(13) { i ->
                val a = i * PI * 2 / 13
                val distance = r * (.1f + hitAge * (1.1f + (i % 4) * .38f))
                p.color = Color.argb((240*(1-hitAge/.32f)).toInt(),255,205,100)
                c.drawCircle(cx+cos(a).toFloat()*distance,cy+r+sin(a).toFloat()*distance,r*.032f,p)
            }
        }
        if (missAge < .33f) {
            val fall = missAge / .33f
            drawBlade(c,cx+width*.23f*fall,impactY+height*.28f*fall,390f*fall,.8f)
        }
        label(c,"THIKA $remaining   •   MONEDHA $coins",cx,height*.69f,width*.038f,Color.WHITE,true)
        repeat(remaining) { i ->
            val x = cx + (i-(remaining-1)/2f)*width*.062f
            drawBlade(c,x,height*.76f,0f,.26f)
        }
        if (!lost && !cleared && !completed) {
            val y = if (flight < 0f) height*.9f else height*.9f+(impactY-height*.9f)*flight
            if(flight>=0f){p.shader=LinearGradient(cx,y,cx,y+height*.2f,intArrayOf(0xccbbddff.toInt(),Color.TRANSPARENT),null,Shader.TileMode.CLAMP);p.style=Paint.Style.FILL;c.drawRoundRect(cx-width*.008f,y,cx+width*.008f,y+height*.2f,width*.008f,width*.008f,p);p.shader=null}
            drawBlade(c,cx,y,0f,.82f)
        }
        val hint = when {
            completed -> "FUSHATA U PËRFUNDUA • RIFILLO"
            lost -> "U PËRPLAS!"
            cleared -> "NIVELI U FITUA!"
            else -> "PREK PËR TË HEDHUR"
        }
        label(c,hint,cx,height*.977f,width*.039f,0xffffd66b.toInt(),true)
        // Keep ambience alive on the loss/victory screens as well; Activity pause stops frames.
        if(cleared)drawVictory(c)
        if(lost){panel(c,width*.12f,height*.81f,width*.88f,height*.91f,0xdd301e41.toInt());label(c,"PROVO SËRISH",cx,height*.85f,width*.05f,0xffffd68a.toInt(),true);label(c,"Vazhdo me video ose rifillo nivelin",cx,height*.885f,width*.03f,Color.WHITE,true)}
        if (running && !shopOpen) postInvalidateOnAnimation()
    }
}

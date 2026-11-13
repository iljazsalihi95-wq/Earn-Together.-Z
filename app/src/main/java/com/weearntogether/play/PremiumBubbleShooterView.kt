package com.weearntogether.play

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Native HD 2D Bubble Shooter: hex grid, six-neighbour matching, combo magic and visible launcher. */
class PremiumBubbleShooterView(context: Context, private val onLevelComplete: (Int) -> Unit = {}) : FrameLayout(context) {
 private val prefs=ProgressPreferences.open(context,"bubble_shooter_hd2d"); private val rows=11; private val cols=9; private val grid=Array(rows){IntArray(cols)}
 private val palette=intArrayOf(0xff35c6fa.toInt(),0xffa478ff.toInt(),0xffffc857.toInt(),0xff49e1aa.toInt(),0xffff638f.toInt()); private val field=FrameLayout(context); private val hud=TextView(context); private val danger=TextView(context); private val launcher=TextView(context); private val nextView=TextView(context); private val aimDots=mutableListOf<View>(); private val fxLayer=FrameLayout(context)
 private var campaignComplete=false;private var score=0;private var best=0;private var level=1;private var streak=0;private var current=1;private var next=2;private var misses=0;private var busy=false;private var cell=1;private var fieldTop=0;private var aiming:Pair<Int,Int>?=null;private var aimPath=emptyList<Pair<Float,Float>>()
 init{setBackgroundColor(0xff071020.toInt());field.background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff07162f.toInt(),0xff1c1042.toInt(),0xff071a38.toInt())).apply{cornerRadius=dp(24).toFloat();setStroke(dp(1),0x6658c8ff)};addView(field,LayoutParams(-1,-1).apply{setMargins(dp(12),dp(116),dp(12),dp(112))});addView(fxLayer,LayoutParams(-1,-1));fxLayer.isClickable=false;hud.apply{setTextColor(Color.WHITE);textSize=14f;gravity=Gravity.CENTER;setTypeface(null,android.graphics.Typeface.BOLD)};addView(hud,LayoutParams(-1,dp(48),Gravity.TOP).apply{topMargin=dp(112)});danger.apply{textSize=11f;gravity=Gravity.CENTER;setTypeface(null,android.graphics.Typeface.BOLD)};addView(danger,LayoutParams(-1,dp(28),Gravity.TOP).apply{topMargin=dp(158)});launcher.gravity=Gravity.CENTER;launcher.text="✦";launcher.textSize=24f;launcher.setTextColor(Color.WHITE);launcher.elevation=dp(16).toFloat();launcher.isClickable=true;launcher.isFocusable=true;launcher.contentDescription="Fire bubble straight ahead";launcher.setOnClickListener{fireStraightAhead()};addView(launcher,LayoutParams(dp(62),dp(62),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{bottomMargin=dp(34)});nextView.gravity=Gravity.CENTER;nextView.text="•";nextView.textSize=18f;nextView.elevation=dp(12).toFloat();addView(nextView,LayoutParams(dp(40),dp(40),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{bottomMargin=dp(46);leftMargin=dp(112)});nextView.apply{isClickable=true;isFocusable=true;contentDescription="Swap current and next bubble";setOnClickListener{swapBubble()}};repeat(18){val d=View(context).apply{visibility=View.GONE;background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(0xeeffffff.toInt());setStroke(dp(1),0xaaffc857.toInt())}};aimDots+=d;addView(d,LayoutParams(dp(7),dp(7)))};restoreOrNew();post{rebuild()}}
 private var paused=false
 private var flight:ValueAnimator?=null
 private var projectileView:View?=null
 private var pendingSettlement:Pair<Int,Int>?=null
 private var activeShot:Pair<Int,Int>?=null
 fun setPaused(value:Boolean){
  paused=value
  if(value){hideAim();flight?.pause()}
  else {
   val pending=pendingSettlement
   if(pending!=null){pendingSettlement=null;settle(pending.first,pending.second)}
   flight?.resume()
  }
 }
 private fun cancelFlight(){activeShot=null;pendingSettlement=null;flight?.removeAllListeners();flight?.cancel();flight=null;projectileView?.let{removeView(it)};projectileView=null;busy=false}
 override fun onDetachedFromWindow(){
  // A projectile already launched is a committed move. Finish it before the
  // view disappears so rotation/navigation cannot silently refund the shot.
  val committed=activeShot ?: pendingSettlement
  cancelFlight()
  activeShot=null
  if(committed!=null)settle(committed.first,committed.second)
  super.onDetachedFromWindow()
 }
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();private fun light(c:Int)=Color.rgb((Color.red(c)+85).coerceAtMost(255),(Color.green(c)+85).coerceAtMost(255),(Color.blue(c)+85).coerceAtMost(255));private fun shade(c:Int)=Color.rgb((Color.red(c)*.55f).toInt(),(Color.green(c)*.55f).toInt(),(Color.blue(c)*.55f).toInt());private fun bubble(c:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xffffffff.toInt(),light(c),c,shade(c))).apply{shape=GradientDrawable.OVAL;setStroke(dp(2),0xbbffffff.toInt())}
 // Older/cloud-restored preferences may contain numeric strings or Long values.
 // Reading them through getInt/getString can throw ClassCastException.
 private fun restoredInt(key:String,fallback:Int):Int=when(val value=prefs.all[key]){
  is Int->value
  is Long->value.coerceIn(Int.MIN_VALUE.toLong(),Int.MAX_VALUE.toLong()).toInt()
  is Float->if(value.isFinite())value.coerceIn(Int.MIN_VALUE.toFloat(),Int.MAX_VALUE.toFloat()).toInt() else fallback
  is Number->value.toInt()
  is String->value.trim().toIntOrNull()?:fallback
  else->fallback
 }
 private fun restoredString(key:String):String?=prefs.all[key] as? String
 private fun restoreOrNew(){campaignComplete=prefs.all["campaign_complete"]==true;score=restoredInt("score",0).coerceAtLeast(0);best=restoredInt("best",0).coerceAtLeast(score);level=restoredInt("level",1).coerceIn(1,250);streak=restoredInt("streak",0).coerceAtLeast(0);misses=restoredInt("misses",0).coerceIn(0,pressureLimit()-1);val raw=restoredString("grid")?.split(',')?.map{it.trim().toIntOrNull()};val valid=raw?.size==rows*cols&&raw.all{it!=null&&it in 0..palette.size};if(valid){raw!!.forEachIndexed{i,v->grid[i/cols][i%cols]=v!!}}else newBoard();val emptySavedBoard=grid.all{row->row.all{it==0}};if(emptySavedBoard&&!campaignComplete)newBoard();val active=grid.flatMap{it.asList()}.filter{it>0}.toSet();val savedCurrent=restoredInt("current",0);val savedNext=restoredInt("next",0);current=if(savedCurrent in active)savedCurrent else randomBoardColor();next=if(savedNext in active)savedNext else randomBoardColor();if(!valid||(emptySavedBoard&&!campaignComplete)||savedCurrent !in active||savedNext !in active)save()}
 private fun newBoard(){grid.forEach{it.fill(0)};val fill=(4+level/45).coerceAtMost(7);val colors=(3+level/55).coerceAtMost(palette.size);for(r in 0 until fill)for(c in 0 until cols)if(Random.nextFloat()>.08f)grid[r][c]=1+Random.nextInt(colors);misses=0}
 private fun save(){best=max(best,score);prefs.edit().putString("grid",grid.flatMap{it.asList()}.joinToString(",")).putInt("score",score).putInt("best",best).putInt("level",level).putInt("streak",streak).putInt("misses",misses).putInt("current",current).putInt("next",next).putBoolean("campaign_complete",campaignComplete).apply()}
 private fun pressureLimit()=(7-level/60).coerceAtLeast(4)
 private fun bubbleSize()=(cell-dp(4)).coerceAtLeast(1)
 private fun gridWidth()=(cols+.5f)*cell
 private fun gridLeft()=maxOf(dp(8),((field.width-gridWidth())/2f).toInt())
 private fun xFor(r:Int,c:Int)=gridLeft()+c*cell+(if(r%2==1)cell/2 else 0)
 private fun addRails(){
  for(right in listOf(false,true))field.addView(View(context).apply{
   contentDescription=if(right)"Bubble wall right" else "Bubble wall left"
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0xffffc857.toInt(),0xffa478ff.toInt(),0xff35c6fa.toInt()))
   alpha=.65f
  },LayoutParams(dp(1).coerceAtLeast(1),(field.height-fieldTop-dp(12)).coerceAtLeast(1)).apply{
   leftMargin=if(right)(gridLeft()+gridWidth()).toInt() else gridLeft()-dp(1)
   topMargin=fieldTop
  })
 }
 private fun yFor(r:Int)=fieldTop+(r*cell*.86f).toInt()
 private fun rebuild(){field.removeAllViews();// Fit all eleven staggered rows inside the actual playfield, including
 // short screens, split-screen and landscape. Width-only sizing clipped
 // lower bubbles and made them impossible to target.
 fieldTop=dp(72)
 val widthCell=((field.width-dp(20))/(cols+0.5f)).toInt()
 val availableHeight=(field.height-fieldTop-dp(12)).coerceAtLeast(1)
 val heightCell=(availableHeight/(1f+(rows-1)*.86f)).toInt()
 cell=minOf(widthCell,heightCell).coerceAtLeast(1);addRails();for(r in 0 until rows)for(c in 0 until cols)if(grid[r][c]>0)addBubble(r,c,grid[r][c]);launcher.background=bubble(palette[current-1]);nextView.background=bubble(palette[next-1]);launcher.text=if(streak>=2)"✦" else "•";launcher.animate().cancel();launcher.scaleX=1f;launcher.scaleY=1f;if(streak>=2)launcher.animate().scaleX(1.12f).scaleY(1.12f).setDuration(360).withEndAction{launcher.animate().scaleX(1f).scaleY(1f).setDuration(360).start()}.start();hud.text=if(campaignComplete)"CAMPAIGN COMPLETE ✦ 250/250\nSCORE $score     BEST $best" else "SCORE  $score     BEST  $best     LEVEL  $level/250\nSTREAK  x$streak     ${if(streak>=2)"MAGIC READY ✦" else "NEXT"}";danger.text=if(campaignComplete)"ALL 250 LEVELS CLEARED • RESTART TO PLAY AGAIN" else "PRESSURE  $misses/${pressureLimit()}  •  MATCH 3+  •  3 COMBOS = MAGIC";danger.setTextColor(if(misses>=pressureLimit()-1)0xffff638f.toInt() else 0xffffc857.toInt())}
 /** Reflow the hex grid when split-screen, rotation or display scaling changes. */
 override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){
  super.onSizeChanged(w,h,oldw,oldh)
  if(w>0&&h>0&&oldw>0&&oldh>0&&(w!=oldw||h!=oldh))post{if(isAttachedToWindow&&!busy)rebuild()}
 }
 private fun addBubble(r:Int,c:Int,v:Int){field.addView(TextView(context).apply{gravity=Gravity.CENTER;text="•";textSize=12f;setTextColor(0xccffffff.toInt());background=bubble(palette[v-1]);elevation=dp(7).toFloat()},LayoutParams(bubbleSize(),bubbleSize()).apply{leftMargin=xFor(r,c);topMargin=yFor(r)})}
 override fun onTouchEvent(e:MotionEvent):Boolean{if(paused || busy || campaignComplete)return true;when(e.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{if(e.y<field.top+fieldTop||e.y>height-dp(82)){hideAim();return true};updateAim(e.x,e.y)};MotionEvent.ACTION_UP->{
   // Release outside the playable aiming area cancels the shot rather than
   // firing at the last valid position seen during a drag.
   val inside=e.y>=field.top+fieldTop && e.y<=height-dp(82)
   if(inside)updateAim(e.x,e.y)
   val target=if(inside)aiming else null
   val path=if(inside)aimPath.toList() else emptyList()
   hideAim()
   if(target!=null)shoot(target.first,target.second,path)
  };MotionEvent.ACTION_CANCEL->hideAim()};return true}
 // Aim at the first bubble encountered by a rising shot, rather than
 // teleporting the projectile to any open cell near the player's finger.
 private fun nearestOpen(x:Float,y:Float):Pair<Int,Int>?{
  val sx=width/2f;val sy=height-dp(64).toFloat()
  val dx=x-sx;val dy=y-sy
  if(dy>=-dp(12) || field.width<=0)return null
  val length=kotlin.math.sqrt(dx*dx+dy*dy).coerceAtLeast(1f)
  var vx=dx/length;val vy=dy/length
  var px=sx;var py=sy
  val radius=cell*.44f
  val left=field.left+gridLeft()+radius
  val right=field.left+gridLeft()+gridWidth()-radius
  var hitX=sx;var hitY=field.top+fieldTop+radius
  var collided:Pair<Int,Int>?=null
  val step=(cell*.22f).coerceAtLeast(2f)
  var hit=false
  for(i in 0 until 1500){
   px+=vx*step;py+=vy*step
   if(px<left){px=2*left-px;vx=-vx}
   if(px>right){px=2*right-px;vx=-vx}
   hitX=px;hitY=py
   if(py<=field.top+fieldTop+radius){hit=true;break}
   var collision=false
   for(r in 0 until rows){
    for(c in 0 until cols){
     if(grid[r][c]==0)continue
     val cx=field.left+xFor(r,c)+cell/2f
     val cy=field.top+yFor(r)+cell/2f
     val xx=px-cx;val yy=py-cy
     if(xx*xx+yy*yy<=(cell*.88f)*(cell*.88f)){
      collided=r to c
      collision=true;break
     }
    }
    if(collision)break
   }
   if(collision){hit=true;break}
  }
  if(!hit)return null
  var best:Pair<Int,Int>?=null;var distance=Float.MAX_VALUE
  // Only attach to the bubble actually hit (or the ceiling). Choosing the
  // nearest legal slot anywhere on the board teleports shots across gaps.
  val contact=collided
  val candidates=if(contact!=null)hexNeighbors(contact.first,contact.second)
      else (0 until cols).map{0 to it}
  for((r,c) in candidates){
   if(grid[r][c]!=0 || !isAttachable(r,c))continue
   val cx=field.left+xFor(r,c)+cell/2f
   val cy=field.top+yFor(r)+cell/2f
   val dd=(cx-hitX)*(cx-hitX)+(cy-hitY)*(cy-hitY)
   if(dd<distance){distance=dd;best=r to c}
  }
  return best
 }
 private fun isAttachable(r:Int,c:Int)=r==0||hexNeighbors(r,c).any{(rr,cc)->grid[rr][cc]!=0}
 private fun updateAim(x:Float,y:Float){
  val target=nearestOpen(x,y)?:run{hideAim();return}
  aiming=target
  // Preview the actual bank-shot direction, including wall reflections.
  // A straight line to the destination falsely suggests shots pass through bubbles.
  val sx=width/2f;val sy=height-dp(64).toFloat()
  val dx=x-sx;val dy=y-sy
  val length=kotlin.math.sqrt(dx*dx+dy*dy).coerceAtLeast(1f)
  var vx=dx/length;val vy=dy/length
  val radius=cell*.44f
  val left=field.left+gridLeft()+radius
  val right=field.left+gridLeft()+gridWidth()-radius
  val targetX=field.left+xFor(target.first,target.second)+cell/2f
  val targetY=field.top+yFor(target.first)+cell/2f
  val points=mutableListOf<Pair<Float,Float>>()
  var px=sx;var py=sy
  val step=(cell*.22f).coerceAtLeast(2f)
  for(i in 0 until 1500){
   px+=vx*step;py+=vy*step
   if(px<left){px=2*left-px;vx=-vx}
   if(px>right){px=2*right-px;vx=-vx}
   points.add(px to py)
   if(py<=field.top+fieldTop+radius ||
      (px-targetX)*(px-targetX)+(py-targetY)*(py-targetY)<=cell*cell*.75f)break
  }
  if(points.isEmpty()){hideAim();return}
  aimPath=points
  aimDots.forEachIndexed{i,d->
   val index=((i+1f)/(aimDots.size+1f)*points.lastIndex).toInt().coerceIn(0,points.lastIndex)
   val point=points[index]
   d.x=point.first-dp(3)/2f;d.y=point.second-dp(3)/2f
   val t=(i+1f)/(aimDots.size+1f)
   d.alpha=.42f+t*.58f;d.scaleX=.7f+t*.45f;d.scaleY=d.scaleX;d.visibility=View.VISIBLE
  }
 }
 // The launcher is also an accessible fire control. It uses the exact same
 // collision prediction and trajectory as a touch shot, never a shortcut.
 private fun fireStraightAhead(){
  if(paused || busy || campaignComplete || field.width<=0)return
  val x=width/2f
  val y=(field.top+fieldTop+dp(8)).toFloat()
  updateAim(x,y)
  val target=aiming
  val path=aimPath.toList()
  hideAim()
  if(target!=null)shoot(target.first,target.second,path)
 }
 private fun hideAim(){aiming=null;aimPath=emptyList();aimDots.forEach{it.visibility=View.GONE}}
 // Animate along the same wall-reflected trajectory shown by the aiming dots.
 // The final segment snaps to the validated hex-grid attachment cell.
 private fun shoot(r:Int,c:Int,path:List<Pair<Float,Float>>){
  if(busy || campaignComplete || r !in 0 until rows || c !in 0 until cols || grid[r][c]!=0 || !isAttachable(r,c))return
  busy=true
  activeShot=r to c
  val size=bubbleSize()
  val projectile=View(context).apply{background=bubble(palette[current-1]);elevation=dp(18).toFloat()}
  addView(projectile,LayoutParams(size,size))
  val start=width/2f to (height-dp(64)).toFloat()
  val destination=(field.left+xFor(r,c)+size/2f) to (field.top+yFor(r)+size/2f)
  val centers=ArrayList<Pair<Float,Float>>(path.size+2)
  centers.add(start)
  // Reduce redundant samples, but retain every reflection point for a smooth flight.
  // Stop at the first approach to the validated attachment point.
  // Never animate beyond the hit bubble and then fly backwards to the slot.
  // Prefer the first valid approach; if discrete tracing skips the threshold,
  // stop at the closest sampled point instead of flying to the ceiling and
  // snapping backwards to the attachment cell.
  val approachIndex=path.indexOfFirst{p->
   val dx=p.first-destination.first;val dy=p.second-destination.second
   dx*dx+dy*dy <= cell*cell*.75f
  }
  val stopIndex=if(approachIndex>=0)approachIndex else path.indices.minByOrNull{i->
   val dx=path[i].first-destination.first;val dy=path[i].second-destination.second
   dx*dx+dy*dy
  } ?: -1
  if(stopIndex>=0)path.forEachIndexed{i,p->if(i<=stopIndex && (i%3==0 || i==stopIndex))centers.add(p)}
  centers.add(destination)
  val lengths=FloatArray(centers.size)
  for(i in 1 until centers.size){
   val dx=centers[i].first-centers[i-1].first
   val dy=centers[i].second-centers[i-1].second
   lengths[i]=lengths[i-1]+kotlin.math.sqrt(dx*dx+dy*dy)
  }
  val total=lengths.last().coerceAtLeast(1f)
  projectileView=projectile
  flight=ValueAnimator.ofFloat(0f,total).apply{
   duration=(total/(cell.coerceAtLeast(1).toFloat())*48f).toLong().coerceIn(180L,1100L)
   interpolator=android.view.animation.LinearInterpolator()
   addUpdateListener{anim->
    val distance=anim.animatedValue as Float
    var index=1
    while(index<lengths.lastIndex && lengths[index]<distance)index++
    val segment=(lengths[index]-lengths[index-1]).coerceAtLeast(.001f)
    val fraction=((distance-lengths[index-1])/segment).coerceIn(0f,1f)
    val a=centers[index-1];val b=centers[index]
    projectile.x=a.first+(b.first-a.first)*fraction-size/2f
    projectile.y=a.second+(b.second-a.second)*fraction-size/2f
   }
   addListener(object:AnimatorListenerAdapter(){
    override fun onAnimationEnd(animation:Animator){
     flight=null;projectileView=null;removeView(projectile)
     activeShot=null
     if(paused)pendingSettlement=r to c else settle(r,c)
    }
   })
   start()
  }
 }

 private fun settle(r:Int,c:Int){if(r !in 0 until rows || c !in 0 until cols || grid[r][c]!=0 || !isAttachable(r,c)){busy=false;rebuild();return};grid[r][c]=current;val g=flood(r,c,current);if(g.size>=3){val color=current;g.forEach{(rr,cc)->grid[rr][cc]=0};streak++;val magic=streak>=3;val magicCells=if(magic)g.flatMap{(rr,cc)->hexNeighbors(rr,cc)}.filter{(rr,cc)->grid[rr][cc]!=0}.toSet() else emptySet();val magicDrops=magicCells.map{(rr,cc)->Triple(rr,cc,grid[rr][cc])};magicCells.forEach{(rr,cc)->grid[rr][cc]=0};val dropped=disconnectedCells().map{(rr,cc)->Triple(rr,cc,grid[rr][cc])};dropped.forEach{(rr,cc,_)->grid[rr][cc]=0};misses=(misses-2).coerceAtLeast(0);score+=g.size*45*level+(dropped.size+magicCells.size)*75*level+streak*35;if(magic){score+=500*level;streak=0;magicBurstFx(r,c)};popFx(g,color,dropped+magicDrops);performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)}else{streak=0;misses++;score+=10*level};if(misses>=pressureLimit()){if(!campaignComplete&&grid[rows-1].any{it!=0}){score=(score-level*150).coerceAtLeast(0);streak=0;newBoard()}else{pushPressureRow()};misses=0;performHapticFeedback(HapticFeedbackConstants.REJECT)};if(grid.all{row->row.all{it==0}}){val completedLevel=level;if(level<250){level++;score+=level*500;post{onLevelComplete(completedLevel)};newBoard()}else{campaignComplete=true;misses=0;post{onLevelComplete(completedLevel)}};streak=0};if(!campaignComplete&&grid[rows-1].any{it!=0}){score=(score-level*150).coerceAtLeast(0);streak=0;newBoard()};val availableColors=grid.flatMap{it.asList()}.filter{it>0}.toSet();current=if(next in availableColors)next else randomBoardColor();next=randomBoardColor();save();if(g.size>=3)postDelayed({busy=false;rebuild()},520)else{busy=false;rebuild()}}
 private fun magicBurstFx(r:Int,c:Int){val cx=field.left+xFor(r,c)+cell/2;val cy=field.top+yFor(r)+cell/2;repeat(3){ringIndex->val ring=TextView(context).apply{text="✦";gravity=Gravity.CENTER;textSize=(22+ringIndex*8).toFloat();setTextColor(if(ringIndex==1)0xffffc857.toInt() else 0xffa478ff.toInt());alpha=.95f;elevation=dp(30).toFloat()};val s=dp(58+ringIndex*18);fxLayer.addView(ring,LayoutParams(s,s).apply{leftMargin=cx-s/2;topMargin=cy-s/2});ring.scaleX=.2f;ring.scaleY=.2f;ring.animate().scaleX(2.8f).scaleY(2.8f).rotationBy(if(ringIndex%2==0)120f else -120f).alpha(0f).setStartDelay((ringIndex*55).toLong()).setDuration(480).withEndAction{fxLayer.removeView(ring)}.start()};field.animate().rotationBy(1.4f).setDuration(70).withEndAction{field.animate().rotation(0f).setDuration(120).start()}.start()}
 private fun hexNeighbors(r:Int,c:Int):List<Pair<Int,Int>>{val diagonal=if(r%2==0)arrayOf(-1 to -1,-1 to 0,1 to -1,1 to 0)else arrayOf(-1 to 0,-1 to 1,1 to 0,1 to 1);return (arrayOf(0 to -1,0 to 1)+diagonal).map{(dr,dc)->r+dr to c+dc}.filter{(rr,cc)->rr in 0 until rows&&cc in 0 until cols}}
 private fun flood(sr:Int,sc:Int,color:Int):Set<Pair<Int,Int>>{val seen=linkedSetOf<Pair<Int,Int>>();val q=ArrayDeque<Pair<Int,Int>>();q.add(sr to sc);while(q.isNotEmpty()){val p=q.removeFirst();if(p in seen)continue;val(r,c)=p;if(r !in 0 until rows||c !in 0 until cols||grid[r][c]!=color)continue;seen.add(p);hexNeighbors(r,c).forEach{q.add(it)}};return seen}
 private fun disconnectedCells():Set<Pair<Int,Int>>{val keep=mutableSetOf<Pair<Int,Int>>();val q=ArrayDeque<Pair<Int,Int>>();for(c in 0 until cols)if(grid[0][c]!=0)q.add(0 to c);while(q.isNotEmpty()){val p=q.removeFirst();if(!keep.add(p))continue;val(r,c)=p;hexNeighbors(r,c).forEach{(rr,cc)->if(grid[rr][cc]!=0&&(rr to cc)!in keep)q.add(rr to cc)}};val out=linkedSetOf<Pair<Int,Int>>();for(r in 0 until rows)for(c in 0 until cols)if(grid[r][c]!=0&&(r to c)!in keep)out+=r to c;return out}
 private fun popFx(group:Set<Pair<Int,Int>>,color:Int,dropped:List<Triple<Int,Int,Int>>){val baseColor=palette[color-1];group.forEachIndexed{i,(r,c)->val x=field.left+xFor(r,c)+cell/2;val y=field.top+yFor(r)+cell/2;repeat(6){k->val spark=View(context).apply{background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(if(k%2==0)0xffffd56a.toInt() else baseColor)};elevation=dp(20).toFloat()};val s=dp(7);fxLayer.addView(spark,LayoutParams(s,s).apply{leftMargin=x-s/2;topMargin=y-s/2});val angle=(k*60+i*17)*Math.PI/180.0;val dist=dp(28+(k%3)*8).toFloat();spark.animate().translationX((cos(angle)*dist).toFloat()).translationY((sin(angle)*dist).toFloat()).scaleX(.1f).scaleY(.1f).alpha(0f).setDuration(320).withEndAction{fxLayer.removeView(spark)}.start()}};dropped.forEachIndexed{i,(r,c,v)->val ghost=View(context).apply{background=bubble(palette[(v-1).coerceIn(0,palette.lastIndex)]);alpha=.95f;elevation=dp(12).toFloat()};val s=bubbleSize();fxLayer.addView(ghost,LayoutParams(s,s).apply{leftMargin=field.left+xFor(r,c);topMargin=field.top+yFor(r)});ghost.animate().translationY(dp(150+(i%4)*30).toFloat()).rotationBy(if(i%2==0)50f else -50f).scaleX(.6f).scaleY(.6f).alpha(0f).setDuration(440).withEndAction{fxLayer.removeView(ghost)}.start()};field.animate().scaleX(1.012f).scaleY(1.012f).setDuration(80).withEndAction{field.animate().scaleX(1f).scaleY(1f).setDuration(130).start()}.start()}
 private fun pushPressureRow(){
  // A descending pressure row must never silently discard the bottom row.
  // Treat imminent overflow as a lost board and preserve the earned level/best score.
  if(grid[rows-1].any{it!=0}){
   score=(score-level*150).coerceAtLeast(0);streak=0;newBoard();return
  }
  for(r in rows-1 downTo 1)for(c in 0 until cols)grid[r][c]=grid[r-1][c]
  val colors=(3+level/55).coerceAtMost(palette.size)
  for(c in 0 until cols)grid[0][c]=if(Random.nextFloat()>.12f)1+Random.nextInt(colors)else 0
 }
 private fun randomBoardColor():Int{val a=mutableListOf<Int>();for(r in 0 until rows)for(c in 0 until cols)if(grid[r][c]>0&&grid[r][c] !in a)a+=grid[r][c];return if(a.isEmpty())1+Random.nextInt(palette.size)else a.random()}
 fun swapBubble(){if(paused || busy || campaignComplete)return;val t=current;current=next;next=t;save();rebuild()};fun restart(){
  // Restart the current board without discarding unlocked campaign levels.
  cancelFlight();campaignComplete=false;streak=0;misses=0
  newBoard();current=randomBoardColor();next=randomBoardColor()
  save();rebuild()
 }
 fun videoRescue(){
  if(busy || campaignComplete)return
  // Rescue the actual lowest occupied row. Clearing the last three empty
  // rows did nothing on most boards while still consuming the rescue action.
  val lowest=(rows-1 downTo 0).firstOrNull{r->grid[r].any{it!=0}}?:return
  grid[lowest].fill(0)
  // Remove floating clusters so the board remains physically consistent.
  disconnectedCells().forEach{(r,c)->grid[r][c]=0}
  misses=0
  streak=0
  val colors=grid.flatMap{it.asList()}.filter{it>0}.toSet()
  if(colors.isEmpty()){
   val completedLevel=level
   if(level<250){level++;score+=level*500;post{onLevelComplete(completedLevel)};newBoard()}else{campaignComplete=true;misses=0;post{onLevelComplete(completedLevel)}}
  }
  val active=grid.flatMap{it.asList()}.filter{it>0}.toSet()
  if(current !in active)current=randomBoardColor()
  if(next !in active)next=randomBoardColor()
  save()
  rebuild()
 }
}

package com.weearntogether.play

import android.content.Intent
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.util.Locale

/** Read-only projections from authenticated ledger APIs. No local money minting. */
class WalletActivity:Activity(){
    private val bg=0xff080e22.toInt();private val card=0xff15203e.toInt();private val gold=0xffffce70.toInt();private val muted=0xffa9b8d8.toInt()
    private lateinit var root:LinearLayout;private lateinit var status:TextView
    private lateinit var accounts:AccountStore;private val api=BackendClient();private val handler=Handler(Looper.getMainLooper())
    private var firstResume=true;private var showingProfile=false
    private var generation=0;private var waiting=false;private var watch:Runnable?=null;private var currentBalance:JSONObject?=null
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun shape(c:Int,r:Int=20)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun euro(c:Long):String=String.format(Locale.US,"€%d.%02d",c/100,kotlin.math.abs(c%100))
    override fun onCreate(state:Bundle?){super.onCreate(state);accounts=AccountStore(this);window.statusBarColor=bg;window.navigationBarColor=bg;if(!accounts.isSignedIn()){finish();return};profile()}
    private fun page(title:String){showingProfile=title=="PROFILE";generation++;watch?.let{handler.removeCallbacks(it)};waiting=false;root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(22),dp(20),dp(30));setBackgroundColor(bg)};val scroll=ScrollView(this).apply{isFillViewport=true;setBackgroundColor(bg);addView(root)};scroll.setOnApplyWindowInsetsListener{_,insets->root.setPadding(dp(20)+insets.systemWindowInsetLeft,dp(22)+insets.systemWindowInsetTop,dp(20)+insets.systemWindowInsetRight,dp(30)+insets.systemWindowInsetBottom);insets};setContentView(scroll);scroll.requestApplyInsets();root.addView(text(title,28,gold,true));status=text("",13,muted);root.addView(status)}
    private fun text(value:String,size:Int=15,color:Int=Color.WHITE,bold:Boolean=false)=TextView(this).apply{this.text=value;textSize=size.toFloat();setTextColor(color);setPadding(0,dp(8),0,dp(8));if(bold)setTypeface(null,Typeface.BOLD)}
    private fun button(label:String,enabled:Boolean=true,action:()->Unit):Button{val b=Button(this).apply{text=label;isAllCaps=false;textSize=14f;setTextColor(if(enabled)bg else muted);setTypeface(null,Typeface.BOLD);background=shape(if(enabled)gold else card,16);isEnabled=enabled;setOnClickListener{action()}};root.addView(b,LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(12)});return b}
    private fun box(title:String,value:String,subtitle:String=""){val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=shape(card);setPadding(dp(18),dp(10),dp(18),dp(14))};panel.addView(text(title,12,muted,true));panel.addView(text(value,29,gold,true));if(subtitle.isNotEmpty())panel.addView(text(subtitle,12,muted));root.addView(panel,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})}
    private fun call(action:String,data:JSONObject=JSONObject(),onError:(()->Unit)?=null,done:(JSONObject)->Unit){if(waiting)return;waiting=true;val id=generation;status.text="Connecting to your secure ledger…";val timeout=Runnable{if(id==generation&&waiting){waiting=false;generation++;status.text="The account service took too long. Reopen this screen or try again.";onError?.invoke()}};watch=timeout;handler.postDelayed(timeout,70_000)
        AuthWorker.executor.execute{val result=runCatching{api.request(action,JSONObject(data.toString()).put("session_token",accounts.requireToken()))};runOnUiThread{if(!isFinishing&&!isDestroyed&&id==generation&&waiting){handler.removeCallbacks(timeout);waiting=false;result.onSuccess{status.text="";done(it)}.onFailure{status.text=it.message?:"Could not connect. Please try again.";onError?.invoke()}}}}}
    private fun profile(){page("PROFILE");button("BACK TO APP"){finish()};call("getWalletProfile"){r->val p=r.getJSONObject("profile");val b=r.getJSONObject("balance");currentBalance=b
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff482879.toInt(),0xff154986.toInt())).apply{cornerRadius=dp(22).toFloat()}}
        val name=p.getString("username");header.addView(TextView(this).apply{text=name.take(1).uppercase(Locale.ROOT);gravity=Gravity.CENTER;textSize=28f;setTextColor(bg);background=shape(gold,40);setTypeface(null,Typeface.BOLD)},LinearLayout.LayoutParams(dp(64),dp(64)))
        val identity=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),0,0,0)};identity.addView(text(name,21,Color.WHITE,true));identity.addView(text(p.getString("email"),13));header.addView(identity,LinearLayout.LayoutParams(0,-2,1f));root.addView(header,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
        root.addView(text("User ID: "+p.getString("user_id"),11,muted));root.addView(text("Joined: "+p.optString("joined_date").take(10),12,muted))
        box("PENDING COINS",b.optLong("pending_coins").toString(),"Awaiting independent verification. No cash value promised.")
        box("VERIFIED COINS",b.optLong("verified_coins").toString(),"Eligible coins may be allocated to an approved, funded period.")
        box("AVAILABLE CASH",euro(b.optLong("available_cents")),"Verified cash after reservations and completed payments.")
        box("TOTAL EARNED",euro(b.optLong("total_earned_cents")));box("TOTAL PAID",euro(b.optLong("total_paid_cents")));box("PENDING CASH OUT",euro(b.optLong("pending_cashout_cents")))
        button("CASH OUT",b.optBoolean("cashout_allowed")){cashout()};if(!b.optBoolean("cashout_allowed"))root.addView(text("Minimum cash out is €5.00",13,muted))
        button("OFFERS & REWARDS • ayeT"){startActivity(Intent(this,AyetOfferwallActivity::class.java))};button("COIN HISTORY"){coinHistory(0)};button("TRANSACTION HISTORY"){transactionHistory(0)};button("CASH OUT HISTORY"){cashoutHistory(0)};button("REFRESH PROFILE"){profile()}
    }}
    private fun cashout(){page("CASH OUT");button("BACK TO PROFILE"){profile()};call("getCashoutEligibility"){r->val b=r.getJSONObject("balance");box("AVAILABLE",euro(b.optLong("available_cents")));box("MINIMUM CASH OUT","€5.00");box("PENDING CASH OUT",euro(b.optLong("pending_cashout_cents")));box("TOTAL PAID",euro(b.optLong("total_paid_cents")));root.addView(text("Payment method: PayPal · manual verified payout",14));root.addView(text("Enter your own PayPal email. It is used only to review and process your payout request.",13,muted));val destination=EditText(this).apply{hint="PayPal email";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;setTextColor(Color.WHITE);setHintTextColor(muted);background=shape(card);setPadding(dp(16),dp(12),dp(16),dp(12));setSingleLine(true)};root.addView(destination,LinearLayout.LayoutParams(-1,dp(54)))
        val amount=EditText(this).apply{hint="Amount in EUR, e.g. 5.00";inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL;setTextColor(Color.WHITE);setHintTextColor(muted);background=shape(card);setPadding(dp(16),dp(12),dp(16),dp(12));setSingleLine(true)};root.addView(amount,LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(12)})
        val requestKey=java.util.UUID.randomUUID().toString();lateinit var send:Button
        send=button("REQUEST CASH OUT",r.optBoolean("eligible")){val email=destination.text.toString().trim();val money=amount.text.toString().trim().replace(',','.');if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){status.text="Enter your PayPal email";return@button};if(!Regex("^(0|[1-9][0-9]{0,9})(\\.[0-9]{1,2})?$").matches(money)){status.text="Enter an amount with at most two decimal places";return@button}
            AlertDialog.Builder(this).setTitle("Confirm cash out request").setMessage("Request €$money for $email? The amount will be reserved immediately for manual verification.").setNegativeButton("Cancel",null).setPositiveButton("Request"){_,_->send.isEnabled=false;call("requestCashout",JSONObject().put("amount",money).put("payment_method","PAYPAL").put("payment_destination",email).put("idempotency_key",requestKey),onError={send.isEnabled=true}){status.text="Cash out request submitted for verification.";button("VIEW CASH OUT HISTORY"){cashoutHistory(0)}}}.show()
        };if(!r.optBoolean("eligible"))root.addView(text("Minimum cash out is €5.00",13,muted))
    }}
    private fun coinHistory(offset:Int){page("COIN HISTORY");button("BACK TO PROFILE"){profile()};call("getCoinHistory",JSONObject().put("offset",offset)){r->val list=r.getJSONArray("transactions");if(list.length()==0)root.addView(text("No coin transactions yet.",15,muted));for(i in 0 until list.length()){val x=list.getJSONObject(i);box(x.optString("source")+" · "+x.optString("status"),x.optLong("coins").toString()+" coins",x.optString("game_id")+"\n"+x.optString("timestamp").take(19)+"\n"+x.optString("verification_reason"))};if(offset+list.length()<r.optInt("total"))button("NEXT PAGE"){coinHistory(offset+50)}}}
    private fun transactionHistory(offset:Int){page("TRANSACTION HISTORY");button("BACK TO PROFILE"){profile()};call("getTransactionHistory",JSONObject().put("offset",offset)){r->val list=r.getJSONArray("transactions");if(list.length()==0)root.addView(text("No verified cash transactions yet.",15,muted));for(i in 0 until list.length()){val x=list.getJSONObject(i);box(x.optString("event_type")+" · "+x.optString("status"),euro(x.optLong("amount_cents")),x.optString("timestamp").take(19)+"\n"+x.optString("transaction_id"))};if(offset+list.length()<r.optInt("total"))button("NEXT PAGE"){transactionHistory(offset+50)}}}
    private fun cashoutHistory(offset:Int){page("CASH OUT HISTORY");button("BACK TO PROFILE"){profile()};call("getCashoutHistory",JSONObject().put("offset",offset)){r->val list=r.getJSONArray("cashouts");if(list.length()==0)root.addView(text("No cash out requests yet.",15,muted));for(i in 0 until list.length()){val x=list.getJSONObject(i);box(x.optString("payment_method")+" · "+x.optString("status"),euro(x.optLong("amount_cents")),x.optString("requested_at").take(19)+"\nReference: "+x.optString("payment_reference").ifBlank{"Awaiting manual verification"}+"\n"+x.optString("review_note"));if(x.optString("status")=="REQUESTED")button("CANCEL REQUEST"){call("cancelCashout",JSONObject().put("cashout_id",x.getString("cashout_id"))){cashoutHistory(offset)}}};if(offset+list.length()<r.optInt("total"))button("NEXT PAGE"){cashoutHistory(offset+50)}}}
    override fun onResume(){super.onResume();if(firstResume){firstResume=false}else if(showingProfile&&!waiting&&accounts.isSignedIn())profile()}
    override fun onDestroy(){generation++;watch?.let{handler.removeCallbacks(it)};super.onDestroy()}
}

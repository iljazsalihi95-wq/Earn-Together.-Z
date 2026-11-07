import { progressAction } from './progress.js';
// Native Cloudflare account service. No Google Apps Script requests or client money authority.
const headers={'content-type':'application/json; charset=utf-8','cache-control':'no-store'};
const reply=(value,status=200)=>new Response(JSON.stringify(value),{status,headers});
class AccountError extends Error{constructor(code,status=400){super(code);this.status=status;}}
const fail=(code,status)=>{throw new AccountError(code,status)};
const enc=new TextEncoder();
const hex=bytes=>Array.from(new Uint8Array(bytes),n=>n.toString(16).padStart(2,'0')).join('');
const digest=async value=>hex(await crypto.subtle.digest('SHA-256',enc.encode(value)));
const random=()=>hex(crypto.getRandomValues(new Uint8Array(32)));
const email=value=>{const s=String(value||'').trim().toLowerCase();if(!/^[^\s@]{1,100}@[^\s@]{1,150}\.[^\s@]{2,30}$/.test(s))fail('INVALID_EMAIL');return s};
const publicUser=u=>({user_id:u.user_id,name:u.name,email:u.email,status:u.status,created_at:u.created_at});
async function passwordHash(value,salt){
 const key=await crypto.subtle.importKey('raw',enc.encode(value),'PBKDF2',false,['deriveBits']);
 return hex(await crypto.subtle.deriveBits({name:'PBKDF2',salt:enc.encode(salt),iterations:100000,hash:'SHA-256'},key,256));
}
function password(value){if(typeof value!=='string'||value.length<8||value.length>256)fail('INVALID_PASSWORD');return value}
function equal(a,b){if(typeof a!=='string'||typeof b!=='string'||a.length!==b.length)return false;let d=0;for(let i=0;i<a.length;i++)d|=a.charCodeAt(i)^b.charCodeAt(i);return d===0}
function decode(value){try{return Uint8Array.from(atob(value.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0))}catch{fail('GOOGLE_TOKEN_INVALID',401)}}
export async function googleIdentity(token,env){
 if(typeof token!=='string'||token.length>12000)fail('GOOGLE_TOKEN_INVALID',401);
 const parts=token.split('.');if(parts.length!==3)fail('GOOGLE_TOKEN_INVALID',401);
 let head,claims;try{head=JSON.parse(new TextDecoder().decode(decode(parts[0])));claims=JSON.parse(new TextDecoder().decode(decode(parts[1])))}catch{fail('GOOGLE_TOKEN_INVALID',401)}
 if(head.alg!=='RS256'||typeof head.kid!=='string')fail('GOOGLE_TOKEN_INVALID',401);
 const now=Math.floor(Date.now()/1000);
 if(!['https://accounts.google.com','accounts.google.com'].includes(claims.iss)||claims.aud!==env.GOOGLE_WEB_CLIENT_ID||!env.GOOGLE_WEB_CLIENT_ID||claims.email_verified!==true||typeof claims.sub!=='string'||!claims.sub||!Number.isFinite(claims.exp)||claims.exp<=now||!Number.isFinite(claims.iat)||claims.iat>now+60)fail('GOOGLE_TOKEN_INVALID',401);
 const response=await fetch('https://www.googleapis.com/oauth2/v3/certs');if(!response.ok)fail('GOOGLE_UNAVAILABLE',503);
 const keys=await response.json();const jwk=keys.keys?.find(k=>k.kid===head.kid&&k.kty==='RSA');if(!jwk)fail('GOOGLE_TOKEN_INVALID',401);
 const key=await crypto.subtle.importKey('jwk',jwk,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['verify']);
 if(!await crypto.subtle.verify('RSASSA-PKCS1-v1_5',key,decode(parts[2]),enc.encode(parts[0]+'.'+parts[1])))fail('GOOGLE_TOKEN_INVALID',401);
 return {...claims,email:email(claims.email)};
}
export class NativeAccounts {
 constructor(state,env){this.storage=state.storage;this.env=env}
 async fetch(request){
  try{
   if(request.method!=='POST')fail('METHOD_NOT_ALLOWED',405);
   const p=await request.json();if(!p||Array.isArray(p)||typeof p!=='object')fail('INVALID_REQUEST');
   // Persist rate limits independently of failed authentication transactions.
   const ip=request.headers.get('x-native-client-ip')||'unknown';
   await this.storage.transaction(async tx=>{
    const now=Date.now(),key='rate:'+ip;
    const rate=await tx.get(key)||{start:now,count:0};
    if(now-rate.start>60000){rate.start=now;rate.count=0}
    if(rate.count>=30)fail('RATE_LIMIT',429);
    rate.count++;await tx.put(key,rate);
   });
   // Network and slow password hashing stay outside storage transactions.
   let identity,hash,salt;
   if(['googleSignIn','linkGoogle'].includes(p.action))identity=await googleIdentity(p.id_token,this.env);
   if(p.action==='register'){email(p.email);password(p.password);salt=random();hash=await passwordHash(p.password,salt)}
   if(p.action==='login'){
    password(p.password);const id=await this.storage.get('email:'+email(p.email));const u=id&&await this.storage.get('user:'+id);
    // Always hash, including unknown addresses, to avoid a cheap account-existence oracle.
    hash=await passwordHash(p.password,u?.salt||'unknown-account-salt');
   }
   const token=p.session_token;const tokenHash=typeof token==='string'?await digest(token):'';
   const result=await this.storage.transaction(async tx=>{
    const now=Date.now();
    const issue=async u=>{const session_token=random();const expires_at=new Date(now+7*86400000).toISOString();await tx.put('session:'+await digest(session_token),{user_id:u.user_id,expires:Date.parse(expires_at)});return {success:true,...publicUser(u),session_token,expires_at}};
    let session=tokenHash&&await tx.get('session:'+tokenHash);let user=session&&session.expires>now&&await tx.get('user:'+session.user_id);
    if(user?.status!=='ACTIVE')user=null;
    if(p.action==='googleChallenge'){const nonce=random();await tx.put('nonce:'+await digest(nonce),now+300000);return {success:true,nonce,expires_in:300}}
    if(p.action==='register'){
     const address=email(p.email);if(await tx.get('email:'+address))fail('EMAIL_EXISTS',409);
     const name=String(p.name||'').trim();if(name.length<2||name.length>100)fail('INVALID_NAME');
     const u={user_id:crypto.randomUUID(),email:address,name,status:'ACTIVE',created_at:new Date(now).toISOString(),salt,password_hash:hash};
     await tx.put('user:'+u.user_id,u);await tx.put('email:'+address,u.user_id);return issue(u);
    }
    if(p.action==='login'){
     const id=await tx.get('email:'+email(p.email));const u=id&&await tx.get('user:'+id);
     if(!u||u.status!=='ACTIVE'||!equal(u.password_hash,hash))fail('INVALID_CREDENTIALS',401);return issue(u);
    }
    if(identity){
     const nonceKey='nonce:'+await digest(String(identity.nonce||''));const expires=await tx.get(nonceKey);if(!expires||expires<now)fail('GOOGLE_TOKEN_INVALID',401);await tx.delete(nonceKey);
     const linked=await tx.get('google:'+identity.sub);const existing=await tx.get('email:'+identity.email);
     if(p.action==='linkGoogle'){
      if(!user)fail('SESSION_EXPIRED',401);if(user.email!==identity.email||(linked&&linked!==user.user_id)||(user.google_id&&user.google_id!==identity.sub))fail('GOOGLE_LINK_FAILED',409);
      user.google_id=identity.sub;await tx.put('google:'+identity.sub,user.user_id);await tx.put('user:'+user.user_id,user);return issue(user);
     }
     if(linked){const u=await tx.get('user:'+linked);if(!u||u.status!=='ACTIVE')fail('ACCOUNT_DISABLED',403);return issue(u)}
     if(existing)fail('GOOGLE_LINK_REQUIRED',409);
     const u={user_id:crypto.randomUUID(),email:identity.email,name:String(identity.name||'Google Player').slice(0,100),status:'ACTIVE',created_at:new Date(now).toISOString(),google_id:identity.sub};await tx.put('user:'+u.user_id,u);await tx.put('email:'+u.email,u.user_id);await tx.put('google:'+identity.sub,u.user_id);return issue(u);
    }
    if(!user)fail('SESSION_EXPIRED',401);
    if(p.action==='logout'){await tx.delete('session:'+tokenHash);return {success:true}}
    if(['validateSession','getProfile'].includes(p.action))return {success:true,...publicUser(user),expires_at:new Date(session.expires).toISOString()};
    if(p.action==='updateProfile'){const name=String(p.name||'').trim();if(name.length<2||name.length>100)fail('INVALID_NAME');user.name=name;await tx.put('user:'+user.user_id,user);return {success:true,...publicUser(user)}}
    if(['getProgress','saveProgress','syncProgress'].includes(p.action)){
     try{return await progressAction(tx,user.user_id,p)}catch(e){
      if(['INVALID_GAME','INVALID_PROGRESS'].includes(e.message))fail(e.message);
      throw e;
     }
    }
    // Values below are account-scoped. No action accepts balances, money or approval from Android.
    if(['balance','coins','verified','status','amount_cents','user_id'].some(k=>p[k]!==undefined))fail('CLIENT_AUTHORITY');
    const prefix='wallet:'+user.user_id+':';
    const wallet=await tx.get(prefix+'balance')||{game_coins:0,pending_coins:0,verified_coins:0,available_cents:0,total_earned_cents:0,total_paid_cents:0,pending_cashout_cents:0,cashout_allowed:false};
    if(['getBalance','getCashoutEligibility','getWalletProfile'].includes(p.action))return {success:true,...wallet,eligible:false,balance:wallet,profile:{username:user.name,email:user.email,user_id:user.user_id,joined_date:user.created_at},reward_rules:{video_reward_coins:100,level_game_coins:20,video_status:'PENDING',automatic_cash_conversion:false}};
    if(p.action==='getOfferwallConfig')return {success:true,provider:'ayet',enabled:!!this.env.AYET_OFFERWALL_URL,url:this.env.AYET_OFFERWALL_URL||'',rewards_require_verified_partner_callback:true};
    if(['convertCoins','requestCashout'].includes(p.action))fail('PARTNER_APPROVAL_REQUIRED',409);
    if(['getCoinHistory','getTransactionHistory','getCashoutHistory'].includes(p.action)){
     const offset=Number(p.offset||0);if(!Number.isSafeInteger(offset)||offset<0)fail('INVALID_OFFSET');
     const rows=Array.from((await tx.list({prefix:prefix+'event:'})).values()).sort((a,b)=>b.timestamp.localeCompare(a.timestamp));
     if(p.action!=='getCoinHistory')return {success:true,total:0,transactions:[],cashouts:[]};return {success:true,total:rows.length,transactions:rows.slice(offset,offset+50)};
    }
    if(p.action==='startRewardSession'){
     const game=String(p.game_id||'');if(!/^[A-Za-z0-9 _-]{1,100}$/.test(game))fail('INVALID_GAME');
     const id=crypto.randomUUID();await tx.put(prefix+'round:'+id,{game,started:now,expires:now+7200000});return {success:true,session_id:id,ad_reward_coins:100,expires_at:new Date(now+7200000).toISOString()};
    }
    if(p.action==='recordRewardEvent'){
     if(!['LEVEL','AD_REWARD'].includes(p.source))fail('INVALID_SOURCE');
     const event=String(p.event_id||'');if(!/^[A-Za-z0-9_-]{8,120}$/.test(event))fail('INVALID_EVENT');
     if(p.source==='LEVEL'&&(!Number.isSafeInteger(p.level)||p.level<1||p.level>10000))fail('INVALID_LEVEL');
     const round=await tx.get(prefix+'round:'+p.session_id);if(!round||round.game!==p.game_id||round.expires<=now)fail('INVALID_SESSION');
     const receipt=prefix+'receipt:'+(p.source==='LEVEL'?p.game_id+':'+p.level:event);
     const previous=await tx.get(receipt);if(previous)return {...previous,duplicate:true};
     if(now-round.started<10000)fail('IMPOSSIBLE_INTERVAL');
     const last=await tx.get(prefix+'last_reward')||0;if(now-last<5000)fail('RATE_LIMIT',429);
     const coins=p.source==='LEVEL'?20:100;const status=p.source==='LEVEL'?'GAME_ONLY':'PENDING';
     const row={transaction_id:crypto.randomUUID(),source:p.source,game_id:p.game_id,coins,status,timestamp:new Date(now).toISOString(),verification_reason:status==='PENDING'?'Awaiting independent partner verification':'Noncash gameplay currency'};
     if(p.source==='LEVEL')wallet.game_coins+=coins;else wallet.pending_coins+=coins;
     const response={success:true,transaction_id:row.transaction_id,status,coins,balance:wallet};
     await tx.put(prefix+'event:'+row.transaction_id,row);await tx.put(prefix+'balance',wallet);await tx.put(prefix+'last_reward',now);await tx.put(receipt,response);return response;
    }
    fail('INVALID_ACTION');
   });return reply(result);
  }catch(e){return reply({success:false,error:e instanceof AccountError?e.message:'INTERNAL_ERROR'},e instanceof AccountError?e.status:500)}
 }
}

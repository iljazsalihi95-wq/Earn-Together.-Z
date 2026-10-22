// Extends the existing backend. Rename its route_ function to emailRoute_ once.
// OAuth client IDs are public identifiers. No client secret is required or stored.
const GOOGLE_WEB_CLIENT='726413248892-hvdca11ahhbb1lm6in5sdviglep69494.apps.googleusercontent.com';
const GOOGLE_ANDROID_CLIENT='726413248892-gbdnvfkep8f9ro7p05ivktnef1a93a1p.apps.googleusercontent.com';

function route_(p){
 if(p.action==='googleChallenge'){
  const nonce=(uid_()+uid_()+uid_()).replace(/-/g,'');
  CacheService.getScriptCache().put('google_nonce_'+digest_(nonce),'1',300);
  return {success:true,nonce,expires_in:300};
 }
 if(p.action!=='googleSignIn'&&p.action!=='linkGoogle')return emailRoute_(p);
 const identity=verifyGoogleToken_(p.id_token);
 const nonce=String(identity.nonce||'');
 if(!/^[a-f0-9]{96}$/.test(nonce))fail_('GOOGLE_TOKEN_INVALID','Google sign-in could not be verified. Try again.');
 const lock=LockService.getScriptLock();lock.waitLock(30000);
 try{
  const cache=CacheService.getScriptCache(),key='google_nonce_'+digest_(nonce);
  if(cache.get(key)!=='1')fail_('GOOGLE_TOKEN_INVALID','Google sign-in expired. Try again.');
  cache.remove(key); // Single use, including failed account-link attempts.
  const users=rows_('USERS');
  const linked=users.find(u=>String(u.google_id)===identity.sub);
  let u;
  if(p.action==='linkGoogle'){
   u=auth_(p).u;
   if((linked&&linked.user_id!==u.user_id)||(u.google_id&&String(u.google_id)!==identity.sub)||String(u.email).toLowerCase()!==identity.email)
    fail_('GOOGLE_LINK_FAILED','Use the Google account with the same email as your player account.');
   // Linking always requires an existing valid player session plus a fresh Google proof.
   u.google_id=identity.sub;
  }else if(linked){
   u=linked;
  }else{
   if(users.some(x=>String(x.email).toLowerCase()===identity.email))
    fail_('ACCOUNT_LINK_REQUIRED','Sign in with your existing password, then select LINK GOOGLE ACCOUNT in Profile.');
   const now=iso_();
   u={user_id:uid_(),name:String(identity.name||'Google Player').trim().slice(0,100)||'Google Player',email:identity.email,password_hash:'',password_salt:'',created_at:now,last_login:now,status:'ACTIVE',provider:'GOOGLE',google_id:identity.sub};
  }
  if(u.status!=='ACTIVE')fail_('GOOGLE_LOGIN_FAILED','Google sign-in could not be completed.');
  u.last_login=iso_();write_('USERS',u,u._row);
  return newSession_(u);
 }finally{lock.releaseLock();}
}

function googleInvalid_(){fail_('GOOGLE_TOKEN_INVALID','Google sign-in could not be verified. Try again.');}
function googleBytes_(s){return Utilities.base64DecodeWebSafe(s).map(x=>x&255);}
function googleJson_(s){return JSON.parse(Utilities.newBlob(googleBytes_(s).map(x=>x>127?x-256:x)).getDataAsString('UTF-8'));}
function googleInteger_(bytes){return BigInt('0x'+hex_(bytes));}
// Public-key RSASSA-PKCS1-v1_5 verification, SHA-256 only. No private-key operations.
function googleRsaVerify_(message,signature,jwk){
 if(jwk.kty!=='RSA'||jwk.alg!=='RS256'||jwk.use!=='sig')return false;
 const modulus=googleBytes_(jwk.n),sig=googleBytes_(signature);
 if(modulus.length<256||modulus.length>512||sig.length!==modulus.length)return false;
 const n=googleInteger_(modulus),s=googleInteger_(sig),e=googleInteger_(googleBytes_(jwk.e));
 if(s>=n||e!==BigInt(65537))return false;
 let value=BigInt(1),base=s,power=e;
 while(power>BigInt(0)){if(power%BigInt(2)===BigInt(1))value=value*base%n;base=base*base%n;power=power/BigInt(2);}
 const actual=value.toString(16).padStart(modulus.length*2,'0');
 const hash=digest_(message),info='3031300d060960864801650304020105000420'+hash;
 const paddingBytes=modulus.length-3-info.length/2;
 if(paddingBytes<8)return false;
 return safeEqual_(actual,'0001'+'ff'.repeat(paddingBytes)+'00'+info);
}
function googleKeys_(){
 const cache=CacheService.getScriptCache(),cached=cache.get('google_jwks_v1');
 if(cached)return JSON.parse(cached).keys;
 const response=UrlFetchApp.fetch('https://www.googleapis.com/oauth2/v3/certs',{muteHttpExceptions:true,followRedirects:false});
 if(response.getResponseCode()!==200)fail_('GOOGLE_UNAVAILABLE','Google verification is temporarily unavailable.');
 const raw=response.getContentText(),data=JSON.parse(raw);
 if(!Array.isArray(data.keys)||data.keys.length<1||data.keys.length>20)googleInvalid_();
 // Short cache follows key rotation without accepting keys supplied by the caller.
 cache.put('google_jwks_v1',raw,300);return data.keys;
}
function verifyGoogleToken_(token){
 if(typeof token!=='string'||token.length>12000)googleInvalid_();
 const parts=token.split('.');if(parts.length!==3||parts.some(x=>!(/^[A-Za-z0-9_-]+$/).test(x)))googleInvalid_();
 let header,claims;try{header=googleJson_(parts[0]);claims=googleJson_(parts[1]);}catch(_){googleInvalid_();}
 if(header.alg!=='RS256'||typeof header.kid!=='string'||header.crit!==undefined)googleInvalid_();
 const key=googleKeys_().find(k=>k.kid===header.kid);
 if(!key||!googleRsaVerify_(parts[0]+'.'+parts[1],parts[2],key))googleInvalid_();
 const now=Math.floor(Date.now()/1000);
 if(!['accounts.google.com','https://accounts.google.com'].includes(claims.iss)||claims.aud!==GOOGLE_WEB_CLIENT||
  (claims.azp!==undefined&&![GOOGLE_WEB_CLIENT,GOOGLE_ANDROID_CLIENT].includes(claims.azp))||
  !Number.isSafeInteger(claims.exp)||claims.exp<=now||!Number.isSafeInteger(claims.iat)||claims.iat>now+60||claims.iat<now-3600||
  (claims.nbf!==undefined&&(!Number.isSafeInteger(claims.nbf)||claims.nbf>now))||
  typeof claims.sub!=='string'||!(/^[0-9]{1,255}$/).test(claims.sub)||claims.email_verified!==true||typeof claims.email!=='string')googleInvalid_();
 claims.email=email_(claims.email);return claims;
}

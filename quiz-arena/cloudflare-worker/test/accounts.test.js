import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
import {NativeAccounts,googleIdentity} from '../src/accounts.js';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
function service(env={}){
 const data=new Map();
 const storage={get:async k=>structuredClone(data.get(k)),put:async(k,v)=>{data.set(k,structuredClone(v))},delete:async k=>data.delete(k),list:async({prefix})=>new Map([...data].filter(([k])=>k.startsWith(prefix))),transaction:async fn=>fn(storage)};
 const api=new NativeAccounts({storage},env);
 return {data,call:async(p,ip='127.0.0.1')=>{const r=await api.fetch(new Request('https://accounts/api',{method:'POST',headers:{'x-native-client-ip':ip},body:JSON.stringify(p)}));return {status:r.status,...await r.json()}}};
}
test('native accounts isolate balances, hash passwords, revoke sessions and keep video claims noncash',async()=>{
 const {data,call}=service();
 const a=await call({action:'register',name:'Player A',email:'a@example.test',password:'password-A'});
 assert.equal(a.success,true);assert.ok(a.session_token);assert.equal(a.password_hash,undefined);
 const u=[...data].find(([k])=>k.startsWith('user:'))[1];assert.notEqual(u.password_hash,'password-A');
 assert.equal((await call({action:'login',email:'a@example.test',password:'wrong-password'})).status,401);
 const b=await call({action:'register',name:'Player B',email:'b@example.test',password:'password-B'});
 const act=(action,p={})=>call({action,session_token:a.session_token,...p});
 assert.equal((await act('getWalletProfile')).reward_rules.video_reward_coins,100);
 assert.equal((await act('getBalance',{coins:9999})).error,'CLIENT_AUTHORITY');
 const s=await act('startRewardSession',{game_id:'html5_block'});
 const key=[...data.keys()].find(k=>k.endsWith('round:'+s.session_id));data.get(key).started-=11000;
 const payload={source:'AD_REWARD',session_id:s.session_id,game_id:'html5_block',event_id:'video-00000001'};
 const video=await act('recordRewardEvent',payload);assert.equal(video.status,'PENDING');assert.equal(video.coins,100);
 assert.equal((await act('recordRewardEvent',payload)).duplicate,true);
 const balance=await act('getBalance');assert.equal(balance.pending_coins,100);assert.equal(balance.available_cents,0);
 assert.equal((await call({action:'getBalance',session_token:b.session_token})).pending_coins,0);
 assert.equal((await act('requestCashout',{amount:'5.00'})).error,'PARTNER_APPROVAL_REQUIRED');
 assert.equal((await act('convertCoins')).error,'PARTNER_APPROVAL_REQUIRED');
 assert.equal((await act('getCoinHistory')).transactions.length,1);
 assert.equal((await act('getOfferwallConfig')).enabled,false);
 const save=await act('saveProgress',{game:'Block Puzzle',level:3,score:90,best_score:90,progress_json:'{"board":[1,0]}'});
 assert.equal(save.progress[0].conflict,false);
 assert.equal((await act('getProgress')).progress[0].level,3);
 assert.equal((await call({action:'getProgress',session_token:b.session_token})).progress.length,0);
 assert.equal((await act('logout')).success,true);
 assert.equal((await act('getBalance')).status,401);
});
test('failed credentials remain rate limited before password work',async()=>{
 const {call}=service();
 for(let i=0;i<30;i++)assert.equal((await call({action:'getBalance'})).status,401);
 assert.equal((await call({action:'register',name:'Attacker',email:'a@example.test',password:'password-A'})).status,429);
});
test('forged Google identity is rejected before account creation',async()=>{
 await assert.rejects(()=>googleIdentity('invalid',{}),/GOOGLE_TOKEN_INVALID/);
 const part=Buffer.from(JSON.stringify({alg:'none',kid:'fake'})).toString('base64url');
 await assert.rejects(()=>googleIdentity(part+'.e30.fake',{}),/GOOGLE_TOKEN_INVALID/);
});

test('Google signed identity signs in once, rejects nonce replay and wrong audience',async()=>{
 const pair=await crypto.subtle.generateKey({name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},true,['sign','verify']);
 const jwk=await crypto.subtle.exportKey('jwk',pair.publicKey);jwk.kid='google-fixture';
 const original=globalThis.fetch;
 globalThis.fetch=async url=>{assert.equal(url,'https://www.googleapis.com/oauth2/v3/certs');return Response.json({keys:[jwk]})};
 try{
  const {call}=service({GOOGLE_WEB_CLIENT_ID:'fixture-client'});
  const nonce=(await call({action:'googleChallenge'})).nonce;
  const encode=v=>Buffer.from(JSON.stringify(v)).toString('base64url');
  const make=async(aud='fixture-client')=>{
   const now=Math.floor(Date.now()/1000);
   const data=encode({alg:'RS256',kid:jwk.kid})+'.'+encode({iss:'https://accounts.google.com',aud,sub:'google-subject',email:'g@example.test',email_verified:true,iat:now,exp:now+300,nonce,name:'Google Player'});
   const sig=await crypto.subtle.sign('RSASSA-PKCS1-v1_5',pair.privateKey,new TextEncoder().encode(data));
   return data+'.'+Buffer.from(sig).toString('base64url');
  };
  const token=await make();
  const result=await call({action:'googleSignIn',id_token:token});assert.equal(result.success,true);assert.ok(result.session_token);
  assert.equal((await call({action:'googleSignIn',id_token:token})).error,'GOOGLE_TOKEN_INVALID');
  assert.equal((await call({action:'googleSignIn',id_token:await make('wrong-client')})).error,'GOOGLE_TOKEN_INVALID');
 }finally{globalThis.fetch=original}
});

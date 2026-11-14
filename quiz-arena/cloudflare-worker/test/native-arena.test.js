import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {DatabaseSync} from 'node:sqlite';
import {webcrypto} from 'node:crypto';
import {nativeArena,nativeIdentity} from '../src/native-arena.js';
import {NativeAccounts} from '../src/accounts.js';
import worker from '../src/index.js';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
const TOKEN='a'.repeat(64);
const uid='native-test-user';
async function harness(){
 const sqlite=new DatabaseSync(':memory:');sqlite.exec(await readFile(new URL('../migrations/0001_arena.sql',import.meta.url),'utf8'));
 const db={prepare(sql){const s=sqlite.prepare(sql);let args=[];const stmt={bind(...values){args=values;return stmt},first:async()=>s.get(...args)||null,all:async()=>({results:s.all(...args)}),run:async()=>({meta:{changes:Number(s.run(...args).changes)}})};return stmt}};
 const values=new Map();const storage={get:async k=>structuredClone(values.get(k)),put:async(k,v)=>values.set(k,structuredClone(v)),delete:async k=>values.delete(k),list:async({prefix})=>new Map([...values].filter(([k])=>k.startsWith(prefix))),transaction:async fn=>fn(storage)};
 const service=new NativeAccounts({storage},{});
 const encoded=new TextEncoder().encode(TOKEN),digest=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',encoded)),n=>n.toString(16).padStart(2,'0')).join('');
 values.set('user:'+uid,{user_id:uid,name:'Player A',email:'a@example.test',status:'ACTIVE',created_at:new Date().toISOString()});
 values.set('session:'+digest,{user_id:uid,expires:Date.now()+3600000});
 const env={ARENA_DB:db,NATIVE_ACCOUNTS:{idFromName:()=>'',get:()=>({fetch:(url,p)=>service.fetch(new Request(url,p))})}};
 const identity={user_id:uid,name:'Player A',backend:'native'};
 const call=(action,p={},who=identity)=>nativeArena(env,{action,locale:'sq',...p},TOKEN,who);
 const state=id=>JSON.parse(sqlite.prepare('SELECT state_json FROM arena_sessions WHERE session_id = ?').get(id).state_json);
 return {sqlite,db,env,identity,call,state,values,service};
}
async function start(h){await h.call('arenaRegister',{name:'Player A'});return h.call('arenaStart',{level:1});}
const address=s=>({session_id:s.session_id,question_id:s.question.id});
async function solve(h,s,right=true){s=await h.call('arenaReady',address(s));const q=h.state(s.session_id).questions[s.index];return h.call('arenaAnswer',{...address(s),choice:right?q.correct:(q.correct+1)%q.choices.length,score:999999,coins:999999});}

test('D1 round owns answer key, starts sixty seconds after narration, survives restart and isolates players',async()=>{
 const h=await harness();let s=await start(h);assert.equal(s.total,10);assert.equal(s.question.choices.length,4);assert.equal(s.presentation_pending,true);assert.equal(s.remaining_ms,60000);assert.equal(s.question.correct,undefined);assert.equal(s.question.explanation,undefined);
 assert.equal((await h.call('arenaStart',{level:1})).session_id,s.session_id);
 await assert.rejects(()=>h.call('arenaAnswer',{...address(s),choice:0}),/PRESENTATION_PENDING/);
 await assert.rejects(()=>h.call('arenaResume',{session_id:s.session_id},{user_id:'other',backend:'native'}),/SESSION_NOT_FOUND/);
 s=await solve(h,s);assert.equal(s.score,110);assert.equal(s.feedback.right,true);assert.equal(s.feedback.source.length>0,true);
 const repeat=await h.call('arenaAnswer',{...address(s),choice:s.feedback.correct});assert.equal(repeat.score,110);
 const next=await h.call('arenaNext',address(s));assert.equal(next.index,1);assert.equal(next.presentation_pending,true);
 const replay=await h.call('arenaNext',address(s));assert.equal(replay.index,1);assert.equal(replay.question.id,next.question.id);
 const resumed=await h.call('arenaResume',{session_id:s.session_id});assert.equal(resumed.index,1);assert.equal(resumed.score,110);
 assert.equal((await h.call('arenaStart',{level:2})).session_id,s.session_id); // Active start resumes rather than replacing progress.
 h.sqlite.close();
});

test('D1 grades a whole round, advances progress once and awards existing twenty GAME coins once',async()=>{
 const h=await harness();let s=await start(h);
 while(!s.finished){s=await solve(h,s);s=await h.call('arenaNext',address(s));}
 assert.equal(s.passed,true);assert.equal(s.score,1150);assert.equal(s.level_award.coins,20);assert.equal(s.level_award.status,'GAME_ONLY');assert.equal(s.wallet.game_coins,20);assert.equal(s.wallet.pending_coins,0);
 const repeated=await h.call('arenaResume',{session_id:s.session_id});assert.equal(repeated.level_award.duplicate,true);assert.equal(repeated.wallet.game_coins,20);
 const boot=await h.call('arenaBootstrap');assert.equal(boot.progress.level,2);assert.deepEqual(boot.progress.completed,[1]);assert.equal(boot.progress.points,1150);
 const ranking=await h.call('arenaLeaderboard');assert.equal(ranking.ranking[0].points,1150);
 assert.equal(h.sqlite.prepare('SELECT COUNT(*) AS n FROM arena_wallet_audit').get().n,1);
 const forbidden=await h.service.fetch(new Request('https://accounts/api',{method:'POST',body:JSON.stringify({action:'awardArenaLevel',session_token:TOKEN,level:2})}));assert.equal(forbidden.status,403);
 await assert.rejects(()=>h.call('arenaStart',{level:3}),/LEVEL_LOCKED/);
 h.sqlite.close();
});

test('lifelines are question-bound, repeat-safe and never supply answer-key-weighted audience advice',async()=>{
 const h=await harness();let s=await start(h);s=await h.call('arenaReady',address(s));
 const fifty=await h.call('arenaLifeline',{...address(s),kind:'fifty'});const correct=h.state(s.session_id).questions[0].correct;
 assert.ok(!fifty.lifeline.hidden.includes(correct));assert.equal(fifty.lifeline.hidden.length,Math.max(0,s.question.choices.length-2));
 await assert.rejects(()=>h.call('arenaAnswer',{...address(s),choice:fifty.lifeline.hidden[0]}),/INVALID_CHOICE/);
 const friend=await h.call('arenaLifeline',{...address(s),kind:'friend'});assert.ok(friend.remaining_ms>140000);
 const repeat=await h.call('arenaLifeline',{...address(s),kind:'friend'});assert.ok(repeat.remaining_ms<=friend.remaining_ms);
 const audience=await h.call('arenaLifeline',{...address(s),kind:'audience'});assert.equal(audience.lifeline.simulated,true);assert.equal(audience.lifeline.votes.reduce((a,b)=>a+b,0),100);
 s=await solve(h,s);s=await h.call('arenaNext',address(s));s=await h.call('arenaReady',address(s));
 await assert.rejects(()=>h.call('arenaLifeline',{...address(s),kind:'friend'}),/LIFELINE_USED/);
 h.sqlite.close();
});

test('expired answers lose points; failed wallet infrastructure does not prevent a completed result',async()=>{
 const h=await harness();let s=await start(h);s=await h.call('arenaReady',address(s));let internal=h.state(s.session_id);internal.deadline=Date.now()-10;h.sqlite.prepare('UPDATE arena_sessions SET state_json = ? WHERE session_id = ?').run(JSON.stringify(internal),s.session_id);
 s=await solve(h,s);assert.equal(s.score,0);assert.equal(s.feedback.choice,-1);assert.equal(s.feedback.reason,'timeout');
 s=await h.call('arenaNext',address(s));
 h.env.NATIVE_ACCOUNTS={idFromName:()=>'',get:()=>({fetch:async()=>Response.json({success:false,error:'UNAVAILABLE'},{status:503})})};
 while(!s.finished){s=await solve(h,s);s=await h.call('arenaNext',address(s));}
 assert.equal(s.passed,true);assert.equal(s.level_award.status,'RETRY_PENDING');assert.equal(s.wallet_pending,true);
 assert.equal((await h.call('arenaBootstrap')).progress.level,2);
 h.sqlite.close();
});

test('current Script account authenticates and imports old progress before D1 play without changing account endpoint',async()=>{
 const h=await harness(),original=globalThis.fetch;
 h.env.LEGACY_ACCOUNT_URL='https://script.google.com/macros/s/accounts/exec';h.env.APPS_SCRIPT_URL='https://script.google.com/macros/s/arena/exec';
 globalThis.fetch=async(url,options)=>{
  const p=JSON.parse(options.body);
  if(p.action==='getWalletProfile')return Response.json({success:true,profile:{user_id:'legacy-user',username:'Ilaz'},balance:{pending_coins:500},reward_rules:{video_reward_coins:100}});
  if(p.action==='arenaBootstrap')return Response.json({success:true,progress:{level:4,points:4321,completed:[1,2,3]},resume:null});
  if(p.action==='getBalance')return Response.json({success:true,balance:{pending_coins:500}});
  throw new Error('Unexpected upstream action');
 };
 try{
  const identity=await nativeIdentity(h.env,'legacy-session-token-12345','test-ip');assert.equal(identity.backend,'legacy');
  let boot=await nativeArena(h.env,{action:'arenaBootstrap',locale:'sq'},'legacy-session-token-12345',identity);
  assert.equal(boot.progress.level,4);assert.equal(boot.progress.points,4321);assert.equal(boot.wallet.pending_coins,500);assert.equal(boot.legacy_progress_imported,true);
  await nativeArena(h.env,{action:'arenaRegister',name:'Ilaz'},'legacy-session-token-12345',identity);
  let s=await nativeArena(h.env,{action:'arenaStart',level:4},'legacy-session-token-12345',identity);assert.equal(s.engine,'D1');assert.equal(s.total,11);
  boot=await nativeArena(h.env,{action:'arenaBootstrap'},'legacy-session-token-12345',identity);assert.equal(boot.resume,s.session_id);
  const play=(action,p)=>nativeArena(h.env,{action,...p},'legacy-session-token-12345',identity);
  while(!s.finished){
   s=await play('arenaReady',address(s));
   s=await play('arenaAnswer',{...address(s),choice:h.state(s.session_id).questions[s.index].correct});
   s=await play('arenaNext',address(s));
  }
  assert.equal(s.level_award.status,'GAME_ONLY');
  assert.equal(s.arena_game_coins,20);
  assert.equal(s.wallet.pending_coins,500);
  const replay=await play('arenaResume',{session_id:s.session_id});
  assert.equal(replay.level_award.duplicate,true);
  assert.equal(replay.arena_game_coins,20);

 }finally{globalThis.fetch=original;h.sqlite.close();}
});

test('private R2 voice audio is reused across cold edge caches after authentication',async()=>{
 const original=globalThis.fetch;let paid=0;const objects=new Map();
 const env={APPS_SCRIPT_URL:'https://script.google.com/macros/s/test/exec',OPENAI_API_KEY:'fixture-not-a-key',ARENA_QUOTA:{idFromName:()=>'',get:()=>({fetch:async()=>new Response('{}')})},ARENA_MEDIA:{get:async key=>objects.has(key)?{body:objects.get(key).slice(0)}:null,put:async(key,bytes)=>objects.set(key,bytes.slice(0))}};
 globalThis.caches={default:{match:async()=>null,put:async()=>{}}};
 globalThis.fetch=async(url,options)=>{
  const p=JSON.parse(options.body);if(String(url).startsWith('https://script.google.com'))return Response.json(p.action==='arenaBootstrap'?{success:true,player_id:'player1'}:{success:true,index:0,level:1,question:{id:'q1',prompt:'Sa është dy plus dy?',choices:['4','5']}});
  paid++;return new Response(new Uint8Array(200),{headers:{'content-type':'audio/mpeg'}});
 };
 const req=()=>new Request('https://arena/api/ai/speech',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({session_token:'legacy-token-long-enough',session_id:'s1',question_id:'q1',locale:'sq'})});
 try{assert.equal((await worker.fetch(req(),env)).headers.get('x-arena-cache'),'miss');assert.equal((await worker.fetch(req(),env)).headers.get('x-arena-cache'),'r2');assert.equal(paid,1);assert.equal(objects.size,1);assert.ok([...objects.keys()][0].match(/^quiz-arena\/speech\/[a-f0-9]{64}\.mp3$/));}
 finally{globalThis.fetch=original;}
});

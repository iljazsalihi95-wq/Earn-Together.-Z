import { PRACTICE_BANK } from './practice-bank.js';

// This engine uses the same native Android contract as the existing Script bridge.
// D1 owns quiz answers/progress. NativeAccounts remains the sole wallet authority.
export class ArenaError extends Error {
 constructor(code,status=400){super(code);this.status=status;}
}
const fail=(code,status)=>{throw new ArenaError(code,status)};
const all=async stmt=>(await stmt.all()).results||[];
const safeName=value=>String(value||'').trim().slice(0,80);
const publicQuestion=q=>q?{id:q.id,prompt:q.prompt,choices:q.choices,category:q.category,source:q.source,recitation:'',difficulty:q.difficulty||1}:null;
export const ARENA_ACTIONS=new Set(['arenaBootstrap','arenaRegister','arenaStart','arenaResume','arenaReady','arenaAnswer','arenaNext','arenaLeaderboard','arenaLifeline']);
export function arenaConfig(level){return {level,name:`Niveli ${level}`,count:Math.min(15,10+Math.floor((level-1)/3)),points:100+level*10,bonus:50,seconds:60,pass:60,video:''};}
export async function nativeAccount(env,p,ip='arena-internal',internal=false){
 if(!env.NATIVE_ACCOUNTS)fail('NATIVE_ACCOUNTS_NOT_CONFIGURED',503);
 const id=env.NATIVE_ACCOUNTS.idFromName('we-earn-together-accounts-v1');
 const r=await env.NATIVE_ACCOUNTS.get(id).fetch('https://accounts/api',{
  method:'POST',headers:{'content-type':'application/json','x-native-client-ip':ip,...(internal?{'x-arena-internal':'1'}:{})},body:JSON.stringify(p)
 });
 const d=await r.json();if(!r.ok||!d.success)fail(d.error||'AUTH_REQUIRED',r.status||401);return d;
}
// Native session tokens are 32 random bytes encoded as hex. Legacy accounts
// retain their own independent token validator during the migration.
export function nativeToken(t){return /^[a-f0-9]{64}$/.test(t);}
export async function legacyAccount(env,p,timeoutMs=15000){
 if(!env.LEGACY_ACCOUNT_URL)fail('LEGACY_ACCOUNTS_NOT_CONFIGURED',503);
 const u=new URL(env.LEGACY_ACCOUNT_URL);if(u.protocol!=='https:'||u.hostname!=='script.google.com')fail('INVALID_UPSTREAM',503);
 const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),timeoutMs);
 try{
  const r=await fetch(u,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(p),redirect:'follow',signal:controller.signal});
  if(!r.ok)fail('ACCOUNT_UPSTREAM_ERROR',502);
  let d;try{d=await r.json();}catch{fail('ACCOUNT_INVALID_JSON',502);}
  if(!d?.success)fail(/^[A-Z_]+$/.test(d?.error)?d.error:'AUTH_REQUIRED',401);return d;
 }finally{clearTimeout(timeout);}
}
export async function nativeIdentity(env,t,ip){
 if(nativeToken(t)&&env.NATIVE_ACCOUNTS){
  try{return {...await nativeAccount(env,{action:'validateSession',session_token:t},ip,true),backend:'native'};}
  catch(e){if(e.status!==401)throw e;}
 }
 const d=await legacyAccount(env,{action:'getWalletProfile',session_token:t});
 if(!d.profile?.user_id)fail('AUTH_REQUIRED',401);
 return {user_id:String(d.profile.user_id),name:String(d.profile.username||'Lojtari'),backend:'legacy',legacy_profile:d};
}
const accountFor=(env,t,identity,p)=>identity.backend==='legacy'?legacyAccount(env,{...p,session_token:t}):nativeAccount(env,{...p,session_token:t},'arena:'+identity.user_id,true);

async function legacyArena(env,p,timeoutMs){
 const bridge={...env,LEGACY_ACCOUNT_URL:env.APPS_SCRIPT_URL};
 return legacyAccount(bridge,p,timeoutMs);
}
async function importLegacy(env,db,t,identity){
 if(identity.backend!=='legacy')return null;
 const old=await db.prepare('SELECT * FROM arena_legacy_progress WHERE player_id = ?').bind(identity.user_id).first();
 if(old)return old;
 try{
  const original=await legacyArena(env,{action:'arenaBootstrap',session_token:t,locale:'sq',protocol:2},4000);
  const pr=original.progress;
  if(!pr||!Array.isArray(pr.completed)||!Number.isSafeInteger(pr.points)||pr.points<0||!Number.isInteger(pr.level)||pr.level<1||pr.level>20)return null;
  const progress_json=JSON.stringify({level:pr.level,points:pr.points,completed:pr.completed.filter(n=>Number.isInteger(n)&&n>=1&&n<=20)}),resume_id=typeof original.resume==='string'?original.resume:null;
  await db.prepare('INSERT OR IGNORE INTO arena_legacy_progress (player_id, progress_json, resume_id, imported_at) VALUES (?, ?, ?, ?)').bind(identity.user_id,progress_json,resume_id,Date.now()).run();
  return {progress_json,resume_id};
 }catch{return null;}
}
async function progress(db,uid){
 const rows=await all(db.prepare('SELECT level, score FROM arena_progress WHERE player_id = ? ORDER BY level').bind(uid));
 const imported=await db.prepare('SELECT progress_json FROM arena_legacy_progress WHERE player_id = ?').bind(uid).first();
 const old=imported?JSON.parse(imported.progress_json):{level:1,points:0,completed:[]};
 const completed=[...new Set([...old.completed,...rows.map(r=>r.level)])].sort((a,b)=>a-b);
 return {level:Math.min(20,Math.max(old.level,1+Math.max(0,...completed))),points:old.points+rows.filter(r=>!old.completed.includes(r.level)).reduce((s,r)=>s+r.score,0),completed};
}
async function wallet(env,t,identity){
 // Legacy authentication already returns an owned, current wallet snapshot. Reuse
 // it rather than serially repeating another slow Apps Script read on every page.
 if(identity.backend==='legacy'&&identity.legacy_profile?.balance)return {wallet:identity.legacy_profile.balance,reward_rules:identity.legacy_profile.reward_rules||{},wallet_backend:'legacy'};
 try{
  const w=await accountFor(env,t,identity,{action:'getBalance'});
  return {wallet:w.balance||w,reward_rules:w.reward_rules||identity.legacy_profile?.reward_rules||{},wallet_backend:identity.backend};
 }catch{
  return {wallet:identity.legacy_profile?.balance||null,reward_rules:identity.legacy_profile?.reward_rules||{},wallet_backend:identity.backend,wallet_pending:true};
 }
}
async function owned(db,id,uid){
 if(typeof id!=='string'||!id||id.length>150)fail('SESSION_NOT_FOUND',404);
 const row=await db.prepare('SELECT * FROM arena_sessions WHERE session_id = ? AND player_id = ?').bind(id,uid).first();
 if(!row)fail('SESSION_NOT_FOUND',404);
 return {...row,state:JSON.parse(row.state_json)};
}
function publicState(row){
 const a=row.state,now=Date.now(),presentation=!!a.presentation&&!a.feedback&&!a.finished;
 return {success:true,session_id:row.session_id,locale:a.locale,level:a.level,score:a.score,correct:a.correct,index:a.index,total:a.questions.length,finished:a.finished||false,passed:a.passed||false,config:a.config,question:publicQuestion(a.questions[a.index]),feedback:a.feedback||null,lifelines:a.lifelines||{},presentation_pending:presentation,presentation_remaining_ms:presentation?Math.max(0,a.presentationUntil-now):0,remaining_ms:presentation?a.config.seconds*1000:Math.max(0,a.deadline-now),engine:'D1'};
}
function begin(a){a.presentation=true;a.presentationUntil=Date.now()+120000;a.deadline=0;}
function shuffled(items){const out=[...items];for(let i=out.length-1;i>0;i--){const j=crypto.getRandomValues(new Uint32Array(1))[0]%(i+1);[out[i],out[j]]=[out[j],out[i]];}return out;}
async function save(db,row){
 const a=row.state;
 const r=await db.prepare('UPDATE arena_sessions SET state_json = ?, finished = ?, revision = revision + 1, updated_at = ? WHERE session_id = ? AND player_id = ? AND revision = ?').bind(JSON.stringify(a),a.finished?1:0,Date.now(),row.session_id,row.player_id,row.revision).run();
 if(r.meta?.changes!==1)fail('SESSION_CHANGED',409);
 row.revision++;return row;
}
async function finishReward(db,env,t,row,identity){
 const a=row.state;if(!a.finished||!a.passed)return {};
 try{
 // Repeated requests repair an interrupted progress/wallet/audit write. Each
 // store has the same player+level idempotency key, so no double credit occurs.
 await db.prepare('INSERT OR IGNORE INTO arena_progress (player_id, level, score, correct, total, session_id, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?)').bind(row.player_id,a.level,a.score,a.correct,a.questions.length,row.session_id,Date.now()).run();
 let awarded;
 if(identity.backend==='legacy'){
   if(!a.reward_session_id){
   const reward=await accountFor(env,t,identity,{action:'startRewardSession',game_id:'html5_quiz_arena'});a.reward_session_id=reward.session_id;await save(db,row);
   return {level_award:{coins:0,status:'RETRY_PENDING'},...await wallet(env,t,identity)};
  }
  awarded=await accountFor(env,t,identity,{action:'recordRewardEvent',source:'LEVEL',game_id:'html5_quiz_arena',session_id:a.reward_session_id,event_id:'arena-level-'+a.level,level:a.level});
 }else awarded=await accountFor(env,t,identity,{action:'awardArenaLevel',level:a.level});
 await db.prepare('INSERT OR IGNORE INTO arena_wallet_audit (player_id, level, session_id, transaction_id, coins, status, awarded_at) VALUES (?, ?, ?, ?, ?, ?, ?)').bind(row.player_id,a.level,row.session_id,awarded.transaction_id,awarded.coins,awarded.status,Date.now()).run();
 return {level_award:{coins:awarded.coins,status:awarded.status,duplicate:!!awarded.duplicate},...await wallet(env,t,identity),...(awarded.balance?{wallet:awarded.balance}:{})};
 }catch{return {level_award:{coins:0,status:'RETRY_PENDING'},...await wallet(env,t,identity)};}
}
export async function nativeArena(env,p,t,identity){
 if(!env.ARENA_DB&&env.DB)env={...env,ARENA_DB:env.DB};
 if(!env.ARENA_DB)fail('ARENA_D1_NOT_CONFIGURED',503);
 const db=env.ARENA_DB,uid=identity.user_id,locale=p.locale||'sq';
 if(!ARENA_ACTIONS.has(p.action))fail('INVALID_ACTION');
 // This repository contains approved Albanian text only. Never claim a translated
 // bank is available until that language's reviewed source has been imported.
 if(locale!=='sq')fail('LANGUAGE_NOT_AVAILABLE',409);
 const bank=Object.values(PRACTICE_BANK);
 const imported=['arenaBootstrap','arenaStart'].includes(p.action)?await importLegacy(env,db,t,identity):await db.prepare('SELECT * FROM arena_legacy_progress WHERE player_id = ?').bind(uid).first();
 if(p.action==='arenaBootstrap'){
  const player=await db.prepare('SELECT name FROM arena_players WHERE player_id = ?').bind(uid).first();
  const active=await db.prepare('SELECT session_id FROM arena_sessions WHERE player_id = ? AND finished = 0 ORDER BY updated_at DESC LIMIT 1').bind(uid).first();
  return {success:true,engine:'D1',player_id:uid,player:{name:player?.name||identity.name},progress:await progress(db,uid),levels:Array.from({length:20},(_,i)=>arenaConfig(i+1)),categories:[...new Set(bank.map(q=>q.category))],available:bank.length,resume:active?.session_id||imported?.resume_id||null,legacy_progress_imported:identity.backend==='legacy'?!!imported:undefined,...await wallet(env,t,identity)};
 }
 if(p.action==='arenaRegister'){
  const name=safeName(p.name);if(name.length<2)fail('INVALID_NAME');
  await db.prepare('INSERT INTO arena_players (player_id, name, locale, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(player_id) DO UPDATE SET name = excluded.name, locale = excluded.locale, updated_at = excluded.updated_at').bind(uid,name,locale,Date.now()).run();
  return {success:true,engine:'D1',player_id:uid,progress:await progress(db,uid)};
 }
 if(p.action==='arenaLeaderboard'){
  const rows=await all(db.prepare(`SELECT p.name,
   COALESCE(json_extract(l.progress_json, '$.points'), 0) + COALESCE((SELECT SUM(g.score) FROM arena_progress g WHERE g.player_id = p.player_id AND NOT EXISTS (SELECT 1 FROM json_each(COALESCE(l.progress_json, '{"completed":[]}'), '$.completed') WHERE value = g.level)), 0) AS points,
   MIN(20, MAX(COALESCE(json_extract(l.progress_json, '$.level'), 1), COALESCE((SELECT MAX(g.level) + 1 FROM arena_progress g WHERE g.player_id = p.player_id), 1))) AS level
   FROM arena_players p LEFT JOIN arena_legacy_progress l ON l.player_id = p.player_id
   WHERE EXISTS (SELECT 1 FROM arena_progress g WHERE g.player_id = p.player_id) OR COALESCE(json_extract(l.progress_json, '$.points'), 0) > 0
   ORDER BY points DESC LIMIT 50`));
  return {success:true,engine:'D1',ranking:rows.map((r,i)=>({...r,rank:i+1}))};
 }
 let row;
 if(p.action==='arenaStart'){
  const player=await db.prepare('SELECT name FROM arena_players WHERE player_id = ?').bind(uid).first();if(!player)fail('REGISTER_FIRST');
  const old=await db.prepare('SELECT * FROM arena_sessions WHERE player_id = ? AND finished = 0 ORDER BY updated_at DESC LIMIT 1').bind(uid).first();
  if(old)return publicState({...old,state:JSON.parse(old.state_json)});
  if(imported?.resume_id)return {...await legacyArena(env,{...p,session_token:t,protocol:2}),engine:'SCRIPT_RESUME'};
  const level=p.level,pr=await progress(db,uid);if(!Number.isInteger(level)||level<1||level>20||level>pr.level)fail('LEVEL_LOCKED');
  const config=arenaConfig(level),filtered=bank.filter(q=>!p.category||q.category===p.category);
  if(filtered.length<2)fail('NOT_ENOUGH_APPROVED_QUESTIONS');
  const graded=filtered.filter(q=>(q.difficulty||1)<=Math.min(3,1+Math.floor((level-1)/4)));
  const pool=graded.length>=Math.min(config.count,filtered.length)?graded:filtered;
  config.count=Math.min(config.count,pool.length);
  let reward=null;if(identity.backend==='legacy')try{reward=await legacyAccount(env,{action:'startRewardSession',game_id:'html5_quiz_arena',session_token:t},4000);}catch{}
  const state={reward_session_id:reward?.session_id||'',locale,level,config,questions:shuffled(pool).slice(0,config.count),category:p.category||'',index:0,score:0,correct:0,feedback:null,lifelines:{},finished:false,passed:false};begin(state);
  row={session_id:crypto.randomUUID(),player_id:uid,state,revision:0};
  const now=Date.now();
  // A partial unique index prevents two concurrent starts from creating two rounds.
  await db.prepare('INSERT OR IGNORE INTO arena_sessions (session_id, player_id, state_json, revision, finished, created_at, updated_at) VALUES (?, ?, ?, 0, 0, ?, ?)').bind(row.session_id,uid,JSON.stringify(state),now,now).run();
  const created=await db.prepare('SELECT * FROM arena_sessions WHERE player_id = ? AND finished = 0 ORDER BY updated_at DESC LIMIT 1').bind(uid).first();
  return publicState({...created,state:JSON.parse(created.state_json)});
 }
 try{row=await owned(db,p.session_id,uid);}catch(e){
  if(e.message!=='SESSION_NOT_FOUND'||identity.backend!=='legacy')throw e;
  const legacy=await legacyArena(env,{...p,session_token:t,protocol:2});
  if(legacy.finished&&imported?.resume_id===p.session_id){
   let latest=legacy.progress;
   if(!latest)try{latest=(await legacyArena(env,{action:'arenaBootstrap',session_token:t,locale:'sq',protocol:2},4000)).progress;}catch{}
   if(latest&&Number.isSafeInteger(latest.points)&&Number.isInteger(latest.level)&&Array.isArray(latest.completed))await db.prepare('UPDATE arena_legacy_progress SET progress_json = ?, resume_id = NULL WHERE player_id = ?').bind(JSON.stringify(latest),uid).run();
   else await db.prepare('UPDATE arena_legacy_progress SET resume_id = NULL WHERE player_id = ?').bind(uid).run();
  }
  return {...legacy,engine:'SCRIPT_RESUME'};
 }
 const a=row.state,q=a.questions[a.index];
 if(p.action==='arenaResume')return {...publicState(row),...await finishReward(db,env,t,row,identity)};
 if(a.finished)return {...publicState(row),...await finishReward(db,env,t,row,identity)};
 if(p.question_id!==q.id){if(['arenaNext','arenaReady'].includes(p.action))return publicState(row);fail('QUESTION_CHANGED',409);}
 if(p.action==='arenaReady'){
  if(a.presentation&&!a.feedback){a.presentation=false;a.deadline=Date.now()+60000;await save(db,row);}
 }else if(p.action==='arenaAnswer'){
  if(!Number.isInteger(p.choice)||p.choice< -1||p.choice>=q.choices.length)fail('INVALID_CHOICE');
  if(!a.feedback){
   if(a.presentation)fail('PRESENTATION_PENDING',409);
   if(p.choice===-1&&Date.now()<a.deadline)fail('TIMEOUT_NOT_REACHED',409);
   const choice=Date.now()>=a.deadline?-1:p.choice,right=choice===q.correct;
   if(right){a.correct++;a.score+=a.config.points;}
   a.feedback={right,choice,submitted_choice:p.choice,reason:choice===-1?'timeout':right?'correct':'incorrect',correct:q.correct,explanation:q.explanation,source:q.source};await save(db,row);
  }
 }else if(p.action==='arenaNext'){
  if(!a.feedback)fail('ANSWER_FIRST',409);
  if(a.index<a.questions.length-1){a.index++;a.feedback=null;begin(a);await save(db,row);}
  else{a.finished=true;a.passed=a.correct/a.questions.length*100>=a.config.pass;if(a.correct===a.questions.length)a.score+=a.config.bonus;await save(db,row);}
 }else if(p.action==='arenaLifeline'){
  const kind=p.kind;if(!['fifty','friend','audience','hint','switch'].includes(kind))fail('INVALID_LIFELINE');
  if(a.lifelines[kind]){
   if(a.lifelines[kind].question_id!==q.id)fail('LIFELINE_USED',409);
  }else{
   if(a.feedback||a.presentation||Date.now()>=a.deadline)fail('LIFELINE_UNAVAILABLE',409);
   const r={kind,question_id:q.id};
   if(kind==='fifty')r.hidden=shuffled(q.choices.map((_,i)=>i).filter(i=>i!==q.correct)).slice(0,Math.max(0,q.choices.length-2));
   else if(kind==='audience'){const w=q.choices.map(()=>10+crypto.getRandomValues(new Uint32Array(1))[0]%61),sum=w.reduce((n,x)=>n+x,0);r.votes=w.map(x=>Math.floor(x*100/sum));r.votes[0]+=100-r.votes.reduce((n,x)=>n+x,0);r.simulated=true;}
   else if(kind==='switch'){
    const pool=bank.filter(x=>!a.questions.some(y=>y.id===x.id)&&(!a.category||x.category===a.category));if(!pool.length)fail('NO_REPLACEMENT');a.questions[a.index]=shuffled(pool)[0];begin(a);
   }else r.reserved=true;
   if(kind==='friend')a.deadline+=90000;
   a.lifelines[kind]=r;await save(db,row);
  }
  return {...publicState(row),lifeline:a.lifelines[kind]};
 }
 return {...publicState(row),...await finishReward(db,env,t,row,identity)};
}

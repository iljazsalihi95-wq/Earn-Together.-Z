const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const tables={
 'Arena - Lojtaret':[['id','name','date','locale','avatar','points','level','wins','games','last','status']],
 'Arena - Sesionet':[['id','user','start','end','category','level','count','correct','wrong','points','seconds','status','state']],
 'Arena - Progresi':[['user','level','points','right','wrong','date','session','passed','video','explained']],
 'Arena - Media':[['id','type','level','category','title','url','license','locale','status','notes']],
 'Arena - Nivelet':[['level','name','count','points','bonus','seconds','pass','video','status'],[1,'First',2,100,50,30,50,'','PLANIFIKUAR'],[2,'Second',2,110,55,29,60,'','PLANIFIKUAR']],
 'Arena - Islame':[['header'],['q1','PO','Kurani','single_select','Question one','A','B','C','D','B','Explanation one','112:1','sq','MIRATUAR'],['q2','PO','Kurani','single_select','Question two','A','B','C','D','C','Explanation two','Source','sq','MIRATUAR'],['unapproved','PO','Kurani','single_select','Review','A','B','C','D','A','Review','Source','sq','RISHIKIM']],
 'Arena - Interaktive':[['header']],'Arena - Njohuri V2':[['header']],'Pyetjet':[['title'],['note'],['header']]
};
const sheets=Object.fromEntries(Object.entries(tables).map(([name,rows])=>[name,{getDataRange:()=>({getValues:()=>rows.map(r=>r.slice()),getDisplayValues:()=>rows.map(r=>r.map(String))}),getRange:(row,col,n,w)=>({setValues:values=>values.forEach((r,i)=>{rows[row-1+i]=r.slice()})}),appendRow:r=>rows.push(r.slice())}]));
let user='user1',sequence=0,apiCalls=0,lockHeld=false,speechCalls=0,onSpeech=null;const files=new Map();const props=new Map(),cache=new Map();
const context={console,Date,Math,JSON,Set,Number,String,Error,SpreadsheetApp:{openById:()=>({getSheetByName:n=>sheets[n]})},LockService:{getScriptLock:()=>({waitLock(){assert.equal(lockHeld,false,'nested/gameplay lock held during voice');lockHeld=true},releaseLock(){lockHeld=false}})},CacheService:{getScriptCache:()=>({get:k=>cache.get(k),put:(k,v)=>cache.set(k,v)})},PropertiesService:{getScriptProperties:()=>({getProperty:k=>props.get(k),setProperty:(k,v)=>props.set(k,v),deleteProperty:k=>props.delete(k)})},Utilities:{getUuid:()=>`session-${++sequence}`,DigestAlgorithm:{SHA_256:'sha256'},computeDigest:(_,s)=>[...require('node:crypto').createHash('sha256').update(s).digest()],base64Encode:b=>Buffer.from(b).toString('base64')},DriveApp:{getFileById:id=>files.get(id),getFolderById:()=>({createFile:blob=>{const id='file-'+files.size;const file={getId:()=>id,getBlob:()=>blob};files.set(id,file);return file}}),createFolder:()=>({getId:()=> 'private-folder'})},UrlFetchApp:{fetch:(url,options)=>{if(url.includes('/audio/speech')){assert.equal(lockHeld,false,'OpenAI must run outside the gameplay lock');speechCalls++;const payload=JSON.parse(options.payload);assert.equal(payload.voice,'onyx');if(onSpeech){const hook=onSpeech;onSpeech=null;hook()}const bytes=Buffer.from('mp3-test-fixture');const blob={getBytes:()=>bytes,setName(){return this}};return {getResponseCode:()=>200,getBlob:()=>blob}}apiCalls++;return {getContentText:()=>JSON.stringify({success:true,profile:{user_id:user,username:'Player'}})}}}};
vm.createContext(context);vm.runInContext(fs.readFileSync('quiz-arena/google-apps-script/Code.gs','utf8'),context);
const call=(action,p={})=>JSON.parse(JSON.stringify(context.arenaRoute_({action,session_token:'valid-token-12345678901234567890',...p})));
assert.throws(()=>context.arenaRoute_({action:'arenaBootstrap'}),/AUTH_REQUIRED/);
assert.equal(call('arenaBootstrap').available,2);assert.equal(call('arenaBootstrap',{locale:'de'}).available,0);
call('arenaRegister',{name:'=formula'});assert.equal(tables['Arena - Lojtaret'][1][1],"'=formula");
assert.throws(()=>call('arenaStart',{level:2}),/LEVEL_LOCKED/);
let s=call('arenaStart',{level:1});assert.equal(s.question.correct,undefined);assert.equal(s.question.explanation,undefined);const id=s.session_id;assert.equal(call('arenaStart',{level:1}).session_id,id);
assert.throws(()=>call('arenaVoice',{session_id:id,phase:'explanation'}),/ANSWER_FIRST/);
assert.throws(()=>call('arenaAnswer',{session_id:id,question_id:s.question.id,choice:4}),/INVALID_CHOICE/);
const q=s.question.id,right=q==='q1'?1:2;
s=call('arenaAnswer',{session_id:id,question_id:q,choice:right,score:999999});assert.equal(s.score,100);assert.equal(s.feedback.right,true);assert.ok(s.feedback.explanation);assert.equal(call('arenaAnswer',{session_id:id,question_id:q,choice:0}).score,100);
s=call('arenaNext',{session_id:id});assert.equal(s.index,1);s=call('arenaAnswer',{session_id:id,question_id:s.question.id,choice:0});assert.equal(s.feedback.right,false);s=call('arenaNext',{session_id:id});assert.equal(s.finished,true);assert.equal(s.passed,true);assert.equal(s.score,100);assert.equal(s.progress.level,2);
assert.equal(call('arenaNext',{session_id:id}).score,100);assert.equal(tables['Arena - Progresi'].length,2);assert.equal(call('arenaLeaderboard').ranking[0].points,100);
// Another authenticated account cannot read or answer the session.
cache.clear();user='user2';assert.throws(()=>call('arenaResume',{session_id:id}),/SESSION_NOT_FOUND/);
// Existing player resume and durable server grading survive cache eviction.
cache.clear();user='user1';assert.equal(call('arenaResume',{session_id:id}).finished,true);
assert.throws(()=>call('arenaVoice',{session_id:id}),/NO_QUESTION/);
console.log('PASS premium arena: auth, approved bank, language, locked levels, hidden answers, server score, explanation, duplicates, ranking, ownership, durable resume');

// Reproduce Next while OpenAI synthesis is still in flight. No global lock may block gameplay.
cache.clear();user='voice-user';call('arenaRegister',{name:'Voice test'});
let v=call('arenaStart',{level:1}),oldQuestion=v.question.id;
v=call('arenaAnswer',{session_id:v.session_id,question_id:oldQuestion,choice:oldQuestion==='q1'?1:2});
props.set('OPENAI_API_KEY','unit-test-placeholder');
onSpeech=()=>{
 assert.throws(()=>call('arenaVoice',{session_id:v.session_id,phase:'explanation'}),/VOICE_PENDING/);
 const next=call('arenaNext',{session_id:v.session_id,question_id:oldQuestion});assert.equal(next.index,1);
 const replay=call('arenaNext',{session_id:v.session_id,question_id:oldQuestion});assert.equal(replay.index,1);assert.equal(replay.feedback,null);
};
const spoken=call('arenaVoice',{session_id:v.session_id,phase:'explanation'});assert.equal(spoken.voice,'onyx');assert.ok(spoken.audio);assert.equal(speechCalls,1);
assert.throws(()=>call('arenaVoice',{session_id:v.session_id,phase:'explanation'}),/ANSWER_FIRST/);
const questionAudio=call('arenaVoice',{session_id:v.session_id,phase:'question'});const calls=speechCalls;
assert.equal(call('arenaVoice',{session_id:v.session_id,phase:'question'}).cache_key,questionAudio.cache_key);assert.equal(speechCalls,calls);
assert.equal(lockHeld,false);assert.equal([...props.keys()].filter(k=>k.startsWith('voicelease:')).length,0);
console.log('PASS male voice, synthesis without gameplay lock, simultaneous Next, duplicate voice lease, idempotent Next replay and private audio reuse');

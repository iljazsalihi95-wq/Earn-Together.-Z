/** Quiz Arena Premium — install ONLY in the existing quiz Apps Script project.
 * OPENAI_API_KEY stays in Script Properties. No public arbitrary-text synthesis.
 * Server verifies existing WET account sessions; client scores are never accepted.
 */
const QUIZ_SHEET='1UZjTbjKVszSL6mfcQJwQKoIlyiqJ_liZxTQ18l7M_Xk';
const WET_AUTH='https://script.google.com/macros/s/AKfycbw4GAz9_Om3YVU1Eil30WgEHvFkvW3ygo4sGqXNG8QBOGitUXGOkXWbntpJO4JybXPr/exec';
const QUIZ_TABS=['Arena - Islame','Arena - Interaktive','Arena - Njohuri V2'];
const ARENA_PROGRESS_TABS=['Arena - Lojtaret','Arena - Sesionet','Arena - Nivelet','Arena - Progresi','Arena - Media'];
function getQuizSpreadsheet_(){return SpreadsheetApp.openById(QUIZ_SHEET);}
function arenaError_(code){throw new Error(code);}
function arenaJson_(v){return ContentService.createTextOutput(JSON.stringify(v)).setMimeType(ContentService.MimeType.JSON);}
function doGet(){return arenaJson_({success:true,service:'quiz-arena',version:5,authenticatedActions:true,voice:'onyx'});}
function doPost(e){try{const p=JSON.parse(e.postData.contents);return arenaJson_(arenaRoute_(p));}catch(e){const code=/^[A-Z_]+$/.test(e.message)?e.message:'SERVER_ERROR';return arenaJson_({success:false,error:code,message:code});}}
function arenaHash_(s){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,s).map(b=>('0'+((b+256)%256).toString(16)).slice(-2)).join('');}
function arenaIdentity_(p){
 if(typeof p.session_token!=='string'||p.session_token.length<20||p.session_token.length>4096)arenaError_('AUTH_REQUIRED');
 const cache=CacheService.getScriptCache(),key='identity:'+arenaHash_(p.session_token),old=cache.get(key);if(old)return JSON.parse(old);
 const r=UrlFetchApp.fetch(WET_AUTH,{method:'post',contentType:'application/json',payload:JSON.stringify({action:'getWalletProfile',session_token:p.session_token}),muteHttpExceptions:true});
 let data;try{data=JSON.parse(r.getContentText());}catch(_){arenaError_('AUTH_UNAVAILABLE');}
 if(!data.success||!data.profile||!data.profile.user_id)arenaError_('AUTH_REQUIRED');
 const identity={id:String(data.profile.user_id),name:String(data.profile.username||'')};cache.put(key,JSON.stringify(identity),60);return identity;
}
function arenaRows_(tab){const sh=getQuizSpreadsheet_().getSheetByName(tab);if(!sh)arenaError_('MISSING_TABLE');return sh.getDataRange().getValues();}
function arenaSafe_(s){s=String(s||'').slice(0,80);return /^[=+@-]/.test(s)?"'"+s:s;}
function arenaBank_(locale){
 if(!['sq','de','en','tr'].includes(locale))arenaError_('INVALID_LANGUAGE');const out=[];
 for(const tab of QUIZ_TABS){const sh=getQuizSpreadsheet_().getSheetByName(tab);if(!sh)continue;
  for(const r of sh.getDataRange().getDisplayValues().slice(1)){
   if(r[1]!=='PO'||r[13]!=='MIRATUAR'||r[12]!==locale)continue;
   const q={id:String(r[0]),category:r[2],type:r[3],question:r[4],choices:r.slice(5,9),correct:String(r[9]).charCodeAt(0)-65,explanation:r[10],source:r[11],recitation:r[15]||''};
   if(q.question&&q.choices.every(x=>x&&x.trim())&&q.correct>=0&&q.correct<4&&q.explanation)out.push(q);
  }
 }
 // Preserve the original approved bank; only four-option questions enter competitive rounds.
 if(locale==='sq')for(const r of arenaRows_('Pyetjet').slice(3)){
  if(r[1]!=='PO'||String(r[13]).includes('RISHIKIM')||r[8])continue;
  const q={id:'sheet-'+String(r[0]),category:String(r[2]),type:'single_select',question:String(r[3]||''),choices:r.slice(4,8).map(String),correct:String(r[9]).charCodeAt(0)-65,explanation:String(r[10]||''),source:String(r[11]||''),recitation:''};
  if(q.question&&q.choices.every(Boolean)&&q.correct>=0&&q.correct<4&&q.explanation)out.push(q);
 }
 return out.filter((q,i)=>out.findIndex(x=>x.id===q.id||x.question.trim().toLowerCase()===q.question.trim().toLowerCase())===i);
}
function arenaLevels_(){return arenaRows_('Arena - Nivelet').slice(1).filter(r=>Number(r[0])>=1&&Number(r[0])<=20&&r[8]!=='DEAKTIVIZUAR').map(r=>({level:Number(r[0]),name:String(r[1]),count:Number(r[2]),points:Number(r[3]),bonus:Number(r[4]),seconds:Number(r[5]),pass:Number(r[6]),video:arenaHttps_(r[7])}));}
function arenaHttps_(s){return /^https:\/\//.test(String(s))?String(s):'';}
function arenaPlayer_(id){return arenaRows_('Arena - Lojtaret').slice(1).find(r=>r[0]===id);}
function arenaState_(id){const done=arenaRows_('Arena - Progresi').slice(1).filter(r=>r[0]===id&&r[7]==='PO');return {level:Math.min(20,1+Math.max(0,...done.map(r=>Number(r[1])))),points:done.reduce((s,r)=>s+Number(r[2]),0),completed:done.map(r=>Number(r[1]))};}
function arenaSession_(id,user){const rows=arenaRows_('Arena - Sesionet'),i=rows.findIndex((r,j)=>j>0&&r[0]===id&&r[1]===user);if(i<1)arenaError_('SESSION_NOT_FOUND');let state;try{state=JSON.parse(rows[i][12]);}catch(_){arenaError_('SESSION_INVALID');}return {row:i+1,values:rows[i],state};}
function arenaSave_(s){s.values[12]=JSON.stringify(s.state);getQuizSpreadsheet_().getSheetByName('Arena - Sesionet').getRange(s.row,1,1,13).setValues([s.values]);}
function arenaBeginQuestion_(a,presentation){a.presentation=!!presentation;a.presentationUntil=presentation?Date.now()+90000:0;a.deadline=Date.now()+a.config.seconds*1000+(presentation?90000:0);}
function arenaPublic_(s){const a=s.state,q=a.questions[a.index];if(a.feedback&&typeof a.feedback.choice!=='number'){const last=(a.answers||[]).find(x=>x.id===q.id);if(last)a.feedback.choice=last.choice;}return {session_id:s.values[0],level:a.level,score:a.score,correct:a.correct,index:a.index,total:a.questions.length,finished:a.finished||false,passed:a.passed||false,config:a.config,question:q?{id:q.id,category:q.category,prompt:q.question,choices:q.choices,recitation:q.recitation}:null,feedback:a.feedback||null,presentation_pending:!!a.presentation&&!a.feedback&&Date.now()<a.presentationUntil,presentation_remaining_ms:Math.max(0,(a.presentationUntil||0)-Date.now()),remaining_ms:a.presentation&&Date.now()<a.presentationUntil?a.config.seconds*1000:Math.max(0,a.deadline-Date.now())};}
function arenaRoute_(p){
 const u=arenaIdentity_(p),lock=LockService.getScriptLock();
 // Copy the owned session under the short gameplay lock; synthesis runs after release.
 if(p.action==='arenaVoice'){let snapshot;lock.waitLock(5000);try{snapshot=arenaSession_(String(p.session_id||''),u.id);}finally{lock.releaseLock();}return arenaVoice_(u,snapshot,p);}
 lock.waitLock(15000);
 try{
 const locale=String(p.locale||'sq');
 if(p.action==='arenaBootstrap'){
  const bank=arenaBank_(locale),state=arenaState_(u.id),open=arenaRows_('Arena - Sesionet').slice(1).filter(r=>r[1]===u.id&&r[11]==='ACTIVE');
  return {success:true,player_id:u.id,progress:state,levels:arenaLevels_(),categories:[...new Set(bank.map(q=>q.category))],available:bank.length,resume:open.length?open[open.length-1][0]:null};
 }
 if(p.action==='arenaRegister'){
  const name=String(p.name||'').trim();if(name.length<2||name.length>80)arenaError_('INVALID_NAME');if(!['sq','de','en','tr'].includes(locale))arenaError_('INVALID_LANGUAGE');
  const sh=getQuizSpreadsheet_().getSheetByName('Arena - Lojtaret'),rows=sh.getDataRange().getValues(),i=rows.findIndex((r,j)=>j>0&&r[0]===u.id),state=arenaState_(u.id);
  const row=[u.id,arenaSafe_(name),i>0?rows[i][2]:new Date(),locale,'',state.points,state.level,state.completed.length,arenaRows_('Arena - Sesionet').slice(1).filter(r=>r[1]===u.id&&r[11]==='FINISHED').length,new Date(),'ACTIVE'];
  if(i>0)sh.getRange(i+1,1,1,11).setValues([row]);else sh.appendRow(row);return {success:true,player_id:u.id,progress:state};
 }
 if(p.action==='arenaLeaderboard'){const rows=arenaRows_('Arena - Lojtaret').slice(1).filter(r=>r[10]==='ACTIVE').sort((a,b)=>Number(b[5])-Number(a[5]));return {success:true,ranking:rows.slice(0,50).map((r,i)=>({rank:i+1,name:r[1],points:Number(r[5]),level:Number(r[6])}))};}
 if(p.action==='arenaStart'){
  if(!arenaPlayer_(u.id))arenaError_('REGISTER_FIRST');const progress=arenaState_(u.id),level=Number(p.level),config=arenaLevels_().find(x=>x.level===level);if(!config||level>progress.level)arenaError_('LEVEL_LOCKED');
  const sh=getQuizSpreadsheet_().getSheetByName('Arena - Sesionet');const active=sh.getDataRange().getValues().slice(1).find(r=>r[1]===u.id&&r[11]==='ACTIVE');if(active)return Object.assign({success:true},arenaPublic_(arenaSession_(active[0],u.id)));
  let bank=arenaBank_(locale).filter(q=>!p.category||q.category===p.category);if(bank.length<config.count)arenaError_('NOT_ENOUGH_APPROVED_QUESTIONS');
  for(let i=bank.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[bank[i],bank[j]]=[bank[j],bank[i]];}
  const state={questions:bank.slice(0,config.count),locale,level,config,index:0,score:0,correct:0,answers:[],deadline:Date.now()+config.seconds*1000,feedback:null,finished:false};
  state.presentationEnabled=p.protocol===2;arenaBeginQuestion_(state,state.presentationEnabled);
  const id=Utilities.getUuid(),row=[id,u.id,new Date(),' ',arenaSafe_(p.category||'ALL'),level,config.count,0,0,0,0,'ACTIVE',JSON.stringify(state)];sh.appendRow(row);return Object.assign({success:true},arenaPublic_({values:row,state}));
 }
 const s=arenaSession_(String(p.session_id||''),u.id),a=s.state;
 if(p.action==='arenaReady'){if(p.question_id!==a.questions[a.index].id)return Object.assign({success:true},arenaPublic_(s));if(a.presentation&&!a.feedback&&!a.finished){a.presentation=false;a.deadline=Date.now()+a.config.seconds*1000;arenaSave_(s);}return Object.assign({success:true},arenaPublic_(s));}
 if(p.action==='arenaResume')return Object.assign({success:true},arenaPublic_(s));
 if(p.action==='arenaAnswer'){
  if(a.finished)return Object.assign({success:true},arenaPublic_(s));const q=a.questions[a.index];
  if(p.question_id!==q.id)arenaError_('QUESTION_CHANGED');if(a.feedback)return Object.assign({success:true},arenaPublic_(s));
  if(!Number.isInteger(p.choice)||p.choice < -1||p.choice>3)arenaError_('INVALID_CHOICE');const choice=Date.now()>a.deadline?-1:p.choice,right=choice===q.correct;
  if(right){a.correct++;a.score+=a.config.points;}a.answers.push({id:q.id,choice,right});a.feedback={right,choice,reason:choice===-1?'timeout':right?'correct':'incorrect',correct:q.correct,explanation:q.explanation,source:q.source};arenaSave_(s);return Object.assign({success:true},arenaPublic_(s));
 }
 if(p.action==='arenaNext'){
  // Replaying a timed-out Next returns the current question, without skipping it.
  if(p.question_id&&p.question_id!==a.questions[a.index].id)return Object.assign({success:true},arenaPublic_(s));
  if(a.finished)return Object.assign({success:true},arenaPublic_(s));if(!a.feedback)arenaError_('ANSWER_FIRST');
  if(a.index<a.questions.length-1){a.index++;a.feedback=null;a.presentationEnabled=a.presentationEnabled||p.protocol===2;arenaBeginQuestion_(a,a.presentationEnabled);arenaSave_(s);return Object.assign({success:true},arenaPublic_(s));}
  a.finished=true;a.passed=a.correct/a.questions.length*100>=a.config.pass;if(a.correct===a.questions.length)a.score+=a.config.bonus;
  s.values[3]=new Date();s.values[7]=a.correct;s.values[8]=a.questions.length-a.correct;s.values[9]=a.score;s.values[10]=Math.round((Date.now()-new Date(s.values[2]).getTime())/1000);s.values[11]='FINISHED';
  // Progress row is the idempotency authority; replaying a level never adds points twice.
  const sh=getQuizSpreadsheet_().getSheetByName('Arena - Progresi'),rows=sh.getDataRange().getValues(),old=rows.findIndex((r,i)=>i>0&&r[0]===u.id&&Number(r[1])===a.level);
  if(a.passed&&(old<1||rows[old][7]!=='PO')){const row=[u.id,a.level,a.score,a.correct,a.questions.length-a.correct,new Date(),s.values[0],'PO','JO','PO'];if(old>0)sh.getRange(old+1,1,1,10).setValues([row]);else sh.appendRow(row);}
  arenaSave_(s);const players=getQuizSpreadsheet_().getSheetByName('Arena - Lojtaret'),pr=players.getDataRange().getValues(),pi=pr.findIndex(r=>r[0]===u.id),progress=arenaState_(u.id);if(pi>0){pr[pi][5]=progress.points;pr[pi][6]=progress.level;pr[pi][7]=progress.completed.length;pr[pi][8]=arenaRows_('Arena - Sesionet').slice(1).filter(r=>r[1]===u.id&&r[11]==='FINISHED').length;pr[pi][9]=new Date();players.getRange(pi+1,1,1,11).setValues([pr[pi]]);}
  return Object.assign({success:true,progress},arenaPublic_(s));
 }
 arenaError_('INVALID_ACTION');
 }finally{lock.releaseLock();}
}
function arenaVoice_(u,s,p){
 const a=s.state,q=a.questions[a.index];if(!q||a.finished)arenaError_('NO_QUESTION');const explanation=p.phase==='explanation';if(explanation&&!a.feedback)arenaError_('ANSWER_FIRST');
 // Narration only; authentic Quran recitation remains a separate recording.
 const reactions={sq:['Saktë.','Këtë herë jo.'],de:['Richtig.','Leider nicht.'],en:['Correct.','Not this time.'],tr:['Doğru.','Bu sefer olmadı.']};
 const text=explanation?(reactions[a.locale][a.feedback.right?0:1]+' '+q.choices[q.correct]+'. '+q.explanation):(q.question+'. '+q.choices.map((x,i)=>String.fromCharCode(65+i)+'. '+x).join('. '));
 if(text.length>3500)arenaError_('VOICE_TOO_LONG');const props=PropertiesService.getScriptProperties(),model=props.getProperty('ARENA_VOICE_MODEL')||'gpt-4o-mini-tts',voice='onyx';
 const instructions='Speak naturally in '+({sq:'Albanian',de:'German',en:'English',tr:'Turkish'}[a.locale])+', as a warm adult male television quiz presenter, with a clear resonant lower register. Pause between question and each option. Do not add facts or recite scripture.';
 const hash=arenaHash_('male-v2|'+a.locale+'|'+model+'|'+voice+'|'+instructions+'|'+text),media=getQuizSpreadsheet_().getSheetByName('Arena - Media');
 const lock=LockService.getScriptLock(),leaseKey='voicelease:'+hash,lease=Date.now()+':'+Utilities.getUuid();let key;
 lock.waitLock(5000);
 try{
  const cached=media.getDataRange().getValues().slice(1).find(r=>r[0]===hash&&r[8]==='PRIVATE_READY');
  if(cached){const bytes=DriveApp.getFileById(String(cached[5])).getBlob().getBytes();return {success:true,audio:Utilities.base64Encode(bytes),cache_key:hash,ai_generated:true,voice};}
  const pending=props.getProperty(leaseKey);if(pending&&Date.now()-Number(pending.split(':')[0])<90000)arenaError_('VOICE_PENDING');
  const day=new Date().toISOString().slice(0,10),userKey='voice:'+day+':'+arenaHash_(u.id),globalKey='voice:'+day+':global';
  const used=Number(props.getProperty(userKey)||0),all=Number(props.getProperty(globalKey)||0);if(used>=20||all>=100)arenaError_('VOICE_DAILY_LIMIT');key=props.getProperty('OPENAI_API_KEY');if(!key)arenaError_('VOICE_UNAVAILABLE');
  props.setProperty(userKey,String(used+1));props.setProperty(globalKey,String(all+1));props.setProperty(leaseKey,lease);
 }finally{lock.releaseLock();}
 try{
  // Slow OpenAI request MUST NOT hold the gameplay lock.
  const r=UrlFetchApp.fetch('https://api.openai.com/v1/audio/speech',{method:'post',contentType:'application/json',headers:{Authorization:'Bearer '+key},payload:JSON.stringify({model,voice,input:text,instructions,response_format:'mp3'}),muteHttpExceptions:true});
  if(r.getResponseCode()!==200)arenaError_('VOICE_UNAVAILABLE');const blob=r.getBlob(),bytes=blob.getBytes();if(bytes.length>1000000)arenaError_('VOICE_TOO_LARGE');
  lock.waitLock(5000);
  try{
   let folderId=props.getProperty('ARENA_PRIVATE_AUDIO_FOLDER');if(!folderId){folderId=DriveApp.createFolder('Quiz Arena Private Audio').getId();props.setProperty('ARENA_PRIVATE_AUDIO_FOLDER',folderId);}
   const file=DriveApp.getFolderById(folderId).createFile(blob.setName(hash+'.mp3'));
   media.appendRow([hash,'AI_NARRATOR',a.level,q.category,q.id,file.getId(),'OpenAI generated male narrator',a.locale,'PRIVATE_READY',explanation?'explanation':'question']);
  }finally{lock.releaseLock();}
  return {success:true,audio:Utilities.base64Encode(bytes),cache_key:hash,ai_generated:true,voice};
 }finally{
  lock.waitLock(5000);try{if(props.getProperty(leaseKey)===lease)props.deleteProperty(leaseKey);}finally{lock.releaseLock();}
 }
}
function testQuizSetup(){const report={tabs:QUIZ_TABS.concat(ARENA_PROGRESS_TABS).map(name=>({name,exists:!!getQuizSpreadsheet_().getSheetByName(name)})),questions:Object.fromEntries(['sq','de','en','tr'].map(locale=>[locale,arenaBank_(locale).length])),levels:arenaLevels_().length,voiceKeyConfigured:!!PropertiesService.getScriptProperties().getProperty('OPENAI_API_KEY')};console.log(JSON.stringify(report));return report;}
function testArenaProgressTabs(){return testQuizSetup().tabs;}

/** Owner/editor smoke test; never routed from doPost. Logs metadata, never audio/key. */
function testArenaNarration(){
 const q=arenaBank_('sq').find(q=>q.id.startsWith('WET-'))||arenaBank_('sq')[0];
 if(!q)arenaError_('NO_QUESTION');
 const s={state:{questions:[q],index:0,locale:'sq',level:1,finished:false,feedback:{right:true}}};
 const question=arenaVoice_({id:'owner-editor-smoke'},s,{phase:'question'});
 const explanation=arenaVoice_({id:'owner-editor-smoke'},s,{phase:'explanation'});
 const reused=arenaVoice_({id:'owner-editor-smoke'},s,{phase:'question'});
 const report={success:true,questionBytes:Utilities.base64Decode(question.audio).length,explanationBytes:Utilities.base64Decode(explanation.audio).length,cacheReused:question.cache_key===reused.cache_key,aiGenerated:question.ai_generated&&explanation.ai_generated,voice:question.voice};
 console.log(JSON.stringify(report));return report;
}

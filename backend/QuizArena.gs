/** Add this module to the EXISTING We Earn Together Apps Script project.
 * Uses the existing quiz spreadsheet. No new project or database is created.
 * route_ dispatches only quizArenaStart / quizArenaFinish here.
 */
const ARENA_SHEET_ID='1UZjTbjKVszSL6mfcQJwQKoIlyiqJ_liZxTQ18l7M_Xk';
function arenaQuestions_(){
 const db=SpreadsheetApp.openById(ARENA_SHEET_ID),extra=db.getSheetByName('Arena - Pyetjet e tua');
 const rows=db.getSheetByName('Pyetjet').getDataRange().getDisplayValues().slice(3).concat(extra?extra.getDataRange().getDisplayValues().slice(1):[]);
 return rows.filter(r=>r[1]==='PO'&&!String(r[13]).includes('RISHIKIM')).map(r=>{
  const choices=r.slice(4,9).map((v,i)=>({v,i})).filter(x=>x.v),correct=choices.findIndex(x=>x.i===String(r[9]).charCodeAt(0)-65);
  return {id:'sheet-'+r[0],question:r[3],category:r[2],choices:choices.map(x=>x.v),correct,explanation:r[10],source:r[11]};
 }).filter(q=>q.choices.length>=2&&q.choices.length<=5&&q.correct>=0);
}
function quizArenaRoute_(p){
 const u=auth_(p).u;
 if(p.action==='quizArenaStart'){
  const name=String(p.name||'').trim();if(name.length<2||name.length>80)fail_('INVALID_NAME','Enter a name of 2–80 characters');
  if(!Array.isArray(p.question_ids)||p.question_ids.length<1||p.question_ids.length>20||new Set(p.question_ids).size!==p.question_ids.length)fail_('INVALID_QUESTIONS','Choose 1–20 unique questions');
  const bank=arenaQuestions_(),questions=p.question_ids.map(id=>bank.find(q=>q.id===id));if(questions.some(q=>!q))fail_('QUIZ_CHANGED','Refresh the question bank');
  const round=Utilities.getUuid(),started=Date.now();CacheService.getScriptCache().put('arena_'+round,JSON.stringify({user:u.user_id,name,questions,started}),10800);
  return {success:true,round_id:round,started_at:started,questions:questions.map(q=>({id:q.id,question:q.question,category:q.category,choices:q.choices}))};
 }
 if(p.action!=='quizArenaFinish')fail_('INVALID_ACTION','Unsupported quiz action');
 if(typeof p.round_id!=='string'||!/^[a-f0-9-]{36}$/i.test(p.round_id))fail_('INVALID_ROUND','Invalid round');
 const visitor='wet:'+u.user_id+':'+p.round_id;
 const lock=LockService.getScriptLock();lock.waitLock(30000);
 try{
  const sh=SpreadsheetApp.openById(ARENA_SHEET_ID).getSheetByName('Rezultatet');
  const expected=['Timestamp','Emri','Pikët','Pyetje','Përqindja','Kaloi','Koha sek.','Gjuha','Visitor ID','Quiz Version'];
  if(JSON.stringify(sh.getRange(3,1,1,10).getDisplayValues()[0])!==JSON.stringify(expected))fail_('QUIZ_SCHEMA','Result columns changed');
  const values=sh.getDataRange().getValues(),old=values.slice(3).find(r=>r[8]===visitor);
  if(old)return {success:true,saved:true,score:Number(old[2]),total:Number(old[3]),percent:Number(old[4]),seconds:Number(old[6]),duplicate:true};
  const raw=CacheService.getScriptCache().get('arena_'+p.round_id);if(!raw)fail_('ROUND_EXPIRED','The round expired; local result remains saved');
  const session=JSON.parse(raw);if(session.user!==u.user_id)fail_('FORBIDDEN','This round belongs to another player');
  if(!Array.isArray(p.answers)||p.answers.length!==session.questions.length)fail_('INVALID_ANSWERS','Answer every question');
  const ids=new Set();let score=0;
  for(const a of p.answers){if(!a||ids.has(a.id)||!Number.isInteger(a.choice))fail_('INVALID_ANSWERS','Invalid answer');ids.add(a.id);const q=session.questions.find(q=>q.id===a.id);if(!q||a.choice < -1||a.choice>=q.choices.length)fail_('INVALID_ANSWERS','Invalid choice');if(a.choice===q.correct)score++;}
  const total=session.questions.length,percent=Math.round(score/total*100),seconds=Math.max(0,Math.round((Date.now()-session.started)/1000));
  // Prevent sheet formulas through the display name. No photos or authentication tokens are written.
  const name=/^[=+@-]/.test(session.name)?"'"+session.name:session.name;
  sh.appendRow([new Date(),name,score,total,percent,percent>=50?'PO':'JO',seconds,'Shqip',visitor,'WET Arena 2']);
  CacheService.getScriptCache().remove('arena_'+p.round_id);
  return {success:true,saved:true,score,total,percent,seconds};
 }finally{lock.releaseLock();}
}

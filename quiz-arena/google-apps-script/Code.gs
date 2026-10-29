/** Quiz Arena Premium — Google Apps Script source.
 * Deploy in a NEW Apps Script project bound to the Quiz Arena Sheet.
 * Script Properties: OPENAI_API_KEY (secret, never exposed), QUIZ_SHEET_ID (optional).
 * This script intentionally does not publish an unauthenticated voice proxy.
 */
const QUIZ_TABS = ['Arena - Islame','Arena - Interaktive','Arena - Njohuri V2'];
const COL = {id:0,active:1,category:2,type:3,question:4,correct:9,locale:12,status:13};
function getQuizSpreadsheet_() {
  const id = PropertiesService.getScriptProperties().getProperty('QUIZ_SHEET_ID');
  return id ? SpreadsheetApp.openById(id) : SpreadsheetApp.getActiveSpreadsheet();
}
function getQuizQuestions(locale, category, limit) {
  locale = String(locale || 'sq').toLowerCase();
  const max = Math.min(Math.max(Number(limit)||20,1),100);
  const out = [];
  const ss = getQuizSpreadsheet_();
  for (const tab of QUIZ_TABS) {
    const sheet = ss.getSheetByName(tab);
    if (!sheet || sheet.getLastRow()<2) continue;
    const rows = sheet.getRange(2,1,sheet.getLastRow()-1,16).getDisplayValues();
    for (const r of rows) {
      if (r[COL.active] !== 'PO' || r[COL.status] !== 'MIRATUAR') continue;
      if (r[COL.locale] !== locale || (category && r[COL.category]!==category)) continue;
      out.push({id:r[0],category:r[2],type:r[3],question:r[4],options:r.slice(5,9)});
      if (out.length>=max) return out;
    }
  }
  return out;
}
/** Admin-only smoke test: no API key or secrets printed. */
function testQuizSetup() {
  const ss=getQuizSpreadsheet_();
  const result={spreadsheet:ss.getName(),tabs:QUIZ_TABS.map(t=>({name:t,exists:!!ss.getSheetByName(t)})),voiceKeyConfigured:!!PropertiesService.getScriptProperties().getProperty('OPENAI_API_KEY')};
  Logger.log(JSON.stringify(result));return result;
}
/** Optional private/admin voice synthesis; never deploy this as an open web endpoint.
 * Audio is returned to the calling server-side script as base64, not exposed as a public URL.
 * Do NOT use this for Quran recitation: use verified human reciters with licensed audio.
 */
function synthesizePrivateVoice_(text, voice) {
  const key=PropertiesService.getScriptProperties().getProperty('OPENAI_API_KEY');
  if(!key) throw new Error('OPENAI_API_KEY is missing from Script Properties');
  if(!text || String(text).length>1500) throw new Error('Invalid text length');
  const response=UrlFetchApp.fetch('https://api.openai.com/v1/audio/speech',{
    method:'post',contentType:'application/json',
    headers:{Authorization:'Bearer '+key},
    payload:JSON.stringify({model:'tts-1',voice:voice||'alloy',input:String(text),response_format:'mp3'}),
    muteHttpExceptions:true
  });
  if(response.getResponseCode()!==200) throw new Error('Voice request failed: HTTP '+response.getResponseCode());
  return Utilities.base64Encode(response.getBlob().getBytes());
}

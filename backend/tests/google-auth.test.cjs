const {readFileSync}=require('node:fs');
const vm=require('node:vm'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {privateKey,publicKey}=crypto.generateKeyPairSync('rsa',{modulusLength:2048});
const jwk={...publicKey.export({format:'jwk'}),kid:'test-key',alg:'RS256',use:'sig'};
const cache=new Map(),users=[];let sessions=0;
const context={console,BigInt,Date,JSON,Number,String,Array,Math,Set,
 Utilities:{base64DecodeWebSafe:x=>[...Buffer.from(x,'base64url')],newBlob:x=>({getDataAsString:()=>Buffer.from(x).toString('utf8')})},
 uid_:()=>crypto.randomUUID(),digest_:x=>crypto.createHash('sha256').update(x).digest('hex'),hex_:x=>Buffer.from(x).toString('hex'),safeEqual_:(a,b)=>a===b,
 fail_:(c,m)=>{throw Object.assign(Error(m),{apiCode:c})},email_:x=>x.trim().toLowerCase(),
 CacheService:{getScriptCache:()=>({put:(k,v)=>cache.set(k,v),get:k=>cache.get(k),remove:k=>cache.delete(k)})},
 LockService:{getScriptLock:()=>({waitLock(){},releaseLock(){}})},
 UrlFetchApp:{fetch:()=>({getResponseCode:()=>200,getContentText:()=>JSON.stringify({keys:[jwk]})})},
 rows_:()=>users.map((u,i)=>({...u,_row:i+2})),write_:(table,u,row)=>{if(row)users[row-2]={...u};else users.push({...u})},
 iso_:()=>new Date().toISOString(),auth_:p=>{if(p.session_token!=='valid-session')context.fail_('SESSION_EXPIRED','expired');return {u:{...users[0],_row:2}}},
 newSession_:u=>({success:true,user_id:u.user_id,session_token:'test-session-'+(++sessions)}),emailRoute_:p=>({legacy:p.action})};
vm.createContext(context);vm.runInContext(readFileSync(__dirname+'/../GoogleAuth.gs','utf8'),context);
const web='726413248892-hvdca11ahhbb1lm6in5sdviglep69494.apps.googleusercontent.com';
function token(changes={},headerChanges={}){const now=Math.floor(Date.now()/1000);const h=Buffer.from(JSON.stringify({alg:'RS256',kid:'test-key',...headerChanges})).toString('base64url');const p=Buffer.from(JSON.stringify({iss:'https://accounts.google.com',aud:web,sub:'123456789',email:'player@gmail.com',email_verified:true,iat:now,exp:now+3600,nonce:context.route_({action:'googleChallenge'}).nonce,...changes})).toString('base64url');const data=h+'.'+p;return data+'.'+crypto.sign('RSA-SHA256',Buffer.from(data),privateKey).toString('base64url');}
function rejects(t,code='GOOGLE_TOKEN_INVALID',extra={}){assert.throws(()=>context.route_({action:'googleSignIn',id_token:t,...extra}),e=>e.apiCode===code);}
for(const changes of [{exp:1},{iat:9999999999},{aud:'attacker'},{iss:'attacker'},{azp:'attacker'},{email_verified:false},{sub:''},{nonce:'wrong'},{nbf:9999999999}])rejects(token(changes));
rejects(token({}, {alg:'none'}));rejects(token({}, {kid:'unknown'}));rejects('broken');
const valid=token();rejects(valid.slice(0,-12)+'AAAAAAAAAAAA');
const first=context.route_({action:'googleSignIn',id_token:valid});assert.equal(users.length,1);assert.equal(users[0].provider,'GOOGLE');assert.equal(users[0].password_hash,'');rejects(valid);
const again=context.route_({action:'googleSignIn',id_token:token()});assert.equal(again.user_id,first.user_id);assert.equal(users.length,1);
users[0].status='DISABLED';rejects(token(),'GOOGLE_LOGIN_FAILED');users[0].status='ACTIVE';
users[0].google_id='';users[0].provider='EMAIL';users[0].password_hash='preserved-hash';
rejects(token(),'ACCOUNT_LINK_REQUIRED');assert.equal(users.length,1);
assert.throws(()=>context.route_({action:'linkGoogle',id_token:token()}),e=>e.apiCode==='SESSION_EXPIRED');
assert.throws(()=>context.route_({action:'linkGoogle',session_token:'valid-session',id_token:token({email:'other@gmail.com'})}),e=>e.apiCode==='GOOGLE_LINK_FAILED');
const linked=context.route_({action:'linkGoogle',session_token:'valid-session',id_token:token()});assert.equal(linked.user_id,first.user_id);assert.equal(users[0].password_hash,'preserved-hash');assert.equal(users.length,1);
assert.equal(context.route_({action:'getProgress'}).legacy,'getProgress');
console.log('PASS: RSA signatures, tampering, claims, expiry, nonce replay, duplicate email, session linking, status, stable user ID, legacy routes');

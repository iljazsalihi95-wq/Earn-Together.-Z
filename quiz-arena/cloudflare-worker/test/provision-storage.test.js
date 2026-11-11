import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp,writeFile,readFile,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {provision} from '../scripts/provision-storage.mjs';
const env={CLOUDFLARE_API_TOKEN:'private-token-never-log',CLOUDFLARE_ACCOUNT_ID:'private-account-never-log'};
const uuid='12345678-1234-1234-1234-123456789abc';
const ok=result=>Response.json({success:true,result});
test('provision reuses exact named D1 and private R2 resources, applies repeatable schema and generates real bindings',async()=>{
 const root=await mkdtemp(join(tmpdir(),'arena-storage-')),sent=[],logs=[];
 await writeFile(join(root,'wrangler.toml'),'name = "quiz-arena-backend"\nmain = "quiz-arena/cloudflare-worker/src/index.js"\n');
 try{
  const request=async(url,p)=>{
   sent.push({url,p});assert.equal(p.headers.authorization,'Bearer '+env.CLOUDFLARE_API_TOKEN);assert.ok(p.signal instanceof AbortSignal);
   if(url.includes('/d1/database?'))return ok([{name:'unrelated',uuid:'bad'},{name:'lazy-ai-studio',uuid}]);
   if(url.endsWith('/r2/buckets'))return ok({buckets:[{name:'site-creator-r2',jurisdiction:'eu'}]});
   if(url.endsWith('/query')){assert.ok(JSON.parse(p.body).sql.includes('CREATE TABLE IF NOT EXISTS arena_sessions'));return ok([{success:true}]);}
   throw new Error('Unexpected operation');
  };
  await provision({root,env,request,log:s=>logs.push(s)});
  const output=await readFile(join(root,'.arena-wrangler.toml'),'utf8');assert.ok(output.includes(`database_id = "${uuid}"`));assert.ok(output.includes('binding = "BUCKET"'));assert.ok(output.includes('jurisdiction = "eu"'));assert.equal(sent.length,3);assert.ok(!logs.join('').includes(env.CLOUDFLARE_API_TOKEN));assert.ok(!logs.join('').includes(env.CLOUDFLARE_ACCOUNT_ID));
 }finally{await rm(root,{recursive:true,force:true});}
});
test('provision failures redact upstream payloads and credentials; missing credentials perform no request',async()=>{
 let requests=0;await assert.rejects(()=>provision({env:{},request:async()=>{requests++;}}),/credentials are missing/);assert.equal(requests,0);
 const request=async()=>Response.json({success:false,errors:[{message:'private-token-never-log'}]},{status:403});
 await assert.rejects(()=>provision({env,request}),e=>e.message.includes('HTTP 403')&&!e.message.includes(env.CLOUDFLARE_API_TOKEN));
 await assert.rejects(()=>provision({env,request:async()=>{throw new Error('private-token-never-log');}}),e=>e.message.includes('could not connect')&&!e.message.includes(env.CLOUDFLARE_API_TOKEN));
});

test('missing existing resources stop resolution without creating replacements',async()=>{
 let requests=[];
 await assert.rejects(()=>provision({env,request:async(url,p)=>{requests.push(p.method);return ok([]);}}),/lazy-ai-studio was not found/);
 assert.deepEqual(requests,['GET']);
 requests=[];
 await assert.rejects(()=>provision({env,request:async(url,p)=>{requests.push(p.method);return url.includes('/d1/database?')?ok([{name:'lazy-ai-studio',uuid}]):ok({buckets:[]});}}),/site-creator-r2 was not found/);
 assert.deepEqual(requests,['GET','GET']);
});

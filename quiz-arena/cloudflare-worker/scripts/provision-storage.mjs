import {readFile,writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

// Run only in the existing repository deployment. Prints resource names, never
// credentials, account identifiers, token fragments or upstream error payloads.
export async function provision({env=process.env,root=process.cwd(),request=fetch,log=console.log}={}){
 const token=env.CLOUDFLARE_API_TOKEN,account=env.CLOUDFLARE_ACCOUNT_ID;
 if(!token||!account)throw new Error('Cloudflare deployment credentials are missing');
 const base=`https://api.cloudflare.com/client/v4/accounts/${encodeURIComponent(account)}`;
 async function api(path,method='GET',payload){
  const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),20000);
  try{
   let r;try{r=await request(base+path,{method,headers:{authorization:`Bearer ${token}`,'content-type':'application/json'},...(payload?{body:JSON.stringify(payload)}:{}),signal:controller.signal});}catch{throw new Error('Cloudflare storage request timed out or could not connect');}
   let d;try{d=await r.json();}catch{throw new Error(`Cloudflare storage operation returned HTTP ${r.status}`);}
   if(!r.ok||d.success===false)throw new Error(`Cloudflare storage operation failed (HTTP ${r.status}); verify D1 and R2 permissions`);
   return d.result;
  }finally{clearTimeout(timeout);}
 }
 const dbName='lazy-ai-studio',bucketName='site-creator-r2';
 const databases=await api('/d1/database?name='+encodeURIComponent(dbName));
 let database=databases.find(d=>d.name===dbName);
 if(!database)throw new Error('Existing D1 lazy-ai-studio was not found; no database was created');
 if(!/^[a-f0-9-]{36}$/i.test(database.uuid||''))throw new Error('D1 returned an invalid database identifier');
 const buckets=await api('/r2/buckets');
 const bucket=(buckets.buckets||[]).find(b=>b.name===bucketName);
 if(!bucket)throw new Error('Existing R2 site-creator-r2 was not found; no bucket was created');
 const jurisdiction=bucket.jurisdiction;if(jurisdiction!==undefined&&jurisdiction!==''&&!/^[a-z0-9-]{1,30}$/.test(jurisdiction))throw new Error('Existing R2 bucket returned an invalid jurisdiction');
 const migration=await readFile(new URL('../migrations/0001_arena.sql',import.meta.url),'utf8');
 const statements=await api('/d1/database/'+database.uuid+'/query','POST',{sql:migration});
 if(!Array.isArray(statements)||statements.some(r=>r.success===false))throw new Error('D1 schema migration failed');
 const config=await readFile(resolve(root,'wrangler.toml'),'utf8');
 if(/\[\[(?:d1_databases|r2_buckets)\]\]/.test(config))throw new Error('Review existing storage bindings before provisioning');
 const generated=config+`\n[[d1_databases]]\nbinding = "DB"\ndatabase_name = "${dbName}"\ndatabase_id = "${database.uuid}"\nmigrations_dir = "quiz-arena/cloudflare-worker/migrations"\n\n[[r2_buckets]]\nbinding = "BUCKET"\nbucket_name = "${bucketName}"\n${jurisdiction?`jurisdiction = "${jurisdiction}"\n`:''}`;
 await writeFile(resolve(root,'.arena-wrangler.toml'),generated,{mode:0o600});
 log('Existing D1 lazy-ai-studio and R2 site-creator-r2 resolved; arena schema and deployment config are ready');
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 provision().catch(e=>{console.error(e.message);process.exitCode=1;});
}

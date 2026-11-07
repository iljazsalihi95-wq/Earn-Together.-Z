// Same Android contract as the owner-provided Codice.js, with monotonic versions.
export const GAMES=['LazyGarden','Block Puzzle','Bubble Shooter'];
export function validateProgress(p){
 const invalid=()=>{throw new Error('INVALID_PROGRESS')};
 if(!p||!GAMES.includes(p.game))throw new Error('INVALID_GAME');
 const n=k=>{const v=Number(p[k]||0);if(!Number.isSafeInteger(v)||v<0||v>1000000000)invalid();return v};
 const state=typeof p.progress_json==='string'?p.progress_json:JSON.stringify(p.progress_json||{});
 if(state.length>30000)invalid();
 try{const q=JSON.parse(state);if(!q||typeof q!=='object'||Array.isArray(q))invalid()}catch{invalid()}
 return {game:p.game,level:n('level'),score:n('score'),best_score:n('best_score'),progress_json:state,base_updated_at:String(p.base_updated_at||'')};
}
export async function progressAction(tx,user,p){
 const prefix='progress:'+user+':';
 if(p.action==='getProgress'){
  if(p.game&&!GAMES.includes(p.game))throw new Error('INVALID_GAME');
  const rows=p.game?[await tx.get(prefix+p.game)]:await Promise.all(GAMES.map(g=>tx.get(prefix+g)));
  return {success:true,progress:rows.filter(Boolean)};
 }
 const items=p.action==='syncProgress'?p.progress:[p];
 if(!Array.isArray(items)||items.length>3)throw new Error('INVALID_PROGRESS');
 // Validate the entire batch before changing any record.
 const prepared=items.map(validateProgress);
 if(new Set(prepared.map(x=>x.game)).size!==prepared.length)throw new Error('INVALID_PROGRESS');
 const saved=[];
 for(const item of prepared){
  const old=await tx.get(prefix+item.game);
  if(old&&old.updated_at!==item.base_updated_at){saved.push({...old,conflict:true});continue}
  const updated_at=new Date(Math.max(Date.now(),old?Date.parse(old.updated_at)+1:0)).toISOString();
  const row={game:item.game,level:item.level,score:item.score,best_score:Math.max(item.best_score,old?.best_score||0),progress_json:item.progress_json,updated_at};
  await tx.put(prefix+item.game,row);saved.push({...row,conflict:false});
 }
 return {success:true,progress:saved};
}

import test from 'node:test';
import assert from 'node:assert/strict';
import {progressAction} from '../src/progress.js';
function store(){const data=new Map();return {data,get:async k=>structuredClone(data.get(k)),put:async(k,v)=>data.set(k,structuredClone(v))}}
const item={game:'Block Puzzle',level:2,score:120,best_score:200,progress_json:'{"board":[1,2,3]}'};
test('original Android progress contract survives reopening and isolates accounts',async()=>{
 const tx=store();const first=(await progressAction(tx,'A',{action:'saveProgress',...item})).progress[0];
 assert.equal(first.conflict,false);assert.equal(first.best_score,200);
 const read=await progressAction(tx,'A',{action:'getProgress'});assert.equal(read.progress[0].progress_json,item.progress_json);
 assert.equal((await progressAction(tx,'B',{action:'getProgress'})).progress.length,0);
 const stale=(await progressAction(tx,'A',{action:'saveProgress',...item,score:999})).progress[0];assert.equal(stale.conflict,true);assert.equal(stale.score,120);
 const second=(await progressAction(tx,'A',{action:'saveProgress',...item,score:150,best_score:10,base_updated_at:first.updated_at})).progress[0];
 assert.equal(second.conflict,false);assert.equal(second.best_score,200);assert.ok(second.updated_at>first.updated_at);
});
test('three-game batch validates before write and rejects malformed board or duplicate games',async()=>{
 const tx=store();
 await assert.rejects(()=>progressAction(tx,'A',{action:'syncProgress',progress:[item,{...item,game:'Bubble Shooter',score:-1}]}),/INVALID_PROGRESS/);assert.equal(tx.data.size,0);
 for(const value of ['[]','null','broken','x'.repeat(30001)])await assert.rejects(()=>progressAction(tx,'A',{action:'saveProgress',...item,progress_json:value}),/INVALID_PROGRESS/);
 await assert.rejects(()=>progressAction(tx,'A',{action:'syncProgress',progress:[item,item]}),/INVALID_PROGRESS/);
 await assert.rejects(()=>progressAction(tx,'A',{action:'getProgress',game:'unknown'}),/INVALID_GAME/);
 const result=await progressAction(tx,'A',{action:'syncProgress',progress:[item,{...item,game:'Bubble Shooter'},{...item,game:'LazyGarden'}]});assert.equal(result.progress.length,3);
});

const fs=require('node:fs'),assert=require('node:assert/strict');
const snapshot=JSON.parse(fs.readFileSync('app/src/main/assets/quiz-arena/sheet-bank-sq.json','utf8'));
assert.equal(snapshot.sheetId,'1UZjTbjKVszSL6mfcQJwQKoIlyiqJ_liZxTQ18l7M_Xk');
assert.ok(snapshot.questions.length>=200);const ids=new Set(),prompts=new Set();
for(const q of snapshot.questions){assert.ok(q.id&&!ids.has(q.id));ids.add(q.id);const p=q.question.trim().toLocaleLowerCase('sq');assert.ok(!prompts.has(p));prompts.add(p);assert.ok(q.options.length>=2&&q.options.length<=5);assert.equal(q.options.length,new Set(q.options).size);assert.ok(q.correctIndex>=0&&q.correctIndex<q.options.length);assert.ok(q.explanation&&q.source);assert.ok(q.difficulty>=1&&q.difficulty<=3);}
const added=JSON.parse(fs.readFileSync('quiz-arena/content/premium-sq-verified-2026-11-05.json','utf8')).questions;
assert.equal(added.length,75);for(const q of added){const bundled=snapshot.questions.find(x=>x.id===q.id);assert.deepEqual(bundled.options,q.options);assert.equal(bundled.correctIndex,q.correctIndex);assert.equal(bundled.difficulty,q.difficulty);assert.ok(q.sources[0].url||q.calculation);}
assert.ok(!ids.has('general-sq-025'));assert.ok(!ids.has('logic-v3-percent-sq'));assert.ok(!ids.has('sheet-40'));
console.log(`PASS ${snapshot.questions.length} unique approved Sheet questions; all 75 new questions connected to offline runtime with source/difficulty/answer validation`);

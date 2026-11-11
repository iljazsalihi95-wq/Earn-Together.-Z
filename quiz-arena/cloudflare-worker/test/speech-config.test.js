import test from 'node:test';
import assert from 'node:assert/strict';
import { speechConfiguration } from '../src/index.js';
test('Dashboard voice configuration is normalized and Albanian uses instruction-capable speech',()=>{
 assert.deepEqual(speechConfiguration({ARENA_HOST_VOICE:' Onyx ',OPENAI_TTS_MODEL:' tts-1 '},'sq'),{voice:'onyx',model:'gpt-4o-mini-tts'});
 assert.deepEqual(speechConfiguration({},'sq'),{voice:'onyx',model:'gpt-4o-mini-tts'});
 assert.throws(()=>speechConfiguration({ARENA_HOST_VOICE:'sq-AL'},'sq'),/INVALID_HOST_VOICE/);
 assert.equal(speechConfiguration({ARENA_HOST_VOICE:'cedar'},'sq',true).voice,'echo');
});

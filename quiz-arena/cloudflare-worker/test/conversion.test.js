import test from 'node:test';
import assert from 'node:assert/strict';
import {conversionQuote} from '../src/conversion.js';
test('fixed 10000 to EUR1 rate uses exact integer cents without rounding up',()=>{
 for(const [coins,cents,remainder] of [[0,0,0],[99,0,99],[100,1,0],[9999,99,99],[10000,100,0],[20000,200,0],[50000,500,0],[100000,1000,0],[Number.MAX_SAFE_INTEGER,90071992547409,91]]){
  const q=conversionQuote(coins);assert.equal(q.estimated_cents,cents);assert.equal(q.remainder_coins,remainder);assert.equal(q.quote_only,true);assert.equal(q.cashout_eligible,false);
 }
 for(const invalid of [-1,1.2,NaN,Infinity,'10000',Number.MAX_SAFE_INTEGER+1])assert.throws(()=>conversionQuote(invalid),/INVALID_COINS/);
});

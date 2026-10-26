const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const tables={USERS:[{user_id:'A',status:'ACTIVE',name:'Player A',email:'a@example.test',created_at:'2025-01-01T00:00:00Z'},{user_id:'B',status:'ACTIVE',name:'Player B',email:'b@example.test',created_at:'2025-01-01T00:00:00Z'}],WALLET_LEDGER:[]};let now=Date.parse('2026-10-24T12:00:00Z'),sequence=0,locked=false;
class Clock extends Date{constructor(...args){super(...(args.length?args:[now]))}static now(){return now}}
const props={WALLET_ADMIN_EMAIL:'owner@example.test'},context={console,Date:Clock,Set,BigInt,SCHEMA:{},Object,Number,JSON,
 uid_:()=>`id-${String(++sequence).padStart(12,'0')}`,iso_:()=>new Clock().toISOString(),digest_:s=>crypto.createHash('sha256').update(s).digest('hex'),
 fail_:(code,message)=>{throw Object.assign(new Error(message),{apiCode:code})},safeEqual_:(a,b)=>a===b,email_:s=>{if(typeof s!=='string'||!/^\S+@\S+\.\S+$/.test(s))throw Error('email');return s.toLowerCase()},
 db_:()=>({getSheetByName:n=>!!tables[n]}),rows_:n=>(tables[n]||[]).map(x=>({...x})),write_:(n,o)=>{assert(tables[n],n);tables[n].push({...o})},
 auth_:p=>{const u=tables.USERS.find(x=>p.session_token==='token-'+x.user_id&&x.status==='ACTIVE');if(!u)throw Object.assign(Error('session'),{apiCode:'SESSION_EXPIRED'});return {u,s:{}}},
 preWalletRoute_:p=>({success:true,legacy_action:p.action}),Session:{getActiveUser:()=>({getEmail:()=> 'owner@example.test'}),getEffectiveUser:()=>({getEmail:()=> 'owner@example.test'})},PropertiesService:{getScriptProperties:()=>({getProperty:k=>props[k],setProperty:(k,v)=>props[k]=v})},
 LockService:{getScriptLock:()=>({waitLock:()=>{assert(!locked,'nested lock');locked=true},releaseLock:()=>locked=false})}};
vm.createContext(context);vm.runInContext(fs.readFileSync(__dirname+'/../Wallet.gs','utf8'),context);vm.runInContext('walletInitSchema_()',context);Object.keys(context.SCHEMA).forEach(n=>tables[n]=[]);
const call=(action,p={},user='A')=>context.route_({action,session_token:'token-'+user,...p}),reject=(f,code)=>assert.throws(f,e=>e.apiCode===code),advance=ms=>now+=ms;
assert.equal(call('login').legacy_action,'login');reject(()=>call('getBalance',{user_id:'B'}),'CLIENT_AUTHORITY');reject(()=>context.route_({action:'getBalance'}),'SESSION_EXPIRED');
const session=call('startRewardSession',{game_id:'Block Puzzle'});advance(11000);let e=call('recordRewardEvent',{source:'LEVEL',event_id:'event-0001',session_id:session.session_id,game_id:'Block Puzzle',level:1});assert.equal(e.status,'PENDING');assert.equal(e.coins,10);
assert.equal(call('recordRewardEvent',{source:'LEVEL',event_id:'event-0001',session_id:session.session_id,game_id:'Block Puzzle',level:1}).duplicate,true);assert.equal(call('getBalance').pending_coins,10);assert.equal(call('getBalance').available_cents,0);reject(()=>call('requestCashout',{amount:'5.00',payment_method:'PAYPAL',payment_destination:'a@example.test',idempotency_key:'cashout-0001'}),'INSUFFICIENT_CASH');
reject(()=>call('recordRewardEvent',{source:'LEVEL',event_id:'event-0002',session_id:session.session_id,game_id:'Block Puzzle',level:2},'B'),'INVALID_SESSION');assert.equal(call('getCoinHistory',{},'B').total,0);
advance(6000);let rejected=call('recordRewardEvent',{source:'LEVEL',event_id:'event-0002',session_id:session.session_id,game_id:'Block Puzzle',level:2,timestamp:'2000-01-01T00:00:00Z'});assert.equal(rejected.status,'REJECTED');assert.equal(call('getBalance').available_cents,0);
context.walletReviewCoins_(e.transaction_id,'VERIFIED','evidence-reviewed-001','Independent game evidence reviewed');assert.equal(call('getBalance').verified_coins,10);assert.equal(call('getBalance').available_cents,0);
// Funded revenue below is an isolated test fixture, never production or actual revenue.
advance(3600000);const before=new Clock(now-7200000).toISOString(),after=new Clock(now-1).toISOString();const pool={period_id:'period-test-001',start_date:before,end_date:after,gross_confirmed_revenue:'10.00',player_reward_pool:'4.99',funded_amount:'4.99',revenue_reference:'test-revenue-001',funding_reference:'test-funding-001',partner_approval_reference:'test-partner-approval'};
context.walletCreatePool_(pool);context.walletCalculatePool_('period-test-001');assert.equal(call('getBalance').available_cents,0);context.walletApprovePool_('period-test-001');assert.equal(call('getBalance').available_cents,499);assert.equal(call('getCashoutEligibility').eligible,false);
const converted=tables.CONVERSIONS.length;context.walletApprovePool_('period-test-001');assert.equal(tables.CONVERSIONS.length,converted);context.walletClosePool_('period-test-001');assert.equal(call('getBalance').available_cents,499);
// New, non-overlapping period; no old coins are re-used.
advance(7200000);const newStart=new Clock(now-1000).toISOString();const correction=context.walletAdjustCoins_('A',1,'fixture-bonus-002','Isolated test adjustment');context.walletReviewCoins_(correction.transaction_id,'VERIFIED','fixture-evidence-002','Independent fixture evidence');advance(1000);
context.walletCreatePool_({...pool,period_id:'period-test-002',start_date:newStart,end_date:new Clock(now-1).toISOString(),player_reward_pool:'0.01',funded_amount:'0.01',revenue_reference:'test-revenue-002',funding_reference:'test-funding-002'});context.walletCalculatePool_('period-test-002');context.walletApprovePool_('period-test-002');assert.equal(call('getBalance').available_cents,500);assert.equal(call('getCashoutEligibility').eligible,true);
let out=call('requestCashout',{amount:'5.00',payment_method:'PAYPAL',payment_destination:'a@example.test',idempotency_key:'cashout-0001'});assert.equal(call('getBalance').available_cents,0);assert.equal(call('getBalance').pending_cashout_cents,500);assert.equal(call('requestCashout',{amount:'5.00',payment_method:'PAYPAL',payment_destination:'a@example.test',idempotency_key:'cashout-0001'}).duplicate,true);reject(()=>call('requestCashout',{amount:'5.00',payment_method:'PAYPAL',payment_destination:'a@example.test',idempotency_key:'cashout-0002'}),'INSUFFICIENT_CASH');
assert.equal(call('getCashoutHistory',{},'B').cashouts.length,0);context.walletReviewCashout_(out.cashout.cashout_id,'REJECTED','','Review rejected; release reservation');assert.equal(call('getBalance').available_cents,500);
out=call('requestCashout',{amount:'5.00',payment_method:'PAYPAL',payment_destination:'a@example.test',idempotency_key:'cashout-0003'});context.walletReviewCashout_(out.cashout.cashout_id,'UNDER_REVIEW','','Manual verification started');context.walletReviewCashout_(out.cashout.cashout_id,'APPROVED','','Destination and request verified');context.walletReviewCashout_(out.cashout.cashout_id,'PAID','fixture-paid-reference','Fixture payment evidence reviewed');assert.equal(call('getBalance').total_paid_cents,500);assert.equal(call('getBalance').available_cents,0);assert.equal(call('getCashoutHistory').cashouts[0].status,'PAID');assert.equal(call('getCashoutHistory').cashouts[0].payment_reference,'fixture-paid-reference');
const alloc=context.walletAllocate_([{user_id:'A',coins:1},{user_id:'B',coins:2}],100);assert.equal(alloc.users.reduce((s,u)=>s+u.cents,0),100);assert.equal(context.walletMoney_('5.00'),500);reject(()=>context.walletMoney_('5.001'),'INVALID_AMOUNT');assert(tables.AUDIT_LOG.length>10);
console.log('PASS: isolation, no client balance authority, duplicate events/session ownership, impossible timestamps, pending/rejected cash blocked, exact funded period conversion, 4.99/5.00 boundary, idempotency, competing withdrawals, reservation release, paid history, cent rounding and legacy auth dispatch. In-memory Script services; no real funds/payments seeded.');

advance(120000);
let videoSession=call('startRewardSession',{game_id:'html5_arrows'});
assert.equal(videoSession.ad_reward_coins,10);
advance(11000);
let video=call('recordRewardEvent',{source:'AD_REWARD',event_id:'html5-video-0001',session_id:videoSession.session_id,game_id:'html5_arrows'});
assert.equal(video.status,'PENDING');assert.equal(video.coins,10);assert.equal(call('getBalance').available_cents,0);
props.WALLET_HTML5_VIDEO_COINS='7';advance(6000);
let video2=call('recordRewardEvent',{source:'AD_REWARD',event_id:'html5-video-0002',session_id:videoSession.session_id,game_id:'html5_arrows'});
assert.equal(video2.coins,7);props.WALLET_HTML5_VIDEO_COINS='20';
const replay=call('recordRewardEvent',{source:'AD_REWARD',event_id:'html5-video-0002',session_id:videoSession.session_id,game_id:'html5_arrows'});
assert.equal(replay.duplicate,true);assert.equal(replay.coins,7);
reject(()=>call('recordRewardEvent',{source:'AD_REWARD',event_id:'html5-forge-0001',session_id:videoSession.session_id,game_id:'html5_arrows',coins:9999}),'CLIENT_AUTHORITY');
props.WALLET_HTML5_VIDEO_COINS='-1';reject(()=>context.walletVideoCoins_(),'REWARD_CONFIG');
console.log('PASS: HTML5 video coins use a configurable server rule, remain PENDING, preserve amounts on retry and never mint cash.');


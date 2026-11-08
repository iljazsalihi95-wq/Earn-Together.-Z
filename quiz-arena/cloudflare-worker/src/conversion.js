// Fixed owner-requested display rate. A quote never creates spendable cash.
export function conversionQuote(coins){
 if(!Number.isSafeInteger(coins)||coins<0)throw new Error('INVALID_COINS');
 return {currency:'EUR',coins_per_euro:10000,verified_coins:coins,
  estimated_cents:Math.floor(coins/100),remainder_coins:coins%100,
  quote_only:true,cashout_eligible:false};
}

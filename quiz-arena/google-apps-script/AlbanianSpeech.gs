/** Audio-only normalization. Keep database/visual text untouched. Bump cache version when edited. */
function arenaNumberSq_(n){
 const units=['zero','një','dy','tre','katër','pesë','gjashtë','shtatë','tetë','nëntë','dhjetë','njëmbëdhjetë','dymbëdhjetë','trembëdhjetë','katërmbëdhjetë','pesëmbëdhjetë','gjashtëmbëdhjetë','shtatëmbëdhjetë','tetëmbëdhjetë','nëntëmbëdhjetë'];
 if(n<0)return 'minus '+arenaNumberSq_(-n);
 if(n<20)return units[n];
 if(n<100)return ['','','njëzet','tridhjetë','dyzet','pesëdhjetë','gjashtëdhjetë','shtatëdhjetë','tetëdhjetë','nëntëdhjetë'][Math.floor(n/10)]+(n%10?' e '+units[n%10]:'');
 if(n<1000)return units[Math.floor(n/100)]+'qind'+(n%100?' e '+arenaNumberSq_(n%100):'');
 for(const [scale,word] of [[1000000000,'miliardë'],[1000000,'milionë'],[1000,'mijë']])if(n>=scale)return arenaNumberSq_(Math.floor(n/scale))+' '+word+(n%scale?' e '+arenaNumberSq_(n%scale):'');
}
function arenaNormalizeVoice_(text,locale){
 if(locale!=='sq')return text;
 let t=String(text);
 for(const [pattern,spoken] of [['a(?:\\s*\\.\\s*|\\s+)s','alejhi selam'],['r(?:\\s*\\.\\s*|\\s+)a','radijallahu anhu'],['xh(?:\\s*\\.\\s*|\\s+)sh','xhel-le shanuhu']])
  t=t.replace(new RegExp('(?<![\\p{L}\\d])'+pattern+'\\.?(?![\\p{L}\\d])','giu'),spoken);
 t=t.replace(/ﷺ/g,'sal-lallahu alejhi ve sel-lem');
 // A colon in quiz literature denotes chapter:verse, never a clock.
 t=t.replace(/\b(\d{1,3}):(\d{1,3})\b/g,(_,s,a)=>'surja '+arenaNumberSq_(+s)+', ajeti '+arenaNumberSq_(+a));
 t=t.replace(/\b(\d{1,2})[./](\d{1,2})[./](\d{4})\b/g,(_,d,m,y)=>arenaNumberSq_(+d)+' '+['','janar','shkurt','mars','prill','maj','qershor','korrik','gusht','shtator','tetor','nëntor','dhjetor'][+m]+' '+arenaNumberSq_(+y));
 t=t.replace(/\b(\d+)[,.](\d+)\b/g,(_,a,b)=>arenaNumberSq_(+a)+' presje '+[...b].map(x=>arenaNumberSq_(+x)).join(' '));
 t=t.replace(/(?<![\p{L}\d])\d{1,12}(?![\p{L}\d])/gu,n=>arenaNumberSq_(+n)).replace(/%/g,' për qind');
 // Preserve Albanian digraphs. Do not alter s to sh or invent phonetic spellings.
 return t.replace(/Kur[’']ani/gi,'Kurani').replace(/\s+/g,' ').trim();
}

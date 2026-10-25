const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const http=require('node:http'),fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../app/src/main/assets/html5');
const mime={'.html':'text/html','.js':'application/javascript','.css':'text/css'};
const server=http.createServer((req,res)=>{const file=path.join(root,new URL(req.url,'http://localhost').pathname.replace(/^\//,'')||'index.html');if(!file.startsWith(root+path.sep)){res.writeHead(403).end();return}fs.readFile(file,(e,data)=>{res.writeHead(e?404:200,{'content-type':mime[path.extname(file)]||'text/plain'});res.end(e?'Missing':data)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));const browser=await chromium.launch({headless:true});try{
 const page=await browser.newPage({viewport:{width:390,height:844},hasTouch:true});const errors=[];page.on('pageerror',e=>errors.push(e.message));const url=`http://127.0.0.1:${server.address().port}/index.html`;
 await page.goto(url);await page.locator('[data-game="word"]').click();assert.equal(await page.locator('[data-level]:disabled').count(),39);await page.locator('[data-level="1"]').click();
 const level=await page.evaluate(()=>GardenGames.wordLevel(1));
 for(const word of level.words){const used=[];for(const c of word){const i=level.letters.findIndex((v,i)=>v===c&&!used.includes(i));used.push(i);await page.locator(`[data-letter="${i}"]`).focus();await page.keyboard.press('Enter')}await page.locator('#submit').click()}
 await page.getByRole('heading',{name:'Level complete!',exact:true}).waitFor();assert.equal(await page.locator('.overlay').count(),1);await page.getByRole('button',{name:'Level map',exact:true}).click();assert.equal(await page.locator('[data-level]:not(:disabled)').count(),2);
 await page.reload();await page.locator('[data-game="word"]').click();assert.equal(await page.locator('[data-level]:not(:disabled)').count(),2);await page.locator('#back').click();await page.locator('[data-game="memory"]').click();await page.locator('[data-level="1"]').click();
 await page.waitForFunction(()=>!document.querySelector('#banner').textContent.includes('Take a moment'));
 const cards=await page.locator('[data-card]').evaluateAll(nodes=>nodes.map(n=>({id:n.dataset.card,symbol:n.querySelector('use').getAttribute('href')})));
 const groups={};for(const c of cards)(groups[c.symbol]??=[]).push(c.id);for(const pair of Object.values(groups)){for(const i of pair)await page.locator(`[data-card="${i}"]`).click()}
 await page.getByRole('heading',{name:'Level complete!',exact:true}).waitFor();await page.getByRole('button',{name:'Next challenge',exact:true}).click();await page.locator('#video').waitFor();await page.waitForTimeout(2200);const before=await page.locator('#moves').textContent();await page.locator('#video').click();assert.equal(await page.locator('#moves').textContent(),before);assert.match(await page.locator('#toast').textContent(),/No rewarded video/);
 for(const size of [{width:390,height:844},{width:844,height:390}]){await page.setViewportSize(size);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false)}
 fs.mkdirSync('html5-browser-results',{recursive:true});await page.screenshot({path:'html5-browser-results/memory.png',fullPage:true});await page.goto(url);await page.screenshot({path:'html5-browser-results/hub.png',fullPage:true});assert.deepEqual(errors,[]);console.log('HTML5 UI smoke passed: both games, completion, unlocks, persistence, no-fill ads, responsive width');
 }finally{await browser.close();server.close()}
})().catch(e=>{console.error(e);server.close();process.exitCode=1});

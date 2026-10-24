"""Build static GamePix catalogue from publisher-authorized JSON metadata only."""
import concurrent.futures,json,pathlib,urllib.request,urllib.parse,shutil
SID='71376'
base=f'https://feeds.gamepix.com/v2/json?sid={SID}&pagination=96&page='
def fetch(page):
 with urllib.request.urlopen(base+str(page),timeout=45) as r:return json.load(r)
first=fetch(1);last=int(urllib.parse.parse_qs(urllib.parse.urlsplit(first['last_page_url']).query)['page'][0]);assert last<500
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
 pages=[first]+list(executor.map(fetch,range(2,last+1)))
items=[g for p in pages for g in p['items']]
assert items and len({g['id'] for g in items})==len(items)
for g in items:
 u=urllib.parse.urlsplit(g['url']);assert u.scheme=='https' and u.hostname=='play.gamepix.com' and urllib.parse.parse_qs(u.query).get('sid')==[SID]
out=pathlib.Path('_site');out.mkdir(exist_ok=True);(out/'gamepix').mkdir(exist_ok=True)
html=pathlib.Path('docs/gamepix/index.html').read_text();start=html.index('<script id="catalogData" type="application/json">')+len('<script id="catalogData" type="application/json">');end=html.index('</script>',start)
data=json.dumps({'sid':SID,'items':items},ensure_ascii=False,separators=(',',':')).replace('<','\\u003c')
(out/'gamepix/index.html').write_text(html[:start]+data+html[end:]);shutil.copyfile('docs/ads.txt',out/'ads.txt');(out/'.nojekyll').touch();(out/'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta http-equiv="refresh" content="0;url=./gamepix/"><title>We Earn Together</title><a href="./gamepix/">Open games</a></html>')
print(f'Built {len(items)} official games with SID {SID}')

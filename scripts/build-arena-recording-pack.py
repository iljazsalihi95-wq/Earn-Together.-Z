#!/usr/bin/env python3
"""Export the existing bundled question banks; never generate new quiz content."""
import argparse, json, re, zipfile
from collections import Counter
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
out=args.output;out.mkdir(parents=True,exist_ok=True)
entries=[];by_id={}
for name in ('sheet-bank-sq.json','general-sq.json'):
    for q in json.loads((ROOT/'app/src/main/assets/quiz-arena'/name).read_text())['questions']:
        row=dict(q);row['bank']=name
        if row['id'] in by_id:
            assert row['question']==by_id[row['id']]['question'],row['id']
            continue
        by_id[row['id']]=row;entries.append(row)
# Also include the 20 authored mathematics questions encoded in the Android source.
source=(ROOT/'app/src/main/java/com/weearntogether/play/ArenaOfflineStore.kt').read_text()
pattern=r'MathQ\(("(?:[^"\\]|\\.)*"),listOf\((.*?)\),(\d+),("(?:[^"\\]|\\.)*")\)'
math=re.findall(pattern,source)
assert len(math)==20,len(math)
for i,(prompt,choices,correct,explanation) in enumerate(math,101):
    q=dict(id=f'arena-math-{i}',category='Matematikë',question=json.loads(prompt),options=json.loads('['+choices+']'),correctIndex=int(correct),explanation=json.loads(explanation),source='Llogaritje aritmetike/gjeometri elementare — verifikuar lokalisht',bank='ArenaOfflineStore.kt')
    assert q['id'] not in by_id
    entries.append(q);by_id[q['id']]=q
counts=Counter(q['bank'] for q in entries)
prompt='''Spoken Albanian quiz-show presenter. Adult male voice, warm, clear, confident and natural standard Albanian (sq-AL). SPOKEN WORD ONLY. No singing, no melody, no rap, no chant, no backing vocals, no instruments or background music. Read only the supplied Albanian text, word for word. Do not improvise or add an introduction or answer. Respect ë and ç and Albanian digraphs. Calm studio delivery, natural pitch, deliberate pace, brief pause after the question and between alternatives. All clips must use the same voice/persona. End cleanly without an outro.'''
(out/'PROMPT_SUNO.txt').write_text(prompt+'\n\nSuno mund ta këndojë tekstin edhe me këtë prompt. Përdor vetëm versionin që e lexon saktë; nëse ndryshon fjalët, mos e përdor në aplikacion.\n',encoding='utf-8')
manifest={'version':1,'language':'sq','questions':{}}
full=['QUIZ ARENA — PYETJET EKZISTUESE PËR INCIZIM','Burimi: bankat e ruajtura në repository. Ky eksport nuk është rishikim i ri i saktësisë së pyetjeve.','Mos i bashko pyetjen dhe shpjegimin në një audio.','']
labels=['a','bë','cë','dë','e']
for n,q in enumerate(entries,1):
    assert q['correctIndex'] in range(len(q['options']))
    ident=q['id'];assert re.fullmatch(r'[\w-]+',ident)
    spoken=q['question']+'\n\n'+'\n\n'.join(f'Alternativa {labels[i]}. {v}.' for i,v in enumerate(q['options']))
    explanation='Përgjigjja e saktë është alternativa '+labels[q['correctIndex']]+'. '+q['options'][q['correctIndex']]+'. '+q['explanation']
    directory=out/'incizimet';directory.mkdir(exist_ok=True)
    (directory/(ident+'-question.txt')).write_text(spoken+'\n',encoding='utf-8')
    (directory/(ident+'-explanation.txt')).write_text(explanation+'\n',encoding='utf-8')
    full+= [f'{n:03d} | {ident} | {q["category"]}',f'PYETJA — {ident}-question.mp3',spoken,'',f'SHPJEGIMI — {ident}-explanation.mp3',explanation,'Burimi: '+str(q.get('source',q.get('sources',''))),'-'*40,'']
    manifest['questions'][ident]={'prompt':q['question'],'choices':q['options'],'correct':q['correctIndex']}
(out/'TE_GJITHA_PYETJET.txt').write_text('\n'.join(full),encoding='utf-8')
(out/'manifest-plan.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(out/'UDHEZIM.txt').write_text(f'''Paketa përmban {len(entries)} pyetje ekzistuese: {counts['sheet-bank-sq.json']} nga banka Sheet, {counts['general-sq.json']} shtesë nga banka e përgjithshme dhe {counts['ArenaOfflineStore.kt']} nga kodi i matematikës. Pyetjet e përgjithshme me të njëjtat ID janë përfshirë vetëm një herë. Disa banka mund të kenë pyetje me përmbajtje të përafërt; ID-të janë ruajtur veçmas.

1. Vendos PROMPT_SUNO.txt te stili/prompt-i.
2. Kopjo vetëm tekstin e një skedari question.txt si tekst për lexim.
3. Shkarko dhe emërto audion sipas ID-së: sheet-1-question.mp3, për shembull.
4. Incizo veçmas explanation.txt dhe emërtoje sheet-1-explanation.mp3.
5. Ruaj të njëjtin zë/persona për të gjitha. Mos përfshi numrin e skedarit ose etiketën PYETJA në audio.
6. Dërgo MP3-të në grupe. Nuk nevojitet një incizim i vetëm i gjatë.

Manifesti është PLAN, jo listë audiosh të përfunduara. Mos zëvendëso manifestin aktiv derisa MP3-të përkatëse të jenë dorëzuar dhe kontrolluar. Audio duhet të ruhet te app/src/main/assets/quiz-arena/voice-sq/. Aplikacioni verifikon ID-në, tekstin dhe rendin e alternativave para se të luajë incizimin. Shpjegimi jepet vetëm pas përgjigjes.

Përmbajtja vjen nga skedarët ekzistues; nuk deklarohet kontroll i ri burimor ose përditësim i Sheet-it live.
''',encoding='utf-8')
archive=out.with_suffix('.zip')
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(out.rglob('*')):
        if p.is_file():z.write(p,p.relative_to(out))
print(json.dumps({'questions':len(entries),'clips_planned':len(entries)*2,'archive':str(archive),'complete_text':str(out/'TE_GJITHA_PYETJET.txt')},ensure_ascii=False))

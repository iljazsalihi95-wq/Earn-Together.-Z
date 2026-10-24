from pathlib import Path

# Idempotent one-shot integration patch for the native Games hub.
p = Path('app/src/main/java/com/weearntogether/play/MainActivity.kt')
s = p.read_text()

marker = 'OPEN GAMEPIX CATALOG'
if marker in s:
    print('GamePix catalogue already connected; nothing to patch.')
    raise SystemExit(0)

old = 'private fun gamesHub(){screen("GAMES");label("Choose a game visually. Your progress is saved offline and synced to your account.");missionCard('
new = 'private fun gamesHub(){screen("GAMES");label("Choose a game visually. Your progress is saved offline and synced to your account.");section("GAMEPIX ONLINE GAMES");button("OPEN GAMEPIX CATALOG • 96+ GAMES"){startActivity(Intent(this,GamePixActivity::class.java))};label("Official GamePix catalogue • Publisher SID 71376 • GamePix ads/tracking preserved",12,gold);missionCard('

if old not in s:
    raise SystemExit('gamesHub anchor not found and GamePix marker is absent')

s = s.replace(old, new, 1)
p.write_text(s)
print('GamePix catalogue connected.')

# Përditësimi i Quiz Arena ekzistues

Repository dhe aplikacioni Android mbeten të njëjtët. Ky përditësim nuk vendos çelësa AI në APK dhe nuk ndryshon lojërat e tjera ose sistemin financiar.

## Çfarë është e lidhur

- Android LIVE → Worker `/api/arena` → deployment-i ekzistues Apps Script → Sheet-i ekzistues.
- 212 pyetje shqip aktive në bankën e përbashkët, pas shtimit të 75 pyetjeve më 05.11.2026 dhe përjashtimit të tre përsëritjeve. 75 pyetjet e reja kanë kontroll burimesh ose llogaritjesh; pyetjet e vjetra ruajnë statusin e tyre të mëparshëm dhe nuk etiketohen si të riverifikuara këtë ditë.
- E njëjta bankë është e përfshirë në APK për OFFLINE. Pikët lokale nuk publikohen si LIVE.
- Ndihmat LIVE kanë nevojë për Apps Script v7; ndihmat OFFLINE funksionojnë në APK. Audioja dhe biseda AI përdorin Worker v2. Pa këto vendosje, APK shfaq mungesën e shërbimit dhe përdor TTS kur shqipja ekziston në pajisje.

## Ruajtja e versionit funksional

Para zëvendësimit në editorin Apps Script: përdor historinë e projektit për të ruajtur një version të kodit aktiv. Kodi në repository para këtij përditësimi ruhet gjithashtu në commit-in `4d12ee551a814ad091ef1dc4646292c46b5d990d`.

Ndryshimi konkret që kërkon v7: versioni i mëparshëm nuk ka `arenaLifeline`, nuk ruan kufijtë e ndihmave dhe nuk publikon vështirësinë e pyetjes. Rrugët ekzistuese të vlerësimit, llogarisë dhe zërit ruhen.

## Apps Script — i njëjti projekt dhe deployment

1. Në projektin që shërben deployment-in `AKfycbzu9rso0BO_UhLBPi3TMqIQl44tZ12r0n-t143ydmWOoiVMPbm_YA1uF0rImIfEcvaERw`, ruaj versionin funksional.
2. Përditëso vetëm `Code.gs` nga `quiz-arena/google-apps-script/Code.gs`. Mbaj `AlbanianSpeech.gs` ekzistues; versioni përkatës gjendet në të njëjtën dosje. Mos fshi skedarët e tjerë ose Script Properties.
3. Deploy → Manage deployments → Edit në deployment-in ekzistues → New version → Deploy. Mbaje të njëjtën URL `/exec`.
4. Hap URL-në ekzistuese `/exec`. Përgjigjja duhet të ketë `version: 7` dhe `lifelines: true`.

Mos krijo projekt ose deployment paralel. Ky proces nuk duhet të prekë Script-in financiar të We Earn Together.

## Cloudflare — i njëjti Worker

Emri mbetet `quiz-arena-backend`, URL-ja mbetet `https://quiz-arena-backend.lazisalihi.workers.dev/`.

Rruga më e thjeshtë pa kompjuter është GitHub Actions:

1. Te repository ekzistues → Settings → Secrets and variables → Actions, sigurohu që janë konfiguruar `CLOUDFLARE_API_TOKEN` dhe `CLOUDFLARE_ACCOUNT_ID`. Token-i duhet të lejojë vendosjen e Worker-it ekzistues dhe migrimin e Durable Object të kuotave në llogarinë e duhur. Mos i shkruaj çelësat në chat, në kod ose në APK.
2. Actions → `Deploy existing Quiz Arena Worker` → Run workflow → main.
3. Workflow teston kodin, vendos vetëm Worker-in e emërtuar në `wrangler.toml` dhe krijon lidhjen `ARENA_QUOTA` për kufijtë e kostos. Përdor `--keep-vars`, ndërsa sekretet ekzistuese Cloudflare ruhen.
4. `/health` duhet të tregojë `version: 2`, `openai_configured: true`, `apps_script_bridge: true`, `ai_quota_configured: true`.

Alternativë nga një mjedis ku Wrangler është i autentikuar: `npx wrangler@4.136.3 deploy --config wrangler.toml --keep-vars`.

Mos e ngjit vetëm `src/index.js` në editor: ai importon normalizimin e shqiptimit dhe kërkon lidhjen Durable Object. Artefakti `Quiz-Arena-backend-update` i build-it përmban bundle-in e plotë dhe konfigurimin. Vendosja pa `ARENA_QUOTA` refuzon kërkesat AI me kosto.

## Konfigurimi AI

`OPENAI_API_KEY` mbetet sekret në Cloudflare. Prezantuesja përdor `coral`, shoku `onyx`; modeli zanor i paracaktuar është `gpt-4o-mini-tts`. Vlera të personalizuara mund të vendosen me `ARENA_HOST_VOICE`, `OPENAI_TTS_MODEL`, `OPENAI_TEXT_MODEL`.

Kufijtë fillestarë janë 40 gjenerime për lojtar në ditë, 400 gjithsej në ditë dhe 8 në minutë për lojtar. Cache shmang gjenerimet e përsëritura. Janë kufij numri gjenerimesh, jo garanci për një shumë të caktuar në euro.

Portreti përpunohet dhe ruhet në telefon. Për shërbimet AI dërgohen teksti i pyetjes/dialogut dhe emri i zgjedhur i lojtarit; fotografia nuk dërgohet.

## Kontrolli pas vendosjes

Hyr me llogarinë ekzistuese, regjistro emrin, nis LIVE, dëgjo pyetjen, prit nisjen e kohës, përdor një ndihmë dhe rihap aplikacionin. Përdorimi i saj duhet të mbetet i regjistruar. Përgjigjja e saktë nuk shfaqet para vlerësimit. Telefonata duhet të ketë njoftimin e prezantueses, zilen, mendimin e shokut dhe falënderimin.

Testet automatike dhe emulatori nuk zëvendësojnë dëgjimin e zërit shqip dhe provën në telefon fizik. Gjuhët shtesë kanë pyetje të përkthyera kur banka i përmban; ndërfaqja e plotë është ende shqip dhe kjo shënohet në zgjedhësin e gjuhës.

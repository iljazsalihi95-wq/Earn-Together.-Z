# Quiz Arena — urdhri i punës dhe kriteret e pranimit

Projekti: `iljazsalihi95-wq/Earn-Together.-Z`, dega `main`, brenda aplikacionit nativ We Earn Together.

## Puna e autorizuar

- Lidh backend-in ekzistues me D1 ekzistues `lazy-ai-studio` (binding `DB`) për sesionet, përgjigjet, nivelet dhe progresin; ruaj llogaritë ekzistuese dhe autoritetin e portofolit.
- Përdor R2 ekzistues `site-creator-r2` (binding `BUCKET`) privat për MP3-të e zërit që të ripërdoren pas autentikimit. Përdor çelësin ekzistues të serverit. Ripërdor Worker-in, D1 dhe R2 e krijuar; mos krijo zëvendësime.
- Rregullo studion blu/vjollcë/ari, prezantuesin me foton ekzistuese, lojtarin, veshjet, publikun dhe telefonatën me personazh të dukshëm.
- Lidh lëvizjen e gojës me audion reale dhe heshtjen e saj. Përdor regjistrimet ekzistuese dhe zërin AI të backend-it.
- Pyetja lexohet para nisjes së 60 sekondave. Vetëm lojtari zgjedh përgjigjen. Pas bllokimit shfaqen saktësia, shpjegimi dhe burimi; vazhdimi bëhet me butonin e lojtarit.
- Telefonata ndihmëse jep mendim dhe ia lë zgjedhjen lojtarit. Ndihmat, rezultatet dhe kërkesat e përsëritura duhet të ruhen saktë.
- Shto Reward Coin në hyrjen dhe rezultatin e Arenës, me video të përfunduar nga SDK-ja dhe portofolin real të llogarisë. Shfaq statusin në pritje/verifikuar dhe ruaj dallimin nga Game Coins.
- Ruaj pyetjet e plota ekzistuese dhe ushtrimin offline. Mbaj reklamat në pushimet natyrore të lojës.

## Pranimi kërkon prova

1. Teste backend për autentikim, izolim përdoruesish, përgjigje të kontrolluara në server, 60 sekonda pas leximit, ruajtje dhe riprovime pa pikë/coin të dyfishta.
2. Teste për ruajtjen dhe ripërdorimin privat të audios R2.
3. APK që ndërtohet dhe teste Android për studion, telefonatën, veshjet, kohën dhe shpjegimin.
4. Deploy i Worker-it me binding-e reale D1/R2 dhe kontroll `/health` pas deploy-it.
5. Testim i veçantë i zërit, hyrjes dhe videos reale në telefon. Një build i suksesshëm nuk i konfirmon vetë këto shërbime.

Statusi i çdo kontrolli raportohet sipas rezultatit të vërtetë të ekzekutimit.

## Kontrollet e këtij ndryshimi

- 28/28 teste Worker, me SQLite real për skemën dhe logjikën D1: kaluan.
- Testet ekzistuese të llogarive, portofolit, bankës së pyetjeve dhe Arenës: kaluan.
- `compileDebugKotlin` dhe `compileDebugAndroidTestKotlin`: kaluan me Gradle 8.10.2 / JDK 17 / Android SDK 35.
- Paketimi provues i Worker-it me Wrangler: kaloi.
- Ekzekutimi i testeve Android në emulator dhe funksionimi i shërbimeve në telefon: kërkojnë rezultat të veçantë.
- Aktivizimi D1/R2 në prodhim: konfirmohet vetëm pas deploy-it dhe kontrollit të health-it.

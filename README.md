# NutriNapló – kalória- és vitaminkövető (Android)

Saját használatra készült, teljesen magyar nyelvű táplálkozásnapló Kotlin + Jetpack Compose alapon.

## Mit tud?

- **Kalória és makrók**: napi cél a profilod alapján (Mifflin–St Jeor + aktivitás + cél), fehérje/szénhidrát/zsír követés, 6 étkezés (reggeli … nassolás).
- **Teljes vitamin- és ásványianyag-elemzés**: 13 vitamin + 10 ásványi anyag + rost, telített zsír, koleszterin, cukor.
  A „Tápanyagok” fülön látod, **miből van hiányod** (napi vagy 7 napos átlag), és mely ételek a legjobb források.
- **Vízkövetés**: poharanként, napi céllal, heti/havi grafikonnal.
- **Óriás ételadatbázis**:
  - ~1460 alapélelmiszer magyarul, teljes vitaminprofillal (USDA SR28),
  - 107 magyar étel receptek alapján számolva (gulyásleves, pörkölt, lecsó, töltött káposzta, túrós csusza, főzelékek, lángos, bejgli …),
  - online keresés és **vonalkód-olvasás** az Open Food Facts milliós termékadatbázisában,
  - saját ételek felvétele (pl. a csomagolás tápértéktáblázatából).
- **AI ételfelismerés (ingyenes)**: fotó vagy szöveges leírás alapján („2 tojásos rántotta, 1 szelet kenyér”), Google Gemini ingyenes kerettel.
  A felismert ételeket az app a helyi adatbázishoz illeszti, így a vitaminadatok is megvannak.
- Kedvencek, legutóbbiak, étkezés másolása tegnapról, testsúlynapló grafikonnal, sorozatszámláló.
- Sötét mód, minden adat csak a telefonon tárolódik, mentés/visszatöltés JSON fájlba.

## APK készítése

### A) GitHub-bal (semmit nem kell telepíteni)
1. Hozz létre egy új (akár privát) GitHub repót, és töltsd fel bele ennek a mappának a teljes tartalmát.
2. A feltöltés után az **Actions** fülön automatikusan elindul az „APK build” (kb. 5–8 perc).
3. Ha zöld pipát látsz, kattints a futásra, és alul az **Artifacts** résznél töltsd le a `NutriNaplo-apk` csomagot.
4. A zip-ben lévő `app-release.apk`-t másold a telefonra, és telepítsd (engedélyezni kell az „ismeretlen forrásból” telepítést).

### B) Android Studióval
1. Nyisd meg a mappát Android Studióban (File → Open).
2. Várd meg a Gradle szinkronizálást, majd: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. Az APK helye: `app/build/outputs/apk/debug/app-debug.apk`.

## AI kulcs beállítása (ingyenes)
1. Nyisd meg: <https://aistudio.google.com/apikey> és jelentkezz be Google-fiókkal.
2. „Create API key” → másold ki a kulcsot.
3. Az appban: **Profil → AI ételfelismerés** → illeszd be.

Bankkártya nem kell. Az ingyenes keretnek napi limitje van, és a Google a beküldött képeket felhasználhatja a szolgáltatás fejlesztésére.

## Adatforrások
- USDA National Nutrient Database for Standard Reference, Release 28 – közkincs. Az amerikai dúsított (vasas, folsavas) liszttermékek értékeit a magyar, nem dúsított termékekhez igazítottuk.
- Open Food Facts – ODbL licenc.
- Ajánlott napi bevitel: NIH / EFSA felnőtt referenciaértékek.

Az értékek tájékoztató jellegűek, nem helyettesítik orvos vagy dietetikus tanácsát.

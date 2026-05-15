East Prussia 1945 — IJA 2025/26 projekt
================================================

Název projektu:
East Prussia 1945 — tahová taktická strategie v JavaFX

Autoři:
- xdubsil00
- xbobekp00

Použité technologie:
- Java SE 21
- JavaFX
- Maven

Stručný popis:
Projekt implementuje tahovou strategickou hru inspirovanou boji ve Východním Prusku v lednu až únoru 1945. Hra obsahuje dvě historicky inspirované mapy, jednotky německé a sovětské strany, pohyb po terénu, boj, protiútoky, obsazování budov, ekonomiku, nákup jednotek ve fabrikách, opravy, jednoduchého bota a systém replay/logu.

Scénáře:
1. Balga / Heiligenbeil corridor
   Scénář inspirovaný boji kolem Heiligenbeilské kapsy a pobřežního koridoru.

2. Allenstein lakes approaches
   Scénář inspirovaný postupem v oblasti jezer a přístupů k Allensteinu/Olsztynu.

Scénáře jsou historicky inspirované herní abstrakce, nikoliv přesná archivní rekonstrukce jednotek a pozic.

Adresářová struktura:
src/             zdrojové soubory Java
data/            referenční datové tabulky
lib/             mapy, scénáře, obrázky a další externí soubory
pom.xml          Maven konfigurace
readme.txt       základní informace k projektu
README.md        rozšířený popis projektu
ai_audit.md      povinný záznam využití AI
git_history.txt  povinný záznam Git historie
rozdeleni.txt    volitelné přerozdělení bodů mezi členy týmu

Důležité soubory:
lib/maps/*.map        definice map
lib/maps/*.units      počáteční rozmístění jednotek
lib/maps/*.buildings  počáteční vlastnictví budov
data/terrain.tsv      referenční hodnoty terénů
data/units.tsv        referenční hodnoty jednotek
data/units-damage.tsv referenční tabulka poškození

Překlad:
V kořenovém adresáři projektu spusťte:

mvn clean package

Spuštění:
Doporučený způsob spuštění je:

mvn javafx:run

Poznámka:
Maven vytvoří výstupy v adresáři target/. JavaFX aplikace mohou podle prostředí vyžadovat speciální runtime/modulové argumenty, proto je jako hlavní podporovaný způsob spuštění uvedeno mvn javafx:run.

Ovládání:
- kolečko myši: přiblížení / oddálení mapy
- tažení levým tlačítkem: posun mapy
- levé kliknutí: výběr nebo zobrazení informací o poli/jednotce
- modře zvýrazněné pole: možný pohyb
- červeně zvýrazněné pole: možný útok
- tlačítko Capture: obsazení budovy pěchotou
- tlačítko Wait: ukončení akce vybrané jednotky
- Next turn: ukončení tahu aktuálního hráče

Konvence hráčů:
P1 = Sověti
P2 = Němci

Pohyb:
Hra používá hexagonální mapu, proto má každé pole až šest sousedních polí. To je záměrná úprava oproti obecnému čtvercovému modelu, protože grafické rozhraní i mapy jsou navrženy jako hexová taktická mapa.

Herní mechaniky:
- jednotky mají HP, cenu, pohyb, typ pohybu a výzbroj
- boj je deterministický
- obrana terénu a překryvů ovlivňuje výsledné poškození
- přeživší obránce může provést protiútok
- budovy může obsazovat pouze pěchota
- města poskytují příjem
- továrny umožňují nákup nových jednotek dané frakce
- jednotky lze opravovat na vlastních vhodných budovách
- vítězství je odvozeno od vlastnictví HQ

Replay/log:
Hra umožňuje uložit a načíst replay/log odehrané partie.

Replay není plnohodnotný dlouhodobý save-game systém. Slouží hlavně k přehrávání záznamu odehrané partie.

Ovládání replay:
- Save replay: uložit záznam partie
- Load replay: načíst záznam partie
- Next replay turn: krok dopředu
- Prev turn: krok zpět
- Resume play: pokračování ve hře z aktuálního stavu replaye

Bot:
Projekt obsahuje jednoduchého bota. Bot umí odehrát tahy za zvolenou stranu a je možné spustit i režim Bot vs Bot pro pozorování automatické hry.

Známá omezení:
- hra používá hexové sousedství se šesti sousedy
- replay slouží jako log/replay, ne jako univerzální save-game systém
- AI bot je jednoduchý heuristický bot
- historické scénáře jsou abstrahované pro hratelnost
- architektura odděluje model od vykreslování, ale část controller logiky zůstává v JavaFX třídách

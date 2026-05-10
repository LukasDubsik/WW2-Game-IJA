# AI Audit Log – tým xlogin00

**Datum poslední aktualizace:** 20. 4. 2026

---

## Souhrn použití AI

Tento soubor byl sestaven v ChatGPT na základě skutečné komunikace nad tímto projektem a následně upraven do podoby vhodné pro odevzdání. Struktura vychází ze vzorového souboru `ai_audit_sample.md`, ale obsah odpovídá reálnému způsobu použití AI v tomto projektu. AI zde nebyla použita jako náhrada samostatné práce ani jako generátor celého řešení, ale především jako pomocný nástroj při tvorbě časově náročných, opakujících se nebo prezentačních částí implementace.

Celkově odhadujeme, že AI se podílela přibližně na **25 % výsledného projektu**, a to včetně části grafických assetů / vizuálních podkladů, pomocných tříd, dílčích UI prvků a některých boilerplate nebo datových částí kódu. **Jádro herní logiky, architektura programu, hlavní návrhová rozhodnutí, integrace mechanik a finální propojení systému byly vytvořeny a dokončeny ručně.**

AI byla používána především:
- pro urychlení tvorby opakujícího se nebo technicky rutinního kódu,
- pro návrhy částí uživatelského rozhraní, které byly následně ručně upraveny,
- pro kostry některých podpůrných tříd a enumů,
- pro pomoc s formulací některých textových a prezentačních částí,
- pro část grafických assetů nebo podkladů, které byly následně ručně vybírány, upravovány a zasazovány do projektu.

Naopak bez použití AI byly řešeny zejména:
- celková koncepce hry a architektura projektu,
- návrh modelu herního světa,
- hlavní logika pohybu, tahového systému a combat flow,
- rozhodnutí o podobě map, scénářů, jednotek, zbraní a herních mechanik,
- ruční ladění hodnot jednotek, armamentů, terénů a UX detailů,
- finální integrace všech částí do funkčního celku.

---

## 1. Architektura projektu a hlavní herní logika
* **Nástroj:** Bez použití AI jako primárního autora
* **Datum:** průběžně během vývoje projektu
* **Použití:**
  Celková architektura aplikace, rozdělení na model / view / app vrstvy, základní tok hry, návrh tříd a hlavní mechaniky byly navrženy ručně. AI byla případně používána pouze konzultačně při dílčích implementačních dotazech.
* **Úprava studentem:**
  Veškeré klíčové návrhy, rozhodnutí o struktuře projektu i výsledná integrace byly provedeny ručně.
* **Míra generování:** 0–10 % (pouze konzultační pomoc, nikoli převzetí architektury).

---

## 2. Uživatelské rozhraní a informační panely
* **Nástroj:** ChatGPT
* **Datum:** průběžně během vývoje GUI
* **Prompt / zadání:**
  Iterace nad podobou JavaFX rozhraní, spodní lišty, zobrazením tahu, informačního panelu, HP zobrazení, popisů dlaždic a jednotek a celkového zpřehlednění UI.
* **Použití:**
  AI pomohla vytvořit kostru některých částí rozhraní, zejména informačních panelů, některých bloků JavaFX UI a pomocných prezentačních metod.
* **Úprava studentem:**
  Výstupy byly následně ručně upravovány, zjednodušovány, stylově sjednocovány a dopracovávány tak, aby odpovídaly výsledné podobě projektu. Nešlo o přímé převzetí hotového UI, ale o urychlení tvorby jinak zdlouhavého rozhraní.
* **Míra generování:** přibližně 40–60 % v dotčených UI úsecích, ale výrazně méně v rámci projektu jako celku.

---

## 3. Podpůrné třídy pro zbraně, armamenty a část enum / datových struktur
* **Nástroj:** ChatGPT
* **Datum:** pozdější fáze vývoje combat systému
* **Prompt / zadání:**
  Návrh podpůrných tříd a enumů pro armamenty / weapon tags / target classes, včetně struktury pomocných datových modelů a některých boilerplate metod.
* **Použití:**
  AI pomohla především s přípravou kostry tříd, enumů, getterů, opakujících se metod a mechanické části implementace.
* **Úprava studentem:**
  Samotné typy zbraní, jejich historické názvy, význam, rozdělení, hodnoty soft / hard attack, range a konkrétní herní balans byly určeny a upraveny ručně. AI zde nepřipravovala finální design mechanik, ale jen technickou kostru pro jejich zavedení.
* **Míra generování:** přibližně 50 % kostry daných tříd, finální obsah a hodnoty ručně.

---

## 4. Grafické assety a vizuální podklady
* **Nástroj:** ChatGPT
* **Datum:** průběžně během vizuálního zpracování projektu
* **Použití:**
  AI byla použita při získávání nebo přípravě grafických assetů, případně jako pomoc při jejich výběru, seskupení nebo zasazení do hry.
* **Úprava studentem:**
  Assety byly ručně vybírány, kontrolovány, případně upravovány a integrovány do projektu. Výsledná vizuální podoba mapy a práce s assety v programu byla provedena ručně.
* **Míra generování:** nelze přesně vyjádřit po řádcích kódu; AI zde sloužila jako podpůrný nástroj pro vizuální část projektu.

---

## 5. Boilerplate a technicky rutinní části implementace
* **Nástroj:** ChatGPT
* **Datum:** průběžně během celého vývoje
* **Prompt / zadání:**
  Opakované dotazy na Java syntaxi, návrh menších pomocných metod, opakující se logiku, formátování dat, struktury enumů, dílčí utility a podobně.
* **Použití:**
  AI pomáhala zejména tam, kde šlo o rutinní, časově náročné, ale technicky méně kreativní části práce.
* **Úprava studentem:**
  Výstupy byly kontrolovány, přepisovány do kontextu projektu a upravovány tak, aby odpovídaly zbytku kódu a fungovaly v konkrétní architektuře.
* **Míra generování:** vysoká v jednotlivých izolovaných úsecích, nízká ve výsledném celku.

---

## 6. Mechaniky pohybu, tahů a combat flow
* **Nástroj:** ChatGPT jako konzultační a pomocný nástroj
* **Datum:** průběžně během implementace herních mechanik
* **Použití:**
  AI byla používána při rozpracování některých dílčích implementačních kroků, návrhů pomocných metod a při rozdělování větších problémů na menší implementační fáze.
* **Úprava studentem:**
  Celkový návrh mechanik, jejich pořadí, návaznosti, gameplay význam i finální funkční propojení byly vytvořeny a laděny ručně. AI zde nesloužila jako autor výsledné mechaniky, ale jako pomocník při implementaci jednotlivých částí.
* **Míra generování:** nízká až střední v jednotlivých blocích, nízká v konečném systému jako celku.

---

## Závěrečné zhodnocení

AI byla v tomto projektu použita především jako **akcelerátor rutinní práce**, nikoli jako náhrada samostatného návrhu nebo implementace celého řešení. Největší přínos měla u:
- opakujícího se UI kódu,
- boilerplate tříd,
- pomocných datových struktur,
- dílčích textů a prezentace,
- části assetů.

Naopak klíčové části práce — tedy **architektura, návrh hry, propojení mechanik, rozhodnutí o historickém rámci, mapách, jednotkách, zbraních, balancu a finální integraci systému** — byly vytvořeny ručně.

**Celkový kvalifikovaný odhad podílu AI na výsledném projektu je přibližně 20 %.**
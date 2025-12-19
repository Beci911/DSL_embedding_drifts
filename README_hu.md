````markdown
# Drift Detection DSL – Fordítóprogram Projekt Dokumentáció

## 1. Projekt Célja

A projekt célja egy saját tervezésű, domain-specifikus nyelv (DSL) megalkotása, amely Machine Learning modellek adatainak elcsúszásának megfigyelésére szolgál (Data Drift & Concept Drift). Az "embedding drift" olyan jelenséget jelent, amikor egy neurális háló kimeneti embedding reprezentációja az új adatok hatására eltolódik, jelezve a tanulási minta változását.

A projekt fő feladatai:

* Nyelv tervezése és nyelvtan definiálása.
* Lexikális és szintaktikai elemző (parser) implementálása.
* Szemantikai ellenőrzés és intelligens hibakezelés.
* Tesztesetek készítése és futtatása.
* A fejlesztési folyamat dokumentálása.

---

## 2. Nyelv Tervezése

### 2.1 Nyelvi Cél és Alkalmazás

A nyelv célja, hogy a felhasználó konfigurálhassa, mely adatforrásokon, milyen időközönként és milyen statisztikai módszerekkel kell vizsgálni a drifteket.

### 2.2 Kulcsszavak és Szimbólumok

* `monitor`, `source`, `baseline`, `drift_check`, `feature_drift`, `metadata`, `alert`
* Időegységek: `minutes`, `hours`, `daily`, `weekly`, `monthly`
* Metódusok: `wasserstein_distance`, `kl_divergence`, `psi`

### 2.3 Szintaktikai Szerkezetek

1. **Szekvencia (Sequence):** Kötelező sorrend a blokkban.

   ```plaintext
   monitor MyModel {
       source: prod_db
       baseline: train_db
       drift_check ...
   }
   ```

2. **Választás (Choice / Alternáció):** A blokkokon belül előre definiált értékek közül lehet választani.

   ```plaintext
   // A drift_check blokkon belül:
   method: wasserstein_distance | kl_divergence | psi
   ```

3. **Ismétlés (Repetition):** Több elem listája.
   ```plaintext
   alert: slack, email, pagerduty
   feature_drift on [age, income, zip_code]
   ```

4. **Opcionális Szerkezetek (Optional):** Nem kötelező blokkok.

   ```plaintext
   feature_drift on [feature1, feature2]
   metadata { ... }
   ```

5. **Aggregáció (Aggregation):** Összetett konfigurációk.

   ```plaintext
   MonitorConfig {
       DriftCheckConfig
       FeatureDriftConfig
       MetadataConfig
   }
   ```

### 2.4 Bemeneti Példa

```plaintext
monitor MyModel {
    source: prod_db
    baseline: train_db
    
    drift_check every 30_minutes {
        method: wasserstein_distance
        threshold: 0.15
        alert: slack, email
    }

    feature_drift on [age, income] {
        method: ks_test
        significance: 0.05
    }

    metadata {
        owner: "DataTeam"
        version: "1.0"
    }
}
```

### 2.5 Nyelvtani Szabályok és Grammar

A nyelvtan JavaCC segítségével készült.

#### Opciók

```java
options {
  STATIC = false;
  DEBUG_PARSER = false;
  JDK_VERSION = "1.8";
}
```

#### Parser Kezdő és Végpont

```java
PARSER_BEGIN(DriftConfigParser)
package parser;
import ast.*;
import java.util.ArrayList;
import java.util.List;

public class DriftConfigParser {}
PARSER_END(DriftConfigParser)
```

#### Tokenek

Fehér karakterek és kommentek:

```java
SKIP : { " " | "\t" | "\n" | "\r" | <"//" (~["\n","\r"])* ("\n"|"\r"|"\r\n")> | <"/*" (~["*"])* "*" ("*" | (~["*","/"] (~["*"])* "*"))* "/"> }
```

Kulcsszavak, időegységek, szimbólumok és literálok a parserben külön TOKEN-ekként definiáltak.

#### Nyelvtan szabályok

* `Root()`: Egy vagy több `MonitorBlock`
* `MonitorBlock()`: Kötelező mezők: `source`, `baseline`, `drift_check`; opcionális: `feature_drift`, `metadata`
* `DriftCheckBlock()`: Szekvencia, ismétlés és opcionális szerkezet
* `FeatureDriftBlock()`: Több elem ismétlése, opcionális mezők
* `MetadataBlock()`: Opcionális `description`, aggregáció

---

## 3. Elemző Generátor Választása

### JavaCC (Java Compiler Compiler)

A projekt elemzőjét a **JavaCC** segítségével generáltuk, amely a legelterjedtebb parser generátor Java környezethez.

* **Technológia:** **LL(k)** típusú, **Top-Down**  elemző.
* **Működési elv:**
    * **Top-Down:** A legfelső szabálytól (gyökér) indulva bontja le a bemenetet elemi tokenekre.
    * **Lookahead (k):** Képes *k* lépéssel előretekinteni, ami elengedhetetlen a kétértelmű nyelvtani helyzetek (pl. `30_minutes` vs. `variable_name`) feloldásához.
* **Kiemelt előnyök a projektben:**
    * **Kódinjektálás:** A Java kód közvetlenül a `.jj` nyelvtanfájlba ágyazható, így az **AST (Absztrakt Szintaxis Fa)** építése valós időben, az elemzéssel párhuzamosan történik, utólagos bejárás nélkül.
    * **Típusbiztonság:** A generált kód statikusan típusos Java, ami közvetlen integrációt tesz lehetővé a szemantikai **Validator** modullal.
    * **Megfelelőség:** Teljesíti a "Nem Python-alapú elemző" használatáért járó pluszpont követelményét.

## 4. Implementáció

### 4.1 Fő Modulok

1. Parser (szintaktikai elemzés JavaCC-vel)
2. Validator (szemantikai ellenőrzés)
3. Generator (Python kód generálása a konfigurációból)
4. ErrorHandler (intelligens hibajavítás)

### 4.2 Tesztelés

* 11 teszteset (5 pozitív, 6 negatív)
* Pozitív: helyes szintaxis, opcionális blokkok variálása, teljes funkcionalitás
* Negatív: szintaxis és szemantikai hibák

#### Minta Kimenet

Sikeres futás:

```plaintext
Processing: test1.drift
Validation Successful.
-> Generated Python Script: generated/test1.py
```

Hibakezelés:

```plaintext
Parse Error:
Line 4, Column 20
Encountered: "wassertein_distance"
Did you mean 'wasserstein_distance'?
```

### 4.3 Python Kód Generálás

A `PythonGenerator` osztály a monitor konfigurációból Python szkriptet generál, amely a drift és feature drift ellenőrzéseket futtatja.

---

## 5. Fejlesztési Kihívások és Megoldások

A fejlesztés során több technikai akadályt kellett leküzdeni a robusztus működés érdekében:

### 5.1. Token Ütközések és "Greedy Matching"
A lexikális elemzésnél (Tokenizálás) a JavaCC "mohó" (greedy) illesztést alkalmaz. Ez problémát okozott az alulvonást (`_`) tartalmazó tokeneknél.
* **Probléma:** Az általános azonosítók (pl. `my_variable`) és a specifikus időegység literálok (pl. `30_minutes`) szerkezete hasonló. A lexer hajlamos volt az időegységet is sima azonosítónak tekinteni.
* **Megoldás:** A `.jj` nyelvtanfájlban a token definíciók sorrendjének optimalizálásával és a reguláris kifejezések pontosításával (pl. számjegyekkel kezdődő tokenek prioritása) biztosítottuk a helyes felismerést.

### 5.2. Informatív Hibakezelés
Az alapértelmezett `ParseException` üzenetek gyakran nem nyújtanak elég segítséget a felhasználónak (pl. csak azt közlik, hogy "szintaktikai hiba").
* **Megoldás:** Implementáltunk egy saját `ErrorHandler` modult. A `try-catch` blokkban elkapott kivételből kinyerjük a hibás tokent, majd **Levenshtein-távolság** algoritmus segítségével megkeressük a hozzá leghasonlóbb valid kulcsszót. Így a rendszer nem csak jelzi a hibát, hanem javítási javaslatot is tesz (*"Did you mean...?").*

---

## 6. Adatstruktúra (AST Vizualizáció)

A parser a bemeneti szöveg feldolgozása során egy hierarchikus objektummodellt, úgynevezett Absztrakt Szintaxis Fát (AST) épít fel. Ez a struktúra képezi az alapját a későbbi validációnak és kódgenerálásnak.

Az alábbi ábra a `MonitorConfig` objektum felépítését szemlélteti:

```text
MonitorConfig (Gyökérelem)
 │
 ├── monitorName: String ("FraudModel")
 ├── source: String ("kafka_stream")
 ├── baseline: String ("training_v1")
 │
 ├── driftCheck: DriftCheckConfig (Kötelező)
 │    ├── method: String ("wasserstein_distance")
 │    ├── threshold: Double (0.15)
 │    ├── interval: Integer + Unit (2 hours)
 │    └── alerts: List<String> ["slack", "email"]
 │
 ├── featureDrift: FeatureDriftConfig (Opcionális)
 │    ├── features: List<String> ["age", "income"]
 │    ├── method: String ("ks_test")
 │    └── significance: Double (0.05)
 │
 └── metadata: MetadataConfig (Opcionális)
      ├── owner: String
      └── version: String
```

## 7. Futtatási Útmutató

### Előfeltételek

* Java JDK 8+
* JavaCC telepítve

### Fordítás

```bash
javacc -OUTPUT_DIRECTORY=src/parser grammar/DriftConfig.jj
mkdir bin
javac -d bin -sourcepath src src/main/*.java src/ast/*.java src/parser/*.java
```

### Futtatás

```bash
# Egy fájl elemzése és Python generálása, ha nem akarunk pzthon scriptet generálni -g flag elhagyása
java -cp bin main.Main test/positive/test1.drift -g
# Teljes tesztkészlet
java -cp bin main.Main --run-all-tests
```

````

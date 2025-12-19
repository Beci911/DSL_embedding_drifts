# Drift Detection DSL — Compiler Project Documentation

## 1. Project Goal

This project implements a small domain-specific language (DSL) designed to configure and run drift detection for machine learning models (data drift and concept drift). "Embedding drift" refers to shifts in a neural network's embedding outputs caused by changes in incoming data, indicating that the underlying data distribution or patterns have changed.

Primary project responsibilities:

- Design the DSL and define its grammar.
- Implement the lexical and syntactic analyzer (parser).
- Add semantic validation and helpful error handling.
- Create and run test cases.
- Document the development process.

---

## 2. Language Design

### 2.1 Purpose and Use

The DSL lets users configure which data sources to monitor, at what intervals, and with which statistical or distance-based methods to check for drift.

### 2.2 Keywords and Symbols

- `monitor`, `source`, `baseline`, `drift_check`, `feature_drift`, `metadata`, `alert`
- Time units: `minutes`, `hours`, `daily`, `weekly`, `monthly`
- Methods: `wasserstein_distance`, `kl_divergence`, `psi`

### 2.3 Syntactic Constructs

1. Sequence: required ordering inside a block.

```plaintext
monitor MyModel {
    source: prod_db
    baseline: train_db
    drift_check ...
}
```

2. Choice/alternation: select from predefined values inside a block.

```plaintext
// inside a drift_check block:
method: wasserstein_distance | kl_divergence | psi
```

3. Repetition: lists of items.
```plaintext
alert: slack, email, pagerduty
feature_drift on [age, income, zip_code]
```

4. Optional structures: non-mandatory blocks.

```plaintext
feature_drift on [feature1, feature2]
metadata { ... }
```

5. Aggregation: composed configuration objects.

```plaintext
MonitorConfig {
    DriftCheckConfig
    FeatureDriftConfig
    MetadataConfig
}
```

### 2.4 Example Input

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

### 2.5 Grammar and Parser

The grammar is implemented using JavaCC.

#### Options

```java
options {
  STATIC = false;
  DEBUG_PARSER = false;
  JDK_VERSION = "1.8";
}
```

#### Parser top-level

```java
PARSER_BEGIN(DriftConfigParser)
package parser;
import ast.*;
import java.util.ArrayList;
import java.util.List;

public class DriftConfigParser {}
PARSER_END(DriftConfigParser)
```

#### Tokens

Whitespace and comments:

```java
SKIP : { " " | "\t" | "\n" | "\r" | <"//" (~["\n","\r"])* ("\n"|"\r"|"\r\n")> | <"/*" (~["*"])* "*" ("*" | (~["*","/"] (~["*"])* "*"))* "/"> }
```

Keywords, time units, symbols and literals are defined as separate TOKENs in the parser.

#### Grammar rules

- `Root()`: one or more `MonitorBlock`
- `MonitorBlock()`: required fields `source`, `baseline`, `drift_check`; optional `feature_drift`, `metadata`
- `DriftCheckBlock()`: sequence with repetition and optional pieces
- `FeatureDriftBlock()`: repeated elements, optional values
- `MetadataBlock()`: optional `description`, aggregation

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
* **Megoldás:** Implementáltunk egy saját `ErrorHandler` modult. A `try-catch` blokkban elkapott kivételből kinyerjük a hibás tokent, majd **Levenshtein-távolság** algoritmus segítségével megkeressük a hozzá leghasonlóbb valid kulcsszót. Így a rendszer nem csak jelzi a hibát, hanem javítási javaslatot is tesz (*"Did you mean...?"*).

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
# Egy fájl elemzése és Python generálása
java -cp bin main.Main test/positive/test1.drift -g
# Teljes tesztkészlet
java -cp bin main.Main --run-all-tests
```

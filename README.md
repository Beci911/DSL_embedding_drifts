# Drift Detection DSL — Compiler Project Documentation

## 1. Project Goal

This project implements a small domain-specific language (DSL) to configure and run drift detection for machine learning systems (data drift and concept drift). "Embedding drift" describes shifts in neural-network embeddings caused by changes in incoming data, which can signal changes in data distribution or behavior.

Key objectives:

- Define the DSL syntax and grammar.
- Implement a lexer/parser and produce an AST.
- Add semantic validation and helpful error messages.
- Provide test cases and automation.
- Document development and usage.

---

## 2. Language Design

### Purpose

The DSL allows users to declare monitors that compare a live data source against a baseline using specified drift detection methods at a configured interval.

### Keywords and tokens


### Constructs


Example:

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

### Grammar and parser

The grammar is implemented with JavaCC. Tokens include keywords, identifiers, numeric literals, string literals and a few special symbols. Whitespace and comments are skipped via `SKIP` rules.

Key grammar rules:



## 3. Elemző Generátor Választása

### JavaCC (Java Compiler Compiler)

* **Technológia:** **LL(k)** típusú, **Top-Down**  elemző.
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
* Negatív: szintaxis és szemantikai hibák

#### Minta Kimenet

Sikeres futás:

Processing: test1.drift
Validation Successful.
-> Generated Python Script: generated/test1.py
```


```plaintext
Parse Error:
Line 4, Column 20
Encountered: "wassertein_distance"

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

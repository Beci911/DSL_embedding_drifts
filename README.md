# Drift Detection DSL — Compiler Project Documentation

## 1. Project Goal

This project implements a small domain-specific language (DSL) to configure and run drift detection for machine learning systems (data drift and concept drift). "Embedding drift" refers to shifts in neural network embeddings caused by changes in incoming data, which can indicate changes in data distribution or system behavior.

Key objectives:

* Define the DSL syntax and grammar.
* Implement a lexer/parser and produce an AST.
* Add semantic validation and helpful error messages.
* Provide test cases and automation.
* Document development and usage.

---

## 2. Language Design

### Purpose

The DSL allows users to declare monitors that compare a live data source against a baseline using specified drift detection methods at a configured interval.

### Keywords and Tokens

*(To be defined in detail: keywords, identifiers, literals, symbols, etc.)*

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

### Grammar and Parser

The grammar is implemented using JavaCC. Tokens include keywords, identifiers, numeric literals, string literals, and a few special symbols. Whitespace and comments are skipped via `SKIP` rules.

Key grammar rules:

*(Details to be added here.)*

---

## 3. Parser Generator Choice

### JavaCC (Java Compiler Compiler)

* **Technology:** **LL(k)** type, **Top-Down** parser.

  * **Top-Down:** Starts from the top-level rule (root) and breaks down input into elemental tokens.
  * **Lookahead (k):** Can look *k* steps ahead, essential for resolving ambiguous grammar situations (e.g., `30_minutes` vs. `variable_name`).
* **Key advantages for this project:**

  * **Code Injection:** Java code can be embedded directly into the `.jj` grammar file, allowing AST (Abstract Syntax Tree) construction in real time during parsing, without post-traversal.
  * **Type Safety:** Generated code is statically typed Java, enabling direct integration with the semantic **Validator** module.
  * **Compliance:** Meets the requirement of using a non-Python-based parser.

---

## 4. Implementation

### 4.1 Main Modules

1. **Parser** (syntactic analysis using JavaCC)
2. **Validator** (semantic validation)
3. **Generator** (Python code generation from the configuration)
4. **ErrorHandler** (intelligent error correction)

### 4.2 Testing

* 11 test cases (5 positive, 6 negative)
* Negative: syntax and semantic errors

#### Sample Output

Successful run:

```plaintext
Processing: test1.drift
Validation Successful.
-> Generated Python Script: generated/test1.py
```

Parse error example:

```plaintext
Parse Error:
Line 4, Column 20
Encountered: "wassertein_distance"
```

### 4.3 Python Code Generation

The `PythonGenerator` class generates a Python script from a monitor configuration, which executes the drift and feature drift checks.

---

## 5. Development Challenges and Solutions

Several technical challenges were addressed to ensure robust functionality:

### 5.1 Token Conflicts and Greedy Matching

In lexical analysis (tokenization), JavaCC applies "greedy" matching. This caused issues with tokens containing underscores (`_`).

* **Problem:** General identifiers (e.g., `my_variable`) and specific time-unit literals (e.g., `30_minutes`) have similar structures. The lexer could mistakenly treat time units as simple identifiers.
* **Solution:** Optimized the order of token definitions in the `.jj` grammar file and refined regular expressions (e.g., giving precedence to tokens starting with digits) to ensure correct recognition.

### 5.2 Informative Error Handling

Default `ParseException` messages often provide insufficient user guidance (e.g., just "syntax error").

* **Solution:** Implemented a custom `ErrorHandler` module. From the caught exception in a `try-catch` block, the invalid token is extracted, and the **Levenshtein distance** algorithm is used to find the closest valid keyword. This allows the system to not only indicate the error but also suggest a correction (*"Did you mean...?"*).

---

## 6. Data Structure (AST Visualization)

During parsing, a hierarchical object model called the Abstract Syntax Tree (AST) is constructed. This structure underpins validation and code generation.

The following diagram illustrates the structure of a `MonitorConfig` object:

```plaintext
MonitorConfig (Root)
 │
 ├── monitorName: String ("FraudModel")
 ├── source: String ("kafka_stream")
 ├── baseline: String ("training_v1")
 │
 ├── driftCheck: DriftCheckConfig (Required)
 │    ├── method: String ("wasserstein_distance")
 │    ├── threshold: Double (0.15)
 │    ├── interval: Integer + Unit (2 hours)
 │    └── alerts: List<String> ["slack", "email"]
 │
 ├── featureDrift: FeatureDriftConfig (Optional)
 │    ├── features: List<String> ["age", "income"]
 │    ├── method: String ("ks_test")
 │    └── significance: Double (0.05)
 │
 └── metadata: MetadataConfig (Optional)
      ├── owner: String
      └── version: String
```

---

## 7. Execution Guide

### Prerequisites

* Java JDK 8+
* JavaCC installed

### Compilation

```bash
javacc -OUTPUT_DIRECTORY=src/parser grammar/DriftConfig.jj
mkdir bin
javac -d bin -sourcepath src src/main/*.java src/ast/*.java src/parser/*.java
```

### Execution

```bash
# Parse a file and generate Python
java -cp bin main.Main test/positive/test1.drift -g

# Run full test suite
java -cp bin main.Main --run-all-tests
```

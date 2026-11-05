# Java Version Examples

One self-contained Maven project per Java version, from Java 8 to Java 25.

Each module declares its own `maven.compiler.release`, so its sources are compiled
against exactly that version's API and language level. A module only contains
examples for features that are final in its release; preview features are not used,
because `--enable-preview` only works with the current JDK release.

## Layout

```
java_featues/                    single project (this directory)
  pom.xml                        parent aggregator
  javaN/pom.xml                  submodule per Java version
  javaN/src/main/java/           package root com.example.javaN.*
  javaN/src/test/java/           JUnit 5 smoke tests
```

| Module | Release | Examples |
|--------|---------|----------|
| `java8` | 8 | lambdas, functional interfaces, default methods, Base64 |
| `java9` | 9 | module-info, Optional/Stream enhancements, private interface methods, Process API |
| `java10` | 10 | `var` local variables, Optional comparison |
| `java11` | 11 | String methods, `var` in lambda parameters, `Optional.isEmpty()`, `Predicate.not()`, `Collection.toArray(IntFunction)` |
| `java12` | 12 | `Collectors.teeing()`, `String.indent()`/`transform()`, `Files.mismatch()`, compact numbers, collection/optional/stream comparison |
| `java13` | 13 | collection/optional/stream comparison |
| `java14` | 14 | switch expressions, collection/optional/stream comparison |
| `java15` | 15 | text blocks, collection/optional/stream comparison |
| `java16`–`java20` | 16–20 | `instanceof` pattern matching, collection/optional/stream comparison |
| `java21` | 21 | sequenced collections, record patterns, pattern matching for `switch`, virtual threads — each compared with Java 20 |
| `java22` | 22 | unnamed variables and patterns, Foreign Function & Memory API, `Console.isTerminal()`, multi-file source launcher — each compared with Java 21 |
| `java23` | 23 | Markdown documentation comments (JEP 467) — compared with Java 22 |
| `java24` | 24 | Stream Gatherers (JEP 485), Class-File API (JEP 484) — each compared with Java 23 |
| `java25` | 25 | Flexible Constructor Bodies (JEP 513), Scoped Values (JEP 506), Module Import Declarations (JEP 511), Compact Source Files & Instance Main Methods (JEP 512) — each compared with Java 24 |

## Requirements

- JDK 22 or newer (any JDK that supports `--release 8` through `--release 22`; tested with JDK 26)
- Maven 3.9+

Modern JDKs print an "obsolete release" warning for `--release 8`; the build still succeeds.

## Build

Run from the `java_featues/` directory (the aggregator project root):

```bash
cd java_featues
mvn clean compile        # compile all submodules
mvn test                 # compile and run all smoke tests
mvn clean package        # build a jar per submodule under javaN/target/
```

A single version submodule can be built on its own:

```bash
cd java_featues
mvn -pl java17 clean test
```

## Run an example

Compile the submodule, then run its class with that submodule's classes on the classpath:

```bash
cd java_featues
mvn -pl java11 compile
java -cp java11/target/classes com.example.java11.localvarlambda.LambdaVarExample
```

Java 9 examples can also be run as a real module:

```bash
cd java_featues
java --module-path java9/target/classes -m com.example.java9/com.example.java9.moduleinfo.ModuleInfo
```

## Tests

Every module has `src/test/java/com/example/javaN/ExamplesTest.java` with one test
per example. Each test invokes the example's `main`, asserts that it does not throw,
and asserts that it printed something. Each `java21` example is organized around one feature: it shows the Java 20 way
and the Java 21 way side by side, and its javadoc explains the difference.

Each `java22` example is organized around one feature: it shows the Java 21 way
and the Java 22 way side by side, and its javadoc explains the difference.

Each `java23` example is organized around one feature: it shows the Java 22 way
and the Java 23 way side by side, and its javadoc explains the difference.

Each `java24` example is organized around one feature: it shows the Java 23 way
and the Java 24 way side by side, and its javadoc explains the difference.

Each `java25` example is organized around one feature: it shows the Java 24 way
and the Java 25 way side by side, and its javadoc explains the difference.

Because the tests capture standard output, they verify execution rather than print
the example output. Run an example directly (see above) to see its output.

## Adding an example

1. Create a package directory under `java_featues/javaN/src/main/java/com/example/javaN/` (from repo root), i.e. `javaN/src/main/java/com/example/javaN/` from the `java_featues/` project root.
2. Declare the matching package in the source file.
3. Use only APIs and syntax available in release `N`.
4. Add a test method in that submodule's `ExamplesTest` that calls the new `main`.

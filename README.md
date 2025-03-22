# How to run CodeSummarisation

(Ensure you have atleast [Java 8](https://www.oracle.com/java/technologies/downloads/))

### Windows

```
mvnw clean dependency:copy-dependencies install
java -cp target\dependency\*;target\code-summarisation-1.0.0.jar Application
```

### Linux

```
./mvnw clean dependency:copy-dependencies install
java -cp target/dependency/\*:target/code-summarisation-1.0.0.jar Application
```

## Note

CodeSummarisation only needs an input folder to run. "output" and "summary" folders are generated.

## Known Issues

1. JavaSymbolSolver seems to have a bug related to imports, due to which some inputs may not be parsed correctly.
2. A pattern will not be recognised if it doesn't adhere to specifications outlined in Shvets, 2018.

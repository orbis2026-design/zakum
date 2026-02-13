# Zakum (v1 scaffold)

A clean v1 scaffold for your network-core library plugin (`Zakum`) plus an example dependent plugin (`ZakumBattlePass`).

## Build
Requires **Java 21**.

### Option A: If you have Gradle installed
Run:
- `gradle build`

### Option B: Generate the wrapper once, then use it
Because this environment can't reliably ship the wrapper JAR, generate it once locally:
- `gradle wrapper --gradle-version 9.3.1`
Then build with:
- Linux/macOS: `./gradlew build`
- Windows: `gradlew.bat build`

Output jars:
- `zakum-core/build/libs/Zakum-<version>.jar`
- `zakum-battlepass/build/libs/ZakumBattlePass-<version>.jar`

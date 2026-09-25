# Guzhenren

[简体中文](README.md) | English

A single-player, hardcore, survival-oriented xianxia RPG mod.

> In development ahead of the 1.0.0 release. Gameplay is not final, so this file lists no content.

This is a translation of [README.md](README.md); where the two differ, the Chinese version prevails.

## Requirements

|                  |                                       |
|------------------|---------------------------------------|
| Minecraft        | `1.21.1`                              |
| NeoForge         | `21.1.x`                              |
| Java             | `21`                                  |
| mod id / package | `guzhenren` · `com.unknown.guzhenren` |
| Required         | Epic Fight · GeckoLib                 |
| Optional         | JEI · Curios                          |

Exact versions live in `gradle.properties` and `build.gradle`.

## Build and run

`build.gradle` references the Epic Fight and GeckoLib jars in `run/mods/` by file name. That directory is not tracked, so put the matching jars there before the first build (CI downloads them from Modrinth).

```text
gradlew.bat build              # compile, jar, run L1/L2 tests
gradlew.bat runClient          # dev client
gradlew.bat runData            # regenerate data
gradlew.bat runGameTestServer  # run L3 GameTests
```

Use `./gradlew` on other systems. `runData` writes `src/generated/resources`, which is a source set: regenerate and commit it after any provider change.

## Tests

| Layer | Location                                       | Scope                        |
|-------|------------------------------------------------|------------------------------|
| L1    | `src/pureTest/java`                            | Plain JVM, no modded runtime |
| L2    | `src/test/java`                                | Registries loaded, no world  |
| L3    | `src/main/java/com/unknown/guzhenren/gametest` | Live world and ticks         |

With `-PwikiDir=<wiki root>`, L2 also checks the source against the player wiki; without it the check is skipped.

## License

All rights reserved. `LICENSE.txt` is the MIT license inherited from the NeoForge MDK template and **does not cover the mod's code**.

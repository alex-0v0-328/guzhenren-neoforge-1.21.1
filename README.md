# 蛊真人
<sub>Guzhenren — a xianxia RPG mod for NeoForge 1.21.1</sub>

单人、硬核、生存向的仙侠 RPG 模组。
<sub>A single-player, hardcore, survival-oriented xianxia RPG mod.</sub>

> 1.0.0 正式版发布前处于开发阶段，玩法内容尚未定型，本文不列具体内容。
> <sub>In development ahead of the 1.0.0 release. Gameplay is not final, so this file lists no content.</sub>

## 环境与依赖 <sub>Requirements</sub>

|                  |                                       |
|------------------|---------------------------------------|
| Minecraft        | `1.21.1`                              |
| NeoForge         | `21.1.x`                              |
| Java             | `21`                                  |
| mod id / package | `guzhenren` · `com.unknown.guzhenren` |
| 必需 / Required  | Epic Fight · GeckoLib                 |
| 可选 / Optional  | JEI · Curios                          |

精确版本以 `gradle.properties` 与 `build.gradle` 为准。
<sub>Exact versions live in `gradle.properties` and `build.gradle`.</sub>

## 构建与运行 <sub>Build and run</sub>

`build.gradle` 按文件名引用 `run/mods/` 下的 Epic Fight 与 GeckoLib jar。该目录不入库，首次构建前需放入同名 jar（CI 从 Modrinth 下载）。
<sub>`build.gradle` references the Epic Fight and GeckoLib jars in `run/mods/` by file name. That directory is not tracked, so put the matching jars there before the first build (CI downloads them from Modrinth).</sub>

```text
gradlew.bat build              # 编译、打包、运行 L1/L2 测试 / compile, jar, run L1/L2 tests
gradlew.bat runClient          # 开发客户端 / dev client
gradlew.bat runData            # 重新生成数据 / regenerate data
gradlew.bat runGameTestServer  # 运行 L3 GameTest / run L3 GameTests
```

其他系统用 `./gradlew`。`runData` 的产物 `src/generated/resources` 属于源码集，provider 改动后需重新生成并提交。
<sub>Use `./gradlew` elsewhere. `runData` writes `src/generated/resources`, which is a source set: regenerate and commit it after any provider change.</sub>

## 测试 <sub>Tests</sub>

| 层 / Layer | 位置 / Location                                | 范围 / Scope                                            |
|------------|------------------------------------------------|---------------------------------------------------------|
| L1         | `src/pureTest/java`                            | 纯 JVM，不启动模组运行时 / plain JVM, no modded runtime |
| L2         | `src/test/java`                                | 加载注册表，无世界 / registries loaded, no world        |
| L3         | `src/main/java/com/unknown/guzhenren/gametest` | 真实世界与 tick / live world and ticks                  |

传入 `-PwikiDir=<wiki 根目录>` 时，L2 额外核对源码与玩家向 wiki 的一致性；不传则跳过。
<sub>With `-PwikiDir=<wiki root>`, L2 also checks the source against the player wiki; without it the check is skipped.</sub>

## 许可 <sub>License</sub>

模组本体版权所有，保留所有权利。`LICENSE.txt` 是继承自 NeoForge MDK 模板的 MIT 协议，**不覆盖模组代码**。
<sub>All rights reserved. `LICENSE.txt` is the MIT license inherited from the NeoForge MDK template and **does not cover the mod's code**.</sub>

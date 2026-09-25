# 蛊真人

简体中文 | [English](README.en.md)

单人、硬核、生存向的仙侠 RPG 模组。

> 1.0.0 正式版发布前处于开发阶段，玩法内容尚未定型，本文不列具体内容。

## 环境与依赖

|               |                                       |
|---------------|---------------------------------------|
| Minecraft     | `1.21.1`                              |
| NeoForge      | `21.1.x`                              |
| Java          | `21`                                  |
| mod id / 包名 | `guzhenren` · `com.unknown.guzhenren` |
| 必需依赖      | Epic Fight · GeckoLib                 |
| 可选依赖      | JEI · Curios                          |

精确版本以 `gradle.properties` 与 `build.gradle` 为准。

## 构建与运行

`build.gradle` 按文件名引用 `run/mods/` 下的 Epic Fight 与 GeckoLib jar。该目录不入库，首次构建前需放入同名 jar（CI 从 Modrinth 下载）。

```text
gradlew.bat build              # 编译、打包、运行 L1/L2 测试
gradlew.bat runClient          # 开发客户端
gradlew.bat runData            # 重新生成数据
gradlew.bat runGameTestServer  # 运行 L3 GameTest
```

其他系统用 `./gradlew`。`runData` 的产物 `src/generated/resources` 属于源码集，provider 改动后需重新生成并提交。

## 测试

| 层  | 位置                                           | 范围                     |
|-----|------------------------------------------------|--------------------------|
| L1  | `src/pureTest/java`                            | 纯 JVM，不启动模组运行时 |
| L2  | `src/test/java`                                | 加载注册表，无世界       |
| L3  | `src/main/java/com/unknown/guzhenren/gametest` | 真实世界与 tick          |

传入 `-PwikiDir=<wiki 根目录>` 时，L2 额外核对源码与玩家向 wiki 的一致性；不传则跳过。

## 许可

模组本体版权所有，保留所有权利。`LICENSE.txt` 是继承自 NeoForge MDK 模板的 MIT 协议，**不覆盖模组代码**。

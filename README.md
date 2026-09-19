# 蛊真人
<sub>Guzhenren — current technical reference for a NeoForge 1.21.1 xianxia RPG mod. Java 21; Epic Fight & GeckoLib required; L1/L2/L3 tests; mod code all rights reserved.</sub>

围绕**空窍、肉身、魂魄、流派、脑海**五个代码域展开的仙侠 RPG 模组。玩家阅读入口仍按空窍、肉身、脑海组织；代码把魂魄独立成域，把气道与力道放在 `path/` 子域。本文只写已由源码核对的技术规格。

## 工具链

|                  |                                       |
|------------------|---------------------------------------|
| Minecraft        | `1.21.1`                              |
| NeoForge         | `21.1.238`                            |
| Parchment        | `2024.11.17`                          |
| Java             | `21`                                  |
| mod id / package | `guzhenren` · `com.unknown.guzhenren` |
| 必需依赖         | `Epic Fight` · `GeckoLib`             |
| 可选依赖         | `JEI` · `Curios`                      |

Windows 下从项目目录运行：

```text
gradlew.bat build
gradlew.bat runGameTestServer
gradlew.bat runData
```

如需做源码与玩家向 wiki 的一致性检查，显式传入 wiki 根目录：

```text
gradlew.bat build -PwikiDir="C:\path\to\project-wiki"
```

未指定 `-PwikiDir` 时，一致性检查跳过，不会自行猜目录或修改 wiki。`runData` 使用隔离的 `run-data/`；GameTest 使用隔离的 `run-gametest/`。`src/generated/resources` 属于源码集，provider 改动后必须重新生成并提交产物。

入口是 `Guzhenren.java`（通用）与 `GuzhenrenClient.java`（客户端）。`runClient` 的视觉、手感和整包兼容仍由 Alex 手工验收。

## 五域数据层

玩家持久状态主要是 NeoForge data attachment：九个不可变 record attachment 由各域 service 写入；另有 `BORN`（序列化、不同步）与 `ESSENCE_CARRY`（不序列化、不同步）两个特殊字段。

| 域              | attachment                                               | 内容                           |
|-----------------|----------------------------------------------------------|--------------------------------|
| `aperture` 空窍 | `ApertureData`、`ApertureNourishData`、`ApertureStorage` | 空窍、温养会话、蛊虫仓与本命格 |
| `body` 肉身     | `BodyData`                                               | 体质、种族、年龄、寿元与形态   |
| `soul` 魂魄     | `SoulData`                                               | 当前魂魄与上限                 |
| `path` 流派     | `PathData`、`PathQiData`、`PathStrengthData`             | 造诣道痕、八种气、力道数据     |
| `mind` 脑海     | `MindData`                                               | 才情与念/意/情三池             |

除 `ApertureStorage` 外，玩家状态 attachment 以 `OWNER_ONLY` 同步给持有者，并由各自 `CODEC` 持久化。`ApertureStorage` 序列化但不同步，客户端通过容器槽位、原版点击通道和 `ContainerData` 读取视图。`ESSENCE_CARRY` 是真元回复余数，原地更新且不序列化；`BORN` 只保存出生初始化闩。玩家数据不写进自定义 payload。

## 入口与服务边界

- 空窍、肉身、魂魄、流派和脑海的写入分别经过对应 service；调用点不自行复制公式。
- `PlayerDataService` 只负责跨域生命周期：登录、出生、睡眠、clone、respawn 与 reset；登录还会迁移旧体质、同步天赋道痕并刷新生命、攻击和 Epic Fight 派生属性。
- 每秒心跳由 `PlayerTickEvents` 按固定顺序驱动老化、存储喂养、气效果、寿元、真元、温养、脑海、压力和致死检查。
- 六个自定义 payload 全部只上行传客户端意图：开存储、选辅修、开炼蛊、温养 `START/CANCEL`、冲击窍壁（无字段）和 Dash（`vertical`/`horizontal`/`yRot`）。下行玩家数据只走 attachment；容器使用原版槽位、按钮和 `ContainerData`。

## 内容系统

物品分为一次性蛊与需照顾蛊；`TendedGuItem` 的 `RefinedGuState` 是栈组件，野生栈没有该组件，炼化后才进入温养、喂养和使用流程。炼蛊使用 `GuRecipe` 数据配方与 `RefinementMenu`，输入、时钟和输出是临时容器状态，当前只有两张测试蛊方。自然生成的希望蛊、三种豕蛊和四种横冲系甲虫由群系数据与实体 AI 接入，右键捕捉得到未炼化物品。

Epic Fight 保存耐力与普通消耗，GZR 通过 `EpicFightIntegration` 提供派生上限、技能与 Dash 桥接，Dash 与重拳命中还有白色激波环尾迹作视觉反馈（沿路径原地绽放、纯视觉无伤害；重拳需面板攻击 ≥16，dash 环对本人第一人称隐藏）；GeckoLib 负责豕蛊、横冲系甲虫与野猪的模型和动画。甲虫共用一套几何与五段动画，横冲为浅色、直撞为原色、四/五转横冲直撞共用深色；物品图标保持独立。野猪为独立地面中立生物，在温带森林自然生成，仅受击个体反击，服务端结算冲撞与顶飞，掉落原版猪肉；支持 `/summon guzhenren:wild_boar`，不提供刷怪蛋、繁殖或驯服。JEI 目前只有 optional 元数据，没有 Java 插件或构建依赖；Curios 为 optional，使用 API `compileOnly`、完整 jar `localRuntime`，其数据 provider 在 `runData` 生成槽位文件。FTB Quests 不进入核心模组，属于整合包层边界。

## 测试

`src/pureTest/java` 是不启动 modded runtime 的 L1 纯 JVM 测试；`src/test/java` 是可加载注册表但没有 world 的 L2；`src/main/java/.../gametest` 是需要真实 world/tick 的 L3 GameTest。三层都使用 JUnit/GameTest 的真实约束，不 mock Minecraft。构建或 GameTest 通过不等于客户端视觉、手感或整包兼容已验收。

## 许可

模组本体版权所有，保留所有权利。`LICENSE.txt` 是继承自 NeoForge MDK 模板的 MIT 协议，**不覆盖模组代码**。

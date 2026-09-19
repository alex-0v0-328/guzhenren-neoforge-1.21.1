# GZR 领域参考

本文件按任务涉及的领域读取，不作为每轮自动加载的规则入口。工作权限、工作流与当前状态见 [AGENTS.md](AGENTS.md)。§2、§3、§5..§11、§13、§15 保留原编号以兼容既有指向；§4 已并入根 AGENTS 的提交工作流，§12 已删除（2026-09-14）。"本文件"中的领域陷阱指本参考，规则和当前状态的唯一入口是根 AGENTS。

## 2. 语言、注释、风格（铁律）

- 聊天**中文**；代码 / 注释 / 提交信息 **英文，美式拼写**（如 `handwritten`、`full bright`）。**本文件正文中文**。
- 注释按需解释不明显的原因、契约与限制；允许必要的英文行注释和 javadoc，不设行数上限，不重复代码。设计理由归 wiki，领域陷阱归本参考，TODO 归代码或玩家《TODO总表》。
- ⚠ **`//region` 标签的内容是承诺**：描述必须与代码一致。改机制先改标签，标签和代码都不许说谎。
- javadoc 跨包引用使用 **FQN**，避免仅为文档引用在数据层 import 服务层。服务层（`attachment/service/` 全部 + `PlayerDataService`）公开方法的引用类型参数与返回值标 `@NotNull`（org.jetbrains），`@Nullable` 语义处保持 `@Nullable`，原始类型与私有方法不标。嵌套 `Math.max/min` 夹逼用 `Math.clamp`（client UI 既有 `Mth.clamp` 不动）。
- 不对全项目运行 IDEA「清理代码」或 Reformat；仅整理本次触及代码，避免无关排版与行尾差异。
- 双语夹注按读者需要使用，不强制添加；使用时采用 `English [中文]`。`[无]` = `display.none`，`[%s]` = `command.info.detail`。
- 术语一律查 `en_us.json` 或 wiki《原著词汇 与命名》，**禁止自造中英**。陷阱对：
  **`SPACE` 宇道 / `TIME` 宙道**（不是空间道/时间道——猜错命名一条真实存在的道）；
  **炼化 `refine` ≠ 炼蛊 `refinement`**（认主 vs 无中生有，且 `GuPath.REFINEMENT` [炼道] 已占词，绝不写裸 `Refinement` 类）；
  **痈（鼻失嗅）是正确的字，不修**；基础力道 = `StrengthPathBranch.NORMAL`，是其余力道蛊的默认归类；
  `Race.HUMAN` [人族] ≠ `GuPath.HUMAN` [人道]；`Rank.maxHealth` 是玩家 HP，`HEALTH_PER_RANK` 才是蛊虫 HP（同词两主语）。
- 排版以 `.editorconfig` 与邻近代码为准，不手工对齐参数列或数值，不批量重排既有格式。无通配符 import；import 纯 ASCII 排序（`ModContainer` 在 `common.Mod` 前）。
- Nerd Font 只用于 `//region` 标签与 TODO 锚点（`` `U+F0AD` ``，数码点防 GBK 丢失）；`⚠`/`☠` 保持纯 Unicode（`⚠` = 规则，`☠` = 陷阱已真实发生过，`⚠ KEPT` = 看着像缺陷其实是设计）。一注释一语言。
- ☠ Alex 的简写：**斜杠是「两档」，不是分数**——「2/3 倍」= 两倍/三倍两档。看到 `A/B` 先问是不是两档。**「一次性消耗」= `ConsumedGuItem`（需照顾且用完消失），不是 `OneShotGuItem`**。
- 文档区间用 `..`，约等用 `≈`，避免裸 `~` 被 GFM 渲染为删除线。Markdown 表格以渲染可读为准，不手工计算 CJK 列宽。
- Obsidian 笔记已有文件名或属性标题时，正文首行不重复同名 H1；只有没有标题来源时才写 H1。

## 3. 环境与工具

构建与运行都走 Windows JDK（`gradlew.bat`），在对应 mod 目录下：
- **Windows Git Bash**：`MSYS_NO_PATHCONV=1 cmd.exe /c "gradlew.bat build"`（防 `/c` 被 MSYS 路径转换）；git 即 Windows git，push 可直接 `git push origin main`。
- **WSL**：无 JDK，走 interop `cmd.exe /c 'gradlew.bat build'`；原生 git push 无凭据，必须 `cmd.exe /c "git push origin main"`。
- ⚠ **行尾视角**：Windows 端 `core.autocrlf=true`、WSL git 未设——**WSL 下 `git status` 会把全树报成 modified（纯 CRLF 假象）**；WSL 里判断真实改动用 `git diff --ignore-cr-at-eol`，提交走 Windows Git Bash。`ModItems.java` 索引端是混合行尾（历史混入 CRLF）——新增行必须 LF，否则 `git diff --check` 报行尾空白。
- `runData` 重生成 lang + json 到 `src/generated`（provider 改动后必跑），实际数据运行配置由 `build.gradle` 的 `data` run 指定 `gameDirectory=run-data`；不要按旧经验移走 `run/mods` 整包。Epic Fight 使用 compileOnly + localRuntime + testRuntimeOnly，GeckoLib 使用 compileOnly + localRuntime + testImplementation，`runData`/GameTest 的 classpath 必须可用；其他 jar 只有在出现当前可复现的构造期错误时才按证据处理。
- 本地 `run/config/fml.toml` 设 `versionCheck = false`：NeoForge 更新检查访问 `mc-update-check.anthonyhilyard.com` 时，Iceberg/Advancement Plaques 经常超时；只关自动更新提示，不影响游戏，需恢复时改回 `true`。
- ⚠ Gradle 不转发 stdin：`runServer` 无法注入命令。运行期行为验证只能靠 `runClient`（Alex 跑）或 GameTest。
- ⚠ `Level` 是 `AutoCloseable`：IDE 误报"未用 try-with-resources"。vanilla 签名改不了的方法挂 `@SuppressWarnings("resource")`；局部变量一律内联消除（`player.level().xxx()`）。
- ⚠ Alex 的 IDEA 常在会话中实时改文件（拼写/语法修正）——Edit 报「文件已被修改」先重读再套用；审 diff 先甄别改动来源，他的措辞类改动按「保持现状」处理。
- ⚠ `awk` 数的是字节，CJK 行宽统计假阳性；中文内容写完按码点验证。
- 1.21.1 原版行为查证：反编译源在 `~/.gradle/caches/neoformruntime/intermediate_results/`——`decompile_*_output.jar`（原版）与 `sourcesAndCompiledWithNeoForge_*_output.jar`（**NeoForge 补丁后的，才是真实运行时行为**）。

**测试集三层**（规格与接入决策树在 wiki《工程/测试集.md》）：
- L1 `pureTest`（`src/pureTest/java`，包镜像 main）：纯 JVM 零引导，`gradlew.bat pureTest` 秒级内环；准入 = 不碰注册对象（`Items.X`/`ModItems.X`/`ItemStack`/`MobEffect` 实例），越线当场崩、退回 L2。
- L2 `test`（`src/test/java`）：modded JVM，注册表可用、无 world；碰注册对象的测试住这里。
- L3 GameTest（`main` 源集 `gametest/` 包，`@GameTestHolder`）：`gradlew.bat runGameTestServer` 自动跑全部用例、退出码=失败数；能验证自动运行逻辑（world/tick/服务端状态），独立 gameDirectory `run-gametest/` 隔离 `run/mods` 整包（imblocker 在专用服务器崩，EF/Curios 走 classpath 照常）；场景模板 `empty9x9x9.nbt`（全空气，生成器 `tools/gen_template.py`）；CI 有同名步骤。客户端视觉和手感仍归 Alex 的 `runClient` 实测。
- seam 范式照 `TendedGuItemTest`（package-private static、宙道时长过 `waited`、测试同包）。1.21.1 用 `assertBlockState`（无 `assertBlock(Block,Pos)`）；装 EF 时 `makeMockServerPlayerInLevel` 会被 `checkPacket` 拒——手工 mock ServerPlayer 范式见 `ModGameTests`。
  ⚠ GameTest 实体物理直驱：`setNoAi(true)` 连 travel/move 一起跳过，`setDeltaMovement` 无效；确定性位移用 `gu.move(MoverType.SELF, vec)`（碰撞与 `checkFallDamage` 都在里面）。同批测试的 mock 玩家会泄漏到邻居结构（6 格内可触发 Flee 抢占），goal 时序断言先想抖动或干脆绕开 goal。
  ⚠ GameTest 夹具陷阱（各有实发）：mock `ServerPlayer` 带 60 tick 出生无敌——战斗断言前先推进夹具刻；`assertValueEqual` 走泛型 equals——float/double 不匹配会静默假败；查世界坐标先过 `helper.absolutePos()`；流体只向最短下坡蔓延（盆地测试自己围墙）；`succeedWhen` 多实体随机窗口用闩式 `boolean[]` 断言；GeckoLib 控制器时钟测试只能住 L2（GameTest 专用服务器碰 `ClientLevel` 会崩）。GameTest 失败先查夹具（断言数值类型、mock 玩家摆放），再动生产代码。

## 5. 数据模型速查（guzhenren）

五个数据包（aperture/body/soul/path/mind）、**九个 record attachment**，**不可变**；写入只能过服务。data/ 与 service/ 同构五包；qi 与 strength 是 path 包内的子域（力道/气道/宙道皆流派，soul 非流派独立成包）。⚠ **类名以其所在包名为前缀**（path 包内即 `PathQiData`/`PathQiEntry`/`PathStrengthData`/`PathQiService`/`PathStrengthService`）。

| Attachment         | key                | 存什么                                                                                                                             | sync          | serialize |
|--------------------|--------------------|------------------------------------------------------------------------------------------------------------------------------------|---------------|-----------|
| `APERTURE`         | `aperture_data`    | `List<Aperture>` 0..2，列表序 = 开窍序；`Aperture.second` 标第二窍（可先于第一窍开出）；**无窍 = 空列表**                          | ✅ OWNER_ONLY | ✅        |
| `APERTURE_STORAGE` | `aperture_storage` | 每窍 `List<ItemStack>` + 本命格 `ItemStack`；**不 sync**（菜单走槽通道）                                                           | ❌            | ✅        |
| `NOURISH`          | `nourish_data`     | 温养会话：`cultivating` + `target` 窍索引 + 饿锚（进度与石化闩在 `Aperture` 上，每窍一份）                                         | ✅            | ✅        |
| `BODY`             | `body_data`        | physiques/extremePhysique/race/ageParts/lifespanParts/lastDayIndex/deathQiLifespanLost/halfZombieEndTick/zombieTier/lastBilledTick | ✅            | ✅        |
| `SOUL`             | `soul_data`        | maxSoul/currentSoul                                                                                                                | ✅            | ✅        |
| `PATH`             | `path_data`        | 稀疏 `Map<GuPath, PathEntry(attainment, marks)>`；道痕按来源 tag 分桶                                                              | ✅            | ✅        |
| `STRENGTH`         | `strength_data`    | `Set<BeastStrength>` + 稀疏 `Map<HumanStrength,Integer>`                                                                           | ✅            | ✅        |
| `QI`               | `qi_data`          | 稀疏 `Map<QiKind, PathQiEntry(amount, holdEndTick)>`；当前量按游戏时刻推算，不逐 tick 写                                           | ✅            | ✅        |
| `MIND`             | `mind_data`        | brilliance + 密 `Map<WisdomType, MindPool>`（三池永在）                                                                            | ✅            | ✅        |

非 record 两个：`ESSENCE_CARRY`（`float[MAX_APERTURES]`，**既不 sync 也不 serialize**，guzhenren 唯一原地突变）、`BORN`（`Boolean`，出生掷才情的闩；`copy` 必须带它，serialize 不 sync）。

- **服务是唯一写入者**（service/ 同五包 + 根级 `PlayerDataService`）：aperture 包 `ApertureService`/`ApertureEssenceService`/`ApertureNourishService`/`ApertureStorageService` + 分帧任务 `AperturePressureExplosionTask`；body 包 `BodyService`/`BodyHealthService`/`BodyAttackService`；soul 包 `SoulService`；path 包 `PathQiService`/`PathService`/`PathStrengthService`/`PathTimeFlowService`（宙道）；mind 包 `MindService`；`EpicFightIntegration` 是外部战斗系统的唯一桥接点。
  ⚠ 跨域授予按业务流程计两条：`reconcileTalentPaths` 对齐十绝道痕与人气；`BodyService.setRace` 经 `PathService` 撤回/授予种族道痕与造诣。种族在 body、道痕在 path，不能再按旧目录归属说它们同域。出现第三条独立流程时再评估 coordinator，不为计数机械抽象。跨域只读不算授予。
- 温养边界：服务和 payload 都拒绝负索引与越界索引；无窍时读取目标返回安全索引，心跳取消旧温养会话，不能调用 `Math.clamp(..., 0, -1)`。
- 极值资源：long 加法与道痕总和用饱和运算，减去 `Long.MIN_VALUE` 也不能先直接取负；脑海阈值保留先除后乘并饱和，标签缩减精确计算中间乘积。身体年→份与寿元加减/死气比例返还保留完整中间表达式，最后再夹值；宙道 long 每步量乘法饱和。普通平衡值不因此改变。
- ⚠ 读写签名规则：**读吃 `Player`，写吃 `ServerPlayer`**。唯一例外 `PlayerDataService`（clone 期间新实体是 `Player`）。不要预先放宽到 `LivingEntity`（会丢编译期保证）。
- ⚠ ⚠ **`EnumMap`/`EnumSet` 永远 `new EnumMap<>(Class)` + `putAll` / `noneOf` + `addAll`，绝不 copy 构造器**——对空的非 Enum 源抛异常，而所有 `DEFAULT` 都是 `Map.of()`/`Set.of()`。
- 派生、绝不存储：`talent`（`fromPercent`，1..19 是洞→NONE）、`maxEssence`、`soulTier`、`markTotal()`、`usableJin`、`Title`（from rank）、`ApertureStatus`（`ApertureService.status(p, i)` **按窍**：Zombie/Half-Zombie 或石化→`DEAD`，否则 `NORMAL`；三闸读 `== NORMAL`——regen/温养/冲刷）。耐力当前值与上限均由 Epic Fight 保存；GZR 只派生百分比修饰符。
- 记录骨架：`DEFAULT` → `CODEC`（**每字段 `optionalFieldOf`**）→ `STREAM_CODEC` → 紧凑构造器夹值 → 派生 getter → `with*`。`Aperture`（13 个活动字段 + decode-only legacy carrier；`second` 在末位）、`BodyData`（10 分量）、`GuRecipe`（7 分量）的 `STREAM_CODEC` 是手写的（composite 上限 6），编解码顺序无编译期检查。
  ☠ 紧凑构造器体内**字段尚未赋值**——构造期校验只能吃参数，调 accessor 读到 null（`GuRecipe.validate` 实发）。
- ⚠ ⚠ **默认 0 常常是真值，不是"没有"**：`USED_AT`/`REFINED_AT` 未盖章 = 新世界第一下进 cd；`halfZombieEndTick`/`starvedSinceTick` 默认 `-1` 不是 `0`（`0` 是真实游戏时刻）；`RefinedGuState.damageTaken` 0 = 无伤（野生自动满血）。**安全含义永远做零值**。
- ⚠ `Ticks` 是全模组唯一时间常量（`HALF_SECOND 10 · SECOND 20 · HALF_MINUTE 600 · MINUTE 1200 · DAY 24000 · HALF_DAY 12000`）。**没有 config**，别发明；别处不许再声明 `24000`。
- ⚠ 主修 [primary path]：派生自本命蛊但**存储在 `Aperture`**（存储不 sync，存即同步），**每窍各论**——蛊栈上 `VITAL_APERTURE` 组件记录绑哪窍（缺省 = 主窍），入本命槽即绑定+立主修，炼蛊单蛊产出仍绑主窍；**只有本命蛊死亡才清**（`onVitalGuLost` 按组件清那窍），取出不清、跨窍挪动不清旧窍（既有「取出不清」的自然结果）；辅修 ≠ 主修（紧凑构造器强制），绑定辅修道的本命蛊静默清辅修；两个可空 `GuPath` 是数据模型仅有的 null（`ofNullableEnum`，ordinal+1、0=未设）。
- ⚠ `ApertureStorage.with` 增长到 index；`ApertureData.with` 拒绝越界——两者相反，都是故意的。
- ⚠ `ApertureStorage` 写入**绝不走 `ApertureService.store()`**（`BodyHealthService.refresh` 挂在那，移个物不该重算 max health）。
- ⚠ `isAwakened` = **有第一窍**（存在 `!second` 的窍）——希望蛊/炼蛊三闸/命令闸读它；`hasAperture` = 有任何窍——HUD/B 面板/元石/温养读它。只有第二窍时 `primary()`（位置 0）兜底读到第二窍，玩家等级随之（Alex 拍板）；**血量除外**——`healthRank` 只认第一窍，未开窍恒凡人（§6）。希望蛊开第一窍走 `insertFirst` + `ApertureStorage.shiftRight()`（存储与本命格整体后移一格，绑定 0 显式改 1）+ `ApertureNourishService.shiftTargetForInsertedFirst`（温养中的 target 跟窍平移，否则静默改养新第一窍）；CODEC 解码把位置 1 的窍自动补 `second=true`（旧档按位置暗示）。

## 6. 数值与公式（guzhenren）

```
maxEssence   = baseEssence × stage.essenceMultiplier × rank.rankBase
regenPerDay  = 100 × talent.regenRate × rank.rankBase × stage.essenceMultiplier   (100 是名义基数 BASE_REGEN_PER_DAY)
maxHealth    = healthRank.maxHealth -- 20/40/60/80/100；血量只按第一窍（`ApertureService.healthRank`），未开窍（哪怕只有第二窍）恒凡人 20
attackDamage = 1 + Σ beast.attackBonus + usableJin × HumanStrength.ATTACK_PER_JIN(0.125)
                 + (zombie ? 5 << zombieTier : 0) + Σ AttackContributor.attackBonus(amplifier)
```

- 资质：十绝 100（weight 10）/ 甲 80..99（20）/ 乙 60..79（30）/ 丙 40..59（30）/ 丁 20..39（10）；regenRate 20/8/4/2/1。⚠ `Talent` 常量**高到低**声明，`shift(+1)` = `ordinal-1`，封死在枚举里。
- 才情：普通 1 / 尚可 4 / 不俗 16 / 卓越 64 / 旷世 256 念每秒，权重 15/25/25/25/10；出生掷、与资质独立；`Brilliance` **没有 NONE**（凡人也思考）。
- 十绝体质：道痕 1000 均分给 `getTalentPaths()`（双道体 500/500）打 `EXTREME_PHYSIQUE` tag + **人气 100**（`TALENT_HUMAN_QI`，入 `PathQiData`）。⚠ **两半去不同地方**。纯梦求真体存在但**永远摇不出来**（`randomTenExtreme` 排除）。
  ⚠ "之后"的体质要在 `BodyService.setExtremePhysique` 完成后读，否则十绝出生零道痕且不报错。
- 脑海：念 50000 **可溢到 6/5 × max**（再多炸 `mind_ocean_shattered`）；入缓冲锁 `bufferUsed`（睡觉离开缓冲且念 ≤ cap 才清，用过缓冲只回一半缺口）；意 12 / 情 8 硬上限永不死。`WisdomType.isBurstable()`；夹值只在 `MindService.setCurrent`；自然回复恒停 cap。
- 魂：0 即死（current 与 max 都算，一个检查抓两个）；复活还 **1**，上限为 0 时回 100。
- 称号：`Title.fromRank` 派生不存储——NONE→凡人 · 一转..五转→蛊师 · 六转..九转→蛊仙；`display.realm`（rank+stage）与 `display.realm_title`（realm+title）两个键，中英连法不同，**绝不在 Java 里拼**。
- 种族：生为人族**不掷骰**；异人只有**龙人**是玩家能变的（其余 10 个 NPC 永久专属，别发明变化蛊补路线）；`/gzr body race set` 仍列全部 12 个（造 NPC/测试用）。改种族 = 10 道痕 + 造诣 `up`×1（**位移不是设定**，换回人族 down×1 天然可逆；无上大宗师封顶时切换会亏一档，接受的代价）。
- 蛊虫生命值：`TendedGuItem` 专属，默认 `HEALTH_PER_RANK` = **12**；`GuSpec.maxHealth()` 可覆盖（现无蛊覆盖，僵尸蛊也走默认）。**不要加到 `Rank` 上**——`Rank.maxHealth` 已是玩家 HP，同词两主语。
- 承受上限：`capacity`（默认 100，大力真武体 300）；`total ≤ cap` 1:1；`[cap, cap×10)` 线性 20 步收尾；`≥ cap×10` 锁 `cap+20`（120/320）。全力以赴只**解除斜坡**（回到你的 total），不发力气。兽力不在斜坡里。`usableJin` 保持 int（×0.125 精确）。
- 攻击力（double 必须二进制精确，禁 0.1/0.2/0.3 类）：白豕/黑豕 +3.0 一次 · 熊 +4.0 · 斤力 +0.125×9 · 十斤 +1.25×9 · 钧 +3.75×30 · 十钧 +37.5×30；层数 9/9/30/30 合计恰好 9999 斤。
  花豕 +5.0（60s）/ 蛮力天牛 +8.0（30s）/ 龙丸蛐蛐跳高是 `MobEffect` 修饰符走 `AttackContributor`，**不进** `BodyAttackService` 的 `addAttributeModifier`。
  ⚠ `BodyHealthService`/`BodyAttackService` 的修饰符 **transient** + 未变 no-op + 降帽后 clamp 当前血。⚠ `MobEffect` 上的修饰符 **permanent** 才对——两套规矩别互相"修"。⚠ 僵尸攻击力 `5 << zombieTier` 挂 `BodyData.zombieTier` 不挂效果（永僵无 `MobEffect`）。⚠ 攻击加成读数走 `AttackContributor.bonus()`，**别读 raw `ATTACK_DAMAGE`**——它含手持武器，不是肉身事实。
- 炼蛊成功率：`min(100, baseSuccess + attainment.getRefinementBonus())`，十档 0/2/5/10/20/40/50/70/80/100，**一次掷骰在最后窗口后**。⚠ 转数不是概率因子，别加回来。
- 炼化价刻度（占本转巅峰十绝池，池 = 800/8千/8万/80万/800万）：一次性 0 · 力道 1×（含全力以赴） · 变化道 1× · 酒虫 2× · 宙道/智道/土道（石窍蛊）0.125× · 元老蛊 0.02×。**真值只在 `ModItems` 的 `GuSpec` 链里**，本文只是刻度。
- 炼蛊烧魂换算：current 魂扣完啃 `maxSoul`，**1 max = 10 current**，一路啃到死（`soul_collapse` 收尸）。魂耗按升炼转差 1/2/3/4 点每秒（一→二 1，四→五 4）；其余炼法未定。
- 修炼一轮 = 100 个自身时间秒，每秒 `max(1, ceil(maxEssence / 100))` 真元；冲刷窍壁价 `1200 × rankBase`（`PrimevalStoneItem.spend` 先扣可用真元，不足再扣元石）；普通表 40/30/20/10，十绝 60/25/15 无掉基。
- Epic Fight 耐力：基础 15；GZR 资质/体质的上限修饰是未开窍 +0% · 丁/丙 +10% · 乙/甲 +20% · 十绝 +50% · 大力真武 +150%。详见 wiki《耐力》。

## 7. 心跳顺序（guzhenren，`PlayerTickEvents.onPlayerTick`，承重）

每秒（`tickCount % Ticks.SECOND`）一条直线，**大多数步读前一步写的东西**。无强制、无注释可声明，所以记在这里。**重排/插入/"分组"前必读**。

```
 1 tickAging                 → 返回 days；下面每个日钟都吃这个返回。⚠ 寿元不在这里
 2 tickCarried               ─┐
 3 ApertureStorageTick       ─┴─ 两个日钟走法，都吃 (1)
 5 menu.reload               ← 必须跟 (3)：否则开着的菜单 save() 复活刚饿死的
 6 closeDistilling           ← 必须在 (12) 前：1:2 回吐要先于 regen 选池
 7 tickHalfZombie            ← 可能改 Zombie/Half-Zombie；(8)(11)(12) 都读它
 8 pinUndeadHunger           ← 在 (7) 后，否则本 tick 结束的形态还在压饱食条
 9 PathQiService.syncEffects ← 从气池重建气效果；(10)(12) 问 hasEffect
10 tickDeathQi               ← 在 (9) 后为效果，在 (7) 后为僵
11 BodyAttackService.refresh ← 在 (7) 形态与 (9) 效果后——都是 bonus() 的项
11b tickLifespan             ← 在 (9) 后为倍率，在 (7) 后为形态；在 (15) 前，(15) 读寿元耗尽
12 ApertureEssenceService.regen ← 在 (6)(7)(9) 后
13 ApertureNourishService.tick ← 在 (12) 后：本秒回复要先于本秒消耗计费
14 MindService.regen         ← 在 (7) 后：僵每 5 秒付整秒
15 SelfRelianceGuItem.tryAutoUse ← 在 (14) 后：本秒念头回复完成后才检查低血自动催动
16 tickPressure              ← 在 (15) 后：每秒心跳调用；服务内按分钟增长，99% 用截止刻倒计时，交给 (17) 引爆
17 checkLethalState          ← 最后，永远。压力/寿元/魂魄/脑海依次能造致死态
```

- ⚠ **`syncEffects` 在 `tickDeathQi` 和 `regenStep` 前不是装饰**——两者都问 `hasEffect(DEATH_QI)`，池子决定本 tick 效果在不在。
- ⚠ **`checkLethalState` 命中第一个就返回**，四死有固定优先级：空窍压力爆炸 → 寿元 → 魂魄 → 脑海。压力满时**先问 `ApertureNourishService.convertPetrifiedPressure`**（石化且未到五转 → 直升下一转初阶 + 压力 90% + **转正**，消息复用 `impact.success`），问不成才 `detonatePressure`；creative 跳过整个检查，两条都不发生。
  ⚠ **`detonatePressure` 的方块清除是分帧任务**（`attachment/service/aperture/AperturePressureExplosionTask`，由 `event/server/ServerTickEvents` 的 `ServerTickEvent.Post` 驱动——全模组唯一非玩家 tick 的事件源，玩家死了坑照样挖完）：伤害/自伤/压力清零 t0 一次结算；坑由内向外逐壳**静默**挖（`UPDATE_CLIENTS`，无掉落无逐格邻居更新，每 tick 上限 65,536 格），每列半径取名义值的 90%..100%（每次爆炸独立掷）；球壳边缘最后统一 `updateNeighborsAt` 一轮（防浮空沙/水/火把）；未加载区块跳过。**别把破坏搬回玩家心跳、别给球内方块恢复邻居更新**——逐格 `UPDATE_ALL` 就是当初的卡顿根源（vanilla 1.21.1 爆炸量小才扛得住 flag 3）。
  ⚠ **爆炸半径 = `16 × (rank + 1)`（一..五转 32/48/64/80/96）；大力真武体再 +16（48..112）**，伤害球同半径。阶段链 CLEAR→FLOOR→RING→RIM（RIM 永远最后）。坑底按体质逐格决策（`floorBlockAt`）：北冥冰魄 y≥0 冰 / -32..0 浮冰 / <-32 蓝冰（越深越不化），且 FLOOR 后多一个 RING 阶段——球外 8..16 格宽噪声环带，表面 草/泥土/菌丝/灰化土/砂土 按列 50/50 转 雪块/细雪（走 `getHeight(MOTION_BLOCKING)`）；炎煌雷泽 坑底 1/4 列岩浆 + 其余岩浆块；万金妙华 1/8 列零星，y<0 深层金矿 / 0..32 金矿 / ≥32 金块；大力真武与其余 6 体质无铺地。概率全走列噪声（同 `columnJitter` 换盐），阈值常量在任务类顶部。
- ⚠ **新步不免费**：说清它必须跟在哪步后、为什么，并加进这张表。压力服务自身负责分钟门控与 99% 截止刻，不能把门控移回心跳调用方。
  ⚠ 慢于一秒的东西也在这里，门控 `tickCount % N`（N 必须整除 20）；非倍数会**静默不发生**。
- ⚠ **寿元/年龄是两个钟两个锚**：`tickAging` 保 `lastDayIndex`、返回天数给日钟走法；`tickLifespan` 保 `lastBilledTick`、扣寿元。**两者不可互替**。锚是 `dayTime` 不是 `gameTime`（`/time add 24000` 仍当场老一岁）；时间倒退重锚不计费；都读主世界。
- ⚠ 寿元按「份」存：`PARTS_PER_TICK 6`、1 年 = 144,000 份，NBT 键 `age_parts`/`lifespan_parts`；调用方只说「年」，只有 `BodyService` 知道「份」。单位比 tick 细是给将来「放慢」的蛊留的。
- ⚠ **宙道只动 `perStep`，不动日切**：宙道倍率只改「这一秒从他身上出多少份」，不改日夜滚动；寿元走 `perStep` 所以被加速（同一动词=真元/念头，符号错不了）。⚠ 时间锚（`PathQiEntry.holdEndTick`/`halfZombieEndTick`/`USED_AT`）**永不缩放**；效果自身时长是世界钟（五分钟就是五真实分钟）。

**出生 / 死亡 / 克隆**（`PlayerDataService`）：
- 出生：`onBirth` 只掷才情，触发点只有两个——首登（`onJoin` 查 `BORN`）与 `resetAll`；`BORN` 闩 + `copy` **必须带它**（否则每次死亡重掷才情）。将来的「出生给一次」都进 `onBirth`，每次登录的东西不进。
- 死亡：四致死检查 空窍压力爆炸→寿元→魂魄→脑海（`checkLethalState` 先中先回）；任何死（含原版）都由原版实体死亡、clone 和 respawn 流程处理，不写入 `Physique`；复活移除 Zombie/Half-Zombie，保留 Extreme。⚠ `vital_gu_lost` 是**伤害不是死法**——一次性扣 80% 剩余血，自己杀不死人（它不需要 `onRespawn` 行）。
- 克隆唯一裁决处 `onClone`（无 `copyOnDeath`）：维度往返恒 `copy`；死亡 + keepInventory **on** = `copy` 再解死；**off** = `resetAll`（gamerule 读 **server**）。
- 复活「解死」只回裸值：寿元 86 · 魂 1（max 本身为 0 时回 100）· 三念池清零 + 清缓冲 · `halfZombieEndTick=-1` · `zombieTier=-1`。⚠ **每新增一种死法都要在 `onRespawn` 加一行**。

**体质 [Physique]**（`BodyData.physiques`，可累积）：`ZOMBIE` 与 `HALF_ZOMBIE` 互斥，`EXTREME` 可与其中一个共存；具体 `ExtremePhysique` 只保存在 `BodyData.extremePhysique`。死亡不是体质，仍由原版实体死亡、clone 和 respawn 流程处理；重生移除 Zombie/Half-Zombie，保留 Extreme。Zombie 写寿元 = 1（`ZOMBIE_LIFESPAN`）；Half-Zombie 不动寿元。真元：Zombie/Half-Zombie 下所有空窍均为 `ApertureStatus.DEAD`，自然回复、温养和冲刷都停（元石仍可用）。念：Zombie 每 5 秒付整秒的 20%，Half-Zombie 不减；耐力：都不耗；饱食：都 pin 20；呼吸：都不溺水；攻击：+5/10/20/40 挂 `BodyData.zombieTier`（`5 << tier`；命令直接加入体质时 tier=-1 无攻击；`rung()` 从二转起算，不是 `tier()`）。死气：Half-Zombie 沾到变 Zombie（Zombie 无血地板、无寿元债）。一个时间戳 `halfZombieEndTick` 背两条规则：形态内 / 结束后 5min 内再用僵尸蛊 = Zombie / 之后清。⚠ 死气要**整体跳过**（不只冻寿元）——`drainByDeathQi` 记账 `deathQiLifespanLost`，生气清账退 3/4。⚠ 「不老」不能停日计数（`tickAging` 返回天数给蛊饿日钟，吞掉会全饿死）。

## 8. 蛊虫体系速查（item/）

**两个分支，类即判据**（`reusable`/`feedable` 布尔已删，勿加回）：
- 一次性 [one-shot] ×17：希望蛊 + 舍利 ×5 + 生机叶 + 寿蛊 ×4 + 胆识蛊 + 第二空窍蛊 ×5 —— 炼化即使用、**即时**（`useDurationTicks` final 恒 0）+ 40 tick（2 秒）冷却、堆叠、无组件。胆识蛊 +10 魂上限。**希望蛊是例外**：`stacksTo(1)`（骰值在栈上，组件整栈共享）、80 tick 仪式、直接继承 `MortalGuItem`。
- 需照顾 [tended] ×53（真值在 `ModItems`）：`TendedGuItem`，`stacksTo(1)`，`RefinedGuState` 一个族。其中 `ConsumedGuItem`（用完消失）×11：更蛊 ×2 + 恶念蛊 ×4 + 随意蛊 ×2 + 石窍蛊 ×3；其余 42：兽力虚影 6（灌注 3 + 即时 3）+ 人力钧力 4 + 突进 4（横/纵/冲四/冲五）+ 自力更生 3 + 苦力 1 + 全力以赴 3 + 酒虫 4 + 元老蛊 5 + 天元宝莲 3 + 僵尸蛊 9。另有蛊材 32（元石 + 酒 ×5 + 气道 21 + 人窍 ×5）+ 元泉方块物品 1；**合计 103 件注册物品**。
**GuSpec 决定形状，只有两种**：`channel(essencePerRound)` → 灌注 7 只（兽力灌注 3 + 人力钧力 4）；`costPerUse(essence)` → 即时 51 只（0 价组 ×24：元老蛊 5 · 更蛊 2 · 恶念蛊 4 · 随意蛊 2 · 石窍蛊 3 · 第二空窍蛊 5 · 天元宝莲 3 被动）。`GuSpec.validate()` 注册期跑：channel 必须整除饱食率，不自洽直接起不来并报是哪只蛊。

**四手势**：

| 手势                 | 行为                                                     | 蓄力                                            |
|----------------------|----------------------------------------------------------|-------------------------------------------------|
| 右键                 | 野生→炼化（**按着不放持续浇灌**，无蓄力梯）；已炼化→使用 | 使用 5/10/20 tick（`useChargeByGap`，全档一律） |
| 右键 + 副手食物      | 只喂不用                                                 | **即时**（全套唯一 0）                          |
| 蹲 + 右键 + 副手食物 | 喂且用                                                   | 按使用的 5/10/20                                |
| 蹲 + 右键 + 副手元石 | 元老蛊取 64（0 真元，蓄力）；其余边灌边补真元            | —                                               |
| 蹲 + 右键 + 无副手   | 落回普通右键                                             | —                                               |

- ⚠ ⚠ **没有左键**（已删，勿复活 `hasSwing` 等）。蹲键 = `isCrouching()`（姿态），**绝不用 `isShiftKeyDown()`**（创造飞行/切换蹲下时误判）。`hasSneakUse` 返回 false 必须落回普通右键（承重，否则蹲着无法炼化）。`holdingFood` 只问"副手是不是它的食物"。
- ⚠ 拒绝 = 红字 actionbar、不消耗不进 CD；成功不说话（唯一例外：随机结果用 `inform` 无色条）。红字走 `sendSuccess` 不走 `sendFailure`（部分拒绝是结果不是失败）。
- ⚠ `spend` 的顺序：`super.spend` **之后**补发冷却（`addCooldown` 无条件 put，先发被冲掉）。炼化完成盖 `REFINED_AT`（`TendedGuItem.POST_REFINE_COOLDOWN_TICKS` = 20 tick）+ 原版冷却**只补足不缩短**（该物品已有冷却就不动，长冷却护住）；`cooldownRefusal` 取效果冷却与炼化挡板之 max（`allowsUseDuringEffectCooldown` 只豁免前者）→ gate/蹲下/autoUse 三路统一拦，扫条丢失时按章剩余重画；`spend` 发两者之 max。
- ⚠ 蛊**永不被用尽**：入账上限在玩家身上（蛊可转手）。`*_TO_CONSUME`/`spent()` 是命名坑。`ConsumedGuItem.drive` 返回 1 让基类自己的 `spend` 收尾。
- ⚠ 强制催动（饱食 0）：**先用后死**，返回的 1 是死亡不是收益。灌注永远不会把饱食推到 2 以下（`essenceAboveHungerFloor`，饿不死）。创造：饿到 0 不死、不付食物（两个 `hasInfiniteMaterials` 闸）。
- ⚠ 饱食规则（无时钟族除外）：**每日 -1**；**≤1 播报「蛊饿了」**（`HUNGRY_THRESHOLD` 恒 1，全档统一，不随餐长）；归 0 饿死。播报闩 = `days > 0`（`tickKept` 每秒跑，条只在日切动，无闩每秒喊）。
  ⚠ ⚠ **喂食时钟（`FedClock` + `FED_AT`/`FED_WARNED`/`FED_LEFT`）整族已删，勿为任何蛊再建第二种时钟。** 元老蛊、更蛊、天元宝莲、石窍蛊都无时钟（`NoClock`）永不进食、永不饿死。
- ⚠ 天元宝莲 [Treasure Lotus Gu，木道被动 ×3]：`TreasureLotusGuItem`，`payOwnUpkeep` 每秒回 maxEssence 5% + 产 1/10/100 元石。产量链：有伤时全进 `HEAL_BANK`（组件，伤愈清账，每点生命 100/1000/10000 颗）→ 元老蛊（背包+空窍，`storeStones`）→ 背包 → 快捷栏 → 副手 → 掉落。已炼化右键被拒（`payoutGate` 恒 `no_use` 红字，`feedsFromOffhand` false → fail 无挥臂）；未炼化长按炼化不受影响。
- ⚠ 灌注节奏两档：转数高于此蛊 → 蛊定速 1 tick/步（步至多 `essencePerHungerPoint`）；等于或低于 → 池子定速 5 tick/步取池 1/5、1/4、1/3、1/2（合每秒八成）。别统一成一档。每一步**先喂副手再算真元**（`eat` 在 `stepAmount` 前，顺序承重）。饱食按 `essencePerHunger`（builder，原 `hungerEvery`）的坎连续结算，不等攒满一轮，松手已投作数。
- ⚠ 生命值恢复走 `unitsPerHealth`（= `unitsPerHunger`）；**先回血再回饱食**；生命值绝不走 `GuClock.eat`；`autoFeed` 故意不接血。野生蛊掉血不能回；炼化不治疗；`bornRefined` 原样带 `damageTaken`。
- 死只有一个漏斗 `died`：饿死/力竭/炼废三条消息；本命蛊主人在线才记账（**缺 else 是决定**），本命蛊掉到 0 永不死。
- ⚠ `MortalGuItem` 构造器会跑 `GuSpec.validate`；一个 feed tag 一个速率；饥饿条是自定义组件，**不是 durability()**（砂轮合蛊会删进度）。
- 本命蛊丢失代价（`onVitalGuLost`）：受到当前血量 80% 的伤害（通常剩余 20%，杀不死自己）· 魂 current/2 · 三念池 /2；上限不动；主修清**蛊栈上 `VITAL_APERTURE` 指的那窍**。今天唯一丢失途径 = 饿死（`starved` 单一漏斗）；玩家自己死不掉本命蛊。

**野生蛊虫实体**（entity/，希望蛊 + 三豕蛊 + 四种横冲系甲虫）：
- ⚠ **飞行蛊免疫摔落伤**：`FlyingGuEntity.checkFallDamage` 覆写为空（原版蜂/蝙蝠/鹦鹉/FlyingMob 同款）。`checkFallDamage` 对任何向下位移累积 fallDistance（不看 noGravity），自驱下降（希望蛊俯冲贴身、落地 goal、逃跑锥）落地即 `ceil(距离-3)` 伤害，1 点血必死——2026-09-09 甲虫实发，三类全中招。
- 捕捉 = 裸右键**永不开窍门槛**（希望蛊是开窍唯一钥匙；item/ 一样无 `isAwakened`）。无模型 ≠ 无碰撞箱（希望蛊 `NoopRenderer` 空画 + `sized()` 才可点）。
- ⚠ 粒子必须 full bright：`END_ROD` 恒全亮，`DUST` 被世界光照染色——**配色解决不了光照问题**。END_ROD 粒子只在 `HopeGuEntity`（`FlyingGuEntity` 基类不持粒子配置）。
- ⚠ `FOLLOW_RANGE` 是寻路搜索半径不是察觉距离；`DETECT_RANGE` 才是。希望蛊靠近是直接驱动 `deltaMovement`，**不走寻路**（一团直线飞的光点不该用寻路，别加回）。
- 希望蛊生成：`MobCategory.AMBIENT`（上限 15 顶不破）；**必须显式判地表** `y >= seaLevel`（`NaturalSpawner` 均匀取样，≈94% 在地下；`canSeeSky` 树叶算遮挡会清空森林地面）；群系走 `#guzhenren:hope_gu_spawns` **逐个列 39 个陆地群系，排除 `river` / `frozen_river`**（不能 `#is_overworld`——海洋表面恰在海平面）；一簇 1..2。
- 希望蛊一分钟窗口 = **任何玩家**进入察觉范围即开、只开一次、走开不停；到点 `discard()` 不是 `kill()`。希望蛊只找**未开窍**玩家（`seeks` 管靠近，抓取永远自由——死锁铁律，别补对称）。被杀零掉落。
- **豕蛊实体 ×3**（白/黑/花，`BoarGuEntity` 一类，**GeckoLib 管线**——一套 `geometry.boar_gu` + 三张变体贴图 `{black,white,flower}_boar_gu.png`，共享静态 `DefaultedEntityGeoModel`，`BoarGuGeoRenderer` 贴图注入；资产在 `assets/guzhenren/{geo,animations}/entity/boar_gu.*`，动画 `animation.boar_gu.{takeoff,fly,land}`——takeoff/land 服务端 `triggerAnim` 一次性、fly 由「空中」state handler 常驻 loop）。
  行为三 goal（全 MOVE flag，priority 0/1/2 自然抢占）：**0 `FleePlayerGoal`**——6 格内非 creative/spectator 玩家 = 威胁，反向锥选点 ×3 速逃，**逃出 10 格才恢复**；RESTING/LANDING 被威胁抢占时先 `takeOff()`。⚠ 选点用 `AirAndWaterRandomPos` 不用 `DefaultRandomPos.getPosAway`——后者要求落点可站立，飞行实体开阔空域恒 null（探针实证）。**1 `LandRestGoal`**——`wantsToLand` 且无威胁时落本列 `getHeight(MOTION_BLOCKING)` 点，休息 160..200t 清 delta 偶尔转头，600t 下降超时/导航失败 `takeOff()` 防悬死。**2 `WanderCourseGoal`**——锥形随机飞（`trigger()` 旁路原版 `noActionTime >= 100` 拒启闸保留），路径走完下一 tick 重选 + **每 100t 强制换向**，每次重选掷 1/10 `requestLanding()`。
  ⚠ 豕蛊 `FLYING_SPEED` 属性 **0.3**（希望蛊保持 0.1）——0.1 下导航驱动实际 ≈0.03 m/s 等于不动；wander ≈1.7 m/s、flee ≈4 m/s，观感待 Alex `runClient` 实测回调（`EntityRegistrationEvents.RESTING_GU_FLYING_SPEED`）。
  自然生成：**39 个陆地群系同希望蛊**（tag `#guzhenren:boar_gu_spawns`，与 hope 清单同列表双写）、y≥海平面地表同款、**每种权重 4（希望蛊 8 的一半）、一簇 1**；无生成蛋。捕捉与零掉落同希望蛊规矩。
- **横冲系甲虫 ×4**：一套 `geometry.rhinoceros_beetle`，横冲浅色、直撞原色、横冲直撞四/五转共用深色；原模型比例、0.4×0.4格碰撞箱、阴影半径0.2。导出包含 `animation.idle/walk/lift/fly/land` 五段，运行只用休息/起飞/飞行/降落，不增加地面巡游。
  共享豕蛊的飞行休息行为；客户端与服务端阶段同步。四种均1点实体生命、自由右键捕捉、死亡无掉落，不继承伤势到物品。独立甲虫群系tag复用39陆地列表、y≥海平面、AMBIENT、每群1，横冲/直撞/四转/五转权重4/4/2/1，无额外时间天气条件；命令可召唤，无生成蛋或放生。详细资源与设计见 wiki《蛊虫 与蛊材》，时长见玩家《时间与时间戳总表》。

**酒虫 ×4**（即时）：饱食条 **8** 点（1/2/4/8 瓶回 1 点），**一次用掉 3**——满条 8 点用一次剩 5，每日 -1 后**五天不喂就饿死**（知情选择）。**只在本转可用**（别转可炼化不可用，rank 闸读主窍）。三阶段：抽干真元 → 回复改道进精炼池（同率）→ 精炼点 1 当 2 花；到期余量按 1:2 回吐（结算 `current + distilled×2` 受 `maxEssence` 钳制，精炼价值最多静默损一半——2026-08-30 拍板为设计损耗）。**按窍入列**：每次使用唤醒下一个未精炼的窍（先一后二，`beginDistilling` 顺序扫；两窍可并行精炼，效果时长刷新一天；全部窍都在精炼中才红字）——真相是窍上的 `distilling` 旗标，不是效果；`closeDistilling` 盯「任一窍精炼余量 > 0 且效果没了」全收尾。闸门读 `spendable()` 不读 `currentEssence`。

**元老蛊 ×5**：**无时钟——永不进食、永不饿死**（别加回）。右键全存（免费即时）、蹲+右键取 64（0 真元，蓄力）；完全无法手喂；`payOwnUpkeep` 每秒跑、**只剩回血**（普通背包格也生效；仓空带伤放着不死）；`drawStones` **可以抽空到 0**（炼蛊元石槽走它，不留餐）。五转容量 1e8 是断崖。`STORED_STONES` 是独立 long 组件，不在 `RefinedGuState` 上。

**更蛊 ×2**（`ConsumedGuItem`）：两更蛊每层 ×2、最多 2 层；三更蛊每层 ×3、最多 3 层；两种相加最高 ×13，各五分钟。同种重复使用加一层并刷新时长，到顶后只刷新。refine 100k/1M（0.125× 池）、costPerUse 0。**无时钟（`NoClock`）**：无饱食条、不喂、不饿、永不饿死。**两更，不是二更**（用词）。无宙道蛊材 → 不能手喂、不能回血（炼蛊失败伤口留着）。

**智道蛊 ×6**（`ConsumedGuItem`）：恶念蛊 ×4（二..五转，refine 0.125× 池）+ 随意蛊 ×2（一/二转）。costPerUse 0、hungerPerUse 0（使用不扣饱食，日扣照走）。恶念：立即灌 `THOUGHTS` 恶念（64/640/6.4k/64k）+ 持续效果，**五转从空脑海灌直接死**（解毒是未建的念头转换）。随意：十秒随机念头效果。喂食：恶念是泥土占位（TODO），随意有 `CASUAL_FEED`。

**石窍蛊 ×3**（土道，三..五转，`ConsumedGuItem` 无时钟）：refine 10k/100k/1M（0.125× 池）、costPerUse 0、cd 1 秒；不能回血、不能当本命。**目标选择（`stoneTarget` 纯函数缝）**：主窍 NORMAL → 打主窍；仅当主窍 DEAD 且第二窍 NORMAL 才落第二窍；**转数不符不顺延**（不顺延是规格：让位只给丢失的窍）——转数/巅峰检查都针对目标窍，无可用目标红字。**用一次直达本转巅峰并石化 = `ApertureStatus.DEAD`**：`ApertureNourishService.petrify` 置位（`setStage(PEAK)` + 清进度 + **压力清零**）；三闸全关——温养/冲刷（`== NORMAL`）+ **自然回真元**（`regenStep` 按窍整体跳过并清 carry；**元石仍可用**，天元宝莲等物品回复不算自然恢复）；空窍页只显示「正常/死窍」（`InfoModel.Status`）；命令改转数/阶段不受影响（命令是覆盖层）。**十绝体另一面——压力换升转 + 转正**：压力照常每分钟 +2 涨，满 100% 先走 `convertPetrifiedPressure`（未到五转 → 直升下一转初阶 + 压力落 90% + 写回 `DEFAULT` **转正**——石窍唯一自愈路，落地可用下一转石窍蛊再入循环），五转才正常爆炸。**石化清位只有两条路：满压转正（仅十绝体）/ `resetAll`；非十绝体石化只有 `resetAll`。** 三转暂时共用一张贴图（按注册键三份，分转图来了直接替换文件）；第二空窍蛊/人窍十件同理共用一张。

**第二空窍蛊 ×5**（人道，一..五转，`OneShotGuItem`，`SecondApertureGuItem` 一类，一次性免费——refine 0、无饱食无喂食、堆叠 64、40 tick 冷却）。**唯一能开出第二窍的机制**（`ApertureService.openSecondary`）：开出 = 甲等8成（base 80 固定）+ 本转初阶 + 满真元 + 无体质（永非十绝，永不积压）。**未开窍也能用**（无窍时唯一窍就是第二窍，希望蛊之后 `insertFirst` 插队开第一窍）；**「蛊转数 ≤ 主窍」闸已删**；唯一红字 = 已有第二窍且转数 ≥ 蛊转数（升级必须严格更高，**不受主窍封顶**，按 `secondIndex` 定位；整体覆盖回初阶，主修/辅修随覆盖保留）。**僵时可用**：Zombie/Half-Zombie 下开出的第二窍也读 `DEAD`，没有僵期开窍放行。**第二窍只能温养升小境界，永不冲刷升转**（`canImpact` 仅主窍、`atCeiling` 按 `second` flag）——本转巅峰即天花板（温养按钮消失，存储按钮仍在）；温养/冲刷创造模式免费；B 面板每窍一个按钮栈（温养——主窍进度满时被冲刷替代 / 冲刷只画第一窍 / 空窍存储恒在），双窍列内竖排、单窍底部整宽；辅修选择按窍。

**人窍 ×5**（`GuMaterialItem` 直注册，堆叠 64，人道，一..五转）：纯炼蛊材料（无 use、无蛊方、未来配方输入）；唯一来源 = **keepInventory off 死亡**（resetAll 清几窍掉几颗，各按各转数，掉在尸体处；keepInventory on 不掉）。

**真元级联（一句话总则：先用第一窍，再用第二窍）**：`spendable` = Σ各窍(真元+精炼×2)；`consume` 按窍序各先精炼后真元（`cascadeTake` 纯函数缝有单测钉）；`add` 先填满第一窍再溢第二窍；`regenStep` 按窍各过各的 `status` 闸；炼蛊烧元/灌注/元石 spend 等一切「玩家真元」读数自动变两窍。温养升小境界减压 20 仅主窍自己升时享受（压力只在主窍）。

**僵尸蛊 ×9**（变化道，即时）：二转游僵 · 三转毛僵 · 四转跳僵 · 五转 ×6（天魔尸/梦魇尸/修罗尸/地魁尸/病瘟尸/血鬼尸）。`ZombieGuItem` 一类，时长注册时给；生命值走默认 12（无覆盖）。五分钟是陷阱不是 cd（cd 1 秒防双击）；`hungerPerUse` 4、腐肉唯一食物；唯一拒绝是「已是僵」；六只五转完全相同。⚠ 血鬼尸蛊 = `blood_wight_gu` / Blood Wight Gu，英文不带 Zombie，别规整。

**突进蛊 ×4**（基础力道，即时）：横冲蛊（三转）+ 直撞蛊（三转）+ 横冲直撞蛊 ×2（四/五转）。效果 `CrashGuEffect` 一类三形态（横/纵/双轴），移动走可复用的 Dash 机制：客户端经 `DashPayload` 上报，`EpicFightIntegration.dash` 驱动 Epic Fight 闪避动画（坐标向量 ×3）。主手空或非蛊且效果匹配时，方向键 / 左 Alt 任一先按都可触发；按住不连发，每次重新按左 Alt 触发一次。`V` 预切 Battle 可选；首次 Dash 进入 Battle，Battle 内可继续，效果结束不强制回 Vanilla。

**气道蛊材 ×21**（`QiMaterialItem`，堆叠 64，走蛊虫那条 5/10/20 使用梯子，真元按 tick 均摊、**不存进度**——组件整栈共享，存即希望蛊 bug）：产气 10/40/160/640/2560 按转，注册时给基类一个 `QiKind`（剑/力/元气无叶子类；生气叶子管抵消死气；死气叶子管即时+免费）。效果按**数量档位**触发、**不同种可同时挂着**：力气 +0.25/1/4/16/64 攻击 · 生气每 10t 回 1..5 HP（1心=1HP）· 元气 +20..100% · 剑气无效果。保持期：力气统一 10min、生/元气 2..10min 按档、剑气暂定 5min；之后**每秒掉 1/2/4/8/16**（按锚定档），**掉即断**（`PathQiService.syncGraded` 先检查保持期；已实现）。死气：免费、即时、**不衰减**、每 6s 扣 1 年、压血到一格、压过一切回复；>0 即全强度，重复使用累加；生气 1:1 抵消，**清到 0 才退 3/4 账**；复活清空死气+清账。效果是气池的投影（`PathQiService.syncEffects`，心跳+写入时跑；死气 `syncDeath` 每次只刷 20 tick，跳一拍也不让效果断）——牛奶/clear 解不了死气。⚠ 气道蛊材不设开窍门槛（死气害凡人是要的）。气→气道道痕的转换是未来的杀招/蛊虫，**比例未定，别建**。

## 9. 炼蛊体系速查（recipe/ + menu/RefinementMenu）

- 词汇：炼蛊 = 用蛊材+蛊方造出本来不存在的蛊。只建**以人炼蛊** + **合练/升炼**两法（合练与升炼**不互斥**，是两根轴：炼法 + 蛊输入数——别塞回一个枚举；派生簇已删，`TODO(炼法)` 在 `GuRecipe`）。平炼/逆炼/其余四类未来。
- 输入格：5×5 去四角 = 外圈 12 + 内圈 9 = 21；蛊材只能外圈、蛊虫只能内圈；
  ⚠ ⚠ **外圈谓词是 `!(item instanceof MortalGuItem)`，绝不用 `instanceof GuMaterialItem`**（四转全力以赴蛊方吃原版铁块+金块）。
- ⚠ ⚠ **SHAPED 且精确**：`slots` 与 `ingredients` 平行，第 n 份料必须坐 `slots.get(n)`，**其余格全空**。蛊方在 `ModRecipeProvider` 里写成 **ASCII 图**（`' '` 切角，`'.'` 空格），坏图直接 fail `runData`。**无镜像无旋转**——图就是摆位。一份数量坐一格（16 铁块 = 一格 16 个）。
- 仪式：`windows` 列表 = 各窗口石头数；`WINDOW_TICKS 100`(5s) + `GAP_TICKS 40`(2s)，4 窗口 = 520 tick = **26s**。**菜单即时钟**（`broadcastChanges` 每 tick）——无事件、无 attachment，关窗天然中止；运行中炼制按钮变**停止**，点它干净中止（不毁材料）。
- ⚠ ⚠ **代价是双池每秒扣**：真元 `essencePerSecond` + 魂魄 `soulPerSecond`。魂先扣 current，扣完啃 `maxSoul`（1:10），**一直扣到死**。开炉门槛 = 全程真元的 **40%**；元石槽在他真元 <50% 时补向 **80%**（窗口优先，剩下才补人）。魂耗 = 升炼转差 1/2/3/4 点每秒。⚠ `maxSoul` 还无手段提升（魂道未建），别软化数字。
- ⚠ **最后一 tick 前什么都不扣，网格全程锁死**；失败：蛊材损一半（向上取整）、蛊虫损 `maxHealth()/4`（12/4 = **3**，四次废一只）、一次性蛊虫无损、本命蛊掉 0 不死。`claim()` 只在开炉跑一次。已抽走的元石中止也不退（燃料不是材料）。
- 元石槽收散元石或元老蛊；从元老蛊抽走用 `drawStones`（可抽空）。
- 三闸（以人炼蛊 = 空窍即容器，**未开窍不能炼**）：tab 灰（表现）/ `OpenRefinementPayload` 拒（真门）/ `craft()` 再拒（reset 兜底）。真元闸读 `spendable()` 不读 `currentEssence`（酒虫 phase 1 清空池）。
- 产出：`TendedGuItem.bornRefined` 是**唯一产出门**。恰好一只蛊虫产出 → 继承本命标记（`bind`（记窍位）+ `inherit` **重写主修**，绑主窍）；两只及以上 → 谁都不继承。
- **蛊方选择器**：蛊方按钮开模态列表；选中后它是 `match()` 考虑的唯一蛊方，自动从背包填充，缺的画半透明 ghost 在它点名的格子（格子被错物占了不画）。走 `clickMenuButton`，不开新 payload。两端索引一致只靠 `GuRecipe.known` 按 id 排序（今天返回全部——**脑海存储的接缝**）。`DATA_SELECTED` 传 `index+1`，0 = 未选。⚠ ⚠ **模态必须 translateZ 到 500**——槽位物品在 z=150、手持 232+150、数字 432、tooltip 400，不压就被打穿。ghost 在 `renderLabels` 画（carried item 最后画）。
- 按钮三态（灰/暗底红框/亮），中点**故意可点**（服务端回带数字的红字）。客户端无法匹配蛊方（`getServer()` 为 null），全部实时数字走 `ContainerData`（九格 int）。⚠ ⚠ **`ContainerData` 是 int API 但 short 上线**——真元/魂魄大数永远别放。
- 两张现蛊方（`ModRecipeProvider` + runData）：

| 蛊方           | 配料                                                    | eps | 魂/s | 成功率 | 窗口          |
|----------------|---------------------------------------------------------|-----|------|--------|---------------|
| 四味酒虫       | 酒虫 ×2 + 酸/甜/苦/辣酒各 1                             | 1   | 1    | 50%    | [16,16,16,16] |
| 四转全力以赴蛊 | 三转全力以赴 ×1 + 铁块 ×16（4/格×4）+ 金块 ×8（2/格×4） | 100 | 3    | 30%    | [64,64,64,64] |

## 10. 修炼 / 宙道 / 耐力 三系统速查

**修炼 [cultivation]**（`ApertureNourishData` + `ApertureNourishService`，都在 data/service 的 `aperture/`）：B 面板每窍一个按钮栈（`NourishAperturePayload` 带窍索引）、冲刷 payload 只服务主窍（`ImpactApertureWallPayload`）。温养走初阶→巅峰；冲刷窍壁把巅峰翻成下一转初阶。⚠ **第二窍只有温养，永不冲刷**（升转唯一路 = 更高转第二空窍蛊）；**第二窍到本转巅峰即天花板——温养按钮消失，无冲刷（存储按钮仍在）**；主窍五转巅峰温养/冲刷都消失（存储仍在），低转巅峰**冲刷替代温养**（进度满后温养无意义，冲刷归零进度后温养回归）；**死窍/石窍温养画灰**（`canNourish(p, i)`/`canImpact` 查 `status(p, i) == NORMAL`，运行中的温养被 `nourishSecond` 守卫取消，见 §7/§8）。温养目标存 `ApertureNourishData.target`（服务夹到现存窍）；进度在 `Aperture.nourishProgress`。一轮温养 = 100 个自身时间秒，每秒 `max(1, ceil(该窍 maxEssence / 100))` 真元（级联扣）；冲刷价 `1200 × rankBase`（`PrimevalStoneItem.spend` 先扣可用真元，不足再扣元石）；**创造模式下温养与冲刷免费（`hasInfiniteMaterials` 闸，免付仍掷骰）**；升转必须同时写初阶（否则「二转巅峰」平方上限）；冲刷**无论成败进度归零**；冲刷前先扣价、扣不动则进度不动；按住不动是**客户端**活（`MovementInputUpdateEvent`）。

**宙道 [Time Path]**（`PathTimeFlowService` + `TimeRateUpEffect`）：自身时间加速的「一扇门」，从 `getActiveEffects()` 走出；同类 `TimeRateUpEffect` 新效果无需改 `PathTimeFlowService`。⚠ ⚠ 倍率**相加**：两更最多 ×4 + 三更最多 ×9 = ×13。`rate()` 求和 floor 到 1（⚠ 不是 `1 + Σ(rate-1)`）。一蛊一效果，同种重复使用叠层并刷新时长。三钟：自身时间（赚/花/等）跟着走 · 物品时间（蛊饿不饿/存不活）**不动** · 世界时间（日夜/实体/时间锚）**不动**。☠ 三个动词出门、调用方用一个：`waited`/`perStep`/`steps`。在调用点对 `rate()` 做算术 = 寿元曾倒着走过。只有 `InfoModel` 可读它打印。宙道造诣小节：`rate() > 1` 才显示（`InfoModel` 判，两界面共用）。

**耐力 [Stamina]**（Epic Fight）：GZR 的 `StaminaData`、`StaminaService`、`StaminaEvents`、疾跑 mixin、耐力 HUD/面板/命令均已删除。Epic Fight 独占 current、regen、HUD、重量与普通消耗；`EpicFightIntegration` 只给资质/体质加临时最大值百分比，僵尸/半僵将 STAMINA 型技能消耗改成 0，不取消动作。没有 GZR 跳跃/疾跑/饥饿/疲惫/宙道耐力事件；`keepSkills` 每次 server started 对全部 level 设 true。主手 `MortalGuItem` 每 client tick 强制采矿模式；`SetTargetEvent` 取消全部野生蛊（`WildGuEntity`）的锁定。详见 wiki《耐力》《epic-fight-replacement》。

## 11. effect/ 包：分 pool / timed 两半（按"真相在哪"分，不是按"谁给"分）

- `effect/pool/`：气池与半僵的**投影**——每心跳由 `PathQiService.syncEffects` 重建，**牛奶解不了**（池子是真相）。`StrengthQiEffect`/`LifeQiEffect`/`EssenceQiEffect`/`DeathQiEffect`/`HalfZombieEffect` 等。
- `effect/timed/`：自己的真相挂在原版计时器上。`FlowerBoarGuEffect`/`BruteForceLonghornBeetleGuEffect`/`DragonpillCricketGuEffect`/`VitalityLeafGuEffect`/`AllOutEffortGuEffect`/`LiquorDistillingEffect`/`ZombieGuEffect`/更蛊效果等。
- `effect/AttackContributor`（根）：服务两半——力气/花豕/蛮力天牛实现 `attackBonus(amplifier)`；龙丸蛐蛐碰 `JUMP_STRENGTH` 不实现。
- ⚠ **`MobEffect` 无 `onEffectRemoved` 钩子**：但一个效果能命中自己的最后一 tick（`shouldApplyEffectTickThisTick(duration, …)` 收到剩余时长，`duration == 1` 是自然过期前最后一 tick）。⚠ `applyEffectTick` 必须 `return true`（false 会被原版当场移除）。**它只覆盖自然过期**——牛奶/`/effect clear`/死亡全绕过去。气的四个效果是例外（池子是真相，心跳装回来）。判据：**惩罚可以**挂最后一 tick（龙丸/蛮力 20s 虚弱，喝奶躲掉合理）；**退款/回吐/欠账绝对不行**（酒虫 1:2、死气账），必须走心跳 level 检测。**别互换**。
- ⚠ 死气 choke > 酒虫重定向 > 元气加成（`regenStep` 里先查 `isChoked`）。

## 13. 通用陷阱与"别碰"清单

- **别碰**：化僵杀招（已砍，「就像我从未讲过」；**僵状态**已建，是另一回事）；`isFeatured`/肉体特殊流派（已退役，被 `MarkTag` 取代）；`QiType`（被 `QiKind` 取代，勿复活）；左键模板；`refinePerUse` 封顶；`isAwakened` 检查（item/ 里一个都没有，靠真元拦）；`layersPerStep`（恒 1）；**喂食时钟整族**（已删，勿建第二种时钟）；`Rank.SIX..NINE` 的 0；`EssenceColor`（零调用但别删）；炼蛊概率的转数项；`ATMOSPHERIC_HEAVEN_AND_EARTH` 的数据（无规格，标题）；气 ⇄ 蛊材的比率与物品（元石不是它）；出生种族掷骰（生为人族是设定）；发明变化蛊补完玩家异人路线；**别发明新死法**（真加了新死法才需要在 `onRespawn` 加一行——`vital_gu_lost` 是唯一豁免）；给气造 max；`consume` 三处去 DRY；`EffectIconLayout` 的接口常量（2026-08-30 清单评估知情保留，别转 final 类）；`util/` 包；门面类；为 3+ 阶段（仙形状）建任何东西；`MarkTag.settable`/`isSettable`（已删）；`pathStanding`/`pathStandingEmpty`/`appendCount`（已删）；`display.path_attainment` 翻译键（已删）。
- 心动词与呈现：颜色**只表反馈类别**（绿=变了，红=没变+原因；绝不评价数值）；[GZR] 永远默认色；一个效果服务整条转数阶梯时图标按 amplifier 换（`amplifier = tier()`，图标后缀 = 转数），**别拆成每转一个效果**；不带后缀的旧图留着（vanilla 拼图集要）。HUD 布局 Alex runClient 看过并接受，别擅自"改进"。
  **所有 MobEffect 粒子关闭**（`ModEffects.instance` helper 统一 `showParticles=false, showIcon=true`）；**效果颜色统一白色（`EFFECT_COLOR = 0xFFFFFF`）**，具体颜色表不再是运行真值。
  域强调色唯一真值在 `client/ModPalette`：空窍 `#4FC3F7` · 肉身 `#FFAB91` · 魂魄 `#D388FF` · 流派造诣 `#FFD54F` · 脑海 `#4DD0E1` · 炼蛊 `#81C784`；跨面同义铬色（面板底/边框/槽底/按钮三态/条底边/精炼池蓝）也收 `ModPalette`，单面独用的色留各文件本地。
- 命令：`command/sub/` 与 attachment 同构五包（aperture/body/soul/path/mind；`CmdPath`/`CmdQi`/`CmdStrength` 在 path 包），**命令树顶级同五包**——`/gzr path` 独立成根。☠ path literal 下 `marks`/`attainment`/`qi`/`strength` **全字面量并排**、`<path>` 参数挂在 marks/attainment 之下——`GuPath` 的 `qi`/`strength` 值与子命令同名，字面量在前防 word 参数被劫持（`/gzr path marks <p> set …`，动词在 `<path>` 之后）。`ModEnumArgument` 是 `word()`，**同义字面量不归并成枚举**（`mind wisdom` 三池是字面量，别"整理"）；嵌套枚举要自己的参数名（`ARG_PATH`/`ARG_KIND`）；`awaken`/`reset` 后必须 `refreshCommands`（`onClone` 是唯一豁免）；`requires()` 是呈现、`applyOnAwakened` 才护数据。☠ `/gzr` 是 redirect，补全器读上文必须走 `getLastChild()`（`ModEnumArgument.get` 已是那个缝；`ModEnumArgumentTest` 守它，别因"没人用"简化掉——指纹是 `/guzhenren …` 好使而 `/gzr …` 不好使）。
- 命令树（只此一张）：
  ```
  /gzr info  [aperture|body|soul|path|mind] [targets]     /gzr awaken | reset [targets]
  /gzr aperture [1..2] rank|stage|talent set <v>|up|down
  /gzr aperture essence base|current|distilled set|add|sub <n> · essence refill（均可带 [1..2] 窍索引）
  /gzr body physique add|remove <zombie|half_zombie|extreme> · physique extreme set <v>
  /gzr body race set · lifespan|age set|add|sub
  /gzr soul max|current set|add|sub <n> · soul refill
  /gzr path marks <p> set|add|sub（恒 NATURAL）· path attainment <p> set|up|down
  /gzr path strength grant|revoke <beast>|clear · human <kind> set|add|sub · path qi <kind> set|add|sub（重锚保持期）
  /gzr mind brilliance set|up|down · wisdom <thoughts|wills|emotions> current|max set|add|sub · wisdom <thoughts|wills|emotions> refill
  ```
  动词规律：分档枚举 → `set`/`up`/`down` · 裸数 → `set`/`add`/`sub` · 封顶池 → 加 `refill` · 集合 → `grant`/`revoke`/`clear`。`sub n` = `add -n`。
- 心跳（`PlayerTickEvents`）：`tickCount % 20` 提前返回，**任何按心跳的间隔必须整除 20**（死气 120 正好）。`tickAging` 防时间倒退（`<` 一个比较就是全部守卫）。
- 菜单通用：保存用容器 `addListener`，不是 `slotsChanged`；`loading` 闩；日切在开着的菜单背后写数据后 `PlayerTickEvents` 要 `menu.reload()`（检查放 event/ 不放 service）；`countPages` 的 `+1` 是空页；返回按钮先 `onClose()` 再 `setScreen`；`renderLabels` 只放静态标签，动态元素在 `render()` 画（矩阵已平移）；两种格距 `GRID_SLOT 22`/`SLOT 18`，块宽 `(n-1)×pitch + 16`；GUI 全 `g.fill` 零贴图（别引回贴图面板）；模态浮层必须压 z（容器屏上 500）。⚠ KEPT 空窍存储翻页簇（prev/页码/next）在面板**左下角**（面板下方），返回键留左上——为避开 IPN 整理按钮挪的，别挪回。
- 信息面板（B）：**空窍页双窍 = 左右两列（左一右二，列内窍标题+详情行+列底按钮栈），单窍 = 单列+底部整宽按钮栈**；窍标题行纯显示（`ApertureIndex` 不带点击），存储唯一入口 = 每窍「空窍存储」按钮（`OpenApertureStoragePayload`）。其余 tab **数值列贴右不动，标签列从 `contentLeft()` 起**（旋钮 `LABEL_INSET_DIVISOR`）；悬停高亮与点击区跟随标签列，左留白不响应。面板标题条与分隔线仍在左边框。tab rail 起点在 `CONTENT_TOP`（与首行同基线），不在 `PAD`。单列行用 windowed 不用 clipped（`visibleRows()`，无 `enableScissor`）；`scrollRow` 在 `renderRows()` 夹，不在 `mouseScrolled`。⚠ ⚠ 「画不画」的判断放进 `display/InfoModel`，不放进界面——B 面板和 `/gzr info` 共用这一份，各判各的会出「气道造诣 [无] 道痕1000」。⚠ ⚠ **命名小节（力道造诣/智道造诣/气道造诣/宙道造诣）= 标签 + 它自己的行，永不画值**；造诣与道痕是流派造诣列表的事，每条道出现一次。⚠ KEPT `WisdomPathAchieveHeader` 有构造者但仍是纯标题行（智道行等智道蛊虫/蛊材），别当清理机会删。
- 本命蛊丢出要两个钩子（`onDroppedByPlayer` 只盖 Q 键；`ItemTossEvents` 取消后**必须把栈塞回**背包——CommonHooks 先移除后发事件）。
- 网络（共 **6 个 payload**）：下行数据**零 payload**（attachment sync 就是全部；将来给别人看放宽 sync 谓词）；上行只准 B-panel 按钮与移动类的 client intent（开容器/选辅修/开炼蛊/灌注/冲刷窍壁/Dash）；容器内走原版通道（槽点击/`clickMenuButton`）。`ESSENCE_CARRY` 不 sync 不 serialize（每次 regen 推包就废了）——**跳过 regen 的路都必须清 carry**（死气 choke、死窍/石窍状态、池满）。
- HUD 补充：`ChargeHud` 骑原版 held-item-name 基线（`AIR_LEVEL` 不是 `HOTBAR`，左/右状态堆叠完才定高）；☠ `chargeFraction` 与 caption 必须是**同一对数**，不同步 = 条静默冻结在 tick 上。`PlayerStatsHud` 条 130×9、池数字贴 `y+1`（数字恰好 7 高）；十绝压力只在寿元行后显示文字，99% 附带服务端截止刻 `MM:SS`，不画压力条；HUD 寿元一位小数、年龄零位，底层份数不变。
- 兼容：JEI/Curios optional（`localRuntime` 不是 `runtimeOnly`）；`compat/` = 只为够到别的 mod 的表面；**模组零 FTB Quests 依赖**（无 -api jar、会反依赖；整合包装了 ftb-quests jar 只走任务书 UI，将来接任务走 KubeJS 脚本调 GZR 服务，别在模组侧接）。
- ⚠ Epic Fight：`epic-fight-21.17.3.1-mc1.21.1-neoforge.jar` 是 GZR 硬依赖，构建脚本直接引用 `run/mods/` 内的文件；它与 `firstperson-neoforge-2.7.2-mc1.21.1.jar` 均同步到 `assets/test modpack` 与 `run/mods`。Better Combat、Not Enough Animations、Player Animator、Epic Fight Skill Tree 已从两处移除。Epic Fight 官方将 Sodium、Iris Shaders、First-person Model 列为 fully supported；`compat/EpicFightIntegration` 直接使用这一版的属性、游戏规则与事件 API，升级 Epic Fight 前必须编译 + `runClient` 验证。未来自定义武器/护甲/NPC 必须分别设计 Epic Fight weapon capability / entity patch，不能沿用 Better Combat 预设路线。`run/mods/` 仍镜像整合包第三方 jar（`run/` 已 gitignore，pack 更新时手动同步）。
- ⚠ GeckoLib：`geckolib-neoforge-1.21.1-4.9.2.jar` 是**第二个硬依赖**（豕蛊与横冲系甲虫模型动画），直引 `run/mods`（compileOnly + localRuntime + testImplementation——runData/GameTest classpath 都靠它）；`neoforge.mods.toml` required `[4.9,4.10)`。4.9.2 资产路径 `assets/<modid>/{geo,animations}/entity/`——**无** `geckolib/` 子目录（那是 GL5 约定，别照新 wiki 抄）；升级前编译 + `runClient` 验证。模型权威源在 `C:\alex\code\blockbench\gzr-models\`（只读，含 handoff 文档）。
  ⚠ **直引 `run/mods` 的 jar 在 CI 上不存在**（`run/` 已 gitignore）——`.github/workflows/build.yml` 有 Modrinth 下载步骤（epic-fight 版本 id `8HHhJt6i`、geckolib `tPkJmim6`）；**新增任何直引 `run/mods` 的硬依赖必须同步加下载步骤**，否则 CI 全红而本地全绿（2026-09-04 GeckoLib 实发）。
- 空窍存储补充：`mayPlace` 只收 `MortalGuItem`（一次性蛊进本命槽=陷阱）；`ItemStack.OPTIONAL_CODEC`（内部空是真槽位，只裁**尾部**空洞）；每窍普通存储+本命格总负载 `MAX_LOAD 256`，`GuSlot#getMaxStackSize(ItemStack)` 负责鼠标/Shift 部分转移。⚠ `APERTURE_STORAGE` 不 sync，槽位预检必须吃菜单 `DATA_LOAD`，不可从客户端 attachment 重算；旧档超载以 `max(256, currentLoad)` 为临时上限，只许不增载。页数无额外硬上限。
- 进度系统 [Advancement]（原版进度树，首条 `first_awakening`「开窍！」已落地）：触发器类住 `advancement/`、注册 holder 住 `registry/advancement/ModCriteriaTriggers`（**第十二个 DeferredRegister holder**——全模组 12 holder / 13 register（`ModRecipes` 双持），挂 `BuiltInRegistries.TRIGGER_TYPES.key()`）；☠ 1.21.1 触发器已是内置注册表，别用 vanilla 公开的 `CriteriaTriggers.register(String,..)`——它落 `minecraft:` 命名空间。进度 datagen 住 `datagen/advancement/ModAdvancementProvider`（includeServer）。触发器**唯一挂点 = 「使用完成」的服务/物品落点**（首条在 `HopeGuItem.apply` 的 `awaken` 后），命令是覆盖层不计入进度；vanilla 对重复触发幂等，不用自己做闩；奖励层未建。后续批次接入规则见 wiki《进度系统》。
- 读 wiki 时注意：`玩家向/` 是**版本快照**；设计以 `开发向/` 为准，数值以代码 `ModItems` 为准，冲突问 Alex。

## 15. TODO 汇总（在 wiki，不在这里）

⚠ **TODO 只写在代码内部与 `project-wiki/玩家向/蛊真人MOD 1.0.0 TODO总表.md`，AGENTS.md 不写**。
代码 TODO 清单、短期清单状态、卡链项全在《蛊真人MOD 1.0.0 TODO总表》——查 TODO 去那一个文件。
⚠ **"太远"的精确定义：只存在于第 3 阶段及以后的东西**。凡仙形状的——升仙的 天/地/人 三气、Rank 6..9 及其故意的 0、仙蛊、大道碎片、第二空窍的**仙蛊系列**（凡蛊一..五转已落地）——都是第 3 阶段。**别为之建，也别算它缺**。
⚠ 五个阶段：1 计划（done）· 2 蛊师凡人一转..五转（⬅ 这里）· 3 蛊仙六转..九转 · 4 世界背景 · 5 持续完善。

package com.unknown.guzhenren.item.gu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.unknown.guzhenren.Ticks;
import com.unknown.guzhenren.custom.enums.aperture.Rank;
import com.unknown.guzhenren.custom.enums.aperture.Stage;
import com.unknown.guzhenren.custom.enums.aperture.Talent;
import com.unknown.guzhenren.custom.enums.wisdom.Brilliance;
import com.unknown.guzhenren.custom.enums.wisdom.WisdomType;
import com.unknown.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import com.unknown.guzhenren.effect.timed.FlowerBoarGuEffect;
import com.unknown.guzhenren.effect.timed.HardshipStrengthGuEffect;
import com.unknown.guzhenren.item.material.GuMaterialItem;
import com.unknown.guzhenren.registry.item.ModItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Compares selected manual wiki [设定库] tables with booted registrations and current test sources.
 * Prose, food tags, and gameplay behavior require a separate review; no note is written by this test.
 */

class WikiConsistencyTest {

    private static final String ITEMS_PAGE = "玩家向/蛊真人MOD 1.0.0 蛊材&蛊虫.md";
    private static final String BODY_PAGE = "玩家向/蛊真人MOD 1.0.0 空窍&肉体&脑海&魂魄.md";
    private static final String TIME_PAGE = "玩家向/蛊真人MOD 1.0.0 时间与时间戳总表.md";
    private static final String TEST_PAGE = "开发向/工程/测试集.md";
    private static final List<String> ONE_SHOT_HEADERS = List.of("蛊", "数量", "转", "道", "炼化真元", "效果");
    private static final List<String> CHANNEL_HEADERS = List.of("蛊", "转", "炼化", "真元 / 轮", "饱食", "食物",
            "一轮耗饱食", "一轮结果");
    private static final List<String> INSTANT_HEADERS = List.of("蛊组", "数量", "转", "炼化真元", "使用真元",
            "饱食 / 每次", "食物与修复", "效果入口");
    private static final List<String> MATERIAL_HEADERS = List.of("分组", "数量", "内容");
    private static final Pattern UNSUPPORTED_TEST_ANNOTATION = Pattern.compile(
            "(?m)^\\s*@(?:[\\w.]+\\.)?(ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\\b");
    private static final List<Group> GROUPS = groups();
    private Path wiki;
    @BeforeEach
    void requireExplicitWikiDirectory() {
        String configured = System.getProperty("gzr.wikiDir");
        assumeTrue(configured != null, "Wiki check SKIPPED: supply -PwikiDir=<absolute wiki directory>");
        assertTrue(!configured.isBlank(), "wikiDir must not be blank");
        wiki = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(wiki), () -> "Wiki directory does not exist: " + wiki);
    }
    @Test
    void everyRegisteredItemHasExactlyOneDocumentedGroup() throws IOException {
        List<String> registered = ModItems.ITEMS.getEntries().stream().map(h -> h.getId().getPath()).toList();
        WikiTable.requireCoverage(GROUPS.stream().flatMap(g -> g.ids().stream()).toList(), registered);
        WikiTable page = page(ITEMS_PAGE);
        page.requireExactRows(ONE_SHOT_HEADERS, GROUPS.stream().filter(g -> g.table().equals("蛊") && !g.channel())
                .map(Group::label).toList());
        page.requireExactRows(CHANNEL_HEADERS, GROUPS.stream().filter(g -> g.table().equals("蛊") && g.channel())
                .map(Group::label).toList());
        page.requireExactRows(INSTANT_HEADERS, GROUPS.stream().filter(g -> g.table().equals("蛊组"))
                .map(Group::label).toList());
        page.requireExactRows(MATERIAL_HEADERS, GROUPS.stream().filter(g -> g.table().equals("分组"))
                .map(Group::label).toList());
        for (Group group : GROUPS) {
            WikiTable.Row row = page.row(headers(group), group.label());
            if (!group.channel()) row.expect("数量", List.of((long) group.ids().size()));
        }
        long gu = ModItems.ITEMS.getEntries().stream().filter(h -> h.get() instanceof MortalGuItem).count();
        long material = ModItems.ITEMS.getEntries().stream().filter(h -> h.get() instanceof GuMaterialItem).count();
        page.expectMatch("注册 \\*\\*(\\d+) 件物品\\*\\*：(\\d+) 只蛊虫与 (\\d+) 种蛊材",
                List.of((long) registered.size(), gu, material));
        List<MortalGuItem> items = guItems();
        long tended = items.stream().filter(TendedGuItem.class::isInstance).count();
        long channeled = items.stream().filter(g -> g.spec.channels()).count();
        long consumed = items.stream().filter(ConsumedGuItem.class::isInstance).count();
        page.row("分支", "一次性").expect("数量", List.of(gu - tended));
        page.row("分支", "需照顾").expect("数量", List.of(tended));
        page.expectMatch("(\\d+) 只需照顾蛊中，(\\d+) 只为灌注型、(\\d+) 只为即时型",
                List.of(tended, channeled, tended - channeled));
        page.expectMatch("共 (\\d+) 只在成功使用后消失", List.of(consumed));
        page.expectMatch("### 灌注型（(\\d+) 只）", List.of(channeled));
        page.expectMatch("### 即时型（(\\d+) 只）", List.of(tended - channeled));
    }
    @Test
    void guRanksCostsHungerAndRepairUnitsMatchRegistrations() throws IOException {
        Map<String, MortalGuItem> registered = ModItems.ITEMS.getEntries().stream()
                .filter(h -> h.get() instanceof MortalGuItem)
                .collect(Collectors.toMap(h -> h.getId().getPath(), h -> (MortalGuItem) h.get()));
        WikiTable page = page(ITEMS_PAGE);
        for (Group group : GROUPS) {
            if (group.table().equals("分组")) continue;
            List<MortalGuItem> items = group.ids().stream().map(id -> {
                MortalGuItem item = registered.get(id);
                assertTrue(item != null, () -> ITEMS_PAGE + " missing registered Gu mapping " + id);
                return item;
            }).toList();
            WikiTable.Row row = page.row(headers(group), group.label());
            row.expect("转", values(items, g -> g.rank().ordinal()));
            row.expect(group.channel() ? "炼化" : "炼化真元", values(items, MortalGuItem::refineCost));
            if (!items.stream().allMatch(TendedGuItem.class::isInstance)) {
                assertTrue(items.stream().noneMatch(TendedGuItem.class::isInstance), row.location());
                continue;
            }
            assertTrue(items.stream().allMatch(g -> g.spec.channels() == group.channel()), row.location());
            row.expect(group.channel() ? "真元 / 轮" : "使用真元", values(items, g -> g.spec.essencePerRound()));
            List<GuClock> clocks = items.stream().map(g -> g.spec.buildClock()).toList();
            if (clocks.stream().allMatch(GuClock.NoClock.class::isInstance)) {
                assertTrue(row.cell("饱食 / 每次").startsWith("无"), row.location());
                continue;
            }
            assertTrue(clocks.stream().allMatch(GuClock.HungerBar.class::isInstance), row.location());
            List<GuClock.HungerBar> bars = clocks.stream().map(GuClock.HungerBar.class::cast).toList();
            List<Long> hunger = collapse(bars.stream().map(b -> (long) b.max()).toList());
            if (group.channel()) {
                row.expect("饱食", hunger);
                row.expect("一轮耗饱食", values(items,
                        g -> g.spec.essencePerRound() / g.spec.buildClock().essencePerHungerPoint()));
            } else {
                List<Long> combined = new ArrayList<>(hunger);
                combined.addAll(collapse(bars.stream().map(b -> (long) b.perUse()).toList()));
                row.expect("饱食 / 每次", combined);
            }
            String food = row.cell(group.channel() ? "食物" : "食物与修复");
            if (group.label().equals("全力以赴")) {
                Matcher repair = Pattern.compile("；(.+?) 单位修复").matcher(food);
                assertTrue(repair.find(), row.location());
                row.expectValue("repair units", repair.group(1), values(items, g -> g.spec.unitsPerHealth()));
            } else {
                Matcher repair = Pattern.compile("×([0-9,]+)").matcher(food);
                List<Long> actual = new ArrayList<>();
                while (repair.find()) actual.add(Long.parseLong(repair.group(1).replace(",", "")));
                assertEquals(values(items, g -> g.spec.unitsPerHealth()), actual, row.location() + " repair units");
            }
        }
        page.expectMatch("默认有 \\*\\*([0-9,]+) 生命\\*\\*", values(guItems().stream()
                .filter(TendedGuItem.class::isInstance).toList(), g -> ((TendedGuItem) g).maxHealth()));
    }
    @Test
    void talentBrillianceAndMindCapacitiesMatchEnums() throws IOException {
        WikiTable page = page(BODY_PAGE);
        String[] talents = {"十绝", "甲等", "乙等", "丙等", "丁等"};
        for (int i = 0; i < talents.length; i++) {
            Talent talent = Talent.settable()[i];
            WikiTable.Row row = page.row("资质", talents[i]);
            row.expect("基数", talent.getMinPercent() == talent.getMaxPercent()
                    ? List.of((long) talent.getMinPercent())
                    : List.of((long) talent.getMinPercent(), (long) talent.getMaxPercent()));
            row.expect("出现权重", List.of((long) talent.getWeight()));
            row.expect("自然回复倍率", List.of((long) talent.getRegenRate()));
            row.expect("Epic Fight 耐力上限", List.of((long) talent.getStaminaMaxPercent()));
        }
        String[] brillianceNames = {"普通", "尚可", "不俗", "卓越", "旷世"};
        for (int i = 0; i < brillianceNames.length; i++) {
            Brilliance brilliance = Brilliance.values()[i];
            WikiTable.Row row = page.row("才情", brillianceNames[i]);
            row.expect("念 / 自身时间秒", List.of(brilliance.getThoughtsPerSecond()));
            row.expect("权重", List.of((long) brilliance.getWeight()));
        }
        String[] wisdomNames = {"念", "意", "情"};
        for (int i = 0; i < wisdomNames.length; i++) {
            page.row("内容", wisdomNames[i]).expect("默认上限", List.of(WisdomType.values()[i].getDefaultCapacity()));
        }
    }
    @Test
    void essenceTableMatchesRankAndStageMultipliers() throws IOException {
        WikiTable page = page(BODY_PAGE);
        String[] stages = {"初阶", "中阶", "高阶", "巅峰"};
        String[] ranks = {"一转", "二转", "三转", "四转", "五转"};
        for (int i = 0; i < stages.length; i++) {
            Stage stage = Stage.settable()[i];
            WikiTable.Row row = page.row("小境界倍率", stages[i] + " ×" + stage.getEssenceMultiplier());
            for (int j = 0; j < ranks.length; j++) {
                Rank rank = Rank.settable()[j];
                String column = ranks[j] + " ×" + String.format(java.util.Locale.ROOT, "%,d", rank.getRankBase());
                row.expect(column, List.of((long) Talent.EXTREME.getMinPercent()
                        * rank.getRankBase() * stage.getEssenceMultiplier()));
            }
        }
    }
    @Test
    void cooldownAndSelectedDurationsMatchCode() throws IOException {
        WikiTable page = page(ITEMS_PAGE);
        WikiTable.Row allOut = page.row("蛊", "全力以赴（三 / 四 / 五）");
        List<MortalGuItem> items = GROUPS.stream().filter(g -> g.label().equals("全力以赴")).findFirst()
                .orElseThrow().ids().stream().map(WikiConsistencyTest::gu).toList();
        expectSeconds(allOut, "效果冷却", values(items, g -> g.spec.effectCooldownTicks() / Ticks.SECOND));
        expectSeconds(allOut, "物品冷却", values(items, g -> g.spec.itemCooldownTicks() / Ticks.SECOND));
        WikiTable time = page(TIME_PAGE);
        time.expectMatch("`HALF_SECOND (\\d+) · SECOND (\\d+) · HALF_MINUTE (\\d+)"
                        + " · MINUTE (\\d+) · DAY (\\d+) · HALF_DAY (\\d+)`",
                List.of((long) Ticks.HALF_SECOND, (long) Ticks.SECOND, (long) Ticks.HALF_MINUTE,
                        (long) Ticks.MINUTE, (long) Ticks.DAY, (long) Ticks.HALF_DAY));
        time.row("什么", "花豕 / 蛮力天牛 / 苦力蛊").expectValue("duration seconds",
                time.row("什么", "花豕 / 蛮力天牛 / 苦力蛊").cell("时长").split("；")[0].replace("s", ""),
                List.of((long) FlowerBoarGuEffect.DURATION_TICKS / Ticks.SECOND,
                        (long) BruteForceLonghornBeetleGuEffect.DURATION_TICKS / Ticks.SECOND,
                        (long) HardshipStrengthGuEffect.DURATION_TICKS / Ticks.SECOND));
    }
    @Test
    void documentedTestCountsMatchCurrentSources() throws IOException {
        Path root = Path.of(System.getProperty("gzr.projectDir"));
        WikiTable page = page(TEST_PAGE);
        AssertionError unsupported = assertThrows(AssertionError.class,
                () -> rejectUnsupportedTestAnnotations("@ParameterizedTest\nvoid generated() {}",
                        Path.of("fixture.java")));
        assertTrue(unsupported.getMessage().contains("fixture.java:1"));
        String[] directories = {"src/pureTest/java", "src/test/java", "src/main/java/com/unknown/guzhenren/gametest"};
        String[] names = {"L1 pureTest", "L2 modded test", "L3 GameTest"};
        List<Long> snapshot = new ArrayList<>();
        for (int i = 0; i < directories.length; i++) {
            Pattern annotation = Pattern.compile("(?m)^\\s*@" + (i == 2 ? "GameTest" : "Test") + "(?:\\s|\\()");
            long count = 0;
            long classes = 0;
            try (var sources = Files.walk(root.resolve(directories[i]))) {
                for (Path source : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String sourceText = Files.readString(source);
                    rejectUnsupportedTestAnnotations(sourceText, source);
                    long found = annotation.matcher(sourceText).results().count();
                    if (found > 0) classes++;
                    count += found;
                }
            }
            WikiTable.Row row = page.row("层", names[i]);
            Matcher number = Pattern.compile("^([0-9]+) 条").matcher(row.cell("现状"));
            assertTrue(number.find(), row.location());
            row.expectValue("test count", number.group(1), List.of(count));
            snapshot.add(classes);
            snapshot.add(count);
        }
        page.expectMatch("当前为 L1 (\\d+) 类/(\\d+) 条、L2 (\\d+) 类/(\\d+) 条、L3 (\\d+) 类/(\\d+) 条", snapshot);
    }
    private WikiTable page(String relative) throws IOException {
        Path path = wiki.resolve(relative);
        assertTrue(Files.isRegularFile(path), () -> "Missing mapped wiki page: " + path);
        return WikiTable.parse(path.toString(), Files.readString(path));
    }
    private static void expectSeconds(WikiTable.Row row, String column, List<Long> expected) {
        String cell = row.cell(column);
        assertTrue(cell.endsWith(" 秒"), row.location());
        row.expectValue(column, cell.substring(0, cell.length() - 2), expected);
    }
    private static List<MortalGuItem> guItems() {
        return ModItems.ITEMS.getEntries().stream().map(h -> h.get()).filter(MortalGuItem.class::isInstance)
                .map(MortalGuItem.class::cast).toList();
    }
    private static MortalGuItem gu(String id) {
        return (MortalGuItem) ModItems.ITEMS.getEntries().stream().filter(h -> h.getId().getPath().equals(id))
                .findFirst().orElseThrow().get();
    }
    private static List<Long> values(List<MortalGuItem> items, ToLongFunction<MortalGuItem> getter) {
        return collapse(items.stream().mapToLong(getter).boxed().toList());
    }
    private static List<Long> collapse(List<Long> values) {
        List<Long> result = new ArrayList<>();
        for (long value : values) {
            if (result.isEmpty() || result.getLast() != value) result.add(value);
        }
        return List.copyOf(result);
    }
    private static void rejectUnsupportedTestAnnotations(String source, Path path) {
        Matcher unsupported = UNSUPPORTED_TEST_ANNOTATION.matcher(source);
        if (unsupported.find()) {
            long line = source.substring(0, unsupported.start()).chars().filter(c -> c == '\n').count() + 1;
            throw new AssertionError(path + ":" + line + " unsupported test annotation @" + unsupported.group(1)
                    + "; execution count is not statically supported");
        }
    }
    private static List<String> headers(Group group) {
        return switch (group.table()) {
            case "蛊" -> group.channel() ? CHANNEL_HEADERS : ONE_SHOT_HEADERS;
            case "蛊组" -> INSTANT_HEADERS;
            case "分组" -> MATERIAL_HEADERS;
            default -> throw new AssertionError("Unsupported group table " + group.table());
        };
    }
    private static String ladder(String prefix, int first, int last) {
        return IntStream.rangeClosed(first, last).mapToObj(i -> prefix + i).collect(Collectors.joining(" "));
    }
    private static Group group(String table, String label, String ids) {
        return new Group(table, label, Arrays.asList(ids.split(" ")));
    }
    private static List<Group> groups() {
        return List.of(
                group("蛊", "希望蛊", "hope_gu"),
                group("蛊", "生机叶蛊", "vitality_leaf_gu"),
                group("蛊", "寿蛊 / 十年寿蛊 / 百年寿蛊 / 千年寿蛊",
                        "lifespan_gu tens_lifespan_gu hundreds_lifespan_gu thousands_lifespan_gu"),
                group("蛊", "青铜 / 赤铁 / 白银 / 黄金 / 紫晶舍利蛊",
                        "copper_relics_gu steel_relics_gu silver_relics_gu gold_relics_gu crystal_relics_gu"),
                group("蛊", "胆识蛊", "guts_gu"),
                group("蛊", "第二空窍蛊", ladder("second_aperture_gu_", 1, 5)),
                group("蛊", "白豕蛊", "white_boar_gu"),
                group("蛊", "黑豕蛊", "black_boar_gu"),
                group("蛊", "熊力蛊", "bear_strength_gu"),
                group("蛊", "斤力蛊", "jin_strength_gu"),
                group("蛊", "十斤力蛊", "tens_jin_strength_gu"),
                group("蛊", "钧力蛊", "jun_strength_gu"),
                group("蛊", "十钧力蛊", "tens_jun_strength_gu"),
                group("蛊组", "花豕、龙丸蛐蛐、蛮力天牛", "flower_boar_gu dragonpill_cricket_gu brute_force_longhorn_beetle_gu"),
                group("蛊组", "横冲、直撞", "horizontal_crash_gu vertical_crash_gu"),
                group("蛊组", "横冲直撞", ladder("charging_crash_gu_", 4, 5)),
                group("蛊组", "自力更生", ladder("self_reliance_gu_", 2, 4)),
                group("蛊组", "苦力蛊", "hardship_strength_gu"),
                group("蛊组", "全力以赴", ladder("all_out_effort_gu_", 3, 5)),
                group("蛊组", "酒虫一族",
                        "liquor_worm four_flavors_liquor_worm seven_fragrances_liquor_worm nine_eyes_liquor_worm"),
                group("蛊组", "元老蛊", ladder("primeval_elder_gu_", 1, 5)),
                group("蛊组", "天元宝莲一族", "heavenly_essence_treasure_lotus_gu "
                        + "heavenly_essence_treasure_monarch_lotus_gu heavenly_essence_treasure_king_lotus_gu"),
                group("蛊组", "僵尸蛊", "roaming_zombie_gu hairy_zombie_gu hopping_zombie_gu heavenly_demon_zombie_gu "
                        + "nightmare_zombie_gu asura_zombie_gu earth_chief_zombie_gu plague_zombie_gu blood_wight_gu"),
                group("蛊组", "两更蛊、三更蛊", "second_watch_gu third_watch_gu"),
                group("蛊组", "恶念蛊", ladder("malicious_thought_gu_", 2, 5)),
                group("蛊组", "随意蛊", ladder("casual_gu_", 1, 2)),
                group("蛊组", "石窍蛊", ladder("stone_aperture_gu_", 3, 5)),
                group("分组", "元石", "primeval_stone"),
                group("分组", "酒", "liquor sour_liquor sweet_liquor bitter_liquor spicy_liquor"),
                group("分组", "气道蛊材", ladder("sword_qi_", 1, 5) + " " + ladder("strength_qi_", 1, 5)
                        + " " + ladder("life_qi_", 1, 5) + " " + ladder("essence_qi_", 1, 5) + " death_qi_5"),
                group("分组", "人窍", ladder("human_aperture_", 1, 5)),
                group("分组", "元泉", "spirit_spring"));
    }
    private record Group(String table, String label, List<String> ids) {

        boolean channel() {
            return table.equals("蛊") && ids.size() == 1 && gu(ids.getFirst()).spec.channels();
        }
    }
}

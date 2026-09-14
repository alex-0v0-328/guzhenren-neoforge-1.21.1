package com.unknown.guzhenren.registry.item;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.Ticks;
import com.unknown.guzhenren.custom.enums.aperture.Rank;
import com.unknown.guzhenren.custom.enums.path.GuPath;
import com.unknown.guzhenren.custom.enums.qi.QiKind;
import com.unknown.guzhenren.custom.enums.strength.BeastStrength;
import com.unknown.guzhenren.custom.enums.strength.HumanStrength;
import com.unknown.guzhenren.custom.enums.strength.StrengthPathBranch;
import com.unknown.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import com.unknown.guzhenren.effect.timed.CrashGuEffect;
import com.unknown.guzhenren.effect.timed.DragonpillCricketGuEffect;
import com.unknown.guzhenren.effect.timed.FlowerBoarGuEffect;
import com.unknown.guzhenren.effect.timed.HardshipStrengthGuEffect;
import com.unknown.guzhenren.item.gu.GuSpec;
import com.unknown.guzhenren.item.gu.mortal.BuffGuItem;
import com.unknown.guzhenren.item.gu.mortal.HopeGuItem;
import com.unknown.guzhenren.item.gu.mortal.earth.StoneApertureGuItem;
import com.unknown.guzhenren.item.gu.mortal.human.SecondApertureGuItem;
import com.unknown.guzhenren.item.gu.mortal.LifespanGuItem;
import com.unknown.guzhenren.item.gu.mortal.PrimevalElderGuItem;
import com.unknown.guzhenren.item.gu.mortal.RelicsGuItem;
import com.unknown.guzhenren.item.gu.mortal.VitalityLeafGuItem;
import com.unknown.guzhenren.item.gu.mortal.liquor.LiquorWormItem;
import com.unknown.guzhenren.item.gu.mortal.soul.GutsGuItem;
import com.unknown.guzhenren.item.gu.mortal.strength.AllOutEffortGuItem;
import com.unknown.guzhenren.item.gu.mortal.strength.BeastStrengthGuItem;
import com.unknown.guzhenren.item.gu.mortal.strength.HumanStrengthGuItem;
import com.unknown.guzhenren.item.gu.mortal.strength.SelfRelianceGuItem;
import com.unknown.guzhenren.item.gu.mortal.time.WatchGuItem;
import com.unknown.guzhenren.item.gu.mortal.wisdom.CasualGuItem;
import com.unknown.guzhenren.item.gu.mortal.wisdom.MaliciousThoughtGuItem;
import com.unknown.guzhenren.item.gu.mortal.wood.TreasureLotusGuItem;
import com.unknown.guzhenren.item.gu.mortal.zombie.ZombieGuItem;
import com.unknown.guzhenren.item.material.GuMaterialItem;
import com.unknown.guzhenren.item.material.LiquorItem;
import com.unknown.guzhenren.item.material.PrimevalStoneItem;
import com.unknown.guzhenren.item.material.qi.DeathQiItem;
import com.unknown.guzhenren.item.material.qi.LifeQiItem;
import com.unknown.guzhenren.item.material.qi.QiMaterialItem;
import com.unknown.guzhenren.registry.block.ModBlocks;
import com.unknown.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Every item and the only place a Gu's numbers actually live.
 *
 * <p>DeferredRegister holder: the {@link GuSpec} chain on each registration IS the truth. A figure
 * written down anywhere else is a copy of it, and when the two disagree this file is the one that is
 * right. One line per item, plus the PNG and both lang keys.
 *
 * <p>⚠ The registration id mirrors the Java name (and the PNG); a rank ladder is numbered
 * ({@code sword_qi_1..5}), but an item with its own fiction name keeps that name ({@code blood_wight_gu}).
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @since 1.0.0
 */

public final class ModItems {

    private ModItems() {}
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guzhenren.MOD_ID);
    private static final long PRIMEVAL_STONE_ESSENCE = 20L;
    private static Item.Properties tended() {return new Item.Properties().stacksTo(1);}
    private static Item.Properties oneShot() {return new Item.Properties();}

    //region 一次性蛊虫 -- refining IS the use; one charged press pays, lands and spends
    public static final DeferredItem<Item> HOPE_GU = ITEMS.register("hope_gu",
            () -> new HopeGuItem(oneShot().stacksTo(1), GuSpec.of(Rank.ONE, GuPath.HUMAN)));
    public static final DeferredItem<Item> VITALITY_LEAF_GU = ITEMS.register("vitality_leaf_gu",
            () -> new VitalityLeafGuItem(oneShot(), GuSpec.of(Rank.ONE, GuPath.WOOD)));
    public static final DeferredItem<Item> LIFESPAN_GU = ITEMS.register("lifespan_gu",
            () -> new LifespanGuItem(oneShot(), 1, 9, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> TENS_LIFESPAN_GU = ITEMS.register("tens_lifespan_gu",
            () -> new LifespanGuItem(oneShot(), 10, 19, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> HUNDREDS_LIFESPAN_GU = ITEMS.register("hundreds_lifespan_gu",
            () -> new LifespanGuItem(oneShot(), 100, 199, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> THOUSANDS_LIFESPAN_GU = ITEMS.register("thousands_lifespan_gu",
            () -> new LifespanGuItem(oneShot(), 1000, 1999, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> COPPER_RELICS_GU = ITEMS.register("copper_relics_gu",
            () -> new RelicsGuItem(oneShot(), GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> STEEL_RELICS_GU = ITEMS.register("steel_relics_gu",
            () -> new RelicsGuItem(oneShot(), GuSpec.of(Rank.TWO, GuPath.HEAVEN)));
    public static final DeferredItem<Item> SILVER_RELICS_GU = ITEMS.register("silver_relics_gu",
            () -> new RelicsGuItem(oneShot(), GuSpec.of(Rank.THREE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> GOLD_RELICS_GU = ITEMS.register("gold_relics_gu",
            () -> new RelicsGuItem(oneShot(), GuSpec.of(Rank.FOUR, GuPath.HEAVEN)));
    public static final DeferredItem<Item> CRYSTAL_RELICS_GU = ITEMS.register("crystal_relics_gu",
            () -> new RelicsGuItem(oneShot(), GuSpec.of(Rank.FIVE, GuPath.HEAVEN)));
    //endregion

    //region 兽力虚影流 [Beast Strength Phantom Branch]
    public static final DeferredItem<Item> WHITE_BOAR_GU = ITEMS.register("white_boar_gu",
            () -> new BeastStrengthGuItem(tended(), BeastStrength.WHITE_BOAR, GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                    .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                    .refine(800)
                    .channel(3_600)
                    .hungerBar(36, 1).essencePerHunger(100)
                    .feed(ModItemTags.BOAR_FEED, 1)));
    public static final DeferredItem<Item> BLACK_BOAR_GU = ITEMS.register("black_boar_gu",
            () -> new BeastStrengthGuItem(tended(), BeastStrength.BLACK_BOAR, GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                    .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                    .refine(800)
                    .channel(3_600)
                    .hungerBar(36, 1).essencePerHunger(100)
                    .feed(ModItemTags.BOAR_FEED, 1)));
    public static final DeferredItem<Item> BEAR_STRENGTH_GU = ITEMS.register("bear_strength_gu",
            () -> new BeastStrengthGuItem(tended(), BeastStrength.BEAR, GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                    .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                    .refine(800)
                    .channel(3_600)
                    .hungerBar(36, 1).essencePerHunger(100)
                    .feed(ModItemTags.BEAR_FEED, 1)));
    public static final DeferredItem<Item> FLOWER_BOAR_GU = ITEMS.register("flower_boar_gu",
            () -> new BuffGuItem(tended(), ModEffects.FLOWER_BOAR_GU, FlowerBoarGuEffect.DURATION_TICKS,
                    GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                            .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                            .refine(800)
                            .costPerUse(16)
                            .hungerBar(12, 1).hungerPerUse(4)
                            .feed(ModItemTags.BOAR_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> DRAGONPILL_CRICKET_GU = ITEMS.register("dragonpill_cricket_gu",
            () -> new BuffGuItem(tended(), ModEffects.DRAGONPILL_CRICKET_GU,
                    DragonpillCricketGuEffect.DURATION_TICKS,
                    GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                            .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                            .refine(800)
                            .costPerUse(16)
                            .hungerBar(12, 1).hungerPerUse(4)
                            .feed(ModItemTags.RABBIT_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> BRUTE_FORCE_LONGHORN_BEETLE_GU =
            ITEMS.register("brute_force_longhorn_beetle_gu",
                    () -> new BuffGuItem(tended(), ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU,
                            BruteForceLonghornBeetleGuEffect.DURATION_TICKS,
                            GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                                    .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                                    .refine(800)
                                    .costPerUse(16)
                                    .hungerBar(12, 1).hungerPerUse(4)
                                    .feed(ModItemTags.BEEF_FEED, 1)
                                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    //endregion

    //region 基础力道 [Normal] -- Strength Path Gu outside the three specialized branches
    public static final DeferredItem<Item> HORIZONTAL_CRASH_GU = ITEMS.register("horizontal_crash_gu",
            () -> new BuffGuItem(tended(), ModEffects.HORIZONTAL_CRASH_GU, CrashGuEffect.duration(30),
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 1).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> VERTICAL_CRASH_GU = ITEMS.register("vertical_crash_gu",
            () -> new BuffGuItem(tended(), ModEffects.VERTICAL_CRASH_GU, CrashGuEffect.duration(30),
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 1).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> CHARGING_CRASH_GU_4 = ITEMS.register("charging_crash_gu_4",
            () -> new BuffGuItem(tended(), ModEffects.CHARGING_CRASH_GU,
                    CrashGuEffect.duration(30), 3, GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                    .refine(800_000).costPerUse(16_000)
                    .hungerBar(12, 2).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> CHARGING_CRASH_GU_5 = ITEMS.register("charging_crash_gu_5",
            () -> new BuffGuItem(tended(), ModEffects.CHARGING_CRASH_GU,
                    CrashGuEffect.duration(60), 4, GuSpec.of(Rank.FIVE, GuPath.STRENGTH)
                    .refine(8_000_000).costPerUse(160_000)
                    .hungerBar(12, 3).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_2 = ITEMS.register("self_reliance_gu_2",
            () -> new SelfRelianceGuItem(tended(), 30 * Ticks.SECOND, 1,
                    GuSpec.of(Rank.TWO, GuPath.STRENGTH)
                            .refine(8_000).costPerUse(160)
                            .hungerBar(12, 4).hungerPerUse(4).feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_3 = ITEMS.register("self_reliance_gu_3",
            () -> new SelfRelianceGuItem(tended(), 60 * Ticks.SECOND, 2,
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 8).hungerPerUse(4).feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_4 = ITEMS.register("self_reliance_gu_4",
            () -> new SelfRelianceGuItem(tended(), 120 * Ticks.SECOND, 3,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                            .refine(800_000).costPerUse(16_000)
                            .hungerBar(12, 12).hungerPerUse(4).feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> HARDSHIP_STRENGTH_GU = ITEMS.register("hardship_strength_gu",
            () -> new BuffGuItem(tended(), ModEffects.HARDSHIP_STRENGTH_GU,
                    HardshipStrengthGuEffect.DURATION_TICKS,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                            .refine(800_000).costPerUse(16_000)
                            .hungerBar(12, 12).hungerPerUse(4).feed(ModItemTags.POTATO_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    //endregion

    //region 人力钧力流 [Human Jun Strength Branch] -- one round is one layer
    public static final DeferredItem<Item> JIN_STRENGTH_GU = ITEMS.register("jin_strength_gu",
            () -> new HumanStrengthGuItem(tended(), HumanStrength.JIN, GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                    .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                    .refine(800)
                    .channel(3_600)
                    .hungerBar(36, 1).essencePerHunger(300)
                    .feed(ModItemTags.JIN_FEED, 1)));
    public static final DeferredItem<Item> TENS_JIN_STRENGTH_GU = ITEMS.register("tens_jin_strength_gu",
            () -> new HumanStrengthGuItem(tended(), HumanStrength.TEN_JIN, GuSpec.of(Rank.TWO, GuPath.STRENGTH)
                    .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                    .refine(8_000)
                    .channel(36_000)
                    .hungerBar(36, 2).essencePerHunger(3_000)
                    .feed(ModItemTags.JIN_FEED, 1)));
    public static final DeferredItem<Item> JUN_STRENGTH_GU = ITEMS.register("jun_strength_gu",
            () -> new HumanStrengthGuItem(tended(), HumanStrength.JUN, GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                    .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                    .refine(80_000)
                    .channel(36_000)
                    .hungerBar(36, 4).essencePerHunger(6_000)
                    .feed(ModItemTags.JIN_FEED_SMELTED, 1)));
    public static final DeferredItem<Item> TENS_JUN_STRENGTH_GU = ITEMS.register("tens_jun_strength_gu",
            () -> new HumanStrengthGuItem(tended(), HumanStrength.TEN_JUN, GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                    .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                    .refine(800_000)
                    .channel(360_000)
                    .hungerBar(36, 8).essencePerHunger(60_000)
                    .feed(ModItemTags.JIN_FEED_SMELTED, 1)));
    //endregion

    //region 基础力道 [Normal] -- All-Out Effort Gu unlocks a stockpiled 9999 jin
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_3 = ITEMS.register("all_out_effort_gu_3",
            () -> new AllOutEffortGuItem(tended(), 60, GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                    .refine(80_000)
                    .costPerUse(1_600)
                    .hungerBar(12, 4).hungerPerUse(4)
                    .feed(ModItemTags.ALL_OUT_FEED, 5)
                    .cooldown(80 * Ticks.SECOND)));
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_4 = ITEMS.register("all_out_effort_gu_4",
            () -> new AllOutEffortGuItem(tended(), 90, GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                    .refine(800_000)
                    .costPerUse(16_000)
                    .hungerBar(12, 8).hungerPerUse(4)
                    .feed(ModItemTags.ALL_OUT_FEED, 5)
                    .cooldown(100 * Ticks.SECOND)));
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_5 = ITEMS.register("all_out_effort_gu_5",
            () -> new AllOutEffortGuItem(tended(), 120, GuSpec.of(Rank.FIVE, GuPath.STRENGTH)
                    .refine(8_000_000)
                    .costPerUse(160_000)
                    .hungerBar(12, 16).hungerPerUse(4)
                    .feed(ModItemTags.ALL_OUT_FEED, 5)
                    .cooldown(120 * Ticks.SECOND)));
    //endregion

    //region Liquor Worm [酒虫] -- hunger bar 8 with 3 per use, and only its own rank can drive it
    public static final DeferredItem<Item> LIQUOR_WORM = ITEMS.register("liquor_worm",
            () -> new LiquorWormItem(tended(), GuSpec.of(Rank.ONE, GuPath.FOOD)
                    .refine(1_600).costPerUse(16)
                    .hungerBar(8, 1).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> FOUR_FLAVORS_LIQUOR_WORM = ITEMS.register("four_flavors_liquor_worm",
            () -> new LiquorWormItem(tended(), GuSpec.of(Rank.TWO, GuPath.FOOD)
                    .refine(16_000).costPerUse(160)
                    .hungerBar(8, 2).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> SEVEN_FRAGRANCES_LIQUOR_WORM = ITEMS.register(
            "seven_fragrances_liquor_worm",
            () -> new LiquorWormItem(tended(), GuSpec.of(Rank.THREE, GuPath.FOOD)
                    .refine(160_000).costPerUse(1_600)
                    .hungerBar(8, 4).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> NINE_EYES_LIQUOR_WORM = ITEMS.register("nine_eyes_liquor_worm",
            () -> new LiquorWormItem(tended(), GuSpec.of(Rank.FOUR, GuPath.FOOD)
                    .refine(1_600_000).costPerUse(16_000)
                    .hungerBar(8, 8).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    //endregion

    //region 元老蛊 -- a vault for 元石 that never needs feeding at all
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_1 = ITEMS.register("primeval_elder_gu_1",
            () -> new PrimevalElderGuItem(tended(), 1_000L, GuSpec.of(Rank.ONE, GuPath.SPACE)
                    .refine(16).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_2 = ITEMS.register("primeval_elder_gu_2",
            () -> new PrimevalElderGuItem(tended(), 10_000L, GuSpec.of(Rank.TWO, GuPath.SPACE)
                    .refine(160).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_3 = ITEMS.register("primeval_elder_gu_3",
            () -> new PrimevalElderGuItem(tended(), 100_000L, GuSpec.of(Rank.THREE, GuPath.SPACE)
                    .refine(1_600).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_4 = ITEMS.register("primeval_elder_gu_4",
            () -> new PrimevalElderGuItem(tended(), 1_000_000L, GuSpec.of(Rank.FOUR, GuPath.SPACE)
                    .refine(16_000).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_5 = ITEMS.register("primeval_elder_gu_5",
            () -> new PrimevalElderGuItem(tended(), 100_000_000L, GuSpec.of(Rank.FIVE, GuPath.SPACE)
                    .refine(160_000).costPerUse(0)));
    //endregion

    //region 天元宝莲 [Treasure Lotus Gu] -- wood path; 5% essence per second and minted stones
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_lotus_gu",
            () -> new TreasureLotusGuItem(tended(), 1, 100, GuSpec.of(Rank.THREE, GuPath.WOOD)
                    .refine(80_000).costPerUse(0)));
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_monarch_lotus_gu",
            () -> new TreasureLotusGuItem(tended(), 10, 1_000, GuSpec.of(Rank.FOUR, GuPath.WOOD)
                    .refine(800_000).costPerUse(0)));
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_king_lotus_gu",
            () -> new TreasureLotusGuItem(tended(), 100, 10_000, GuSpec.of(Rank.FIVE, GuPath.WOOD)
                    .refine(8_000_000).costPerUse(0)));
    //endregion

    //region 僵尸蛊 [zombie Gu] -- 变化道; a timed 半生半僵, and a 5-minute window that makes it permanent
    public static final DeferredItem<Item> ROAMING_ZOMBIE_GU = ITEMS.register("roaming_zombie_gu",
            () -> new ZombieGuItem(tended(), 2 * Ticks.MINUTE, GuSpec.of(Rank.TWO, GuPath.TRANSFORMATION)
                    .refine(8_000).costPerUse(160)
                    .hungerBar(12, 2).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HAIRY_ZOMBIE_GU = ITEMS.register("hairy_zombie_gu",
            () -> new ZombieGuItem(tended(), 4 * Ticks.MINUTE, GuSpec.of(Rank.THREE, GuPath.TRANSFORMATION)
                    .refine(80_000).costPerUse(1_600)
                    .hungerBar(12, 4).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HOPPING_ZOMBIE_GU = ITEMS.register("hopping_zombie_gu",
            () -> new ZombieGuItem(tended(), 6 * Ticks.MINUTE, GuSpec.of(Rank.FOUR, GuPath.TRANSFORMATION)
                    .refine(800_000).costPerUse(16_000)
                    .hungerBar(12, 8).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HEAVENLY_DEMON_ZOMBIE_GU = ITEMS.register("heavenly_demon_zombie_gu",
            ModItems::fifthRankZombieGu);
    public static final DeferredItem<Item> NIGHTMARE_ZOMBIE_GU = ITEMS.register("nightmare_zombie_gu",
            ModItems::fifthRankZombieGu);
    public static final DeferredItem<Item> ASURA_ZOMBIE_GU = ITEMS.register("asura_zombie_gu",
            ModItems::fifthRankZombieGu);
    public static final DeferredItem<Item> EARTH_CHIEF_ZOMBIE_GU = ITEMS.register("earth_chief_zombie_gu",
            ModItems::fifthRankZombieGu);
    public static final DeferredItem<Item> PLAGUE_ZOMBIE_GU = ITEMS.register("plague_zombie_gu",
            ModItems::fifthRankZombieGu);
    public static final DeferredItem<Item> BLOOD_WIGHT_GU = ITEMS.register("blood_wight_gu",
            ModItems::fifthRankZombieGu);
    private static ZombieGuItem fifthRankZombieGu() {
        return new ZombieGuItem(tended(), 8 * Ticks.MINUTE, GuSpec.of(Rank.FIVE, GuPath.TRANSFORMATION)
                .refine(8_000_000).costPerUse(160_000)
                .hungerBar(12, 16).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                .cooldown(Ticks.SECOND));
    }
    //endregion

    //region 更蛊 [Watch Gu] -- 宙道; tended like any other, and taken by the one use it is kept for
    public static final DeferredItem<Item> SECOND_WATCH_GU = ITEMS.register("second_watch_gu",
            () -> new WatchGuItem(tended(), ModEffects.SECOND_WATCH_GU, 5 * Ticks.MINUTE,
                    GuSpec.of(Rank.FOUR, GuPath.TIME)
                            .refine(100_000).costPerUse(0)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> THIRD_WATCH_GU = ITEMS.register("third_watch_gu",
            () -> new WatchGuItem(tended(), ModEffects.THIRD_WATCH_GU, 5 * Ticks.MINUTE,
                    GuSpec.of(Rank.FIVE, GuPath.TIME)
                            .refine(1_000_000).costPerUse(0)
                            .cooldown(Ticks.SECOND)));
    //endregion

    //region 恶念蛊 [Malicious Thought Gu] -- 智道; a one-use flood of evil thoughts, taken by its use
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_2 = ITEMS.register("malicious_thought_gu_2",
            () -> new MaliciousThoughtGuItem(tended(), ModEffects.MALICIOUS_THOUGHT_GU, 64L,
                    GuSpec.of(Rank.TWO, GuPath.WISDOM)
                            .refine(1_000).costPerUse(0)
                            .hungerBar(8, 2).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_3 = ITEMS.register("malicious_thought_gu_3",
            () -> new MaliciousThoughtGuItem(tended(), ModEffects.MALICIOUS_THOUGHT_GU, 640L,
                    GuSpec.of(Rank.THREE, GuPath.WISDOM)
                            .refine(10_000).costPerUse(0)
                            .hungerBar(8, 4).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_4 = ITEMS.register("malicious_thought_gu_4",
            () -> new MaliciousThoughtGuItem(tended(), ModEffects.MALICIOUS_THOUGHT_GU, 6_400L,
                    GuSpec.of(Rank.FOUR, GuPath.WISDOM)
                            .refine(100_000).costPerUse(0)
                            .hungerBar(8, 8).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_5 = ITEMS.register("malicious_thought_gu_5",
            () -> new MaliciousThoughtGuItem(tended(), ModEffects.MALICIOUS_THOUGHT_GU, 64_000L,
                    GuSpec.of(Rank.FIVE, GuPath.WISDOM)
                            .refine(1_000_000).costPerUse(0)
                            .hungerBar(8, 16).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    //endregion

    //region 胆识蛊 [Guts Gu] -- 魂道; a one-shot Gu that raises the soul cap
    public static final DeferredItem<Item> GUTS_GU = ITEMS.register("guts_gu",
            () -> new GutsGuItem(oneShot(), GuSpec.of(Rank.ONE, GuPath.SOUL)));
    //endregion

    //region 随意蛊 [Casual Gu] -- 智道; ten seconds of random thoughts, taken by its use
    public static final DeferredItem<Item> CASUAL_GU_1 = ITEMS.register("casual_gu_1",
            () -> new CasualGuItem(tended(), ModEffects.CASUAL_GU, GuSpec.of(Rank.ONE, GuPath.WISDOM)
                    .refine(100)
                    .costPerUse(0)
                    .hungerBar(8, 1).hungerPerUse(0)
                    .feed(ModItemTags.CASUAL_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> CASUAL_GU_2 = ITEMS.register("casual_gu_2",
            () -> new CasualGuItem(tended(), ModEffects.CASUAL_GU, GuSpec.of(Rank.TWO, GuPath.WISDOM)
                    .refine(1_000)
                    .costPerUse(0)
                    .hungerBar(8, 2).hungerPerUse(0)
                    .feed(ModItemTags.CASUAL_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    //endregion

    //region 石窍蛊 [Stone Aperture Gu] -- earth path; never feeds, taken by its use, and the aperture
    // it leaves stands on this rank's peak, petrified
    public static final DeferredItem<Item> STONE_APERTURE_GU_3 = ITEMS.register("stone_aperture_gu_3",
            () -> new StoneApertureGuItem(tended(), GuSpec.of(Rank.THREE, GuPath.EARTH)
                    .refine(10_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> STONE_APERTURE_GU_4 = ITEMS.register("stone_aperture_gu_4",
            () -> new StoneApertureGuItem(tended(), GuSpec.of(Rank.FOUR, GuPath.EARTH)
                    .refine(100_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> STONE_APERTURE_GU_5 = ITEMS.register("stone_aperture_gu_5",
            () -> new StoneApertureGuItem(tended(), GuSpec.of(Rank.FIVE, GuPath.EARTH)
                    .refine(1_000_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    //endregion

    //region 第二空窍蛊 [Second Aperture Gu] -- human path; opens or upgrades the second aperture,
    // a free one-shot taken by its use; Grade-A at 8/10, this rank's first stage, never a physique
    public static final DeferredItem<Item> SECOND_APERTURE_GU_1 = ITEMS.register("second_aperture_gu_1",
            () -> secondApertureGu(Rank.ONE));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_2 = ITEMS.register("second_aperture_gu_2",
            () -> secondApertureGu(Rank.TWO));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_3 = ITEMS.register("second_aperture_gu_3",
            () -> secondApertureGu(Rank.THREE));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_4 = ITEMS.register("second_aperture_gu_4",
            () -> secondApertureGu(Rank.FOUR));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_5 = ITEMS.register("second_aperture_gu_5",
            () -> secondApertureGu(Rank.FIVE));
    private static SecondApertureGuItem secondApertureGu(Rank rank) {
        return new SecondApertureGuItem(oneShot(), GuSpec.of(rank, GuPath.HUMAN));
    }
    //endregion

    //region 蛊材 [Gu materials]
    public static final DeferredItem<Item> PRIMEVAL_STONE = ITEMS.register("primeval_stone",
            () -> new PrimevalStoneItem(new Item.Properties(), PRIMEVAL_STONE_ESSENCE));
    public static final DeferredItem<Item> LIQUOR = ITEMS.register("liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> SOUR_LIQUOR = ITEMS.register("sour_liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> SWEET_LIQUOR = ITEMS.register("sweet_liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> BITTER_LIQUOR = ITEMS.register("bitter_liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> SPICY_LIQUOR = ITEMS.register("spicy_liquor",
            () -> new LiquorItem(new Item.Properties()));
    // The spring's BlockItem: same key as the block, answers to the block's lang entry, and its
    // icon is the fluid's still strip (see ModItemModelProvider) -- no item PNG of its own.
    public static final DeferredItem<Item> SPIRIT_SPRING = ITEMS.register("spirit_spring",
            () -> new BlockItem(ModBlocks.SPIRIT_SPRING.get(), new Item.Properties()));
    //endregion

    //region 人窍 [Human Aperture] -- pure gu material, ranks I..V; a wiped death drops one per aperture
    public static final DeferredItem<Item> HUMAN_APERTURE_1 = humanAperture("human_aperture_1", Rank.ONE);
    public static final DeferredItem<Item> HUMAN_APERTURE_2 = humanAperture("human_aperture_2", Rank.TWO);
    public static final DeferredItem<Item> HUMAN_APERTURE_3 = humanAperture("human_aperture_3", Rank.THREE);
    public static final DeferredItem<Item> HUMAN_APERTURE_4 = humanAperture("human_aperture_4", Rank.FOUR);
    public static final DeferredItem<Item> HUMAN_APERTURE_5 = humanAperture("human_aperture_5", Rank.FIVE);
    private static DeferredItem<Item> humanAperture(String id, Rank rank) {
        return ITEMS.register(id, () -> new GuMaterialItem(new Item.Properties().stacksTo(64), rank, GuPath.HUMAN));
    }
    public static @Nullable Item humanAperture(Rank rank) {
        return switch (rank) {
            case ONE -> HUMAN_APERTURE_1.get();
            case TWO -> HUMAN_APERTURE_2.get();
            case THREE -> HUMAN_APERTURE_3.get();
            case FOUR -> HUMAN_APERTURE_4.get();
            case FIVE -> HUMAN_APERTURE_5.get();
            default -> null;
        };
    }
    //endregion

    //region Qi Path [气道] materials -- 21, ranks I..V
    public static final DeferredItem<Item> SWORD_QI_1 = qiMaterial("sword_qi_1", Rank.ONE, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_2 = qiMaterial("sword_qi_2", Rank.TWO, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_3 = qiMaterial("sword_qi_3", Rank.THREE, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_4 = qiMaterial("sword_qi_4", Rank.FOUR, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_5 = qiMaterial("sword_qi_5", Rank.FIVE, QiKind.SWORD);
    public static final DeferredItem<Item> STRENGTH_QI_1 = qiMaterial("strength_qi_1", Rank.ONE, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_2 = qiMaterial("strength_qi_2", Rank.TWO, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_3 = qiMaterial("strength_qi_3", Rank.THREE, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_4 = qiMaterial("strength_qi_4", Rank.FOUR, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_5 = qiMaterial("strength_qi_5", Rank.FIVE, QiKind.STRENGTH);
    public static final DeferredItem<Item> LIFE_QI_1 = ITEMS.register("life_qi_1",
            () -> new LifeQiItem(qiProperties(), Rank.ONE));
    public static final DeferredItem<Item> LIFE_QI_2 = ITEMS.register("life_qi_2",
            () -> new LifeQiItem(qiProperties(), Rank.TWO));
    public static final DeferredItem<Item> LIFE_QI_3 = ITEMS.register("life_qi_3",
            () -> new LifeQiItem(qiProperties(), Rank.THREE));
    public static final DeferredItem<Item> LIFE_QI_4 = ITEMS.register("life_qi_4",
            () -> new LifeQiItem(qiProperties(), Rank.FOUR));
    public static final DeferredItem<Item> LIFE_QI_5 = ITEMS.register("life_qi_5",
            () -> new LifeQiItem(qiProperties(), Rank.FIVE));
    public static final DeferredItem<Item> ESSENCE_QI_1 = qiMaterial("essence_qi_1", Rank.ONE, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_2 = qiMaterial("essence_qi_2", Rank.TWO, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_3 = qiMaterial("essence_qi_3", Rank.THREE, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_4 = qiMaterial("essence_qi_4", Rank.FOUR, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_5 = qiMaterial("essence_qi_5", Rank.FIVE, QiKind.ESSENCE);
    public static final DeferredItem<Item> DEATH_QI_5 = ITEMS.register("death_qi_5",
            () -> new DeathQiItem(qiProperties(), Rank.FIVE));
    private static DeferredItem<Item> qiMaterial(String id, Rank rank, QiKind kind) {
        return ITEMS.register(id, () -> new QiMaterialItem(qiProperties(), rank, kind));
    }
    private static Item.Properties qiProperties() {return new Item.Properties().stacksTo(64);}
    //endregion

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

package com.unknown.guzhenren.gametest;

import com.mojang.authlib.GameProfile;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.BearEntity;
import com.unknown.guzhenren.entity.BearSpecies;
import com.unknown.guzhenren.entity.BeastEntity;
import com.unknown.guzhenren.entity.TigerEntity;
import com.unknown.guzhenren.entity.WildBoarEntity;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime coverage for the four bear species' attributes, temperament, attacks, death and persistence.
 *
 * <p>Peaceful proactive hunting is unpinned because difficulty is server-global in the shared GameTest world;
 * only the synchronous "retaliation is blocked on Peaceful" seam is tested.
 */
@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BearGameTests {

    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final float PLAYER_MAX_HEALTH = 20.0F;

    private BearGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void bearRegistrationUsesConfiguredShapeAndAttributes(GameTestHelper helper) {
        ground(helper);
        int x = 1;
        for (BearSpecies species : BearSpecies.values()) {
            BearEntity bear = spawnBear(helper, new BlockPos(x, 1, 4), species);
            helper.assertValueEqual(bear.getBbWidth(), 1.2F, species + " width");
            helper.assertValueEqual(bear.getBbHeight(), 1.4F, species + " height");
            helper.assertValueEqual(bear.getMaxHealth(), (float) species.maxHealth(), species + " max health");
            helper.assertValueEqual(bear.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.25D,
                    species + " movement speed");
            helper.assertValueEqual(bear.getAttributeValue(Attributes.FOLLOW_RANGE), 16.0D,
                    species + " follow range");
            x += 2;
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void bearNeutralDoesNotAttackFromProximity(GameTestHelper helper) {
        ground(helper);
        int x = 1;
        for (BearSpecies species : new BearSpecies[]{BearSpecies.BROWN, BearSpecies.AMERICAN_BLACK, BearSpecies.ALBINO}) {
            ServerPlayer player = survivalMock(helper);
            place(player, helper, new Vec3(x + 0.5D, 1.0D, 2.5D));
            BearEntity bear = spawnBear(helper, new BlockPos(x, 1, 4), species);
            helper.onEachTick(() -> {
                bear.getNavigation().stop();
                bear.setDeltaMovement(Vec3.ZERO);
            });
            helper.runAtTickTime(50L, () -> {
                helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                        species + " proximity damaged player");
                helper.assertTrue(bear.getTarget() == null,
                        species + " proximity assigned a target");
                helper.assertTrue(bear.action() == BeastEntity.Action.IDLE || bear.action().isAmbient(),
                        species + " proximity started an attack: " + bear.action());
            });
            x += 2;
        }
        helper.runAtTickTime(55L, helper::succeed);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void bearRevengeTargetsOnlyHurtIndividual(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 0.5D));
        BearEntity victim = spawnBear(helper, new BlockPos(4, 1, 4), BearSpecies.BROWN);
        BearEntity bystander = spawnBear(helper, new BlockPos(2, 1, 4), BearSpecies.AMERICAN_BLACK);
        victim.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.onEachTick(() -> {
            victim.getNavigation().stop();
            bystander.getNavigation().stop();
        });

        helper.runAtTickTime(10L, () -> {
            helper.assertTrue(victim.getTarget() == player,
                    "hurt bear did not target its attacker");
            helper.assertTrue(bystander.getTarget() == null,
                    "nearby bear joined another bear's revenge target");
            helper.assertValueEqual(victim.action(), BeastEntity.Action.ROAR,
                    "hurt bear did not enter its first-lock roar");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearAsianBlackBearHuntsPlayerWithoutDamage(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        // Place our mock closer than any foreign lingering player in the shared world.
        place(player, helper, new Vec3(4.5D, 1.0D, 2.8D));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.ASIAN_BLACK);
        helper.onEachTick(() -> {
            bear.getNavigation().stop();
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(bear.getTarget() == player,
                    "asian black bear did not hunt the nearest survival player");
            helper.assertValueEqual(bear.action(), BeastEntity.Action.ROAR,
                    "asian black bear first lock did not roar");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "asian black bear damaged player before any attack");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearAsianBlackBearHuntsLivestock(GameTestHelper helper) {
        ground(helper);
        // No mock player in this test; pig is the only nearby living target.
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 2));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.ASIAN_BLACK);
        helper.onEachTick(() -> {
            bear.getNavigation().stop();
            pig.getNavigation().stop();
            pig.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(bear.getTarget() == pig,
                    "asian black bear did not hunt the nearest pig");
            helper.assertValueEqual(bear.action(), BeastEntity.Action.ROAR,
                    "asian black bear livestock first lock did not roar");
            helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(),
                    "asian black bear damaged the pig before any attack");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearAsianBlackBearIgnoresModCreatureWildBoar(GameTestHelper helper) {
        ground(helper);
        WildBoarEntity boar = helper.spawn(ModEntityTypes.WILD_BOAR.get(), new BlockPos(5, 1, 4));
        BearEntity bear = spawnBear(helper, new BlockPos(3, 1, 4), BearSpecies.ASIAN_BLACK);
        helper.onEachTick(() -> {
            bear.getNavigation().stop();
            boar.getNavigation().stop();
            bear.setDeltaMovement(Vec3.ZERO);
            boar.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(60L, () -> {
            helper.assertTrue(bear.getTarget() == null,
                    "asian black bear targeted a mod creature");
            helper.assertValueEqual(boar.getHealth(), boar.getMaxHealth(),
                    "wild boar was hurt by a passive asian black bear");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearFirstLockRoarsAndIgnoresDamageDuringRoar(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.BROWN);
        bear.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);

        helper.assertTrue(bear.getTarget() == player, "bear did not target its attacker");
        helper.assertValueEqual(bear.action(), BeastEntity.Action.ROAR,
                "bear first lock did not play roar");
        helper.runAtTickTime(10L, () -> {
            bear.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertValueEqual(bear.action(), BeastEntity.Action.ROAR,
                    "incoming damage replaced roar before it finished");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 120)
    public static void bearDirectionalHurtAfterRoar(GameTestHelper helper) {
        ground(helper);
        // Bears are provoked from the front first (roar), then hit from the side after the roar.
        // Bear facing south (+Z): an attacker on +X is on its left -> head knocked right (HURT_RIGHT).
        ServerPlayer leftFront = survivalMock(helper);
        place(leftFront, helper, new Vec3(4.5D, 1.0D, 8.5D));
        ServerPlayer leftSide = survivalMock(helper);
        place(leftSide, helper, new Vec3(7.5D, 1.0D, 4.5D));
        BearEntity leftBear = spawnBear(helper, new BlockPos(4, 1, 4), BearSpecies.BROWN);

        ServerPlayer rightFront = survivalMock(helper);
        place(rightFront, helper, new Vec3(4.5D, 1.0D, 10.5D));
        ServerPlayer rightSide = survivalMock(helper);
        place(rightSide, helper, new Vec3(1.5D, 1.0D, 6.5D));
        BearEntity rightBear = spawnBear(helper, new BlockPos(4, 1, 6), BearSpecies.BROWN);

        leftBear.hurt(helper.getLevel().damageSources().playerAttack(leftFront), 1.0F);
        rightBear.hurt(helper.getLevel().damageSources().playerAttack(rightFront), 1.0F);

        helper.onEachTick(() -> {
            leftBear.getNavigation().stop();
            rightBear.getNavigation().stop();
            leftFront.setDeltaMovement(Vec3.ZERO);
            leftSide.setDeltaMovement(Vec3.ZERO);
            rightFront.setDeltaMovement(Vec3.ZERO);
            rightSide.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(55L, () -> {
            leftBear.hurt(helper.getLevel().damageSources().playerAttack(leftSide), 1.0F);
            rightBear.hurt(helper.getLevel().damageSources().playerAttack(rightSide), 1.0F);
            helper.assertValueEqual(leftBear.action(), BeastEntity.Action.HURT_RIGHT,
                    "left-side attacker did not play hurt_right");
            helper.assertValueEqual(rightBear.action(), BeastEntity.Action.HURT_LEFT,
                    "right-side attacker did not play hurt_left");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearSwipeHitsAtNineTicksPerSpecies(GameTestHelper helper) {
        ground(helper);
        int x = 1;
        for (BearSpecies species : BearSpecies.values()) {
            ServerPlayer player = survivalMock(helper);
            place(player, helper, new Vec3(x + 0.5D, 1.0D, 2.5D));
            BearEntity bear = spawnBear(helper, new BlockPos(x, 1, 4), species);
            bear.setTarget(player);
            bear.startAction(BeastEntity.Action.ATTACK_SWIPE);
            float initialHealth = player.getHealth();
            int[] hitTick = {-1};
            helper.onEachTick(() -> {
                if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                    hitTick[0] = (int) bear.actionTicks();
                }
                player.setDeltaMovement(Vec3.ZERO);
            });
            helper.runAtTickTime(8L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                    species + " swipe damaged before tick 9"));
            helper.runAtTickTime(12L, () -> {
                helper.assertValueEqual(hitTick[0], 9, species + " swipe hit tick");
                helper.assertValueEqual(player.getHealth(), 20.0F - species.swipeDamage(),
                        species + " swipe damage");
            });
            x += 2;
        }
        helper.runAtTickTime(15L, helper::succeed);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearRearSlamHitsAtSeventeenTicksPerSpecies(GameTestHelper helper) {
        ground(helper);
        int x = 1;
        for (BearSpecies species : BearSpecies.values()) {
            ServerPlayer player = survivalMock(helper);
            place(player, helper, new Vec3(x + 1.5D, 1.0D, 4.5D));
            BearEntity bear = spawnBear(helper, new BlockPos(x, 1, 4), species);
            bear.setTarget(player);
            bear.startAction(BeastEntity.Action.ATTACK_HEAVY);
            float initialHealth = player.getHealth();
            int[] hitTick = {-1};
            helper.onEachTick(() -> {
                if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                    hitTick[0] = (int) bear.actionTicks();
                }
                player.setDeltaMovement(Vec3.ZERO);
            });
            helper.runAtTickTime(16L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                    species + " rear slam damaged before tick 17"));
            helper.runAtTickTime(20L, () -> {
                helper.assertValueEqual(hitTick[0], 17, species + " rear slam hit tick");
                helper.assertValueEqual(player.getHealth(), 20.0F - species.rearDamage(),
                        species + " rear slam damage");
            });
            x += 2;
        }
        helper.runAtTickTime(25L, helper::succeed);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearSwipeAndHeavyCooldownsBlockOverlappingAttacks(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        // Keep the target in follow range but beyond every melee/pounce reach so auto-attacks never fire.
        place(player, helper, new Vec3(4.5D, 1.0D, 13.5D));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.BROWN);
        bear.setTarget(player);
        helper.onEachTick(() -> {
            bear.getNavigation().stop();
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.assertTrue(bear.startAction(BeastEntity.Action.ATTACK_SWIPE), "swipe did not start");
        helper.assertValueEqual(bear.swipeCooldown(), 20, "swipe cooldown did not begin immediately");

        helper.runAtTickTime(5L, () -> {
            helper.assertValueEqual(bear.action(), BeastEntity.Action.ATTACK_SWIPE,
                    "swipe left its action during wind-up");
            helper.assertTrue(!bear.startAction(BeastEntity.Action.ATTACK_SWIPE), "overlapping swipe accepted");
            helper.assertTrue(!bear.startAction(BeastEntity.Action.ATTACK_HEAVY), "overlapping heavy accepted");
        });
        helper.runAtTickTime(21L, () -> {
            helper.assertTrue(bear.action() != BeastEntity.Action.ATTACK_SWIPE,
                    "swipe action did not finish");
            helper.assertTrue(!bear.startAction(BeastEntity.Action.ATTACK_HEAVY),
                    "heavy started during post-attack recovery");
        });
        helper.runAtTickTime(31L, () -> {
            helper.assertTrue(bear.startAction(BeastEntity.Action.ATTACK_HEAVY),
                    "heavy did not start after swipe cooldown and recovery elapsed");
            helper.assertValueEqual(bear.heavyCooldown(), 80, "heavy cooldown did not begin immediately");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearDeathRemovesAtThirtySixTicks(GameTestHelper helper) {
        ground(helper);
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.BROWN);
        bear.startAction(BeastEntity.Action.ATTACK_SWIPE);
        helper.assertTrue(bear.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE),
                "bear did not accept lethal damage");
        helper.assertValueEqual(bear.action(), BeastEntity.Action.DEATH,
                "lethal damage did not enter death action");
        helper.runAtTickTime(32L, () -> {
            helper.assertTrue(!bear.isRemoved(), "bear was removed before death tick 36");
            helper.assertTrue(bear.deathTime < 36, "bear death timer passed 36 too early");
            helper.assertValueEqual(bear.action(), BeastEntity.Action.DEATH,
                    "death action was interrupted");
        });
        helper.runAtTickTime(36L, () -> {
            helper.assertTrue(bear.isRemoved(), "bear remained after death tick 36");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearDropsNoItemsAndExperienceOnce(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.BROWN);
        bear.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);

        int[] xp = {-1};
        helper.runAtTickTime(40L, () -> {
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(),
                    "bear dropped items");
            xp[0] = helper.getEntities(EntityType.EXPERIENCE_ORB).stream()
                    .mapToInt(ExperienceOrb::getValue).sum();
            helper.assertTrue(xp[0] >= 1 && xp[0] <= 3,
                    "experience reward outside one-to-three: " + xp[0]);
        });
        helper.runAtTickTime(60L, () -> {
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(),
                    "bear duplicated items after death");
            int later = helper.getEntities(EntityType.EXPERIENCE_ORB).stream()
                    .mapToInt(ExperienceOrb::getValue).sum();
            helper.assertValueEqual(later, xp[0], "bear experience was duplicated after death");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void bearReloadClearsActionKeepsCooldowns(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        BearEntity original = spawnBear(helper, CENTER, BearSpecies.BROWN);
        original.setTarget(player);
        original.startAction(BeastEntity.Action.ATTACK_SWIPE);
        int savedCooldown = original.swipeCooldown();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        BearEntity restored = spawnBear(helper, new BlockPos(6, 1, 6), BearSpecies.BROWN);
        restored.load(saved);
        helper.assertValueEqual(restored.action(), BeastEntity.Action.IDLE,
                "reloaded bear kept a transient action");
        helper.assertValueEqual(restored.actionTicks(), 0L, "reloaded bear action ticks");
        helper.assertTrue(restored.getTarget() == null, "reloaded bear kept its target");
        helper.assertTrue(restored.getDeltaMovement().lengthSqr() < 1.0E-6D,
                "reloaded bear kept attack movement");
        helper.assertValueEqual(restored.swipeCooldown(), savedCooldown,
                "reloaded bear lost swipe cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearDoesNotAcquireTargetOnPeaceful(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.ASIAN_BLACK);
        Difficulty previous = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        try {
            bear.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(bear.getTarget() == null,
                    "asian black bear acquired a target on peaceful");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "peaceful asian black bear damaged player");
        } finally {
            helper.getLevel().getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearClearsTargetOutsideFollowRange(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.BROWN);
        bear.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.runAtTickTime(1L, () -> place(player, helper, new Vec3(4.5D, 1.0D, -12.0D)));
        helper.runAtTickTime(10L, () -> {
            helper.assertTrue(bear.getTarget() == null,
                    "bear kept a target outside its follow range");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 240)
    public static void beastNightSleepChainTransitions(GameTestHelper helper) {
        ground(helper);
        // Both species are driven together because dayTime is global in the shared GameTest world.
        BearEntity bear = spawnBear(helper, new BlockPos(2, 1, 4), BearSpecies.BROWN);
        TigerEntity tiger = helper.spawn(ModEntityTypes.TIGER.get(), new BlockPos(6, 1, 4));
        helper.getLevel().setDayTime(13000L);
        bear.startAction(BeastEntity.Action.LIE_DOWN);
        tiger.startAction(BeastEntity.Action.LIE_DOWN);
        helper.onEachTick(() -> {
            bear.getNavigation().stop();
            tiger.getNavigation().stop();
        });

        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS, () ->
                helper.assertValueEqual(tiger.action(), BeastEntity.Action.LIE,
                        "tiger did not transition to lie after lie-down"));
        helper.runAtTickTime(BearEntity.LIE_DOWN_TICKS, () ->
                helper.assertValueEqual(bear.action(), BeastEntity.Action.LIE,
                        "bear did not transition to lie after lie-down"));
        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS + 80L, () ->
                helper.assertValueEqual(tiger.action(), BeastEntity.Action.SLEEP,
                        "tiger did not transition to sleep after lying"));
        helper.runAtTickTime(BearEntity.LIE_DOWN_TICKS + 80L, () ->
                helper.assertValueEqual(bear.action(), BeastEntity.Action.SLEEP,
                        "bear did not transition to sleep after lying"));

        helper.runAtTickTime(110L, () -> helper.getLevel().setDayTime(1000L));
        helper.runAtTickTime(111L, () -> {
            helper.assertValueEqual(bear.action(), BeastEntity.Action.GET_UP,
                    "bear did not get up at day");
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.GET_UP,
                    "tiger did not get up at day");
        });
        helper.runAtTickTime(111L + TigerEntity.GET_UP_TICKS + 1L, () ->
                helper.assertValueEqual(tiger.action(), BeastEntity.Action.IDLE,
                        "tiger did not return to idle after getting up"));
        helper.runAtTickTime(111L + BearEntity.GET_UP_TICKS + 1L, () -> {
            helper.assertValueEqual(bear.action(), BeastEntity.Action.IDLE,
                    "bear did not return to idle after getting up");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 180)
    public static void bearDaytimeSitReturnsToIdleAfterOneHundredSixtyTicks(GameTestHelper helper) {
        ground(helper);
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.BROWN);
        bear.startAction(BeastEntity.Action.SIT);
        helper.onEachTick(() -> bear.getNavigation().stop());

        helper.runAtTickTime(80L, () -> helper.assertValueEqual(bear.action(), BeastEntity.Action.SIT,
                "bear sit ended early"));
        helper.runAtTickTime(160L, () -> {
            helper.assertValueEqual(bear.action(), BeastEntity.Action.IDLE,
                    "bear sit did not return to idle after 160 ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearHurtDuringSitSnapsToCombat(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        BearEntity bear = spawnBear(helper, CENTER, BearSpecies.BROWN);
        bear.startAction(BeastEntity.Action.SIT);
        helper.onEachTick(() -> bear.getNavigation().stop());

        helper.runAtTickTime(20L, () -> {
            bear.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(bear.getTarget() == player,
                    "hurt during sit did not assign target");
            helper.assertValueEqual(bear.action(), BeastEntity.Action.ROAR,
                    "hurt during sit did not snap to first-lock roar");
            helper.assertTrue(bear.action() != BeastEntity.Action.SIT,
                    "bear stayed in sit after being hurt");
            helper.succeed();
        });
    }

    private static BearEntity spawnBear(GameTestHelper helper, BlockPos pos, BearSpecies species) {
        return switch (species) {
            case BROWN -> helper.spawn(ModEntityTypes.BROWN_BEAR.get(), pos);
            case ASIAN_BLACK -> helper.spawn(ModEntityTypes.ASIAN_BLACK_BEAR.get(), pos);
            case AMERICAN_BLACK -> helper.spawn(ModEntityTypes.AMERICAN_BLACK_BEAR.get(), pos);
            case ALBINO -> helper.spawn(ModEntityTypes.ALBINO_BEAR.get(), pos);
        };
    }

    private static void ground(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
    }

    private static void place(ServerPlayer player, GameTestHelper helper, Vec3 relative) {
        Vec3 absolute = helper.absoluteVec(relative);
        player.moveTo(absolute.x, absolute.y, absolute.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static ServerPlayer survivalMock(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "gzr-bear-gametest"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {return false;}

            @Override
            public boolean isCreative() {return false;}
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(@NotNull Packet<?> packet) {}

            @Override
            public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener listener) {}
        };
        helper.getLevel().addNewPlayer(player);
        player.getAbilities().instabuild = false;
        for (int tick = 0; tick < 61; tick++) player.tick();
        return player;
    }
}

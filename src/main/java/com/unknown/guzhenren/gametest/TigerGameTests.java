package com.unknown.guzhenren.gametest;

import com.mojang.authlib.GameProfile;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.BeastEntity;
import com.unknown.guzhenren.entity.TigerEntity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime coverage for the tiger's proactive hunting, pounce, attacks, death and persistence.
 *
 * <p>Peaceful proactive hunting is unpinned because difficulty is server-global in the shared GameTest world;
 * only the synchronous "retaliation is blocked on Peaceful" seam is tested.
 */
@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TigerGameTests {

    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final float PLAYER_MAX_HEALTH = 20.0F;

    private TigerGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void tigerRegistrationUsesConfiguredShapeAndAttributes(GameTestHelper helper) {
        ground(helper);
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        helper.assertValueEqual(tiger.getBbWidth(), 1.3F, "tiger width");
        helper.assertValueEqual(tiger.getBbHeight(), 1.4F, "tiger height");
        helper.assertValueEqual(tiger.getMaxHealth(), 48.0F, "tiger max health");
        helper.assertValueEqual(tiger.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.28D,
                "tiger movement speed");
        helper.assertValueEqual(tiger.getAttributeValue(Attributes.FOLLOW_RANGE), 24.0D,
                "tiger follow range");

        TigerEntity white = spawnTiger(helper, new BlockPos(6, 1, 4), ModEntityTypes.WHITE_TIGER);
        helper.assertValueEqual(white.getBbWidth(), 1.3F, "white tiger width");
        helper.assertValueEqual(white.getBbHeight(), 1.4F, "white tiger height");
        helper.assertValueEqual(white.getMaxHealth(), 48.0F, "white tiger max health");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerHuntsPlayerProactively(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        // Place our mock closer than any foreign lingering player in the shared world.
        place(player, helper, new Vec3(4.5D, 1.0D, 2.8D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        helper.onEachTick(() -> {
            tiger.getNavigation().stop();
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(tiger.getTarget() == player,
                    "tiger did not hunt the nearest survival player");
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.ROAR,
                    "tiger first lock did not roar");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "tiger damaged player before any attack");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerHuntsLivestock(GameTestHelper helper) {
        ground(helper);
        // No mock player in this test; pig is the only nearby living target.
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 2));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        helper.onEachTick(() -> {
            tiger.getNavigation().stop();
            pig.getNavigation().stop();
            pig.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(tiger.getTarget() == pig,
                    "tiger did not hunt the nearest pig");
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.ROAR,
                    "tiger livestock first lock did not roar");
            helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(),
                    "tiger damaged the pig before any attack");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerFirstLockRoarsAndIgnoresDamageDuringRoar(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);

        helper.assertTrue(tiger.getTarget() == player, "tiger did not target its attacker");
        helper.assertValueEqual(tiger.action(), BeastEntity.Action.ROAR,
                "tiger first lock did not play roar");
        helper.runAtTickTime(10L, () -> {
            tiger.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.ROAR,
                    "incoming damage replaced roar before it finished");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 120)
    public static void tigerDirectionalHurtAfterRoar(GameTestHelper helper) {
        ground(helper);
        // Tigers are provoked from the front first (roar), then hit from the side after the roar.
        ServerPlayer leftFront = survivalMock(helper);
        place(leftFront, helper, new Vec3(4.5D, 1.0D, 13.5D));
        ServerPlayer leftSide = survivalMock(helper);
        place(leftSide, helper, new Vec3(7.5D, 1.0D, 4.5D));
        TigerEntity leftTiger = spawnTiger(helper, new BlockPos(4, 1, 4), ModEntityTypes.TIGER);

        ServerPlayer rightFront = survivalMock(helper);
        place(rightFront, helper, new Vec3(4.5D, 1.0D, 15.5D));
        ServerPlayer rightSide = survivalMock(helper);
        place(rightSide, helper, new Vec3(1.5D, 1.0D, 6.5D));
        TigerEntity rightTiger = spawnTiger(helper, new BlockPos(4, 1, 6), ModEntityTypes.TIGER);

        leftTiger.hurt(helper.getLevel().damageSources().playerAttack(leftFront), 1.0F);
        rightTiger.hurt(helper.getLevel().damageSources().playerAttack(rightFront), 1.0F);

        helper.onEachTick(() -> {
            leftTiger.getNavigation().stop();
            rightTiger.getNavigation().stop();
            leftFront.setDeltaMovement(Vec3.ZERO);
            leftSide.setDeltaMovement(Vec3.ZERO);
            rightFront.setDeltaMovement(Vec3.ZERO);
            rightSide.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(45L, () -> {
            leftTiger.hurt(helper.getLevel().damageSources().playerAttack(leftSide), 1.0F);
            rightTiger.hurt(helper.getLevel().damageSources().playerAttack(rightSide), 1.0F);
            helper.assertValueEqual(leftTiger.action(), BeastEntity.Action.HURT_RIGHT,
                    "left-side attacker did not play hurt_right");
            helper.assertValueEqual(rightTiger.action(), BeastEntity.Action.HURT_LEFT,
                    "right-side attacker did not play hurt_left");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerSwipeHitsAtEightTicks(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.setTarget(player);
        tiger.startAction(BeastEntity.Action.ATTACK_SWIPE);
        float initialHealth = player.getHealth();
        int[] hitTick = {-1};
        helper.onEachTick(() -> {
            if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                hitTick[0] = (int) tiger.actionTicks();
            }
            player.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(7L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                "tiger swipe damaged before tick 8"));
        helper.runAtTickTime(12L, () -> {
            helper.assertValueEqual(hitTick[0], 8, "tiger swipe hit tick");
            helper.assertValueEqual(player.getHealth(), 12.0F, "tiger swipe damage");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerPounceHitsBetweenTwelveAndEighteenTicks(GameTestHelper helper) {
        ground(helper);
        TigerEntity tiger = spawnTiger(helper, new BlockPos(1, 1, 4), ModEntityTypes.TIGER);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(6.0D, 1.0D, 4.5D));
        tiger.setTarget(player);
        tiger.startAction(BeastEntity.Action.ATTACK_HEAVY);

        Vec3 expectedDirection = player.position().subtract(tiger.position()).normalize();
        float initialHealth = player.getHealth();
        int[] hitTick = {-1};
        Vec3[] capturedKnockback = {Vec3.ZERO};
        helper.onEachTick(() -> {
            if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                hitTick[0] = (int) tiger.actionTicks();
                capturedKnockback[0] = player.getDeltaMovement();
            }
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(11L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                "tiger pounce damaged before its leap"));
        helper.runAtTickTime(18L, () -> {
            helper.assertTrue(hitTick[0] >= 12 && hitTick[0] <= 18,
                    "tiger pounce hit outside ticks 12-18: " + hitTick[0]);
            helper.assertValueEqual(player.getHealth(), 8.0F, "tiger pounce damage");
            Vec3 move = capturedKnockback[0];
            double dot = move.x * expectedDirection.x + move.z * expectedDirection.z;
            helper.assertTrue(dot > 0.0D, "tiger pounce knockback was not along leap direction");
            helper.assertTrue(move.horizontalDistanceSqr() > 1.0E-6D,
                    "tiger pounce did not apply horizontal knockback");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerPounceStoppedByTwoHighWall(GameTestHelper helper) {
        ground(helper);
        for (int z = 3; z <= 5; z++) {
            helper.setBlock(new BlockPos(4, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 2, z), Blocks.STONE);
        }
        TigerEntity tiger = spawnTiger(helper, new BlockPos(1, 1, 4), ModEntityTypes.TIGER);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(6.0D, 1.0D, 4.5D));
        tiger.setTarget(player);
        tiger.startAction(BeastEntity.Action.ATTACK_HEAVY);

        helper.onEachTick(() -> player.setDeltaMovement(Vec3.ZERO));
        // The leap fires at tick 12 and hits the wall mid-flight, so the abort lands around tick 14.
        helper.runAtTickTime(16L, () -> helper.assertTrue(tiger.action() != BeastEntity.Action.ATTACK_HEAVY,
                "tiger pounce stayed active after hitting the wall"));
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "tiger pounce hit through a two-high wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerSwipeAndHeavyCooldownsBlockOverlappingAttacks(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        // Keep the target in follow range but beyond swipe/pounce reach so auto-attacks never fire.
        place(player, helper, new Vec3(4.5D, 1.0D, 13.5D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.setTarget(player);
        helper.onEachTick(() -> {
            tiger.getNavigation().stop();
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.assertTrue(tiger.startAction(BeastEntity.Action.ATTACK_SWIPE), "tiger swipe did not start");
        helper.assertValueEqual(tiger.swipeCooldown(), 20, "tiger swipe cooldown did not begin immediately");

        helper.runAtTickTime(5L, () -> {
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.ATTACK_SWIPE,
                    "tiger swipe left its action during wind-up");
            helper.assertTrue(!tiger.startAction(BeastEntity.Action.ATTACK_SWIPE), "overlapping tiger swipe accepted");
            helper.assertTrue(!tiger.startAction(BeastEntity.Action.ATTACK_HEAVY), "overlapping tiger heavy accepted");
        });
        helper.runAtTickTime(17L, () -> {
            helper.assertTrue(tiger.action() != BeastEntity.Action.ATTACK_SWIPE,
                    "tiger swipe action did not finish");
            helper.assertTrue(!tiger.startAction(BeastEntity.Action.ATTACK_HEAVY),
                    "tiger heavy started during post-attack recovery");
        });
        helper.runAtTickTime(27L, () -> {
            helper.assertTrue(tiger.startAction(BeastEntity.Action.ATTACK_HEAVY),
                    "tiger heavy did not start after swipe cooldown and recovery elapsed");
            helper.assertValueEqual(tiger.heavyCooldown(), 80, "tiger heavy cooldown did not begin immediately");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerDeathRemovesAtThirtyTwoTicks(GameTestHelper helper) {
        ground(helper);
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.startAction(BeastEntity.Action.ATTACK_SWIPE);
        helper.assertTrue(tiger.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE),
                "tiger did not accept lethal damage");
        helper.assertValueEqual(tiger.action(), BeastEntity.Action.DEATH,
                "lethal damage did not enter death action");
        helper.runAtTickTime(28L, () -> {
            helper.assertTrue(!tiger.isRemoved(), "tiger was removed before death tick 32");
            helper.assertTrue(tiger.deathTime < 32, "tiger death timer passed 32 too early");
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.DEATH,
                    "death action was interrupted");
        });
        helper.runAtTickTime(32L, () -> {
            helper.assertTrue(tiger.isRemoved(), "tiger remained after death tick 32");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerDropsNoItemsAndExperienceOnce(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);

        int[] xp = {-1};
        helper.runAtTickTime(40L, () -> {
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(),
                    "tiger dropped items");
            xp[0] = helper.getEntities(EntityType.EXPERIENCE_ORB).stream()
                    .mapToInt(ExperienceOrb::getValue).sum();
            helper.assertTrue(xp[0] >= 1 && xp[0] <= 3,
                    "tiger experience reward outside one-to-three: " + xp[0]);
        });
        helper.runAtTickTime(60L, () -> {
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(),
                    "tiger duplicated items after death");
            int later = helper.getEntities(EntityType.EXPERIENCE_ORB).stream()
                    .mapToInt(ExperienceOrb::getValue).sum();
            helper.assertValueEqual(later, xp[0], "tiger experience was duplicated after death");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void tigerReloadClearsActionKeepsCooldowns(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        TigerEntity original = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        original.setTarget(player);
        original.startAction(BeastEntity.Action.ATTACK_SWIPE);
        int savedCooldown = original.swipeCooldown();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        TigerEntity restored = spawnTiger(helper, new BlockPos(6, 1, 6), ModEntityTypes.TIGER);
        restored.load(saved);
        helper.assertValueEqual(restored.action(), BeastEntity.Action.IDLE,
                "reloaded tiger kept a transient action");
        helper.assertValueEqual(restored.actionTicks(), 0L, "reloaded tiger action ticks");
        helper.assertTrue(restored.getTarget() == null, "reloaded tiger kept its target");
        helper.assertTrue(restored.getDeltaMovement().lengthSqr() < 1.0E-6D,
                "reloaded tiger kept attack movement");
        helper.assertValueEqual(restored.swipeCooldown(), savedCooldown,
                "reloaded tiger lost swipe cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerDoesNotAcquireTargetOnPeaceful(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        Difficulty previous = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        try {
            tiger.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(tiger.getTarget() == null,
                    "tiger acquired a target on peaceful");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "peaceful tiger damaged player");
        } finally {
            helper.getLevel().getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerClearsTargetOutsideFollowRange(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.runAtTickTime(1L, () -> place(player, helper, new Vec3(4.5D, 1.0D, -20.0D)));
        helper.runAtTickTime(10L, () -> {
            helper.assertTrue(tiger.getTarget() == null,
                    "tiger kept a target outside its follow range");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 180)
    public static void tigerNightSleepChainTransitions(GameTestHelper helper) {
        ground(helper);
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        helper.getLevel().setDayTime(13000L);
        tiger.startAction(BeastEntity.Action.LIE_DOWN);
        helper.onEachTick(() -> tiger.getNavigation().stop());

        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS, () ->
                helper.assertValueEqual(tiger.action(), BeastEntity.Action.LIE,
                        "tiger did not transition to lie after lie-down"));
        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS + 80L, () ->
                helper.assertValueEqual(tiger.action(), BeastEntity.Action.SLEEP,
                        "tiger did not transition to sleep after lying"));
        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS + 82L, () ->
                helper.getLevel().setDayTime(1000L));
        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS + 83L, () ->
                helper.assertValueEqual(tiger.action(), BeastEntity.Action.GET_UP,
                        "tiger did not get up at day"));
        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS + 83L + TigerEntity.GET_UP_TICKS + 1L, () -> {
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.IDLE,
                    "tiger did not return to idle after getting up");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 180)
    public static void tigerDaytimeSitReturnsToIdleAfterOneHundredSixtyTicks(GameTestHelper helper) {
        ground(helper);
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.startAction(BeastEntity.Action.SIT);
        helper.onEachTick(() -> tiger.getNavigation().stop());

        helper.runAtTickTime(80L, () -> helper.assertValueEqual(tiger.action(), BeastEntity.Action.SIT,
                "tiger sit ended early"));
        helper.runAtTickTime(160L, () -> {
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.IDLE,
                    "tiger sit did not return to idle after 160 ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerHurtDuringSitSnapsToCombat(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        TigerEntity tiger = spawnTiger(helper, CENTER, ModEntityTypes.TIGER);
        tiger.startAction(BeastEntity.Action.SIT);
        helper.onEachTick(() -> tiger.getNavigation().stop());

        helper.runAtTickTime(20L, () -> {
            tiger.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(tiger.getTarget() == player,
                    "hurt during sit did not assign target");
            helper.assertValueEqual(tiger.action(), BeastEntity.Action.ROAR,
                    "hurt during sit did not snap to first-lock roar");
            helper.assertTrue(tiger.action() != BeastEntity.Action.SIT,
                    "tiger stayed in sit after being hurt");
            helper.succeed();
        });
    }

    private static TigerEntity spawnTiger(GameTestHelper helper, BlockPos pos,
                                          net.neoforged.neoforge.registries.DeferredHolder<
                                                  net.minecraft.world.entity.EntityType<?>,
                                                  net.minecraft.world.entity.EntityType<TigerEntity>> holder) {
        return helper.spawn(holder.get(), pos);
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
                new GameProfile(UUID.randomUUID(), "gzr-tiger-gametest"), false);
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

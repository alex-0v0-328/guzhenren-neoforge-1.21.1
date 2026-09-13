package com.unknown.guzhenren.gametest;

import com.mojang.authlib.GameProfile;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.WildBoarEntity;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Runtime coverage for the Wild Boar's neutral AI, server-side attacks, and death lifecycle. */
@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildBoarGameTests {

    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final float PLAYER_MAX_HEALTH = 20.0F;

    private WildBoarGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarRegistrationUsesConfiguredShapeAndAttributes(GameTestHelper helper) {
        ground(helper);
        WildBoarEntity boar = spawn(helper, CENTER);
        helper.assertValueEqual(boar.getMaxHealth(), 30.0F, "wild boar max health");
        helper.assertValueEqual(boar.getBbWidth(), 1.1F, "wild boar width");
        helper.assertValueEqual(boar.getBbHeight(), 1.2F, "wild boar height");
        helper.assertValueEqual(boar.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.25D,
                "wild boar movement speed");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarDoesNotAttackFromProximity(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.0D));
        WildBoarEntity boar = spawn(helper, CENTER);
        helper.onEachTick(() -> {
            boar.getNavigation().stop();
            boar.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(50L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "neutral proximity damaged player");
            helper.assertTrue(boar.getTarget() == null, "neutral proximity assigned a target");
            helper.assertTrue(boar.action() == WildBoarEntity.Action.IDLE
                            || boar.action() == WildBoarEntity.Action.GRAZE,
                    "neutral proximity started an attack: " + boar.action());
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarDoesNotAttackOnPeaceful(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.0D));
        WildBoarEntity boar = spawn(helper, CENTER);
        Difficulty previous = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        try {
            boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(boar.getTarget() == null, "peaceful wild boar assigned a revenge target");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH, "peaceful revenge damaged player");
        } finally {
            helper.getLevel().getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarRevengeTargetsOnlyHurtIndividual(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 0.5D));
        WildBoarEntity victim = spawn(helper, new BlockPos(4, 1, 4));
        WildBoarEntity bystander = spawn(helper, new BlockPos(2, 1, 4));
        victim.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.onEachTick(() -> {
            victim.getNavigation().stop();
            bystander.getNavigation().stop();
        });

        helper.runAtTickTime(10L, () -> {
            helper.assertTrue(victim.getTarget() == player,
                    "hurt wild boar did not target its attacker");
            helper.assertTrue(bystander.getTarget() == null,
                    "nearby wild boar joined another boar's revenge target");
            helper.assertTrue(victim.action() == WildBoarEntity.Action.ALERT
                            || victim.action() == WildBoarEntity.Action.ATTACK_CHARGE
                            || victim.action() == WildBoarEntity.Action.ATTACK_TOSS,
                    "hurt wild boar did not enter its revenge action: " + victim.action());
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarProjectileDamageIdentifiesShooter(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 0.5D));
        WildBoarEntity boar = spawn(helper, CENTER);
        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        arrow.setOwner(player);
        helper.assertTrue(boar.hurt(helper.getLevel().damageSources().arrow(arrow, player), 1.0F),
                "projectile did not damage wild boar");
        helper.onEachTick(() -> boar.getNavigation().stop());

        helper.runAtTickTime(8L, () -> {
            helper.assertTrue(boar.getTarget() == player,
                    "projectile damage did not assign the projectile shooter");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 140)
    public static void wildBoarClearsTargetOutsideFollowRange(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 0.5D));
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.runAtTickTime(1L, () -> place(player, helper, new Vec3(4.5D, 1.0D, -14.0D)));
        helper.runAtTickTime(70L, () -> {
            helper.assertTrue(boar.getTarget() == null,
                    "wild boar kept a target outside its follow range");
            // A disengaged boar may already have resumed ordinary grazing.
            helper.assertTrue(boar.action() == WildBoarEntity.Action.IDLE
                            || boar.action() == WildBoarEntity.Action.GRAZE,
                    "wild boar kept an attack after its target became invalid: " + boar.action());
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarChargeHitsOnceDuringChargeWindow(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(7.5D, 1.0D, 4.5D));
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_CHARGE);
        float initialHealth = player.getHealth();
        int[] hitTick = {-1};
        helper.onEachTick(() -> {
            if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                hitTick[0] = (int)helper.getTick();
                place(player, helper, new Vec3(8.5D, 1.0D, 0.5D));
                player.setDeltaMovement(Vec3.ZERO);
            }
        });
        helper.runAtTickTime(5L, () -> {
            helper.assertValueEqual(player.getHealth(), initialHealth,
                    "charge damaged before its six tick wind-up");
            helper.assertValueEqual(boar.action(), WildBoarEntity.Action.ATTACK_CHARGE,
                    "charge left its action during wind-up");
            helper.assertTrue(boar.actionTicks() >= 5, "charge wind-up tick counter reset");
        });
        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(hitTick[0] >= 6 && hitTick[0] <= 16,
                    "charge hit outside ticks 6-16: " + hitTick[0]);
            helper.assertValueEqual(player.getHealth(), 12.0F,
                    "charge damage or repeated hit count");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarTossHitsAtNineTicks(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_TOSS);
        float initialHealth = player.getHealth();
        int[] hitTick = {-1};
        helper.onEachTick(() -> {
            if (hitTick[0] < 0) {
                if (player.getHealth() < initialHealth) {
                    hitTick[0] = (int)boar.actionTicks();
                } else {
                    place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
                    player.setDeltaMovement(Vec3.ZERO);
                }
            }
        });
        helper.runAtTickTime(8L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                "toss damaged before its 0.45 second hit point"));
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(hitTick[0], 9, "toss action hit tick");
            helper.assertValueEqual(player.getHealth(), 14.0F, "toss damage");
            helper.assertTrue(player.getDeltaMovement().y() > 0.0D,
                    "toss did not apply upward knockback");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarChargeStopsAtWall(GameTestHelper helper) {
        ground(helper);
        for (int z = 3; z <= 5; z++) {
            helper.setBlock(new BlockPos(4, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 2, z), Blocks.STONE);
        }
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(7.5D, 1.0D, 4.5D));
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_CHARGE);
        helper.onEachTick(() -> player.setDeltaMovement(Vec3.ZERO));
        helper.runAtTickTime(35L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "charge hit through a solid wall");
            helper.assertTrue(boar.action() != WildBoarEntity.Action.ATTACK_CHARGE,
                    "charge remained active after colliding with a wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarTossHonorsCanceledHurt(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        player.setInvulnerable(true);
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_TOSS);
        helper.onEachTick(() -> {
            place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
            player.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "canceled toss still damaged target");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6D,
                    "canceled toss still applied knockback");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarTossHonorsKnockbackResistance(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_TOSS);
        helper.onEachTick(() -> {
            if (player.getHealth() >= PLAYER_MAX_HEALTH) {
                place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
                player.setDeltaMovement(Vec3.ZERO);
            }
        });
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(player.getHealth(), 14.0F,
                    "knockback resistant target did not take toss damage");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6D,
                    "toss ignored full knockback resistance");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarReloadClearsActionKeepsCooldown(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        WildBoarEntity original = spawn(helper, CENTER);
        original.setTarget(player);
        original.startAction(WildBoarEntity.Action.ATTACK_CHARGE);
        int chargeCooldown = original.chargeCooldown();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        WildBoarEntity restored = spawn(helper, new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.assertValueEqual(restored.action(), WildBoarEntity.Action.IDLE,
                "reloaded wild boar kept a transient action");
        helper.assertValueEqual(restored.actionTicks(), 0L, "reloaded wild boar action ticks");
        helper.assertTrue(restored.getTarget() == null, "reloaded wild boar kept its target");
        helper.assertTrue(restored.getDeltaMovement().horizontalDistanceSqr() < 1.0E-6D,
                "reloaded wild boar kept charge movement");
        helper.assertValueEqual(restored.chargeCooldown(), chargeCooldown,
                "reloaded wild boar lost charge cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarDeathPersistsUntilThirtyTwoTicks(GameTestHelper helper) {
        ground(helper);
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.startAction(WildBoarEntity.Action.ATTACK_CHARGE);
        helper.assertTrue(boar.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE),
                "wild boar did not accept lethal damage");
        helper.assertValueEqual(boar.action(), WildBoarEntity.Action.DEATH,
                "lethal damage did not enter death action");
        helper.runAtTickTime(28L, () -> {
            helper.assertTrue(!boar.isRemoved(), "wild boar was removed before death tick 32");
            helper.assertTrue(boar.deathTime < 32, "wild boar death timer passed 32 too early");
            helper.assertValueEqual(boar.action(), WildBoarEntity.Action.DEATH,
                    "death action was interrupted");
        });
        helper.runAtTickTime(32L, () -> {
            helper.assertTrue(boar.isRemoved(), "wild boar remained after death tick 32");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarDropsRawCookedLootingLootAndExperienceOnce(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 8.0D));
        Holder<Enchantment> looting = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(looting, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);

        WildBoarEntity raw = spawn(helper, new BlockPos(1, 1, 2));
        WildBoarEntity cooked = spawn(helper, new BlockPos(7, 1, 2));
        raw.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
        cooked.igniteForSeconds(100);
        cooked.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);

        int[] itemCount = {-1};
        int[] experience = {-1};
        helper.runAtTickTime(2L, () -> {
            List<ItemEntity> items = helper.getEntities(EntityType.ITEM);
            int rawCount = itemCount(items, Items.PORKCHOP);
            int cookedCount = itemCount(items, Items.COOKED_PORKCHOP);
            helper.assertTrue(rawCount >= 1 && rawCount <= 6,
                    "raw porkchop count outside base plus looting range: " + rawCount);
            helper.assertTrue(cookedCount >= 1 && cookedCount <= 6,
                    "cooked porkchop count outside base plus looting range: " + cookedCount);
            helper.assertTrue(items.stream().allMatch(item -> item.getItem().is(Items.PORKCHOP)
                            || item.getItem().is(Items.COOKED_PORKCHOP)),
                    "wild boar dropped an unexpected item");
            experience[0] = experience(helper);
            helper.assertTrue(experience[0] >= 2 && experience[0] <= 6,
                    "experience reward outside one-to-three per boar: " + experience[0]);
            itemCount[0] = items.stream().mapToInt(item -> item.getItem().getCount()).sum();
        });
        helper.runAtTickTime(45L, () -> {
            helper.assertValueEqual(helper.getEntities(EntityType.ITEM).stream()
                            .mapToInt(item -> item.getItem().getCount()).sum(), itemCount[0],
                    "wild boar loot was duplicated after death animation");
            helper.assertValueEqual(experience(helper), experience[0],
                    "wild boar experience was duplicated after death animation");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarChasesAfterOneAlert(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(7.5D, 1.0D, 4.5D));
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        CompoundTag cooldown = new CompoundTag();
        boar.saveWithoutId(cooldown);
        cooldown.putInt("WildBoarChargeCooldown", 100);
        boar.load(cooldown);
        boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        Vec3 start = boar.position();
        int alertSequence = boar.actionSequence();
        helper.runAtTickTime(14L, () -> helper.assertValueEqual(boar.action(), WildBoarEntity.Action.ALERT,
                "initial alert was skipped"));
        helper.runAtTickTime(50L, () -> {
            helper.assertTrue(boar.position().distanceTo(start) > 1.0D, "retaliating boar did not chase");
            helper.assertTrue(boar.action() != WildBoarEntity.Action.ALERT
                    && boar.actionSequence() > alertSequence, "alert replayed instead of chasing");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void wildBoarChargeKeepsRecoveryAnimationAndCooldown(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 4.5D));
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        helper.assertTrue(boar.startAction(WildBoarEntity.Action.ATTACK_CHARGE), "charge did not start");
        helper.assertValueEqual(boar.chargeCooldown(), 100, "charge cooldown did not begin immediately");
        helper.runAtTickTime(18L, () -> {
            helper.assertValueEqual(player.getHealth(), 12.0F, "charge did not hit once");
            helper.assertValueEqual(boar.action(), WildBoarEntity.Action.ATTACK_CHARGE,
                    "hit skipped the charge recovery animation");
            helper.assertTrue(!boar.startAction(WildBoarEntity.Action.ATTACK_TOSS), "overlapping attack accepted");
        });
        helper.runAtTickTime(28L, () -> {
            helper.assertTrue(!boar.startAction(WildBoarEntity.Action.ATTACK_TOSS), "recovery gap was bypassed");
            helper.assertTrue(boar.chargeCooldown() > 60, "charge cooldown drained too quickly");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarChargeStopsBeforeCliff(GameTestHelper helper) {
        ground(helper);
        for (int x = 4; x < 7; x++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.AIR);
        }
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(7.5D, 1.0D, 4.5D));
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_CHARGE);
        helper.runAtTickTime(20L, () -> {
            helper.assertTrue(boar.getX() + boar.getBbWidth() / 2.0D
                    <= helper.absoluteVec(new Vec3(4, 1, 4)).x, "charge crossed the cliff edge");
            helper.assertValueEqual(player.getHealth(), 20.0F, "charge hit across a cliff");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 140)
    public static void wildBoarLosesOccludedTargetAndTossCannotHitThroughWall(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_TOSS);
        for (int x = 0; x < 9; x++) {
            for (int y = 1; y < 4; y++) helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        }
        helper.onEachTick(() -> {
            boar.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.0D, 4.5D)));
            boar.getNavigation().stop();
            boar.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(20L, () -> helper.assertValueEqual(player.getHealth(), 20.0F,
                "toss ignored line of sight"));
        helper.runAtTickTime(90L, () -> helper.assertTrue(boar.getTarget() == player,
                "target cleared before five seconds of lost sight"));
        helper.runAtTickTime(105L, () -> {
            helper.assertTrue(boar.getTarget() == null, "occluded target never cleared");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarDeathCancelsPendingHit(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.setTarget(player);
        boar.startAction(WildBoarEntity.Action.ATTACK_TOSS);
        helper.runAtTickTime(5L, () -> boar.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE));
        helper.runAtTickTime(18L, () -> {
            helper.assertValueEqual(player.getHealth(), 20.0F, "dead boar delivered a pending hit");
            helper.assertValueEqual(boar.action(), WildBoarEntity.Action.DEATH, "death animation was overridden");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarGrazeIsInterruptedWithoutChangingGrass(GameTestHelper helper) {
        ground(helper);
        helper.setBlock(CENTER.below(), Blocks.GRASS_BLOCK);
        WildBoarEntity boar = spawn(helper, CENTER);
        boar.startAction(WildBoarEntity.Action.GRAZE);
        helper.runAtTickTime(25L, () -> {
            helper.assertValueEqual(boar.action(), WildBoarEntity.Action.GRAZE, "graze ended early");
            ServerPlayer player = survivalMock(helper);
            place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
            boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertValueEqual(boar.action(), WildBoarEntity.Action.ALERT, "hurt did not interrupt grazing");
            helper.assertBlockState(CENTER.below(), state -> state.is(Blocks.GRASS_BLOCK), () -> "grazing changed grass");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarLethalChargeRetainsRecovery(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper);
        player.setHealth(1.0F);
        place(player, helper, new Vec3(4.5D, 1.0D, 4.5D));
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        helper.assertTrue(boar.startAction(WildBoarEntity.Action.ATTACK_CHARGE), "charge did not start");
        helper.runAtTickTime(18L, () -> {
            helper.assertTrue(!player.isAlive(), "charge was not lethal");
            helper.assertTrue(boar.getTarget() == null, "dead target retained");
            helper.assertValueEqual(boar.action(), WildBoarEntity.Action.ATTACK_CHARGE,
                    "lethal hit truncated recovery animation");
        });
        helper.runAtTickTime(25L, () -> {
            helper.assertTrue(!boar.action().isAttack(), "charge did not finish");
            helper.succeed();
        });
    }

    private static WildBoarEntity spawn(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(ModEntityTypes.WILD_BOAR.get(), pos);
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

    private static int itemCount(List<ItemEntity> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static int experience(GameTestHelper helper) {
        return helper.getEntities(EntityType.EXPERIENCE_ORB).stream()
                .mapToInt(ExperienceOrb::getValue).sum();
    }

    private static ServerPlayer survivalMock(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "gzr-wild-boar-gametest"), false);
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
        // ServerPlayer grants a fresh login 60 ticks of invulnerability; these fixtures have no
        // network connection ticking them while waiting for the attack under test.
        for (int tick = 0; tick < 61; tick++) player.tick();
        return player;
    }
}

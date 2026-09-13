package com.unknown.guzhenren.gametest;

import com.mojang.authlib.GameProfile;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.BoarGuEntity;
import com.unknown.guzhenren.entity.HopeGuEntity;
import com.unknown.guzhenren.entity.RestingFlyingGuEntity;
import com.unknown.guzhenren.entity.RhinocerosBeetleGuEntity;
import com.unknown.guzhenren.entity.ai.FleePlayerGoal;
import com.unknown.guzhenren.entity.ai.LandRestGoal;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import com.unknown.guzhenren.registry.item.ModItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime coverage for the four Rhinoceros Beetle Gu [甲虫蛊] entities, their shared flight
 * lifecycle, and the fall safety every wild flying Gu family inherits.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeetleGameTests {

    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private BeetleGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void beetleTypesMapToFourCatchItems(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        for (Variant variant : variants()) {
            RhinocerosBeetleGuEntity beetle = spawn(helper, variant.entityType(), CENTER);
            helper.assertTrue(beetle.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                    variant.entityType() + " interaction was not consumed");
            helper.assertValueEqual(player.getInventory().countItem(variant.caughtItem()), 1,
                    variant.caughtItem() + " caught item missing");
            helper.assertTrue(!beetle.isAlive(), "caught beetle was not removed");
        }
        helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "capture spawned an item entity");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void fullInventoryStillProducesCaughtItem(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        Variant variant = variants().get(0);
        RhinocerosBeetleGuEntity beetle = spawn(helper, variant.entityType(), CENTER);
        helper.assertTrue(beetle.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "full inventory capture was not consumed");
        helper.succeedWhen(() -> {
            helper.assertTrue(!beetle.isAlive(), "full inventory capture kept the beetle alive");
            helper.assertTrue(helper.getEntities(EntityType.ITEM).stream()
                    .anyMatch(item -> item.getItem().is(variant.caughtItem())),
                    "full inventory capture lost its item");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void beetleDeathProducesNoDrops(GameTestHelper helper) {
        for (Variant variant : variants()) {
            RhinocerosBeetleGuEntity beetle = spawn(helper, variant.entityType(), CENTER);
            beetle.hurt(helper.getLevel().damageSources().generic(), 2.0F);
            helper.assertTrue(!beetle.isAlive(), "beetle death did not remove the entity");
        }
        helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "beetle death produced an item drop");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void beetleRegistrationUsesSharedFlightShapeAndSpeed(GameTestHelper helper) {
        RhinocerosBeetleGuEntity beetle = spawn(helper, variants().get(0).entityType(), CENTER);
        helper.assertValueEqual(beetle.getMaxHealth(), 1.0F, "beetle max health changed");
        helper.assertValueEqual(beetle.getBbWidth(), 0.4F, "beetle width changed");
        helper.assertValueEqual(beetle.getBbHeight(), 0.4F, "beetle height changed");
        helper.assertValueEqual(beetle.getAttributeValue(Attributes.FLYING_SPEED), 0.3D,
                "beetle flying speed changed");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void beetleFleeGoalUsesSixBlockThreatAndTenBlockRelease(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        RhinocerosBeetleGuEntity beetle = spawn(helper, variants().get(0).entityType(), CENTER);
        movePlayerRelativeTo(player, beetle, 5.0D);
        FleePlayerGoal goal = new FleePlayerGoal(beetle);
        helper.assertTrue(goal.canUse(), "a threat inside six blocks did not start the flee goal");
        goal.start();

        movePlayerRelativeTo(player, beetle, 9.9D);
        helper.assertTrue(goal.canContinueToUse(), "flee goal stopped before the threat escaped ten blocks");
        movePlayerRelativeTo(player, beetle, 10.1D);
        helper.assertTrue(!goal.canContinueToUse(), "flee goal continued after the threat escaped ten blocks");
        goal.stop();

        movePlayerRelativeTo(player, beetle, 6.1D);
        helper.assertTrue(!new FleePlayerGoal(beetle).canUse(),
                "a threat outside six blocks started the flee goal");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void restingBeetleTakesOffWhenThreatened(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        RhinocerosBeetleGuEntity beetle = spawn(helper, variants().get(0).entityType(), CENTER);
        movePlayerRelativeTo(player, beetle, 3.0D);
        beetle.beginResting();
        helper.succeedWhen(() -> helper.assertValueEqual(beetle.phase(), RestingFlyingGuEntity.FlightPhase.FLYING,
                "resting beetle did not take off for a nearby threat"));
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 100)
    public static void interruptedRestWithoutEscapeResumesFlight(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        List<RestingFlyingGuEntity> entities = List.of(
                helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER),
                spawn(helper, variants().get(0).entityType(), CENTER));
        try {
            for (RestingFlyingGuEntity gu : entities) {
                // Drive the real goal lifecycle directly so random escape candidates and neighboring
                // GameTest players cannot turn the failed-escape case into a successful takeoff.
                gu.setNoAi(true);
                gu.setOnGround(true);
                LandRestGoal rest = new LandRestGoal(gu);
                rest.start();
                rest.tick();
                helper.assertValueEqual(gu.phase(), RestingFlyingGuEntity.FlightPhase.RESTING,
                        "normal landing did not enter resting");
                helper.assertTrue(!gu.wantsToLand(), "normal rest retained a landing request");
                player.moveTo(gu.position().add(3.0D, 0.0D, 0.0D));
                helper.assertTrue(!rest.canContinueToUse(), "nearby threat did not interrupt rest");
                rest.stop();
                // No flee goal starts: its path lookup may fail. Once the threat leaves, the
                // ordinary flight goal must be eligible without a reload or another attacker.
                player.moveTo(gu.position().add(20.0D, 0.0D, 0.0D));
                helper.assertValueEqual(gu.phase(), RestingFlyingGuEntity.FlightPhase.FLYING,
                        "interrupted rest stayed stuck when escape did not start");
                helper.assertTrue(!gu.wantsToLand(), "interrupted rest retained a landing request");
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 260)
    public static void savedBeetleResumesFlightLifecycleAfterReload(GameTestHelper helper) {
        Variant variant = variants().get(0);
        helper.setBlock(CENTER.below(), Blocks.STONE);
        RhinocerosBeetleGuEntity original = spawn(helper, variant.entityType(), CENTER);
        original.beginResting();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();
        helper.assertTrue(!original.isAlive(), "saved source beetle was not removed");

        RhinocerosBeetleGuEntity restored = spawn(helper, variant.entityType(), new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.succeedWhen(() -> {
            helper.assertTrue(restored.isAlive(), "reloaded beetle was removed");
            helper.assertValueEqual(restored.phase(), RestingFlyingGuEntity.FlightPhase.FLYING,
                    "reloaded beetle did not resume and leave its saved landing lifecycle");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 300)
    public static void landingFromFlightAltitudeDoesNotKill(GameTestHelper helper) {
        // Guards the FlyingGuEntity fall-check override with a real descent past the three-block
        // safe-fall distance for every wild flying Gu family. NoAi keeps goal timing out of the way
        // so the descent is deterministic; the physics path (move -> checkFallDamage) is exactly the
        // one that used to deal ceil(distance - 3) damage to the one-health motes.
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 0, 6), Blocks.STONE);
        HopeGuEntity hope = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), new BlockPos(2, 7, 2));
        BoarGuEntity boar = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), new BlockPos(4, 7, 4));
        RhinocerosBeetleGuEntity beetle =
                spawn(helper, ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), new BlockPos(6, 7, 6));
        hope.setNoAi(true);
        boar.setNoAi(true);
        beetle.setNoAi(true);
        double startY = hope.getY();
        helper.succeedWhen(() -> {
            // A noAi mob skips travel entirely, so the descent drives Entity#move directly -- the same
            // method travel uses, and the one whose checkFallDamage call used to deal the damage.
            for (Mob gu : List.of(hope, boar, beetle)) {
                if (!gu.onGround()) gu.move(MoverType.SELF, new Vec3(0.0D, -0.3D, 0.0D));
            }
            helper.assertTrue(hope.isAlive(), "hope Gu died landing from flight altitude");
            helper.assertTrue(boar.isAlive(), "boar died landing from flight altitude");
            helper.assertTrue(beetle.isAlive(), "beetle died landing from flight altitude");
            helper.assertTrue(hope.onGround() && boar.onGround() && beetle.onGround(),
                    "descent never finished: hope=" + probe(hope) + " boar=" + probe(boar)
                            + " beetle=" + probe(beetle));
            helper.assertTrue(hope.getY() < startY - 3.0D && boar.getY() < startY - 3.0D
                    && beetle.getY() < startY - 3.0D, "descent did not clear the safe-fall distance");
        });
    }

    private static String probe(Mob gu) {
        return "pos=" + gu.blockPosition() + " vel=" + gu.getDeltaMovement() + " noAi=" + gu.isNoAi()
                + " ticks=" + gu.tickCount;
    }

    private static List<Variant> variants() {
        return List.of(
                new Variant(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), ModItems.HORIZONTAL_CRASH_GU.get()),
                new Variant(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(), ModItems.VERTICAL_CRASH_GU.get()),
                new Variant(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(), ModItems.CHARGING_CRASH_GU_4.get()),
                new Variant(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(), ModItems.CHARGING_CRASH_GU_5.get()));
    }

    private static RhinocerosBeetleGuEntity spawn(GameTestHelper helper,
                                                   EntityType<RhinocerosBeetleGuEntity> type, BlockPos pos) {
        return helper.spawn(type, pos);
    }

    private static void movePlayerRelativeTo(ServerPlayer player, RhinocerosBeetleGuEntity beetle,
                                              double distance) {
        player.moveTo(beetle.getX() + distance, beetle.getY(), beetle.getZ(), 0.0F, 0.0F);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private record Variant(EntityType<RhinocerosBeetleGuEntity> entityType, Item caughtItem) {}

    private static ServerPlayer survivalMock(GameTestHelper helper, boolean connect) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "gzr-beetle-gametest"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {return false;}
            @Override
            public boolean isCreative() {return false;}
        };
        if (!connect) return player;
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(@NotNull Packet<?> packet) {}
            @Override
            public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener listener) {}
        };
        helper.getLevel().addNewPlayer(player);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.5D, 4.5D)));
        player.setNoGravity(true);
        player.getAbilities().instabuild = false;
        return player;
    }
}
